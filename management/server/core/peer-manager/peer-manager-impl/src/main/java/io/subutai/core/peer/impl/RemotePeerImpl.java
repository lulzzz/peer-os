package io.subutai.core.peer.impl;


import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import io.subutai.common.command.CommandCallback;
import io.subutai.common.command.CommandException;
import io.subutai.common.command.CommandRequest;
import io.subutai.common.command.CommandResult;
import io.subutai.common.command.CommandResultImpl;
import io.subutai.common.command.CommandStatus;
import io.subutai.common.command.RequestBuilder;
import io.subutai.common.environment.Containers;
import io.subutai.common.environment.CreateEnvironmentContainerGroupRequest;
import io.subutai.common.environment.CreateEnvironmentContainerResponseCollector;
import io.subutai.common.environment.PrepareTemplatesRequest;
import io.subutai.common.environment.PrepareTemplatesResponseCollector;
import io.subutai.common.exception.HTTPException;
import io.subutai.common.host.ContainerHostInfoModel;
import io.subutai.common.host.ContainerHostState;
import io.subutai.common.host.HostId;
import io.subutai.common.host.HostInfo;
import io.subutai.common.host.HostInterfaces;
import io.subutai.common.metric.ProcessResourceUsage;
import io.subutai.common.metric.ResourceHostMetrics;
import io.subutai.common.network.Gateways;
import io.subutai.common.network.Vni;
import io.subutai.common.network.Vnis;
import io.subutai.common.peer.AlertEvent;
import io.subutai.common.peer.ContainerGateway;
import io.subutai.common.peer.ContainerHost;
import io.subutai.common.peer.ContainerId;
import io.subutai.common.peer.EnvironmentContainerHost;
import io.subutai.common.peer.EnvironmentId;
import io.subutai.common.peer.Host;
import io.subutai.common.peer.LocalPeer;
import io.subutai.common.peer.MessageRequest;
import io.subutai.common.peer.MessageResponse;
import io.subutai.common.peer.Payload;
import io.subutai.common.peer.PeerException;
import io.subutai.common.peer.PeerInfo;
import io.subutai.common.peer.RecipientType;
import io.subutai.common.peer.RemotePeer;
import io.subutai.common.peer.Timeouts;
import io.subutai.common.protocol.P2PConfig;
import io.subutai.common.protocol.P2PCredentials;
import io.subutai.common.protocol.PingDistances;
import io.subutai.common.protocol.TemplateKurjun;
import io.subutai.common.quota.ContainerQuota;
import io.subutai.common.resource.HistoricalMetrics;
import io.subutai.common.resource.PeerResources;
import io.subutai.common.security.PublicKeyContainer;
import io.subutai.common.security.objects.PermissionObject;
import io.subutai.common.settings.SecuritySettings;
import io.subutai.common.settings.SystemSettings;
import io.subutai.common.util.CollectionUtil;
import io.subutai.common.util.JsonUtil;
import io.subutai.common.util.RestUtil;
import io.subutai.core.messenger.api.Message;
import io.subutai.core.messenger.api.MessageException;
import io.subutai.core.messenger.api.Messenger;
import io.subutai.core.object.relation.api.RelationInfoManager;
import io.subutai.core.object.relation.api.RelationManager;
import io.subutai.core.object.relation.api.RelationVerificationException;
import io.subutai.core.object.relation.api.model.RelationInfoMeta;
import io.subutai.core.peer.impl.command.BlockingCommandCallback;
import io.subutai.core.peer.impl.command.CommandResponseListener;
import io.subutai.core.peer.impl.request.MessageResponseListener;
import io.subutai.core.security.api.SecurityManager;


/**
 * Remote Peer implementation
 *
 * TODO use environment web client for environment specific operations!
 */
@PermitAll
public class RemotePeerImpl implements RemotePeer
{
    private static final Logger LOG = LoggerFactory.getLogger( RemotePeerImpl.class );

    private SecurityManager securityManager;
    protected final PeerInfo peerInfo;
    protected final Messenger messenger;
    private final CommandResponseListener commandResponseListener;
    private final MessageResponseListener messageResponseListener;
    protected RestUtil restUtil = new RestUtil();
    protected JsonUtil jsonUtil = new JsonUtil();
    private String baseUrl;
    Object provider;
    private String localPeerId;
    private RelationManager relationManager;
    private LocalPeer localPeer;
    private PeerWebClient peerWebClient;
    private EnvironmentWebClient environmentWebClient;


    public RemotePeerImpl( String localPeerId, SecurityManager securityManager, final PeerInfo peerInfo,
                           final Messenger messenger, CommandResponseListener commandResponseListener,
                           MessageResponseListener messageResponseListener, Object provider,
                           final PeerManagerImpl peerManager )
    {
        this.localPeerId = localPeerId;
        this.securityManager = securityManager;
        this.peerInfo = peerInfo;
        this.messenger = messenger;
        this.commandResponseListener = commandResponseListener;
        this.messageResponseListener = messageResponseListener;
        this.relationManager = peerManager.getRelationManager();
        this.localPeer = peerManager.getLocalPeer();

        String url;

        int port = peerInfo.getPort();

        if ( port == SystemSettings.getSpecialPortX1() || port == SystemSettings.getOpenPort() )
        {
            url = String.format( "http://%s:%s/rest/v1/peer", peerInfo, peerInfo.getPort() );
        }
        else
        {
            url = String.format( "https://%s:%s/rest/v1/peer", peerInfo, peerInfo.getPort() );
        }

        this.baseUrl = url;
        this.provider = provider;

        this.peerWebClient = new PeerWebClient( provider, peerInfo, this );
        this.environmentWebClient = new EnvironmentWebClient( provider, this );
    }


    public void checkRelation() throws RelationVerificationException
    {
        RelationInfoManager relationInfoManager = relationManager.getRelationInfoManager();

        RelationInfoMeta relationInfoMeta = new RelationInfoMeta();
        relationInfoMeta.getRelationTraits().put("receiveHeartbeats", "allow");
        relationInfoMeta.getRelationTraits().put("sendHeartbeats", "allow");
        relationInfoMeta.getRelationTraits().put("hostTemplates", "allow");

        relationInfoManager.checkRelationValidity( localPeer, this, relationInfoMeta, null );
    }


    protected String request( RestUtil.RequestType requestType, String path, String alias, Map<String, String> params,
                              Map<String, String> headers ) throws HTTPException
    {
        try
        {
            checkRelation();
        }
        catch ( RelationVerificationException e )
        {
            throw new HTTPException( e.getMessage() );
        }
        return restUtil.request( requestType,
                String.format( "%s/%s", baseUrl, path.startsWith( "/" ) ? path.substring( 1 ) : path ), alias, params,
                headers, provider );
    }


    protected String get( String path, String alias, Map<String, String> params, Map<String, String> headers )
            throws HTTPException
    {
        return request( RestUtil.RequestType.GET, path, alias, params, headers );
    }


    protected String post( String path, String alias, Map<String, String> params, Map<String, String> headers )
            throws HTTPException
    {

        return request( RestUtil.RequestType.POST, path, alias, params, headers );
    }


    protected String delete( String path, String alias, Map<String, String> params, Map<String, String> headers )
            throws HTTPException
    {

        return request( RestUtil.RequestType.DELETE, path, alias, params, headers );
    }


    @Override
    public String getId()
    {
        return peerInfo.getId();
    }


    @Override
    public PeerInfo check() throws PeerException
    {
        PeerInfo response = peerWebClient.getInfo();
        if ( !peerInfo.getId().equals( response.getId() ) )
        {
            throw new PeerException( String.format(
                    "Remote peer check failed. Id of the remote peer %s changed. Please verify the remote peer.",
                    peerInfo.getId() ) );
        }

        return response;
    }


    @Override
    public boolean isOnline()
    {
        try
        {
            check();
            return true;
        }
        catch ( PeerException e )
        {
            LOG.error( e.getMessage(), e );
            return false;
        }
    }


    @Override
    public boolean isLocal()
    {
        return false;
    }


    @Override
    public String getName()
    {
        return peerInfo.getName();
    }


    @Override
    public String getOwnerId()
    {
        return peerInfo.getOwnerId();
    }


    @Override
    public PeerInfo getPeerInfo()
    {
        return peerInfo;
    }


    @Override
    public TemplateKurjun getTemplate( final String templateName ) throws PeerException
    {
        Preconditions.checkArgument( !Strings.isNullOrEmpty( templateName ), "Invalid template name" );

        String path = "/template/get";

        Map<String, String> params = Maps.newHashMap();

        params.put( "templateName", templateName );


        //*********construct Secure Header ****************************
        Map<String, String> headers = Maps.newHashMap();
        //*************************************************************


        try
        {
            String response = get( path, SecuritySettings.KEYSTORE_PX2_ROOT_ALIAS, params, headers );

            return jsonUtil.from( response, TemplateKurjun.class );
        }
        catch ( Exception e )
        {
            throw new PeerException( "Error obtaining template", e );
        }
    }


    //********** ENVIRONMENT SPECIFIC REST *************************************


    @RolesAllowed( "Environment-Management|Update" )
    @Override
    public void startContainer( final ContainerId containerId ) throws PeerException
    {
        Preconditions.checkNotNull( containerId, "Container id is null" );
        Preconditions.checkArgument( containerId.getPeerId().getId().equals( peerInfo.getId() ) );

        environmentWebClient.startContainer( peerInfo, containerId );
    }


    @RolesAllowed( "Environment-Management|Update" )
    @Override
    public void stopContainer( final ContainerId containerId ) throws PeerException
    {
        Preconditions.checkNotNull( containerId, "Container id is null" );
        Preconditions.checkArgument( containerId.getPeerId().getId().equals( peerInfo.getId() ) );

        environmentWebClient.stopContainer( peerInfo, containerId );
    }


    @RolesAllowed( "Environment-Management|Delete" )
    @Override
    public void destroyContainer( final ContainerId containerId ) throws PeerException
    {
        environmentWebClient.destroyContainer( peerInfo, containerId );
    }


    @Override
    public void setDefaultGateway( final ContainerGateway containerGateway ) throws PeerException
    {
        Preconditions.checkNotNull( containerGateway, "Container host is null" );

        String path = "peer/container/gateway";

        Map<String, String> params = Maps.newHashMap();
        params.put( "containerId", containerGateway.getContainerId().getId() );
        params.put( "gatewayIp", containerGateway.getGateway() );

        //*********construct Secure Header ****************************
        Map<String, String> headers = Maps.newHashMap();
        //*************************************************************

        try
        {
            String alias = SecuritySettings.KEYSTORE_PX2_ROOT_ALIAS;
            post( path, alias, params, headers );
        }
        catch ( Exception e )
        {
            throw new PeerException( "Error setting container gateway ip", e );
        }
    }


    @Override
    public void removePeerEnvironmentKeyPair( final EnvironmentId environmentId ) throws PeerException
    {
        peerWebClient.removePeerEnvironmentKeyPair( environmentId );
    }


    @RolesAllowed( "Environment-Management|Delete" )
    @Override
    public boolean isConnected( final HostId hostId )
    {
        Preconditions.checkNotNull( hostId, "Host id is null" );

        try
        {
            return hostId instanceof ContainerId && ContainerHostState.RUNNING
                    .equals( getContainerState( ( ContainerId ) hostId ) );
        }
        catch ( PeerException e )
        {
            LOG.error( "Error getting container state #isConnected", e );
            return false;
        }
    }


    @PermitAll
    @Override
    public ProcessResourceUsage getProcessResourceUsage( final ContainerId containerId, int pid ) throws PeerException
    {
        Preconditions.checkNotNull( containerId, "Container id is null" );
        Preconditions.checkArgument( pid > 0, "Process pid must be greater than 0" );

        if ( containerId.getEnvironmentId() == null )
        {
            return peerWebClient.getProcessResourceUsage( containerId, pid );
        }
        else
        {
            return environmentWebClient.getProcessResourceUsage( peerInfo, containerId, pid );
        }
    }


    @Override
    public ContainerHostState getContainerState( final ContainerId containerId ) throws PeerException
    {
        Preconditions.checkNotNull( containerId, "Container id is null" );
        Preconditions.checkArgument( containerId.getPeerId().getId().equals( peerInfo.getId() ) );

        return environmentWebClient.getState( peerInfo, containerId );
    }


    @Override
    public Containers getEnvironmentContainers( final EnvironmentId environmentId ) throws PeerException
    {
        Preconditions.checkNotNull( environmentId, "Environment id is null" );

        return peerWebClient.getEnvironmentContainers( environmentId );
    }


    @Override
    public Set<Integer> getCpuSet( final ContainerHost containerHost ) throws PeerException
    {
        Preconditions.checkNotNull( containerHost, "Container host is null" );
        Preconditions.checkArgument( containerHost instanceof EnvironmentContainerHost );

        return environmentWebClient.getCpuSet( peerInfo, containerHost.getContainerId() );
    }


    @RolesAllowed( "Environment-Management|Update" )
    @Override
    public void setCpuSet( final ContainerHost containerHost, final Set<Integer> cpuSet ) throws PeerException
    {
        Preconditions.checkNotNull( containerHost, "Container host is null" );
        Preconditions.checkArgument( containerHost instanceof EnvironmentContainerHost );
        Preconditions.checkArgument( !CollectionUtil.isCollectionEmpty( cpuSet ), "Empty cpu set" );

        environmentWebClient.setCpuSet( peerInfo, containerHost.getContainerId(), cpuSet );
    }


    @Override
    public ContainerQuota getQuota( final ContainerId containerId ) throws PeerException
    {
        Preconditions.checkNotNull( containerId, "Container id is null" );

        return environmentWebClient.getQuota( peerInfo, containerId );
    }


    @Override
    public void setQuota( final ContainerId containerId, final ContainerQuota containerQuota ) throws PeerException
    {
        Preconditions.checkNotNull( containerId, "Container id is null" );
        Preconditions.checkNotNull( containerQuota, "Container quota is null" );

        environmentWebClient.setQuota( peerInfo, containerId, containerQuota );
    }


    @Override
    public ContainerQuota getAvailableQuota( final ContainerId containerId ) throws PeerException
    {
        Preconditions.checkNotNull( containerId, "Container id is null" );

        return environmentWebClient.getAvailableQuota( peerInfo, containerId );
    }


    @PermitAll
    @Override
    public HostInfo getContainerHostInfoById( final String containerHostId ) throws PeerException
    {
        String path = String.format( "/container/info" );
        try
        {
            //*********construct Secure Header ****************************
            Map<String, String> headers = Maps.newHashMap();
            //*************************************************************

            Map<String, String> params = Maps.newHashMap();
            params.put( "containerId", jsonUtil.to( containerHostId ) );
            String response = get( path, SecuritySettings.KEYSTORE_PX2_ROOT_ALIAS, params, headers );
            return jsonUtil.from( response, ContainerHostInfoModel.class );
        }
        catch ( Exception e )
        {
            throw new PeerException( String.format( "Error getting hostInfo from peer %s", getName() ), e );
        }
    }


    @Override
    public CommandResult execute( final RequestBuilder requestBuilder, final Host host ) throws CommandException
    {
        return execute( requestBuilder, host, null );
    }


    @Override
    public CommandResult execute( final RequestBuilder requestBuilder, final Host host, final CommandCallback callback )
            throws CommandException
    {
        Preconditions.checkNotNull( requestBuilder, "Invalid request" );
        Preconditions.checkNotNull( host, "Invalid host" );

        BlockingCommandCallback blockingCommandCallback = getBlockingCommandCallback( callback );

        executeAsync( requestBuilder, host, blockingCommandCallback, blockingCommandCallback.getCompletionSemaphore() );

        CommandResult commandResult = blockingCommandCallback.getCommandResult();

        if ( commandResult == null )
        {
            commandResult = new CommandResultImpl( null, null, null, CommandStatus.TIMEOUT );
        }

        return commandResult;
    }


    protected BlockingCommandCallback getBlockingCommandCallback( CommandCallback callback )
    {
        return new BlockingCommandCallback( callback );
    }


    @Override
    public void executeAsync( final RequestBuilder requestBuilder, final Host host, final CommandCallback callback )
            throws CommandException
    {
        executeAsync( requestBuilder, host, callback, null );
    }


    @Override
    public void executeAsync( final RequestBuilder requestBuilder, final Host host ) throws CommandException
    {
        executeAsync( requestBuilder, host, null );
    }


    protected void executeAsync( final RequestBuilder requestBuilder, final Host host, final CommandCallback callback,
                                 Semaphore semaphore ) throws CommandException
    {
        Preconditions.checkNotNull( requestBuilder, "Invalid request" );
        Preconditions.checkNotNull( host, "Invalid host" );

        if ( !host.isConnected() )
        {
            throw new CommandException( "Host disconnected." );
        }

        if ( !( host instanceof ContainerHost ) )
        {
            throw new CommandException( "Operation not allowed" );
        }

        EnvironmentId environmentId = ( ( EnvironmentContainerHost ) host ).getEnvironmentId();
        CommandRequest request = new CommandRequest( requestBuilder, host.getId(), environmentId.getId() );
        //cache callback
        commandResponseListener.addCallback( request.getRequestId(), callback, requestBuilder.getTimeout(), semaphore );

        //send command request to remote peer counterpart
        try
        {
            //*********construct Secure Header ****************************
            Map<String, String> headers = Maps.newHashMap();
            //************************************************************************


            sendRequest( request, RecipientType.COMMAND_REQUEST.name(), Timeouts.COMMAND_REQUEST_MESSAGE_TIMEOUT,
                    headers );
        }
        catch ( PeerException e )
        {
            throw new CommandException( e );
        }
    }


    @Override
    public <T, V> V sendRequest( final T request, final String recipient, final int requestTimeout,
                                 Class<V> responseType, int responseTimeout, final Map<String, String> headers )
            throws PeerException
    {
        Preconditions.checkArgument( responseTimeout > 0, "Invalid response timeout" );
        Preconditions.checkNotNull( responseType, "Invalid response type" );

        //send request
        MessageRequest messageRequest = sendRequestInternal( request, recipient, requestTimeout, headers );

        //wait for response here
        MessageResponse messageResponse =
                messageResponseListener.waitResponse( messageRequest, requestTimeout, responseTimeout );

        LOG.debug( String.format( "%s", messageResponse ) );
        if ( messageResponse != null )
        {
            if ( messageResponse.getException() != null )
            {
                throw new PeerException( messageResponse.getException() );
            }
            else if ( messageResponse.getPayload() != null )
            {
                LOG.debug( String.format( "Trying get response object: %s", responseType ) );
                final V message = messageResponse.getPayload().getMessage( responseType );
                LOG.debug( String.format( "Response object: %s", message ) );
                return message;
            }
        }

        return null;
    }


    @Override
    public <T> void sendRequest( final T request, final String recipient, final int requestTimeout,
                                 final Map<String, String> headers ) throws PeerException
    {

        sendRequestInternal( request, recipient, requestTimeout, headers );
    }


    protected <T> MessageRequest sendRequestInternal( final T request, final String recipient, final int requestTimeout,
                                                      final Map<String, String> headers ) throws PeerException
    {
        Preconditions.checkNotNull( request, "Invalid request" );
        Preconditions.checkArgument( !Strings.isNullOrEmpty( recipient ), "Invalid recipient" );
        Preconditions.checkArgument( requestTimeout > 0, "Invalid request timeout" );

        MessageRequest messageRequest = new MessageRequest( new Payload( request, localPeerId ), recipient, headers );
        Message message = messenger.createMessage( messageRequest );

        messageRequest.setMessageId( message.getId() );

        try
        {
            messenger.sendMessage( this, message, RecipientType.PEER_REQUEST_LISTENER.name(), requestTimeout, headers );
        }
        catch ( MessageException e )
        {
            throw new PeerException( e );
        }

        return messageRequest;
    }


    @RolesAllowed( "Environment-Management|Write" )
    @Override
    public CreateEnvironmentContainerResponseCollector createEnvironmentContainerGroup(
            final CreateEnvironmentContainerGroupRequest request ) throws PeerException
    {
        Preconditions.checkNotNull( request, "Invalid request" );


        //*********construct Secure Header ****************************
        Map<String, String> headers = Maps.newHashMap();
        //************************************************************************

        CreateEnvironmentContainerResponseCollector response =
                sendRequest( request, RecipientType.CREATE_ENVIRONMENT_CONTAINER_GROUP_REQUEST.name(),
                        Timeouts.CREATE_CONTAINER_REQUEST_TIMEOUT, CreateEnvironmentContainerResponseCollector.class,
                        Timeouts.CREATE_CONTAINER_RESPONSE_TIMEOUT, headers );

        if ( response != null )
        {
            return response;
        }
        else
        {
            throw new PeerException( "Command timed out" );
        }
    }


    @RolesAllowed( "Environment-Management|Write" )
    @Override
    public PrepareTemplatesResponseCollector prepareTemplates( final PrepareTemplatesRequest request )
            throws PeerException
    {
        Preconditions.checkNotNull( request, "Invalid request" );


        //*********construct Secure Header ****************************
        Map<String, String> headers = Maps.newHashMap();
        //************************************************************************

        PrepareTemplatesResponseCollector response =
                sendRequest( request, RecipientType.PREPARE_TEMPLATE_REQUEST.name(),
                        Timeouts.CREATE_CONTAINER_REQUEST_TIMEOUT, PrepareTemplatesResponseCollector.class,
                        Timeouts.CREATE_CONTAINER_RESPONSE_TIMEOUT, headers );

        if ( response != null )
        {
            return response;
        }
        else
        {
            throw new PeerException( "Command timed out" );
        }
    }


    //networking
    @RolesAllowed( "Environment-Management|Write" )
    @Override
    public int setupTunnels( final Map<String, String> peerIps, final String environmentId ) throws PeerException
    {

        Preconditions.checkNotNull( peerIps, "Invalid peer ips set" );
        Preconditions.checkArgument( !peerIps.isEmpty(), "Invalid peer ips set" );
        Preconditions.checkNotNull( environmentId, "Invalid environment id" );

        try
        {
            return peerWebClient.setupTunnels( peerIps, environmentId );
        }
        catch ( Exception e )
        {
            throw new PeerException( String.format( "Error setting up tunnels on peer %s", getName() ), e );
        }
    }


    @RolesAllowed( "Environment-Management|Write" )
    @Override
    public Vni reserveVni( final Vni vni ) throws PeerException
    {
        Preconditions.checkNotNull( vni, "Invalid vni" );

        return peerWebClient.reserveVni( vni );
    }

    //************ END ENVIRONMENT SPECIFIC REST


    @RolesAllowed( "Environment-Management|Read" )
    @Override
    public Gateways getGateways() throws PeerException
    {
        try
        {
            return peerWebClient.getGateways();
        }
        catch ( Exception e )
        {
            throw new PeerException( String.format( "Error obtaining gateways from peer %s", getName() ), e );
        }
    }


    @Override
    public Vnis getReservedVnis() throws PeerException
    {
        return peerWebClient.getReservedVnis();
    }


    @Override
    public PublicKeyContainer createPeerEnvironmentKeyPair( EnvironmentId environmentId ) throws PeerException
    {
        Preconditions.checkNotNull( environmentId, "Invalid environmentId" );

        return peerWebClient.createEnvironmentKeyPair( environmentId );
    }


    @Override
    public void updatePeerEnvironmentPubKey( final EnvironmentId environmentId, final PGPPublicKeyRing publicKeyRing )
            throws PeerException
    {
        Preconditions.checkNotNull( environmentId, "Invalid environmentId" );
        Preconditions.checkNotNull( publicKeyRing, "Public key ring is null" );


        try
        {
            String exportedPubKeyRing =
                    securityManager.getEncryptionTool().armorByteArrayToString( publicKeyRing.getEncoded() );
            final PublicKeyContainer publicKeyContainer =
                    new PublicKeyContainer( environmentId.getId(), publicKeyRing.getPublicKey().getFingerprint(),
                            exportedPubKeyRing );
            peerWebClient.updateEnvironmentPubKey( publicKeyContainer );
        }
        catch ( IOException | PGPException e )
        {
            throw new PeerException( e.getMessage() );
        }
    }


    @Override
    public void addPeerEnvironmentPubKey( final String keyId, final PGPPublicKeyRing pek ) throws PeerException
    {
        Preconditions.checkNotNull( keyId, "Invalid key ID" );
        Preconditions.checkNotNull( pek, "Public key ring is null" );


        try
        {
            String exportedPubKeyRing = securityManager.getEncryptionTool().armorByteArrayToString( pek.getEncoded() );
            peerWebClient.addPeerEnvironmentPubKey( keyId, exportedPubKeyRing );
        }
        catch ( IOException | PGPException e )
        {
            throw new PeerException( e.getMessage() );
        }
    }


    @Override
    public HostInterfaces getInterfaces() throws PeerException
    {
        return peerWebClient.getInterfaces();
    }


    @Override
    public void resetP2PSecretKey( final P2PCredentials p2PCredentials ) throws PeerException
    {
        Preconditions.checkNotNull( p2PCredentials, "Invalid p2p credentials" );

        peerWebClient.resetP2PSecretKey( p2PCredentials.getP2pHash(), p2PCredentials.getP2pSecretKey(),
                        p2PCredentials.getP2pTtlSeconds() );
    }


    @Override
    public String getP2PIP( final String resourceHostId, final String swarmHash ) throws PeerException
    {
        Preconditions.checkArgument( !Strings.isNullOrEmpty( resourceHostId ), "Invalid resource host id" );
        Preconditions.checkArgument( !Strings.isNullOrEmpty( swarmHash ), "Invalid p2p swarm hash" );

        return peerWebClient.getP2PIP( resourceHostId, swarmHash );
    }


    @Override
    public String setupP2PConnection( final P2PConfig config ) throws PeerException
    {
        Preconditions.checkNotNull( config, "Invalid p2p config" );

        return peerWebClient.setupP2PConnection( config );
    }


    @Override
    public void setupInitialP2PConnection( final P2PConfig config ) throws PeerException
    {
        Preconditions.checkNotNull( config, "Invalid p2p config" );

        peerWebClient.setupInitialP2PConnection( config );
    }


    @Override
    public void removeP2PConnection( final String p2pHash ) throws PeerException
    {
        Preconditions.checkArgument( !Strings.isNullOrEmpty( p2pHash ), "Invalid p2p hash" );

        peerWebClient.removeP2PConnection( p2pHash );
    }


    @Override
    public void cleanupEnvironment( final EnvironmentId environmentId ) throws PeerException
    {
        Preconditions.checkNotNull( environmentId, "Invalid environment ID" );
        peerWebClient.cleanupEnvironment( environmentId );
    }


    @Override
    public HostId getResourceHostIdByContainerId( final ContainerId containerId ) throws PeerException
    {
        Preconditions.checkNotNull( containerId, "Container id is null" );
        Preconditions.checkArgument( containerId.getPeerId().getId().equals( peerInfo.getId() ) );

        if ( containerId.getEnvironmentId() == null )
        {
            return peerWebClient.getResourceHosIdByContainerId( containerId );
        }
        else
        {
            return environmentWebClient.getResourceHostIdByContainerId( peerInfo, containerId );
        }
    }


    @Override
    public ResourceHostMetrics getResourceHostMetrics() throws PeerException
    {
        return peerWebClient.getResourceHostMetrics();
    }


    @Override
    public void alert( final AlertEvent alert ) throws PeerException
    {
        peerWebClient.alert( alert );
    }


    @Override
    public HistoricalMetrics getHistoricalMetrics( final String hostname, final Date startTime, final Date endTime )
            throws PeerException
    {
        return peerWebClient.getHistoricalMetrics( hostname, startTime, endTime );
    }


    @Override
    public PeerResources getResourceLimits( final String peerId ) throws PeerException
    {
        return peerWebClient.getResourceLimits( peerId );
    }


    @Override
    public PingDistances getP2PSwarmDistances( final String p2pHash, final Integer maxAddress ) throws PeerException
    {
        return peerWebClient.getP2PSwarmDistances( p2pHash, maxAddress );
    }


    @Override
    public boolean equals( final Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof RemotePeerImpl ) )
        {
            return false;
        }

        final RemotePeerImpl that = ( RemotePeerImpl ) o;

        return getId().equals( that.getId() );
    }


    @Override
    public int hashCode()
    {
        return getId().hashCode();
    }


    @Override
    public String getLinkId()
    {
        return String.format( "%s|%s", getClassPath(), getUniqueIdentifier() );
    }


    @Override
    public String getUniqueIdentifier()
    {
        return getId();
    }


    @Override
    public String getClassPath()
    {
        return this.getClass().getSimpleName();
    }


    @Override
    public String getContext()
    {
        return PermissionObject.PeerManagement.getName();
    }


    @Override
    public String getKeyId()
    {
        return peerInfo.getId();
    }
}

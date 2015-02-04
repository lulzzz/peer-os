package org.safehaus.subutai.core.env.impl.builder;


import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.safehaus.subutai.common.environment.NodeGroup;
import org.safehaus.subutai.common.peer.HostInfoModel;
import org.safehaus.subutai.common.peer.Peer;
import org.safehaus.subutai.common.protocol.Template;
import org.safehaus.subutai.common.util.CollectionUtil;
import org.safehaus.subutai.core.env.impl.entity.EnvironmentContainerImpl;
import org.safehaus.subutai.core.env.impl.entity.EnvironmentImpl;
import org.safehaus.subutai.core.env.impl.exception.NodeGroupBuildException;
import org.safehaus.subutai.core.peer.api.LocalPeer;
import org.safehaus.subutai.core.peer.api.PeerManager;
import org.safehaus.subutai.core.registry.api.TemplateRegistry;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;


/**
 * Creates node groups on a peer
 */
public class NodeGroupBuilder implements Callable<Set<NodeGroupBuildResult>>
{

    private final EnvironmentImpl environment;
    private final TemplateRegistry templateRegistry;
    private final PeerManager peerManager;
    private final Peer peer;
    private final Set<NodeGroup> nodeGroups;
    private final String defaultDomain;


    public NodeGroupBuilder( final EnvironmentImpl environment, final TemplateRegistry templateRegistry,
                             final PeerManager peerManager, final Peer peer, final Set<NodeGroup> nodeGroups,
                             final String defaultDomain )
    {
        Preconditions.checkNotNull( environment );
        Preconditions.checkNotNull( templateRegistry );
        Preconditions.checkNotNull( peerManager );
        Preconditions.checkNotNull( peer );
        Preconditions.checkArgument( !CollectionUtil.isCollectionEmpty( nodeGroups ) );
        Preconditions.checkArgument( !Strings.isNullOrEmpty( defaultDomain ) );

        this.environment = environment;
        this.templateRegistry = templateRegistry;
        this.peerManager = peerManager;
        this.peer = peer;
        this.nodeGroups = nodeGroups;
        this.defaultDomain = defaultDomain;
    }


    public List<Template> fetchRequiredTemplates( UUID sourcePeerId, final String templateName )
            throws NodeGroupBuildException
    {
        List<Template> requiredTemplates = Lists.newArrayList();
        List<Template> templates = templateRegistry.getParentTemplates( templateName );

        Template installationTemplate = templateRegistry.getTemplate( templateName );
        if ( installationTemplate != null )
        {
            templates.add( installationTemplate );
        }
        else
        {
            throw new NodeGroupBuildException( String.format( "Template %s is not found in registry", templateName ),
                    null );
        }


        for ( Template t : templates )
        {
            requiredTemplates.add( t.getRemoteClone( sourcePeerId ) );
        }

        if ( requiredTemplates.isEmpty() )
        {
            throw new NodeGroupBuildException( "Could not fetch template information", null );
        }

        return requiredTemplates;
    }


    @Override
    public Set<NodeGroupBuildResult> call() throws NodeGroupBuildException
    {

        Set<NodeGroupBuildResult> results = Sets.newHashSet();
        LocalPeer localPeer = peerManager.getLocalPeer();

        for ( NodeGroup nodeGroup : nodeGroups )
        {
            NodeGroupBuildException exception = null;
            Set<EnvironmentContainerImpl> containers = Sets.newHashSet();

            try
            {

                Set<HostInfoModel> newHosts =
                        peer.createContainers( environment.getId(), localPeer.getId(), localPeer.getOwnerId(),
                                fetchRequiredTemplates( peer.getId(), nodeGroup.getTemplateName() ),
                                nodeGroup.getNumberOfContainers(),
                                nodeGroup.getContainerPlacementStrategy().getStrategyId(),
                                nodeGroup.getContainerPlacementStrategy().getCriteriaAsList() );


                for ( HostInfoModel newHost : newHosts )
                {
                    containers.add( new EnvironmentContainerImpl( localPeer.getId(), peer, nodeGroup.getName(), newHost,
                            templateRegistry.getTemplate( nodeGroup.getTemplateName() ), nodeGroup.getSshGroupId(),
                            nodeGroup.getHostsGroupId(), defaultDomain) );
                }


                if ( containers.size() < nodeGroup.getNumberOfContainers() )
                {
                    exception = new NodeGroupBuildException( String.format( "Requested %d but created only %d containers",
                            nodeGroup.getNumberOfContainers(), containers.size() ), null );
                }
            }
            catch ( Exception e )
            {
                exception = new NodeGroupBuildException(
                        String.format( "Error creating node group %s on peer %s", nodeGroup, peer ), e );
            }

            results.add( new NodeGroupBuildResult( containers, exception ) );
        }

        return results;
    }
}

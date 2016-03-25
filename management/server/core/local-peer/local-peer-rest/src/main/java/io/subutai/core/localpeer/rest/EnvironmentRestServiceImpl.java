package io.subutai.core.localpeer.rest;


import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import io.subutai.common.host.ContainerHostState;
import io.subutai.common.host.HostId;
import io.subutai.common.metric.ProcessResourceUsage;
import io.subutai.common.peer.ContainerId;
import io.subutai.common.peer.LocalPeer;
import io.subutai.common.quota.ContainerQuota;


/**
 * Environment REST endpoint implementation
 */
public class EnvironmentRestServiceImpl implements EnvironmentRestService
{
    private static final Logger LOGGER = LoggerFactory.getLogger( EnvironmentRestServiceImpl.class );

    private LocalPeer localPeer;


    public EnvironmentRestServiceImpl( final LocalPeer localPeer )
    {
        this.localPeer = localPeer;
    }


    @Override
    public void destroyContainer( final ContainerId containerId )
    {
        Preconditions.checkNotNull( containerId );
        try
        {
            localPeer.destroyContainer( containerId );
        }
        catch ( Exception e )
        {
            LOGGER.error( e.getMessage(), e );
            throw new WebApplicationException( e );
        }
    }


    @Override
    public void startContainer( final ContainerId containerId )
    {
        Preconditions.checkNotNull( containerId );
        try
        {
            localPeer.startContainer( containerId );
        }
        catch ( Exception e )
        {
            LOGGER.error( e.getMessage(), e );
            throw new WebApplicationException( e );
        }
    }


    @Override
    public void stopContainer( final ContainerId containerId )
    {
        Preconditions.checkNotNull( containerId );
        try
        {
            localPeer.stopContainer( containerId );
        }
        catch ( Exception e )
        {
            LOGGER.error( e.getMessage(), e );
            throw new WebApplicationException( e );
        }
    }


    @Override
    public ContainerHostState getContainerState( final ContainerId containerId )
    {
        Preconditions.checkNotNull( containerId );
        Preconditions.checkNotNull( containerId.getId() );
        try
        {
            return localPeer.getContainerState( containerId );
        }
        catch ( Exception e )
        {
            LOGGER.error( e.getMessage(), e );
            throw new WebApplicationException( e );
        }
    }


    @Override
    public ProcessResourceUsage getProcessResourceUsage( ContainerId containerId, int pid )
    {
        Preconditions.checkNotNull( containerId );
        Preconditions.checkArgument( pid > 0 );

        try
        {
            return localPeer.getProcessResourceUsage( containerId, pid );
        }
        catch ( Exception e )
        {
            LOGGER.error( e.getMessage(), e );
            throw new WebApplicationException( e );
        }
    }

    //*********** Quota functions ***************


    @Override
    public Response getCpuSet( final ContainerId containerId )
    {
        try
        {
            Preconditions.checkNotNull( containerId );
            Preconditions.checkArgument( !Strings.isNullOrEmpty( containerId.getId() ) );

            return Response.ok( localPeer.getContainerHostById( containerId.getId() ).getCpuSet() ).build();
        }
        catch ( Exception e )
        {
            LOGGER.error( e.getMessage(), e );
            throw new WebApplicationException( e );
        }
    }


    @Override
    public Response setCpuSet( final ContainerId containerId, final Set<Integer> cpuSet )
    {
        try
        {
            Preconditions.checkNotNull( containerId );
            Preconditions.checkArgument( !Strings.isNullOrEmpty( containerId.getId() ) );

            localPeer.getContainerHostById( containerId.getId() ).setCpuSet( cpuSet );
            return Response.ok().build();
        }
        catch ( Exception e )
        {
            LOGGER.error( e.getMessage(), e );
            throw new WebApplicationException( e );
        }
    }


    @Override
    public Response getQuota( final ContainerId containerId )
    {
        Preconditions.checkNotNull( containerId );
        Preconditions.checkArgument( !Strings.isNullOrEmpty( containerId.getId() ) );
        try
        {
            ContainerQuota resourceValue = localPeer.getQuota( containerId );
            return Response.ok( resourceValue ).build();
        }
        catch ( Exception e )
        {
            LOGGER.error( e.getMessage(), e );
            throw new WebApplicationException( e );
        }
    }


    @Override
    public Response setQuota( final ContainerId containerId, ContainerQuota containerQuota )
    {
        Preconditions.checkNotNull( containerId );
        Preconditions.checkNotNull( containerQuota );
        Preconditions.checkArgument( !Strings.isNullOrEmpty( containerId.getId() ) );

        try
        {
            localPeer.setQuota( containerId, containerQuota );
            return Response.ok().build();
        }
        catch ( Exception e )
        {
            LOGGER.error( e.getMessage(), e );
            throw new WebApplicationException( e );
        }
    }


    @Override
    public Response getAvailableQuota( final ContainerId containerId )
    {
        Preconditions.checkNotNull( containerId );
        Preconditions.checkArgument( !Strings.isNullOrEmpty( containerId.getId() ) );
        try
        {

            ContainerQuota resourceValue = localPeer.getAvailableQuota( containerId );
            return Response.ok( resourceValue ).build();
        }
        catch ( Exception e )
        {
            LOGGER.error( e.getMessage(), e );
            throw new WebApplicationException( e );
        }
    }


    @Override
    public HostId getResourceHostIdByContainerId( final ContainerId containerId )
    {
        Preconditions.checkNotNull( containerId );
        Preconditions.checkArgument( !Strings.isNullOrEmpty( containerId.getId() ) );

        try
        {

            return localPeer.getResourceHostIdByContainerId( containerId );
        }
        catch ( Exception e )
        {
            LOGGER.error( e.getMessage(), e );
            throw new WebApplicationException( e );
        }
    }
}

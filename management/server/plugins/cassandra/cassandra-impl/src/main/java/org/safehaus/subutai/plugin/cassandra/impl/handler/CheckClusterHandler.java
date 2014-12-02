package org.safehaus.subutai.plugin.cassandra.impl.handler;


import java.util.Iterator;
import java.util.logging.Logger;

import org.safehaus.subutai.common.command.CommandException;
import org.safehaus.subutai.common.protocol.AbstractOperationHandler;
import org.safehaus.subutai.common.command.CommandResult;
import org.safehaus.subutai.common.command.RequestBuilder;
import org.safehaus.subutai.common.tracker.TrackerOperation;
import org.safehaus.subutai.core.environment.api.helper.Environment;
import org.safehaus.subutai.core.peer.api.ContainerHost;
import org.safehaus.subutai.plugin.cassandra.api.CassandraClusterConfig;
import org.safehaus.subutai.plugin.cassandra.impl.CassandraImpl;


public class CheckClusterHandler extends AbstractOperationHandler<CassandraImpl, CassandraClusterConfig>
{

    private static final Logger LOG = Logger.getLogger( CheckClusterHandler.class.getName() );
    private String clusterName;
    String startCommand = ". /etc/profile && $CASSANDRA_HOME/bin/cassandra";
    String serviceStartCommand = "service cassandra start";
    String serviceStatusCommand = "service cassandra status";


    public CheckClusterHandler( final CassandraImpl manager, final String clusterName )
    {
        super( manager, clusterName );
        this.clusterName = clusterName;
        trackerOperation = manager.getTracker().createTrackerOperation( CassandraClusterConfig.PRODUCT_KEY,
                String.format( "Starting %s cluster...", clusterName ) );
    }


    @Override
    public void run()
    {
        CassandraClusterConfig config = manager.getCluster( clusterName );
        if ( config == null )
        {
            trackerOperation.addLogFailed( String.format( "Cluster with name %s does not exist", clusterName ) );
            return;
        }

        Environment environment = manager.getEnvironmentManager().getEnvironmentByUUID( config.getEnvironmentId() );
        Iterator iterator = environment.getContainerHosts().iterator();

        ContainerHost host = null;
        while ( iterator.hasNext() )
        {
            host = ( ContainerHost ) iterator.next();

            try
            {
                CommandResult result = host.execute( new RequestBuilder( startCommand ) );
                if ( result.getExitCode() == 0 )
                {
                    result = host.execute( new RequestBuilder( serviceStatusCommand ) );
                    if ( result.getExitCode() == 0 )
                    {
                        if ( result.getStdOut().contains( "running..." ) )
                        {
                            trackerOperation.addLog( result.getStdOut() );
                            trackerOperation.addLogDone( "Start succeeded" );
                        }
                        else
                        {
                            trackerOperation
                                    .addLogFailed( String.format( "Unexpected result, %s", result.getStdErr() ) );
                        }
                    }
                    else
                    {
                        trackerOperation.addLogFailed( String.format( "Start failed, %s", result.getStdErr() ) );
                    }
                }
                else
                {
                    trackerOperation.addLogFailed( String.format( "Start failed, %s", result.getStdErr() ) );
                }
            }
            catch ( CommandException e )
            {
                trackerOperation.addLogFailed( String.format( "Start failed, %s", e.getMessage() ) );
            }
        }
    }


    private void logStatusResults( TrackerOperation po, CommandResult result )
    {

        StringBuilder log = new StringBuilder();

        String status = "UNKNOWN";
        if ( result.getExitCode() == 0 )
        {
            status = "Cassandra is running";
        }
        else if ( result.getExitCode() == 768 )
        {
            status = "Cassandra is not running";
        }
        else
        {
            status = result.getStdOut();
        }

        log.append( String.format( "%s", status ) );

        po.addLogDone( log.toString() );
    }
}

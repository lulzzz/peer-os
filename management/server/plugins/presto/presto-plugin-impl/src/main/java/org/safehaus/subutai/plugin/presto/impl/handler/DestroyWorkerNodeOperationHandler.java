package org.safehaus.subutai.plugin.presto.impl.handler;


import java.util.UUID;

import org.safehaus.subutai.common.protocol.AbstractOperationHandler;
import org.safehaus.subutai.common.protocol.Agent;
import org.safehaus.subutai.common.tracker.ProductOperation;
import org.safehaus.subutai.core.command.api.AgentResult;
import org.safehaus.subutai.core.command.api.Command;
import org.safehaus.subutai.core.db.api.DBException;
import org.safehaus.subutai.plugin.presto.api.PrestoClusterConfig;
import org.safehaus.subutai.plugin.presto.impl.Commands;
import org.safehaus.subutai.plugin.presto.impl.PrestoImpl;

import com.google.common.collect.Sets;


public class DestroyWorkerNodeOperationHandler extends AbstractOperationHandler<PrestoImpl> {
    private final ProductOperation po;
    private final String lxcHostname;


    public DestroyWorkerNodeOperationHandler( PrestoImpl manager, String clusterName, String lxcHostname ) {
        super( manager, clusterName );
        this.lxcHostname = lxcHostname;
        po = PrestoImpl.getTracker().createProductOperation( PrestoClusterConfig.PRODUCT_KEY,
                String.format( "Destroying %s in %s", lxcHostname, clusterName ) );
    }


    @Override
    public UUID getTrackerId() {
        return po.getId();
    }


    @Override
    public void run() {
        productOperation = po;
        PrestoClusterConfig config = manager.getCluster( clusterName );
        if ( config == null ) {
            po.addLogFailed( String.format( "Cluster with name %s does not exist\nOperation aborted", clusterName ) );
            return;
        }

        Agent agent = PrestoImpl.getAgentManager().getAgentByHostname( lxcHostname );
        if ( agent == null ) {
            po.addLogFailed(
                    String.format( "Agent with hostname %s is not connected\nOperation aborted", lxcHostname ) );
            return;
        }

        if ( config.getWorkers().size() == 1 ) {
            po.addLogFailed(
                    "This is the last slave node in the cluster. Please, destroy cluster instead\nOperation aborted" );
            return;
        }

        //check if node is in the cluster
        if ( !config.getWorkers().contains( agent ) ) {
            po.addLogFailed( String.format( "Node %s does not belong to this cluster\nOperation aborted",
                    agent.getHostname() ) );
            return;
        }

        po.addLog( "Uninstalling Presto..." );

        Command uninstallCommand = Commands.getUninstallCommand( Sets.newHashSet( agent ) );
        PrestoImpl.getCommandRunner().runCommand( uninstallCommand );

        if ( uninstallCommand.hasCompleted() ) {
            AgentResult result = uninstallCommand.getResults().get( agent.getUuid() );
            if ( result.getExitCode() != null && result.getExitCode() == 0 ) {
                if ( result.getStdOut().contains( "Package ksks-presto is not installed, so not removed" ) ) {
                    po.addLog( String.format( "Presto is not installed, so not removed on node %s",
                            agent.getHostname() ) );
                }
                else {
                    po.addLog( String.format( "Presto is removed from node %s", agent.getHostname() ) );
                }
            }
            else {
                po.addLog( String.format( "Error %s on node %s", result.getStdErr(), agent.getHostname() ) );
            }
        }
        else {
            po.addLogFailed( String.format( "Uninstallation failed, %s", uninstallCommand.getAllErrors() ) );
            return;
        }

        config.getWorkers().remove( agent );
        po.addLog( "Updating db..." );

        try {
            PrestoImpl.getPluginDAO().saveInfo( PrestoClusterConfig.PRODUCT_KEY, config.getClusterName(), config );
            po.addLogDone( "Cluster info updated in DB\nDone" );
        }
        catch ( DBException e ) {
            po.addLogFailed( "Error while updating cluster info in DB. Check logs.\nFailed" );
        }
    }
}

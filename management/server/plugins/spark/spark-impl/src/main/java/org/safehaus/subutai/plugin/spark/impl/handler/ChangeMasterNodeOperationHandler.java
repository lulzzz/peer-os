package org.safehaus.subutai.plugin.spark.impl.handler;


import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.safehaus.subutai.common.protocol.AbstractOperationHandler;
import org.safehaus.subutai.common.protocol.Agent;
import org.safehaus.subutai.common.protocol.Response;
import org.safehaus.subutai.common.tracker.ProductOperation;
import org.safehaus.subutai.common.util.StringUtil;
import org.safehaus.subutai.core.command.api.AgentResult;
import org.safehaus.subutai.core.command.api.Command;
import org.safehaus.subutai.core.command.api.CommandCallback;
import org.safehaus.subutai.core.db.api.DBException;
import org.safehaus.subutai.plugin.spark.api.SparkClusterConfig;
import org.safehaus.subutai.plugin.spark.impl.Commands;
import org.safehaus.subutai.plugin.spark.impl.SparkImpl;


/**
 * Created by dilshat on 5/7/14.
 */
public class ChangeMasterNodeOperationHandler extends AbstractOperationHandler<SparkImpl> {
    private final ProductOperation po;
    private final String newMasterHostname;
    private final boolean keepSlave;


    public ChangeMasterNodeOperationHandler( SparkImpl manager, String clusterName, String newMasterHostname,
                                             boolean keepSlave ) {
        super( manager, clusterName );
        this.newMasterHostname = newMasterHostname;
        this.keepSlave = keepSlave;
        po = SparkImpl.getTracker().createProductOperation( SparkClusterConfig.PRODUCT_KEY,
                String.format( "Changing master to %s in %s", newMasterHostname, clusterName ) );
    }


    @Override
    public UUID getTrackerId() {
        return po.getId();
    }


    @Override
    public void run() {
        final SparkClusterConfig config = manager.getCluster( clusterName );
        if ( config == null ) {
            po.addLogFailed( String.format( "Cluster with name %s does not exist\nOperation aborted", clusterName ) );
            return;
        }

        if ( SparkImpl.getAgentManager().getAgentByHostname( config.getMasterNode().getHostname() ) == null ) {
            po.addLogFailed( String.format( "Master node %s is not connected\nOperation aborted",
                    config.getMasterNode().getHostname() ) );
            return;
        }

        Agent newMaster = SparkImpl.getAgentManager().getAgentByHostname( newMasterHostname );
        if ( newMaster == null ) {
            po.addLogFailed(
                    String.format( "Agent with hostname %s is not connected\nOperation aborted", newMasterHostname ) );
            return;
        }

        if ( newMaster.equals( config.getMasterNode() ) ) {
            po.addLogFailed(
                    String.format( "Node %s is already a master node\nOperation aborted", newMasterHostname ) );
            return;
        }

        //check if node is in the cluster
        if ( !config.getAllNodes().contains( newMaster ) ) {
            po.addLogFailed(
                    String.format( "Node %s does not belong to this cluster\nOperation aborted", newMasterHostname ) );
            return;
        }

        po.addLog( "Stopping all nodes..." );
        //stop all nodes
        Command stopNodesCommand = Commands.getStopAllCommand( config.getMasterNode() );
        SparkImpl.getCommandRunner().runCommand( stopNodesCommand );
        if ( stopNodesCommand.hasSucceeded() ) {
            po.addLog( "All nodes stopped\nClearing slaves on old master..." );
            //clear slaves from old master
            Command clearSlavesCommand = Commands.getClearSlavesCommand( config.getMasterNode() );
            SparkImpl.getCommandRunner().runCommand( clearSlavesCommand );
            if ( clearSlavesCommand.hasSucceeded() ) {
                po.addLog( "Slaves cleared successfully" );
            }
            else {
                po.addLog(
                        String.format( "Clearing slaves failed, %s, skipping...", clearSlavesCommand.getAllErrors() ) );
            }
            //add slaves to new master, if keepSlave=true then master node is also added as slave
            config.getSlaveNodes().add( config.getMasterNode() );
            config.setMasterNode( newMaster );
            if ( keepSlave ) {
                config.getSlaveNodes().add( newMaster );
            }
            else {
                config.getSlaveNodes().remove( newMaster );
            }
            po.addLog( "Adding nodes to new master..." );
            Command addSlavesCommand = Commands.getAddSlavesCommand( config.getSlaveNodes(), config.getMasterNode() );
            SparkImpl.getCommandRunner().runCommand( addSlavesCommand );
            if ( addSlavesCommand.hasSucceeded() ) {
                po.addLog( "Nodes added successfully\nSetting new master IP..." );
                //modify master ip on all nodes
                Command setMasterIPCommand =
                        Commands.getSetMasterIPCommand( config.getMasterNode(), config.getAllNodes() );
                SparkImpl.getCommandRunner().runCommand( setMasterIPCommand );
                if ( setMasterIPCommand.hasSucceeded() ) {
                    po.addLog( "Master IP set successfully\nStarting cluster..." );
                    //start master & slaves

                    Command startNodesCommand = Commands.getStartAllCommand( config.getMasterNode() );
                    final AtomicInteger okCount = new AtomicInteger( 0 );
                    SparkImpl.getCommandRunner().runCommand( startNodesCommand, new CommandCallback() {

                        @Override
                        public void onResponse( Response response, AgentResult agentResult, Command command ) {
                            okCount.set( StringUtil.countNumberOfOccurences( agentResult.getStdOut(), "starting" ) );

                            if ( okCount.get() >= config.getAllNodes().size() ) {
                                stop();
                            }
                        }
                    } );

                    if ( okCount.get() >= config.getAllNodes().size() ) {
                        po.addLog( "Cluster started successfully" );
                    }
                    else {
                        po.addLog( String.format( "Start of cluster failed, %s, skipping...",
                                startNodesCommand.getAllErrors() ) );
                    }

                    po.addLog( "Updating db..." );
                    //update db
                    try {
                        SparkImpl.getPluginDAO().saveInfo( SparkClusterConfig.PRODUCT_KEY, clusterName, config );
                        po.addLogDone( "Cluster info updated in DB\nDone" );
                    }
                    catch ( DBException e ) {
                        po.addLogFailed( "Error while updating cluster info in DB. Check logs.\nFailed" );
                    }
                }
                else {
                    po.addLogFailed( String.format( "Failed to set master IP on all nodes, %s\nOperation aborted",
                            setMasterIPCommand.getAllErrors() ) );
                }
            }
            else {
                po.addLogFailed( String.format( "Failed to add slaves to new master, %s\nOperation aborted",
                        addSlavesCommand.getAllErrors() ) );
            }
        }
        else {
            po.addLogFailed( String.format( "Failed to stop all nodes, %s", stopNodesCommand.getAllErrors() ) );
        }
    }
}

package org.safehaus.subutai.plugin.mahout.impl.handler;


import com.google.common.collect.Sets;
import org.safehaus.subutai.api.commandrunner.AgentResult;
import org.safehaus.subutai.api.commandrunner.Command;
import org.safehaus.subutai.plugin.mahout.api.MahoutConfig;
import org.safehaus.subutai.plugin.mahout.impl.Commands;
import org.safehaus.subutai.plugin.mahout.impl.MahoutImpl;
import org.safehaus.subutai.shared.operation.AbstractOperationHandler;
import org.safehaus.subutai.shared.operation.ProductOperation;
import org.safehaus.subutai.shared.protocol.Agent;

import java.util.UUID;


/**
 * Created by dilshat on 5/6/14.
 */
public class AddNodeOperationHandler extends AbstractOperationHandler<MahoutImpl>
{
    private final ProductOperation po;
    private final String lxcHostname;


    public AddNodeOperationHandler( MahoutImpl manager, String clusterName, String lxcHostname )
    {
        super( manager, clusterName );
        this.lxcHostname = lxcHostname;
        po = manager.getTracker().createProductOperation( MahoutConfig.PRODUCT_KEY,
            String.format( "Adding node to %s", clusterName ) );
    }


    @Override
    public UUID getTrackerId()
    {
        return po.getId();
    }


    @Override
    public void run()
    {
        MahoutConfig config = manager.getCluster( clusterName );
        if ( config == null )
        {
            po.addLogFailed( String.format( "Cluster with name %s does not exist\nOperation aborted", clusterName ) );
            return;
        }

        //check if node agent is connected
        Agent agent = manager.getAgentManager().getAgentByHostname( lxcHostname );
        if ( agent == null )
        {
            po.addLogFailed( String.format( "Node %s is not connected\nOperation aborted", lxcHostname ) );
            return;
        }

        if ( config.getNodes().contains( agent ) )
        {
            po.addLogFailed(
                String.format( "Agent with hostname %s already belongs to cluster %s", lxcHostname, clusterName ) );
            return;
        }

        po.addLog( "Checking prerequisites..." );

        //check installed ksks packages
        Command checkInstalledCommand = Commands.getCheckInstalledCommand( Sets.newHashSet( agent ) );
        manager.getCommandRunner().runCommand( checkInstalledCommand );

        if ( !checkInstalledCommand.hasCompleted() )
        {
            po.addLogFailed( "Failed to check presence of installed ksks packages\nInstallation aborted" );
            return;
        }

        AgentResult result = checkInstalledCommand.getResults().get( agent.getUuid() );

        if ( result.getStdOut().contains( "ksks-mahout" ) )
        {
            po.addLogFailed(
                String.format( "Node %s already has Mahout installed\nInstallation aborted", lxcHostname ) );
            return;
        }
        else if ( !result.getStdOut().contains( "ksks-hadoop" ) )
        {
            po.addLogFailed( String.format( "Node %s has no Hadoop installation\nInstallation aborted", lxcHostname ) );
            return;
        }

        config.getNodes().add( agent );
        po.addLog( "Updating db..." );
        //save to db
        if ( manager.getDbManager().saveInfo( MahoutConfig.PRODUCT_KEY, config.getClusterName(), config ) )
        {
            po.addLog( "Cluster info updated in DB\nInstalling Mahout..." );
            //install mahout

            Command installCommand = Commands.getInstallCommand( Sets.newHashSet( agent ) );
            manager.getCommandRunner().runCommand( installCommand );

            if ( installCommand.hasSucceeded() )
            {
                po.addLogDone( "Installation succeeded\nDone" );
            }
            else
            {

                po.addLogFailed( String.format( "Installation failed, %s", installCommand.getAllErrors() ) );
            }
        }
        else
        {
            po.addLogFailed( "Could not update cluster info in DB! Please see logs\nInstallation aborted" );
        }
    }
}

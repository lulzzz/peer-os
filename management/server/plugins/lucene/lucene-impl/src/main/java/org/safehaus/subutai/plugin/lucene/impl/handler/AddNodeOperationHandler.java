package org.safehaus.subutai.plugin.lucene.impl.handler;


import org.safehaus.subutai.common.exception.ClusterSetupException;
import org.safehaus.subutai.common.protocol.AbstractOperationHandler;
import org.safehaus.subutai.common.protocol.Agent;
import org.safehaus.subutai.common.settings.Common;
import org.safehaus.subutai.common.tracker.TrackerOperation;
import org.safehaus.subutai.core.command.api.command.AgentResult;
import org.safehaus.subutai.core.command.api.command.Command;
import org.safehaus.subutai.plugin.hadoop.api.HadoopClusterConfig;
import org.safehaus.subutai.plugin.lucene.api.LuceneConfig;
import org.safehaus.subutai.plugin.lucene.api.SetupType;
import org.safehaus.subutai.plugin.lucene.impl.Commands;
import org.safehaus.subutai.plugin.lucene.impl.LuceneImpl;

import com.google.common.collect.Sets;


public class AddNodeOperationHandler extends AbstractOperationHandler<LuceneImpl>
{
    private final String hostname;


    public AddNodeOperationHandler( LuceneImpl manager, String clusterName, String hostname )
    {
        super( manager, clusterName );
        this.hostname = hostname;
        trackerOperation = manager.getTracker().createTrackerOperation( LuceneConfig.PRODUCT_KEY,
                String.format( "Adding node %s to %s", ( hostname != null ? hostname : "" ), clusterName ) );
    }


    @Override
    public void run()
    {
        TrackerOperation po = trackerOperation;
        LuceneConfig config = manager.getCluster( clusterName );
        if ( config == null )
        {
            po.addLogFailed( String.format( "Cluster with name %s does not exist", clusterName ) );
            return;
        }

        try
        {
            Agent agent;
            if ( config.getSetupType() == SetupType.OVER_HADOOP )
            {
                agent = setupHost( config );
            }
            else if ( config.getSetupType() == SetupType.WITH_HADOOP )
            {
                agent = addHost( config );
            }
            else
            {
                throw new ClusterSetupException( "No setup type" );
            }

            config.getNodes().add( agent );

            po.addLog( "Saving cluster info..." );
            manager.getPluginDao().saveInfo( LuceneConfig.PRODUCT_KEY, clusterName, config );
            po.addLog( "Saved cluster info" );

            po.addLogDone( null );
        }
        catch ( ClusterSetupException ex )
        {
            po.addLog( ex.getMessage() );
            po.addLogFailed( "Add worker node failed" );
        }
    }


    public Agent setupHost( LuceneConfig config ) throws ClusterSetupException
    {
        TrackerOperation po = trackerOperation;

        Agent agent = manager.getAgentManager().getAgentByHostname( hostname );
        if ( agent == null )
        {
            throw new ClusterSetupException( "New node is not connected" );
        }

        //check if node is in the cluster
        if ( config.getNodes().contains( agent ) )
        {
            throw new ClusterSetupException( "Node already belongs to cluster" + clusterName );
        }

        po.addLog( "Checking prerequisites..." );

        //check installed ksks packages
        Command checkInstalledCommand = manager.getCommands().getCheckInstalledCommand( Sets.newHashSet( agent ) );
        manager.getCommandRunner().runCommand( checkInstalledCommand );

        if ( !checkInstalledCommand.hasCompleted() )
        {
            throw new ClusterSetupException( "Failed to check installed packages" );
        }

        AgentResult result = checkInstalledCommand.getResults().get( agent.getUuid() );
        boolean skipInstall = false;
        String hadoopPack = Common.PACKAGE_PREFIX + HadoopClusterConfig.PRODUCT_NAME;
        if ( result.getStdOut().contains( Commands.PACKAGE_NAME ) )
        {
            skipInstall = true;
            po.addLog( "Node already has Lucene installed" );
        }
        else if ( !result.getStdOut().contains( hadoopPack ) )
        {
            throw new ClusterSetupException( "Node has no Hadoop installation" );
        }

        //install lucene
        if ( !skipInstall )
        {
            po.addLog( "Installing Lucene..." );
            Command installCommand = manager.getCommands().getInstallCommand( Sets.newHashSet( agent ) );
            manager.getCommandRunner().runCommand( installCommand );

            if ( installCommand.hasSucceeded() )
            {
                po.addLog( "Installation succeeded" );
            }
            else
            {
                throw new ClusterSetupException( "Installation failed: " + installCommand.getAllErrors() );
            }
        }
        return agent;
    }


    public Agent addHost( LuceneConfig config )
    {

        return null;
    }
}

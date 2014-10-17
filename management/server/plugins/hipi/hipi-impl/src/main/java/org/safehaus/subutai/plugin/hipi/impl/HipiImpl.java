package org.safehaus.subutai.plugin.hipi.impl;


import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.safehaus.subutai.common.protocol.AbstractOperationHandler;
import org.safehaus.subutai.common.protocol.ClusterSetupStrategy;
import org.safehaus.subutai.common.tracker.ProductOperation;
import org.safehaus.subutai.core.agent.api.AgentManager;
import org.safehaus.subutai.core.command.api.CommandRunner;
import org.safehaus.subutai.core.container.api.container.ContainerManager;
import org.safehaus.subutai.core.db.api.DbManager;
import org.safehaus.subutai.core.environment.api.EnvironmentManager;
import org.safehaus.subutai.core.environment.api.helper.Environment;
import org.safehaus.subutai.core.tracker.api.Tracker;
import org.safehaus.subutai.plugin.common.PluginDAO;
import org.safehaus.subutai.plugin.hadoop.api.Hadoop;
import org.safehaus.subutai.plugin.hadoop.api.HadoopClusterConfig;
import org.safehaus.subutai.plugin.hipi.api.Hipi;
import org.safehaus.subutai.plugin.hipi.api.HipiConfig;
import org.safehaus.subutai.plugin.hipi.api.SetupType;
import org.safehaus.subutai.plugin.hipi.impl.handler.AddNodeOperationHandler;
import org.safehaus.subutai.plugin.hipi.impl.handler.DestroyNodeOperationHandler;
import org.safehaus.subutai.plugin.hipi.impl.handler.InstallOperationHandler;
import org.safehaus.subutai.plugin.hipi.impl.handler.UninstallOperationHandler;

import com.google.common.base.Preconditions;


public class HipiImpl implements Hipi
{

    protected Commands commands;
    private CommandRunner commandRunner;
    private AgentManager agentManager;
    private DbManager dbManager;
    private Tracker tracker;
    private Hadoop hadoopManager;
    private ExecutorService executor;
    private PluginDAO pluginDao;
    private EnvironmentManager environmentManager;
    private ContainerManager containerManager;


    public HipiImpl( CommandRunner commandRunner, AgentManager agentManager, DbManager dbManager, Tracker tracker,
                     Hadoop hadoopManager, EnvironmentManager environmentManager, ContainerManager containerManager )
    {
        this.commands = new Commands( commandRunner );
        this.commandRunner = commandRunner;
        this.agentManager = agentManager;
        this.dbManager = dbManager;
        this.tracker = tracker;
        this.hadoopManager = hadoopManager;
        this.environmentManager = environmentManager;
        this.containerManager = containerManager;
        pluginDao = new PluginDAO( dbManager );
    }


    public Hadoop getHadoopManager()
    {
        return hadoopManager;
    }


    public Commands getCommands()
    {
        return commands;
    }


    public CommandRunner getCommandRunner()
    {
        return commandRunner;
    }


    public AgentManager getAgentManager()
    {
        return agentManager;
    }


    public DbManager getDbManager()
    {
        return dbManager;
    }


    public Tracker getTracker()
    {
        return tracker;
    }


    public PluginDAO getPluginDao()
    {
        return pluginDao;
    }


    public ContainerManager getContainerManager()
    {
        return containerManager;
    }


    public EnvironmentManager getEnvironmentManager()
    {
        return environmentManager;
    }


    public void init()
    {
        executor = Executors.newCachedThreadPool();
    }


    public void destroy()
    {
        executor.shutdown();
    }


    @Override
    public UUID installCluster( final HipiConfig config )
    {
        Preconditions.checkNotNull( config, "Configuration is null" );
        AbstractOperationHandler operationHandler = new InstallOperationHandler( this, config );
        executor.execute( operationHandler );
        return operationHandler.getTrackerId();
    }


    @Override
    public UUID uninstallCluster( final String clusterName )
    {
        AbstractOperationHandler operationHandler = new UninstallOperationHandler( this, clusterName );
        executor.execute( operationHandler );
        return operationHandler.getTrackerId();
    }


    @Override
    public List<HipiConfig> getClusters()
    {
        return pluginDao.getInfo( HipiConfig.PRODUCT_KEY, HipiConfig.class );
    }


    @Override
    public HipiConfig getCluster( String clusterName )
    {
        return pluginDao.getInfo( HipiConfig.PRODUCT_KEY, clusterName, HipiConfig.class );
    }


    @Override
    public UUID installCluster( HipiConfig config, HadoopClusterConfig hadoopConfig )
    {
        InstallOperationHandler operationHandler = new InstallOperationHandler( this, config );
        operationHandler.setHadoopConfig( hadoopConfig );
        executor.execute( operationHandler );
        return operationHandler.getTrackerId();
    }


    @Override
    public UUID addNode( final String clusterName, final String lxcHostname )
    {
        AbstractOperationHandler operationHandler = new AddNodeOperationHandler( this, clusterName, lxcHostname );
        executor.execute( operationHandler );
        return operationHandler.getTrackerId();
    }


    @Override
    public UUID destroyNode( final String clusterName, final String lxcHostname )
    {
        AbstractOperationHandler operationHandler = new DestroyNodeOperationHandler( this, clusterName, lxcHostname );
        executor.execute( operationHandler );
        return operationHandler.getTrackerId();
    }


    @Override
    public ClusterSetupStrategy getClusterSetupStrategy( Environment env, HipiConfig config, ProductOperation po )
    {
        if ( config.getSetupType() == SetupType.OVER_HADOOP )
        {
            return new OverHadoopSetupStrategy( this, config, po );
        }
        else if ( config.getSetupType() == SetupType.WITH_HADOOP )
        {
            WithHadoopSetupStrategy s = new WithHadoopSetupStrategy( this, config, po );
            s.setEnvironment( env );
            return s;
        }
        return null;
    }
}
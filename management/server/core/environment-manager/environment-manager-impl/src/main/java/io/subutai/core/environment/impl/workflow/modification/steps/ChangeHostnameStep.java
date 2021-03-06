package io.subutai.core.environment.impl.workflow.modification.steps;


import io.subutai.common.environment.Environment;
import io.subutai.common.environment.EnvironmentModificationException;
import io.subutai.common.environment.EnvironmentNotFoundException;
import io.subutai.common.peer.ContainerId;
import io.subutai.common.util.TaskUtil;
import io.subutai.core.environment.impl.EnvironmentManagerImpl;
import io.subutai.core.environment.impl.entity.EnvironmentContainerImpl;
import io.subutai.core.environment.impl.entity.LocalEnvironment;
import io.subutai.core.environment.impl.workflow.modification.steps.helpers.RenameContainerTask;


public class ChangeHostnameStep
{
    private final EnvironmentManagerImpl environmentManager;

    private final LocalEnvironment environment;
    private final ContainerId containerId;
    private String newHostname;

    private String oldHostname;

    protected TaskUtil<Object> renameUtil = new TaskUtil<>();


    public ChangeHostnameStep( final EnvironmentManagerImpl environmentManager, final LocalEnvironment environment,
                               final ContainerId containerId, final String newHostname )
    {
        this.environmentManager = environmentManager;
        this.environment = environment;
        this.containerId = containerId;
        this.newHostname = newHostname;
    }


    public Environment execute() throws EnvironmentModificationException, EnvironmentNotFoundException
    {

        renameUtil.addTask( new RenameContainerTask( environment, containerId, newHostname ) );

        TaskUtil.TaskResults<Object> renameResults = renameUtil.executeParallel();

        TaskUtil.TaskResult<Object> renameResult = renameResults.getResults().iterator().next();

        EnvironmentContainerImpl container = ( EnvironmentContainerImpl ) renameResult.getResult();

        RenameContainerTask task = ( RenameContainerTask ) renameResult.getTask();

        if ( renameResult.hasSucceeded() )
        {
            oldHostname = task.getOldHostname();

            newHostname = task.getNewHostname();
        }
        else
        {
            throw new EnvironmentModificationException(
                    String.format( "Failed to change hostname of container %s. Reason: %s", container.getId(),
                            renameResult.getFailureReason() ) );
        }

        return environmentManager.loadEnvironment( environment.getId() );
    }


    public String getOldHostname()
    {
        return oldHostname;
    }


    public String getNewHostname()
    {
        return newHostname;
    }
}

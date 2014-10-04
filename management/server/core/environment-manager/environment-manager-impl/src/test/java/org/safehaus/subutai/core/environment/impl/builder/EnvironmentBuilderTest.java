package org.safehaus.subutai.core.environment.impl.builder;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.safehaus.subutai.common.protocol.Agent;
import org.safehaus.subutai.common.protocol.EnvironmentBlueprint;
import org.safehaus.subutai.common.protocol.EnvironmentBuildTask;
import org.safehaus.subutai.common.protocol.NodeGroup;
import org.safehaus.subutai.common.protocol.PlacementStrategy;
import org.safehaus.subutai.core.agent.api.AgentManager;
import org.safehaus.subutai.core.container.api.container.ContainerManager;
import org.safehaus.subutai.core.environment.api.exception.EnvironmentBuildException;
import org.safehaus.subutai.core.network.api.NetworkManager;
import org.safehaus.subutai.core.registry.api.Template;
import org.safehaus.subutai.core.registry.api.TemplateRegistry;

import com.google.common.collect.Sets;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Created by bahadyr on 9/25/14.
 */
@RunWith( MockitoJUnitRunner.class )
public class EnvironmentBuilderTest
{
    EnvironmentBuilder environmentBuilder;
    @Mock
    TemplateRegistry templateRegistry;
    @Mock
    AgentManager agentManager;
    @Mock
    NetworkManager networkManager;
    @Mock
    ContainerManager containerManager;


    @Before
    public void setUp() throws Exception
    {
        environmentBuilder = mock( EnvironmentBuilder.class );
        environmentBuilder = new EnvironmentBuilder( templateRegistry, agentManager, networkManager, containerManager );
    }


    @Test
    public void test() throws EnvironmentBuildException
    {

        EnvironmentBuildTask task = createTask();
        environmentBuilder.build( task );
    }


    private EnvironmentBuildTask createTask()
    {

        EnvironmentBuildTask task = mock( EnvironmentBuildTask.class );
        EnvironmentBlueprint environmentBlueprint = mock( EnvironmentBlueprint.class );
        when( task.getPhysicalNodes() ).thenReturn( Sets.newHashSet( "py1" ) );
        when( agentManager.getAgentByHostname( "py1" ) ).thenReturn( mock( Agent.class ) );
        Template template = mock( Template.class );
        when( template.getTemplateName() ).thenReturn( "master" );
        when( templateRegistry.getTemplate( "master" ) ).thenReturn( template );
        when( task.getEnvironmentBlueprint() ).thenReturn( environmentBlueprint );
        when( environmentBlueprint.getName() ).thenReturn( "baha" );
        when( environmentBlueprint.getDomainName() ).thenReturn( "domain" );
        when( environmentBlueprint.isExchangeSshKeys() ).thenReturn( false );
        when( environmentBlueprint.isLinkHosts() ).thenReturn( false );
        NodeGroup nodeGroup = createNodeGroup();
        when( environmentBlueprint.getNodeGroups() ).thenReturn( Sets.newHashSet( nodeGroup ) );
        return task;
    }


    private NodeGroup createNodeGroup()
    {
        NodeGroup nodeGroup = new NodeGroup();
        nodeGroup.setExchangeSshKeys( false );
        nodeGroup.setLinkHosts( false );
        nodeGroup.setName( "group" );
        nodeGroup.setDomainName( "domain" );
        nodeGroup.setNumberOfNodes( 3 );
        nodeGroup.setPlacementStrategy( PlacementStrategy.ROUND_ROBIN );
        nodeGroup.setTemplateName( "master" );
        return nodeGroup;
    }
}

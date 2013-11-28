package org.safehaus.kiskis.mgmt.server.ui;

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Tree;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import org.safehaus.kiskis.mgmt.server.ui.util.AppData;
import org.safehaus.kiskis.mgmt.shared.protocol.Agent;
import org.safehaus.kiskis.mgmt.shared.protocol.CommandJson;
import org.safehaus.kiskis.mgmt.shared.protocol.api.AgentManagerInterface;
import org.safehaus.kiskis.mgmt.shared.protocol.api.ui.AgentListener;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created with IntelliJ IDEA.
 * User: daralbaev
 * Date: 11/8/13
 * Time: 7:24 PM
 */
@SuppressWarnings("serial")
public class MgmtAgentManager extends VerticalLayout implements
        Property.ValueChangeListener, AgentListener {

    private AgentManagerInterface agentManagerInterface;
    private Set<Agent> registeredAgents;
    private Tree tree;
    private HierarchicalContainer container;
    private int id;

    public MgmtAgentManager(AgentManagerInterface agentManagerService) {
        this.agentManagerInterface = agentManagerService;
        setSizeFull();
        setSpacing(true);

        tree = new Tree("List of nodes", getNodeContainer());
        tree.setMultiSelect(true);
        tree.setImmediate(true);
        tree.addListener(this);
        addComponent(tree);
        addComponent(getLxcAgents());

        id = ThreadLocalRandom.current().nextInt(1, 1000000);
        agentManagerService.addListener(this);
    }

    /*
     * Shows a notification when a selection is made.
     */
    public void valueChange(Property.ValueChangeEvent event) {
        if (event.getProperty().getValue() instanceof Set) {
            Tree t = (Tree) event.getProperty();

            Set<Agent> selectedList = new HashSet<Agent>();

            for (Object o : (Set<Object>) t.getValue()) {
                selectedList.add((Agent) tree.getItem(o).getItemProperty("value").getValue());
            }

            AppData.setAgentList(selectedList);
            getWindow().showNotification(
                    "Selected agents",
                    selectedList.toString(),
                    Window.Notification.TYPE_TRAY_NOTIFICATION);
        }
    }

    private Button getLxcAgents() {
        Button button = new Button("Delete agents");
        button.setDescription("Gets LXC agents from Cassandra");
        button.addListener(new Button.ClickListener() {

            @Override
            public void buttonClick(Button.ClickEvent event) {
                Set<Agent> agents = AppData.getAgentList();
                refreshNodecontainer(agents, true, null);
            }
        });
        return button;
    }

    @Override
    public void agentRegistered(Set<Agent> agents) {
        try {
            refreshAgents();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public int getId() {
        return this.id;
    }

    public HierarchicalContainer getNodeContainer() {
        Item item = null;

        registeredAgents = new HashSet<Agent>();
        container = new HierarchicalContainer();
        container.addContainerProperty("name", String.class, null);
        container.addContainerProperty("value", Agent.class, null);
        container.addContainerProperty("icon", ThemeResource.class,
                new ThemeResource("icons/16/document.png"));

        refreshAgents();

        return container;
    }

    private void refreshAgents(){
        Set<Agent> setToRemove = new HashSet<Agent>();
        Set<Agent> setToAdd = new HashSet<Agent>();

        // clear all agents
        setToRemove.clear();
        setToRemove.addAll(registeredAgents);
        setToRemove.removeAll(agentManagerInterface.getRegisteredAgents());
        refreshNodecontainer(setToRemove, true, null);

        // add physical agents
        setToAdd.clear();
        setToAdd.addAll(agentManagerInterface.getRegisteredPhysicalAgents());
        setToAdd.removeAll(registeredAgents);
        refreshNodecontainer(setToAdd, false, null);

        for(Agent agent : registeredAgents){
            // add lxc agents
            Set<Agent> setToAddChild = new HashSet<Agent>();
            setToAddChild.clear();
            setToAddChild.addAll(agentManagerInterface.getChildLxcAgents(agent));
            setToAddChild.removeAll(registeredAgents);
            refreshNodecontainer(setToAddChild, false, agent);
        }

        registeredAgents.clear();
        registeredAgents.addAll(agentManagerInterface.getRegisteredAgents());
    }

    public void refreshNodecontainer(Set<Agent> agents, boolean delete, Agent parent){
        if(delete){
            for(Agent agent : agents){
                container.removeItemRecursively(agent.getHostname());
            }
        } else {
            for(Agent agent : agents){
                Item item = null;
                item = container.addItem(agent.getHostname());
                item.getItemProperty("name").setValue(agent.getUuid());
                item.getItemProperty("value").setValue(agent);
                if(!agent.isIsLXC()){
                    container.setChildrenAllowed(agent.getUuid(), true);
                } else {
                    item.getItemProperty("icon").setValue(new ThemeResource("icons/16/folder.png"));
                    container.setParent(agent.getHostname(), parent.getHostname());
                    container.setChildrenAllowed(agent.getHostname(), false);
                }
            }
        }
    }
}
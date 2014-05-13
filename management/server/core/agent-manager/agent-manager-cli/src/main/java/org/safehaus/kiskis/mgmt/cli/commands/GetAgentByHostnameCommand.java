package org.safehaus.kiskis.mgmt.cli.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.safehaus.kiskis.mgmt.api.agentmanager.AgentManager;
import org.safehaus.kiskis.mgmt.shared.protocol.Agent;


/**
 * Displays the last log entries
 */
@Command(scope = "agent", name = "get-agent-by-hostname", description = "get agent by hostname")
public class GetAgentByHostnameCommand extends OsgiCommandSupport {

    private AgentManager agentManager;

    public AgentManager getAgentManager() {
        return agentManager;
    }

    public void setAgentManager(AgentManager agentManager) {
        this.agentManager = agentManager;
    }

    @Argument(index = 0, name = "hostname", required = true, multiValued = false, description = "agent hostname")
    String hostname;

    protected Object doExecute() {

        Agent agent = agentManager.getAgentByHostname(hostname);
        StringBuilder sb = new StringBuilder();
        sb.append("Hostname: ").append(agent.getHostname());
        for (String ip : agent.getListIP()) {
            sb.append("IP: ").append(ip);
        }
        sb.append("MAC address: ").append(agent.getMacAddress());
        sb.append("Parent hostname: ").append(agent.getParentHostName());
        sb.append("Transport ID: ").append(agent.getTransportId());
        sb.append("UUID: ").append(agent.getUuid());
        System.out.println(sb.toString());
        System.out.println("get-agent-by-hostname command executed");
        return null;
    }
}

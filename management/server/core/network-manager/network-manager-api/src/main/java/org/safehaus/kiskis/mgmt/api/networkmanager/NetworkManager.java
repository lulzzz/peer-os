package org.safehaus.kiskis.mgmt.api.networkmanager;

import org.safehaus.kiskis.mgmt.shared.protocol.Agent;

import java.util.List;

/**
 * Created by daralbaev on 03.04.14.
 */
public interface NetworkManager {
    public boolean configSshOnAgents(List<Agent> agentList);

    public boolean configHostsOnAgents(List<Agent> agentList, String domainName);
}

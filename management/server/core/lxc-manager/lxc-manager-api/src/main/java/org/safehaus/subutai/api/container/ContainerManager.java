package org.safehaus.subutai.api.container;


import org.safehaus.subutai.api.lxcmanager.LxcCreateException;
import org.safehaus.subutai.api.lxcmanager.LxcDestroyException;
import org.safehaus.subutai.shared.protocol.Agent;
import org.safehaus.subutai.shared.protocol.PlacementStrategy;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


public interface ContainerManager {

	public Set<Agent> clone(UUID envId, String templateName, int nodesCount, Collection<Agent> hosts,
	                        PlacementStrategy... strategy) throws LxcCreateException;

	/**
	 * Clones containers in parallel, simultaneously
	 */
	public Set<Agent> clone(String templateName, int nodesCount, Collection<Agent> hosts,
	                        PlacementStrategy... strategy) throws LxcCreateException;

	public boolean attachAndExecute(Agent physicalHost, String cloneName, String cmd);

	public boolean attachAndExecute(Agent physicalHost, String cloneName, String cmd, long t, TimeUnit unit);

	public void cloneDestroy(String hostName, String cloneName) throws LxcDestroyException;

	/**
	 * Destroys containers in parallel, simultaneously
	 */
	public void clonesDestroyByHostname(Set<String> cloneNames) throws LxcDestroyException;

	/**
	 * Destroys containers in parallel, simultaneously
	 */
	public void clonesDestroy(Set<Agent> lxcAgents) throws LxcDestroyException;

	public void clonesDestroy(final String hostName, final Set<String> cloneNames) throws LxcDestroyException;

	public void clonesCreate(final String hostName, final String templateName, final Set<String> cloneNames)
			throws LxcCreateException;
}

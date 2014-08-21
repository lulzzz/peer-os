package org.safehaus.subutai.plugin.accumulo.cli;


import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.safehaus.subutai.plugin.accumulo.api.Accumulo;
import org.safehaus.subutai.plugin.accumulo.api.AccumuloClusterConfig;

import java.util.List;


/**
 * Displays the last log entries
 */
@Command (scope = "accumulo", name = "list-clusters", description = "mydescription")
public class ListClustersCommand extends OsgiCommandSupport {

	private Accumulo accumuloManager;


	public Accumulo getAccumuloManager() {
		return accumuloManager;
	}


	public void setAccumuloManager(Accumulo accumuloManager) {
		this.accumuloManager = accumuloManager;
	}


	protected Object doExecute() {
		List<AccumuloClusterConfig> accumuloClusterConfigList = accumuloManager.getClusters();
		if (!accumuloClusterConfigList.isEmpty()) {
			for (AccumuloClusterConfig accumuloClusterConfig : accumuloClusterConfigList) {
				System.out.println(accumuloClusterConfig.getClusterName());
			}
		} else {
			System.out.println("No Accumulo cluster");
		}

		return null;
	}
}

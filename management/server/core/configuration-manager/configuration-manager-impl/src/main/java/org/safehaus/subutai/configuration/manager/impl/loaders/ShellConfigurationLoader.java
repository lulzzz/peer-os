package org.safehaus.subutai.configuration.manager.impl.loaders;


import com.google.gson.JsonObject;
import org.safehaus.subutai.configuration.manager.api.TextInjector;


/**
 * Created by bahadyr on 8/4/14.
 */
public class ShellConfigurationLoader implements ConfigurationLoader {


	private TextInjector textInjector;


	public ShellConfigurationLoader(final TextInjector textInjector) {
		this.textInjector = textInjector;
	}


	@Override
	public JsonObject getConfiguration(String hostname, String configPathFilename) {
		return null;
	}


	@Override
	public boolean setConfiguration(String hostname, String configFilePath, String jsonObjectConfig) {
		return false;
	}
}

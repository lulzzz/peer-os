package org.safehaus.subutai.api.aptrepositorymanager;


/**
 * Created by dilshat on 6/23/14.
 */
public enum AptCommand {
	LIST_PACKAGES("list packages"),
	ADD_PACKAGE("add package"),
	REMOVE_PACKAGE("remove package"),
	READ_FILE_INSIDE_PACKAGE("read file inside package");


	private String command;


	private AptCommand(final String command) {
		this.command = command;
	}


	/**
	 * Returns corresponding to this enum apt command
	 */
	public String getCommand() {
		return command;
	}
}

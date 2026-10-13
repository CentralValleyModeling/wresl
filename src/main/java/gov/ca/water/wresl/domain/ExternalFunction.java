package gov.ca.water.wresl.domain;

import java.util.*;

public abstract class ExternalFunction {
	public String externalDir = "";
	public String mainFileDir = "";

	public abstract void execute(Stack stack);

	public void setExternalDir(String externalDir) {
		this.externalDir = externalDir;
	}

	public void setMainFileDir(String mainFileDir) { this.mainFileDir = mainFileDir; }

}

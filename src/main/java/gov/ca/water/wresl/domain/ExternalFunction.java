package gov.ca.water.wresl.domain;

import gov.ca.water.utilities.Param;

import java.util.*;

public abstract class ExternalFunction {
	public String externalDir = "";

	public abstract void execute(Stack stack);

	public void setExternalDir(String externalDir) {
		this.externalDir = externalDir;
	}

}

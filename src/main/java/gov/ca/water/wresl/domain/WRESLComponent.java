package gov.ca.water.wresl.domain;

import gov.ca.water.utilities.Param;

public abstract class WRESLComponent {
    public String name = "";
    public String fromWresl = Param.undefined;
    public int line = 0;
}

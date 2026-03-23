package gov.ca.water.wresl.domain;

import gov.ca.water.wresl.compile.Param;

public class Sequence {
    public String sequenceName = Param.undefined;
    public String modelName = Param.undefined;
    public int order = 0;
    public String condition = Param.always;
    public String fromWresl = Param.undefined;
    public String timeStep = Param.undefined;

}

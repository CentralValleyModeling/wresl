package gov.ca.water.wresl.parsing;

import gov.ca.water.wresl.domain.Param;

public class Sequence {
    public String sequenceName = Param.undefined;
    public String modelName = Param.undefined;
    public int order = 0;
    public String condition = Param.always;
    public String timeStep = Param.undefined;

}

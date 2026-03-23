package gov.ca.water.wresl.domain;

import gov.ca.water.wresl.compile.Param;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Alias extends WRIMSComponent implements Serializable {
    private static final long serialVersionUID = 1L;

    public String condition = Param.always;
    public String scope = Param.undefined;
    public String kind = Param.undefined;
    public String units = Param.undefined;
    public String expression = null;
    public ValueEvaluatorParser_DUMMY expressionParser;
    public IntDouble data = null;
    public Set<String> dependants = new HashSet<>();
    public Set<String> neededVarInCycleSet = new HashSet<>();
    public boolean needVarFromEarlierCycle = false;
    public boolean noSolver = false;

    // default is zero
    public String timeArraySize = "0";
    public ValueEvaluatorParser_DUMMY timeArraySizeParser;

}

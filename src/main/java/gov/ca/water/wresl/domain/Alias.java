package gov.ca.water.wresl.domain;

import java.io.Serializable;
import java.util.Set;

public class Alias extends WRIMSComponent implements Serializable {
    private static final long serialVersionUID = 1L;

    public String condition;
    public String scope;
    public String kind;
    public String units;
    public String expression;
    public ValueEvaluatorParser_DUMMY expressionParser;
    public IntDouble data;
    public Set<String> dependants;
    public Set<String> neededVarInCycleSet;
    public boolean needVarFromEarlierCycle;
    public boolean noSolver;

    // default is zero
    public String timeArraySize;
    public ValueEvaluatorParser_DUMMY timeArraySizeParser;

}

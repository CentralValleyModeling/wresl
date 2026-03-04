package gov.ca.water.wresl.domain;

import java.io.Serializable;
import java.util.Set;

public class Alias extends WRIMSComponent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String condition;
    private String scope;
    private String kind;
    private String units;
    private String expression;
    private ValueEvaluatorParser_DUMMY expressionParser;
    private String fromWresl;
    private int line = 1;
    private IntDouble data;
    private Set<String> dependants;
    private Set<String> neededVarInCycleSet;
    private boolean needVarFromEarlierCycle;
    private boolean noSolver;

    // default is zero
    private String timeArraySize;
    private ValueEvaluatorParser_DUMMY timeArraySizeParser;

}

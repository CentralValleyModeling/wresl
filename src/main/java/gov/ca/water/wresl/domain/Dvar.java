package gov.ca.water.wresl.domain;

import java.io.Serializable;
import java.util.Set;

public class Dvar extends WRIMSComponent implements Serializable {
    private static final long serialVersionUID = 1L;

    public String scope;
    public String integer;
    public String format;
    public String kind;
    public String units;
    public String lowerBound;
    public ValueEvaluatorParser_DUMMY lowerBoundParser;
    public String upperBound;
    public ValueEvaluatorParser_DUMMY upperBoundParser;
    public String condition;
    public Number upperBoundValue;
    public Number lowerBoundValue;
    public String expression;
    public Set<String> dependants;
    public IntDouble data;

    // default is zero
    public String timeArraySize;
    public ValueEvaluatorParser_DUMMY timeArraySizeParser;

}

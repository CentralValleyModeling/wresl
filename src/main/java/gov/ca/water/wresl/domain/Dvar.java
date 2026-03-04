package gov.ca.water.wresl.domain;

import java.io.Serializable;
import java.util.Set;

public class Dvar extends WRIMSComponent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String fromWresl;
    private int line=1;
    private String scope;
    private String integer;
    private String format;
    private String kind;
    private String units;
    private String lowerBound;
    private ValueEvaluatorParser_DUMMY lowerBoundParser;
    private String upperBound;
    private ValueEvaluatorParser_DUMMY upperBoundParser;
    private String condition;
    private Number upperBoundValue;
    private Number lowerBoundValue;
    private String expression;
    private Set<String> dependants;
    private IntDouble data;

    // default is zero
    private String timeArraySize;
    private ValueEvaluatorParser_DUMMY timeArraySizeParser;

}

package gov.ca.water.wresl.domain;

import java.io.Serializable;

public class WeightElement extends WRESLComponent implements Serializable  {
    private static final long serialVersionUID = 1L;

    private String weight;
    private String condition;
    private ValueEvaluatorParser_DUMMY weightParser;
    private ValueEvaluatorParser_DUMMY conditionParser;
    private String fromWresl;
    private int line=1;
    private double value;

    // default is zero
    private String timeArraySize;
    private ValueEvaluatorParser_DUMMY timeArraySizeParser;

    private double min=-1;
    private double max=-1;
    private String minTC="";
    private String maxTC="";
}

package gov.ca.water.wresl.domain;

import org.antlr.v4.runtime.tree.ParseTree;

import java.io.Serializable;

public class WeightElement extends WRESLComponent implements Serializable  {
    private static final long serialVersionUID = 1L;

    public String weight;
    public String condition;
    public ParseTree weightParseTree;
    public ParseTree conditionParseTree;
    public double value;

    // default is zero
    public String timeArraySize;
    public ParseTree timeArraySizeParseTree;

    public double min=-1;
    public double max=-1;
    public String minTC="";
    public String maxTC="";
}

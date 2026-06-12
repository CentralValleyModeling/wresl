package gov.ca.water.wresl.domain;

import gov.ca.water.utilities.Param;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.Serializable;

public class WeightElement extends WRESLComponent implements Serializable  {
    private static final long serialVersionUID = 1L;

    public String weight = Param.zero;
    public String condition = Param.always;
    public ParseTree weightParseTree = null;
    public ParseTree conditionParseTree = null;
    public double value = 0.0;

    // default is zero
    public String timeArraySize = Param.zero;
    public ParseTree timeArraySizeParseTree = null;

    public double min = -1;
    public double max = -1;
    public String minTC = "";
    public String maxTC = "";


    public void setValue(double value){
        this.value = value;
    }

    public double getValue(){
        return this.value;
    }
}

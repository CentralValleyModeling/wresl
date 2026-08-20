package gov.ca.water.wresl.domain;

import gov.ca.water.utilities.Param;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Dvar extends Timeseries implements Serializable {
    private static final long serialVersionUID = 1L;

    public String integer = Param.no;
    public String lowerBound = Param.undefined;
    public ParseTree lowerBoundExpressionParseTree = null;
    public String upperBound = Param.undefined;
    public ParseTree upperBoundExpressionParseTree = null;
    public String condition = Param.always;
    public Number upperBoundValue = null;
    public Number lowerBoundValue = null;
    public String expression = Param.undefined;

    // Future time array; default is zero
    public String timeArraySize = Param.zero;
    public ParseTree timeArraySizeExpressionParseTree = null;


    // ------------------------------------------------------------
    // --- SETTERS
    // ------------------------------------------------------------
    public void setInteger(String integer) { this.integer = integer; }


    // ------------------------------------------------------------
    // --- GETTERS
    // ------------------------------------------------------------
    public IntDouble getLastData() {
        return this.data.getLast();
    }
}

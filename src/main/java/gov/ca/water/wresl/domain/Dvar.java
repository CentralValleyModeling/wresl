package gov.ca.water.wresl.domain;

import org.antlr.v4.runtime.tree.ParseTree;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Dvar extends WRESLComponent implements Serializable {
    private static final long serialVersionUID = 1L;

    public String scope = Param.undefined;
    public String integer = Param.no;
    public String format = Param.undefined;
    public String kind = Param.undefined;
    public String units = Param.undefined;
    public String lowerBound = Param.undefined;
    public ParseTree lowerBoundExpressionParseTree = null;
    public String upperBound = Param.undefined;
    public ParseTree upperBoundExpressionParseTree = null;
    public String condition = Param.always;
    public Number upperBoundValue = null;
    public Number lowerBoundValue = null;
    public String expression = Param.undefined;
    public Set<String> dependants = new HashSet<>();
    public IntDouble data = null;

    // default is zero
    public String timeArraySize = "0";
    public ParseTree timeArraySizeExpressionParseTree = null;



    // --------------------
    // --- METHODS
    // --------------------

    // Set data
    public void setData(IntDouble data) {
        this.data = data;
    }

    // Get data
    public IntDouble getData() {
        return this.data;
    }
}

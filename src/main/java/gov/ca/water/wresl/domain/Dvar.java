package gov.ca.water.wresl.domain;

import gov.ca.water.utilities.Param;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Dvar extends WRESLComponent implements Serializable {
    private static final long serialVersionUID = 1L;

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
    public String timeArraySize = Param.zero;
    public ParseTree timeArraySizeExpressionParseTree = null;



    // ------------------------------------------------------------
    // --- SETTERS
    // ------------------------------------------------------------
    public void setData(IntDouble data) {
        this.data = data;
    }

    public void setName(String name) { this.name = name; }

    public void setKind(String kind) { this.kind = kind; }

    public void setUnits(String units) { this.units = units; }

    public void setInteger(String integer) { this.integer = integer; }


    // ------------------------------------------------------------
    // --- GETTERS
    // ------------------------------------------------------------
    public IntDouble getData() {
        return this.data;
    }

    public String getName() { return this.name; }
}

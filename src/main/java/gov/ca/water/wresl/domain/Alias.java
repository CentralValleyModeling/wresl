package gov.ca.water.wresl.domain;

import gov.ca.water.utilities.Param;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Alias extends WRESLComponent implements Serializable {
    private static final long serialVersionUID = 1L;

    public String condition = Param.always;
    public String scope = Param.undefined;
    public String kind = Param.undefined;
    public String units = Param.undefined;
    public String expression = null;
    public ParseTree expressionParseTree = null;
    public IntDouble data = null;
    public Set<String> dependants = new HashSet<>();
    public Set<String> neededVarInCycleSet = new HashSet<>();
    public boolean needVarFromEarlierCycle = false;
    public boolean noSolver = false;

    // default is zero
    public String timeArraySize = Param.zero;
    public ParseTree timeArraySizeParseTree = null;

   // Methods
    public void setData(IntDouble data){
        this.data=data;
    }

    public IntDouble getData() {return this.data; }
}

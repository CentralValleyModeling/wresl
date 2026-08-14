package gov.ca.water.wresl.domain;

import gov.ca.water.io.DSS.CondensedReferenceCacheAndRead;
import gov.ca.water.utilities.Param;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.Serializable;
import java.util.*;

public class Alias extends Timeseries implements Serializable {
    public String expression = null;
    public ParseTree expressionParseTree = null;
    public Set<String> dependants = new HashSet<>();
    public Set<String> neededVarInCycleSet = new HashSet<>();
    public boolean needVarFromEarlierCycle = false;
    public boolean noSolver = false;

    // Time array; default is zero
    public String timeArraySize = Param.zero;
    public ParseTree timeArraySizeParseTree = null;

    // ------------------------------------------------------------
    // --- SETTERS
    // ------------------------------------------------------------
    public void setStartTime(Date startTime) { this.startTime = startTime; }


    // ------------------------------------------------------------
    // --- MISC. METHODS
    // ------------------------------------------------------------
    // Add data
    public void addData(double data){
        this.data.add(data);
    }

}

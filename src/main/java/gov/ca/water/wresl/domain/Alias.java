package gov.ca.water.wresl.domain;

import gov.ca.water.io.DSS.CondensedReferenceCacheAndRead;
import gov.ca.water.utilities.Param;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.Serializable;
import java.util.*;

public class Alias extends Timeseries implements Serializable {
    private static final long serialVersionUID = 1L;

    public String expression = null;
    public ParseTree expressionParseTree = null;
    public boolean noSolver = false;

    // Time array; default is zero
    public String timeArraySize = Param.zero;
    public ParseTree timeArraySizeParseTree = null;


    public Alias copyOf() {
        Alias newAlias = new Alias();

        newAlias = (Alias) super.copyOf();

        newAlias.expression = this.expression;
        newAlias.expressionParseTree = this.expressionParseTree;
        newAlias.noSolver = this.noSolver;
        newAlias.timeArraySize = this.timeArraySize;
        newAlias.timeArraySizeParseTree = this.timeArraySizeParseTree;

        return newAlias;
    }
}

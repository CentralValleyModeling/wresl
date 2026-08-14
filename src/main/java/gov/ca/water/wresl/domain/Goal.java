package gov.ca.water.wresl.domain;

import gov.ca.water.utilities.Param;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.Serializable;
import java.util.*;

public class Goal extends WRESLComponent implements Serializable {
    private static final long serialVersionUID = 1L;


    public List<String> caseName = new ArrayList<>();
    public List<String> caseCondition = new ArrayList<>();
    public List<ParseTree> caseConditionParseTrees = new ArrayList<>();
    public List<String> goalExpression = new ArrayList<>();
    public List<ParseTree> goalExpressionParseTrees = new ArrayList<>();

    // Time array default is zero
    public String timeArraySize = Param.zero;
    public ParseTree timeArraySizeParseTree;

    // Data to be inserted into solver
    private String sign = Param.undefined;
    private IntDouble intDouble = null;
    private LinkedHashMap<String, IntDouble> multiplier = new LinkedHashMap<>();


    // ------------------------------------------------------------
    // --- SETTERS
    // ------------------------------------------------------------
    public void setName(String name) { this.name = name; }


    // ------------------------------------------------------------
    // --- GETTERS
    // ------------------------------------------------------------
    public String getName() { return this.name; }

    public LinkedHashMap<String, IntDouble> getMultiplier() { return this.multiplier; }

    public IntDouble getIntDouble() { return this.intDouble; }

    public String getSign() { return this.sign; }


    // ------------------------------------------------------------
    // --- MISC. METHODS
    // ------------------------------------------------------------
    public boolean isEvalExpressionNumeric() {
        if (this.multiplier.size() == 0) {
            return true;
        } else {
            return false;
        }
    }

}

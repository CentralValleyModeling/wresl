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
    private EvalConstraint solverData;



    // ------------------------------------------------------------
    // --- SETTERS
    // ------------------------------------------------------------
    public void setName(String name) { this.name = name; }

    public void setSolverData(EvalConstraint constraint) { this.solverData = constraint; }


    // ------------------------------------------------------------
    // --- GETTERS
    // ------------------------------------------------------------
    public EvalConstraint getSolverData() { return this.solverData; }

    public String getName() { return this.name; }

    public LinkedHashMap<String, IntDouble> getMultipliers() { return this.solverData.getMultipliers(); }

    public IntDouble getConstant() { return this.solverData.getConstant(); }

    public String getSign() { return this.solverData.getSign(); }


    // ------------------------------------------------------------
    // --- MISC. METHODS
    // ------------------------------------------------------------
    public boolean isEvalExpressionNumeric() {
        return this.solverData.isNumeric();
    }

}

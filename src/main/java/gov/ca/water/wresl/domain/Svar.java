package gov.ca.water.wresl.domain;

import gov.ca.water.utilities.Param;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.Serializable;
import java.util.*;

public class Svar extends WRESLComponent implements Serializable {
    private static final long serialVersionUID = 1L;

    // These properties are the same for all Svar time array
    public Set<String> dependants = new HashSet<>();
    public Set<String> neededVarInCycleSet = new HashSet<>();
    public boolean needVarFromEarlierCycle = false;

    private IntDouble data = null;

    // default is zero
    public String timeArraySize = Param.zero;
    public ParseTree timeArraySizeParseTree = null;

    // These are for:
    // (1) normal Svar if timeArraySize=0
    //		e.g., define someSvar { value 1 }
    //
    // (2) Svar time array if future expressions are the same
    //      e.g.,  define(3) someSvar { value $m }
    public List<String> caseName = new ArrayList<>();
    public List<String> caseCondition = new ArrayList<>();
    public List<ParseTree> caseConditionParseTree = new ArrayList<>();
    public List<String> caseExpression = new ArrayList<>();
    public List<ParseTree> caseExpressionParseTree = new ArrayList<>();


    // --------------------
    // --- CONSTRUCTORS
    // --------------------

    // Copy contents of an svar into another one
    public Svar copyOf() {
        Svar svarCopy = new Svar();

        svarCopy.dependants = this.dependants;
        svarCopy.neededVarInCycleSet = this.neededVarInCycleSet;
        svarCopy.needVarFromEarlierCycle =this.needVarFromEarlierCycle;
        svarCopy.data = this.data.copyOf();
        svarCopy.timeArraySize = this.timeArraySize;
        svarCopy.timeArraySizeParseTree = this.timeArraySizeParseTree;
        svarCopy.caseName = this.caseName;
        svarCopy.caseCondition = this.caseCondition;
        svarCopy.caseConditionParseTree = this.caseConditionParseTree;
        svarCopy.caseExpression = this.caseExpression;
        svarCopy.caseExpressionParseTree = this.caseExpressionParseTree;

        return svarCopy;
    }


    // --------------------
    // --- SETTERS
    // --------------------

    // Set WRESL file related data
    public void setFileData(String wreslFile, int line) {
        this.fromWresl = wreslFile;
        this.line = line;
    }

    // Set case condition related data
    public void addCaseData(String caseName, String caseCondition, String caseExpression, ParseTree caseConditionParseTree, ParseTree caseExpressionParseTree) {
        this.caseName.add(caseName);
        this.caseCondition.add(caseCondition);
        this.caseExpression.add(caseExpression);
        this.caseConditionParseTree.add(caseConditionParseTree);
        this.caseExpressionParseTree.add(caseExpressionParseTree);
    }

    // Set data
    public void setData(IntDouble data) {
        this.data = data;
    }

    // Set name
    public void setName(String name) {
        this.name = name;
    }


    // --------------------
    // --- GETTERS
    // --------------------

    // Get data
    public IntDouble getData() {
        return this.data;
    }




}

package gov.ca.water.wresl.domain;

import gov.ca.water.utilities.ParallelVars;
import gov.ca.water.utilities.Param;
import gov.ca.water.wresl.errors.EvaluationErrorException;
import gov.ca.water.wresl.parsing.Evaluator;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.Serializable;
import java.util.*;

public class Svar extends WRESLComponent implements Serializable {
    private static final long serialVersionUID = 1L;

    // These properties are the same for all Svar time array
    public String dssBPart = Param.undefined;
    public String format = Param.undefined;
    public String kind = Param.undefined;
    public String units = Param.undefined;
    public String convertToUnits = Param.undefined;
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


    // These maps are for time array of Svar if future definitions have different expressions
    //      example 1:  define(3) someSvar {
    //                                      (0) { value 99   }
    //                                      (1) { value a+b  }
    //                                      (2) { value 7    }
    //                                      (3) { value 2*k  }   }
    //      example 2:  define(3) someSvar {
    //                                      (0:2) { value 99   }
    //                                      (3)   { value 2*k  }   }
    public Map<Integer, ArrayList<String>> timeMap_caseName = new HashMap<>();
    public Map<Integer, ArrayList<String>> timeMap_caseCondition = new HashMap<>();
    public Map<Integer, ArrayList<String>> timeMap_caseExpression = new HashMap<>();


    // --------------------
    // --- CONSTRUCTORS
    // --------------------

    // Copy contents of an svar into another one
    public Svar copyOf() {
        Svar svarCopy = new Svar();

        svarCopy.dssBPart = this.dssBPart;
        svarCopy.format = this.format;
        svarCopy.kind = this.kind;
        svarCopy.units = this.units;
        svarCopy.convertToUnits = this.convertToUnits;
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
        svarCopy.timeMap_caseName = this.timeMap_caseName;
        svarCopy.timeMap_caseCondition = this.timeMap_caseCondition;
        svarCopy.timeMap_caseExpression = this.timeMap_caseExpression;

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

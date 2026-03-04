package gov.ca.water.wresl.domain;

import gov.ca.water.wresl.compile.Param;

import java.io.Serializable;
import java.util.*;

public class Svar extends WRIMSComponent implements Serializable {
    private static final long serialVersionUID = 1L;

    // These properties are the same for all Svar time array
    public String fromWresl = Param.undefined;
    public int line = 1;
    public String scope = Param.undefined;
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
    public String timeArraySize = "0";
    public ValueEvaluatorParser_DUMMY timeArraySizeParser;

    // These are for:
    // (1) normal Svar if timeArraySize=0
    //		e.g., define someSvar { value 1 }
    //
    // (2) Svar time array if future expressions are the same
    //      e.g.,  define(3) someSvar { value $m }
    public List<String> caseName = new ArrayList<>();
    public List<String> caseCondition = new ArrayList<>();
    public List<ValueEvaluatorParser_DUMMY> caseConditionParsers = new ArrayList<>();
    public List<String> caseExpression = new ArrayList<>();
    public List<ValueEvaluatorParser_DUMMY> caseExpressionParsers = new ArrayList<>();


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
    // --- SETTERS
    // --------------------

    // Set WRESL file related data
    public void setFileData(String wreslFile, int line) {
        this.fromWresl = wreslFile;
        this.line = line;
    }

    // Set case condition related data
    public void addCaseData(String caseName, String caseCondition, String caseExpression) {
        this.caseName.add(caseName);
        this.caseCondition.add(caseCondition);
        this.caseExpression.add(caseExpression);
    }
}

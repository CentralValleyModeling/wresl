package gov.ca.water.wresl.parsing;

import gov.ca.water.wresl.domain.WRESLComponent;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Handles both regular CASE expressions and those appearing in conditional GOAL statements
public class WRESL_CaseData extends WRESLComponent {
    public String caseCondition;
    public ParseTree caseConditionTree;
    public List<String> caseExpressionList;
    public List<ParseTree> caseExpressionTreeList;



    public WRESL_CaseData() {
        this.caseCondition = null;
        this.caseConditionTree = null;
        this.caseExpressionList = new ArrayList<>();
        this.caseExpressionTreeList = new ArrayList<>();
    }


    public WRESL_CaseData(String caseCondition,
                          ParseTree caseConditionTree,
                          List<String> caseExpressionList,
                          List<ParseTree> caseExpressionTreeList) {
        this.caseCondition = caseCondition;
        this.caseConditionTree = caseConditionTree;
        this.caseExpressionList = caseExpressionList;
        this.caseExpressionTreeList = caseExpressionTreeList;
    }
}

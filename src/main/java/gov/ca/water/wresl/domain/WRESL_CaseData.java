package gov.ca.water.wresl.domain;

import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;
import java.util.Map;

// Handles both regular CASE expressions and those appearing in conditional GOAL statements
public class WRESL_CaseData extends WRESLComponent {
    public String caseCondition;
    public ParseTree caseConditionTree;
    public List<String> caseExpressionList;
    public List<ParseTree> caseExpressionTreeList;
    public List<String> SlackSurplusDvarList;
    public Map<String,String> SlackSurplusDvarWeightMap;

    public WRESL_CaseData(String caseCondition,
                          ParseTree caseConditionTree,
                          List<String> caseExpressionList,
                          List<ParseTree> caseExpressionTreeList,
                          List<String> caseSlackSurplusDvarList,
                          Map<String,String> caseSlackSurplusDvarWeightMap) {
        this.caseCondition = caseCondition;
        this.caseConditionTree = caseConditionTree;
        this.caseExpressionList = caseExpressionList;
        this.caseExpressionTreeList = caseExpressionTreeList;
        this.SlackSurplusDvarList = caseSlackSurplusDvarList;
        this.SlackSurplusDvarWeightMap = caseSlackSurplusDvarWeightMap;
    }
}

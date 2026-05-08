package gov.ca.water.wresl.domain;

import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.List;

public class WRESL_CaseData extends WRESLComponent {
    public String caseCondition;
    public ParseTree caseConditionTree;
    public List<String> caseExpressionList;
    public List<ParseTree> caseExpressionTreeList;
    public List<String> SlackSurplusList;
    public List<String> SlackSurplusWeightList;

    public WRESL_CaseData(String caseCondition,
                          ParseTree caseConditionTree,
                          List<String> caseExpressionList,
                          List<ParseTree> caseExpressionTreeList,
                          List<String> caseSlackSurplusList,
                          List<String> caseSlackSurplusWeightList) {
        this.caseCondition = caseCondition;
        this.caseConditionTree = caseConditionTree;
        this.caseExpressionList = caseExpressionList;
        this.caseExpressionTreeList = caseExpressionTreeList;
        this.SlackSurplusList = caseSlackSurplusList;
        this.SlackSurplusWeightList = caseSlackSurplusWeightList;
    }
}

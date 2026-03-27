package gov.ca.water.wresl.domain;

import org.antlr.v4.runtime.tree.ParseTree;



public class WRESL_CaseData extends WRESLComponent {
    public String caseCondition;
    public String caseExpression;
    public ParseTree caseConditionTree;
    public ParseTree caseExpressionTree;

    public WRESL_CaseData(String caseCondition, String caseExpression, ParseTree caseConditionTree, ParseTree caseExpressionTree) {
        this.caseCondition = caseCondition;
        this.caseExpression = caseExpression;
        this.caseConditionTree = caseConditionTree;
        this.caseExpressionTree = caseExpressionTree;
    }
}

package gov.ca.water.wresl.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class Goal extends WRIMSComponent implements Serializable {
    private static final long serialVersionUID = 1L;

    public String scope;
    public String lhs;
    public ArrayList<String> caseName;

    public ArrayList<Map<String,String>> dvarWeightMapList;
    public ArrayList<ArrayList<String>> dvarSlackSurplusList;
    public ArrayList<String> dvarName; // from slack or surplus
    public ArrayList<String> dvarWeight; // for the slack or surplus. Negative penalty leads to positive weight
    public ArrayList<String> caseCondition;
    public ArrayList<ValueEvaluatorParser_DUMMY> caseConditionParsers;
    public ArrayList<String> caseExpression;
    public Set<String> expressionDependants;
    public ArrayList<EvaluatorParser_DUMMY> caseExpressionParsers;
    public String fromWresl;
    public int line=1;
    public Set<String> neededVarInCycleSet;
    public boolean needVarFromEarlierCycle;

    // default is zero
    public String timeArraySize;
    public ValueEvaluatorParser_DUMMY timeArraySizeParser;

}

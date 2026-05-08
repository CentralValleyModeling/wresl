package gov.ca.water.wresl.domain;

import org.antlr.v4.runtime.tree.ParseTree;

import java.io.Serializable;
import java.util.*;

public class Goal extends WRESLComponent implements Serializable {
    private static final long serialVersionUID = 1L;

    public List<String> caseName = new ArrayList<>();

    public List<Map<String,String>> dvarWeightMapList = new ArrayList<>();
    public List<List<String>> dvarSlackSurplusList = new ArrayList<>();
    public List<String> dvarName = new ArrayList<>(); // from slack or surplus
    public List<String> dvarWeight = new ArrayList<>(); // for the slack or surplus. Negative penalty leads to positive weight
    public List<String> caseCondition = new ArrayList<>();
    public List<ParseTree> caseConditionParseTrees = new ArrayList<>();
    public List<String> caseExpression = new ArrayList<>();
    public Set<String> expressionDependants = new HashSet<>();
    public List<ParseTree> caseExpressionParseTrees = new ArrayList<>();
    public Set<String> neededVarInCycleSet = new HashSet<>();
    public boolean needVarFromEarlierCycle = false;

    // default is zero
    public String timeArraySize = Param.zero;
    public ParseTree timeArraySizeParseTree;

}

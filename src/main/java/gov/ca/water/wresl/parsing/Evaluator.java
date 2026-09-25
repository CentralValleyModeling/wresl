package gov.ca.water.wresl.parsing;

import gov.ca.water.io.DSS.DssOperations;
import gov.ca.water.utilities.ParallelVars;
import gov.ca.water.utilities.Param;
import gov.ca.water.utilities.TimeOperations;
import gov.ca.water.wresl.domain.*;
import gov.ca.water.wresl.errors.EvaluationErrorException;
import gov.ca.water.wresl.errors.SyntaxErrorException;
import gov.ca.water.wresl.grammar.wreslBaseVisitor;
import gov.ca.water.wresl.grammar.wreslLexer;
import gov.ca.water.wresl.grammar.wreslParser;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.*;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static gov.ca.water.wresl.parsing.Utilities.generateExpressionParseTree;
import static gov.ca.water.wresl.parsing.Utilities.getWreslText;

// Package-private class
public class Evaluator extends wreslBaseVisitor<IntDouble> {

    private static String absReferencePath = null;                            // Absolute path of the folder that the main WRESL file is located
    private final Map<String,LookUpTable> tableSeries = new HashMap<>();      // Map that stores lookup table data

    // Variables that are used for common data used by methods
    private StudyDataSet sds = new StudyDataSet();                 // This holds all the information for the study
    private ModelDataSet currentModelDataSet = new ModelDataSet(); // This holds the data for the current model we are working on
    private int futureArrayIndex = 0;                              // Future array index to be used when future arrays are utilized

    // Class describing a lookup table
    private class LookUpTable {
        private String name = null;
        private Map<String, Integer> field = new HashMap<>();
        private List<Number[]> data = new ArrayList<>();
    }

    // Enumerators
    private enum Logical {
        TRUE(1),
        FALSE(-1);
        private final int value;
        Logical(int value) {this.value = value;}
    }

    private enum FlowConversion {
        CFS_TAF,
        CFS_AF,
        TAF_CFS,
        AF_CFS
    }

    // Runtime data
    private int currentDay;
    private int currentMonth;
    private int currentYear;

    // Singleton constructor
    // This setup allows us to treat Evaluator class as if it is a static class (even though
    //   it cannot be static because it extends non-static wreslBaseVisitor class).
    //   This way, client code call Evaluator methods as if they are utility methods.
    private Evaluator() {}
    private static final Evaluator INSTANCE = new Evaluator();


    // ------------------------------------------------------------
    // --- SET REFERENCE FOLDER (USED TO LOCATE LOOKUP TABLE FILES)
    // ------------------------------------------------------------
    public static void setReferencePath(String referenceFolder) {
        INSTANCE.absReferencePath = referenceFolder;
    }


    // ------------------------------------------------------------
    // --- PROCESS A MODEL
    // ------------------------------------------------------------
    // Gateway method to process a model stored in a ModelDataSet object
    public static boolean processModel(StudyDataSet sds, int modelIndex, int currentDay, int currentMonth, int currentYear, int nThreads, boolean showRunTimeMessage) {
        // Set simulation time related parameters
        INSTANCE.currentDay = currentDay;
        INSTANCE.currentMonth = currentMonth;
        INSTANCE.currentYear = currentYear;

        // Store StudyDataSet in common memory to be used by visitor methods
        INSTANCE.sds = sds;

        // Check if condition to process model holds true
        ParseTree modelConditionParseTree = sds.getModelConditionParseTree(modelIndex);
        boolean toBeProcessed = Evaluator.evaluateCondition(null, modelConditionParseTree);
        if (!toBeProcessed) { return false; }

        // Retrieve ModelDataSet
        INSTANCE.currentModelDataSet = sds.getModelDataSet(modelIndex);

        // Clear future arrays
        INSTANCE.currentModelDataSet.clearFutureSvMap();
        INSTANCE.currentModelDataSet.clearFutureAsMap();

        // Reset DVARs to be included in the solution
        INSTANCE.currentModelDataSet.resetConditionalDvarsForSolution();

        // Process Svars
        INSTANCE.processSvars(null, INSTANCE.currentModelDataSet.svList, INSTANCE.currentModelDataSet.svMap, showRunTimeMessage);
        if (showRunTimeMessage) System.out.println("Completed Svar processing.");

        // Process Goals
        INSTANCE.processGoals(nThreads, showRunTimeMessage);

        // Process Dvars
        INSTANCE.processDvars(nThreads, showRunTimeMessage);
        if (showRunTimeMessage) System.out.println("Completed Dvar processing.");

        // Process Weights
        INSTANCE.processWeights(nThreads, showRunTimeMessage);

        return true;
    }

    // Process Aliases
    public static void processAliases(StudyDataSet sds, int modelIndex, boolean showRunTimeMessage) {
        // Store StudyDataSet in common memory to be used by visitor methods
        INSTANCE.sds = sds;

        // Retrieve model data
        INSTANCE.currentModelDataSet = sds.getModelDataSet(modelIndex);

        // Loop through ALIASes; they need to be processed in order that they were defined in WRESL
        List<String> asList = INSTANCE.currentModelDataSet.getAliasList();
        Map<String, Alias> asMap = INSTANCE.currentModelDataSet.getAliasMap();
        for (String asName : asList) {
            Alias as = asMap.get(asName);
            if (showRunTimeMessage) System.out.println("Processing alias " + asName);

            // Process alias at current time
            IntDouble data = INSTANCE.visit(as.expressionParseTree);
            as.addData(data);

            // Process future-array alias

        }

    }

    // Process Svars
    public static void processSvars(StudyDataSet sds, List<String> svList, Map<String, Svar> svMap, boolean showRunTimeMessage) {
        // Store sds in common data space so it can be used by all visitor methods
        if (sds != null) INSTANCE.sds = sds;

        for (String svName: svList) {
            if (showRunTimeMessage) System.out.println("Processing svar "+svName);
            Svar svar = svMap.get(svName);
            System.out.println(svName);

            // Process svar
            INSTANCE.futureArrayIndex = 0;
            INSTANCE.processSvar(svar);

            // If svar utilizes future arrays, process those arrays
            if (svar.timeArraySizeParseTree != null) {
                IntDouble futureArraySize = INSTANCE.visit(svar.timeArraySizeParseTree);
                for (int indx=1; indx<=futureArraySize.getValue().intValue(); indx++) {
                    Svar futureSvar = svar.copyOf();
                    String futureSvName = svName + "__fut__" + indx;
                    futureSvar.setName(futureSvName);
                    INSTANCE.futureArrayIndex = indx;
                    INSTANCE.processSvar(futureSvar);
                    INSTANCE.currentModelDataSet.addFutureSvar(futureSvar);
                }
            }
        }
    }

    // Process a single Svar
    private static void processSvar(Svar svar) throws EvaluationErrorException {
        int index = -1;
        // Process case conditions and figure out which case expression to use
        if (svar.caseConditionParseTree == null) {
            index = 0;
        } else {
            for (int i = 0; i < svar.caseName.size(); i++) {
                // Process case conditions until one of them turns true
                ParseTree caseConditionParseTree = svar.caseConditionParseTree.get(i);
                if (caseConditionParseTree == null) {
                    index = i;
                } else {
                    if (INSTANCE.evaluateCondition(null, caseConditionParseTree)) {
                        index = i;
                        break;
                    }
                }
            }
        }
        // If index is still -1, case conditions were not defined properly; generate error
        if (index == -1) {
            throw new EvaluationErrorException(svar.fromWresl, svar.line, "A viable condition cannot be found for Svar " + svar.name + " defined in file " + svar.fromWresl + " at line " + svar.line + "!");
        }
        // We know which expression to evaluate; evaluate caseExpression
        IntDouble data = INSTANCE.visit(svar.caseExpressionParseTree.get(index));
        svar.setData(data);
    }

    // Process Dvars
    private void processDvars(int nThreads, boolean showRunTimeMessage) {

        // Initialize
        List<String> timeArrayDvList = new ArrayList<>();
        List<String> dvTimeArrayList = new ArrayList<>();
        List<Dvar> dvList = INSTANCE.currentModelDataSet.getDvars();
        int threshold = (int) Math.ceil(dvList.size()/nThreads);
        ForkJoinPool pool = new ForkJoinPool(nThreads);

        // Instantiate parallel work
        ParallelAction<Dvar> task = new ParallelAction<>(
                dvList,
                0,
                dvList.size(),
                threshold,
                item -> processDvar(item, timeArrayDvList, dvTimeArrayList, showRunTimeMessage)
        );
        pool.invoke(task);

        // Store time array related data
        INSTANCE.currentModelDataSet.setTimeArrayDvList(timeArrayDvList);
        INSTANCE.currentModelDataSet.setDvTimeArrayList(dvTimeArrayList);
    }

    // Process a single Dvar
    private void processDvar(Dvar dvar, List<String> timeArrayDvList, List<String> dvTimeArrayList, boolean showRunTimeMessage) {
        if (showRunTimeMessage) System.out.println("Processing DVAR " + dvar.name);

        // Process lower bound
        if (dvar.lowerBoundExpressionParseTree != null) {
            dvar.lowerBoundValue = visit(dvar.lowerBoundExpressionParseTree).getValue().doubleValue();
            if (dvar.lowerBoundValue == null) {
                throw new EvaluationErrorException(dvar.fromWresl, dvar.line, "Error in evaluating the lower bound of DVAR " + dvar.name + "!");
            }
        }

        // Process upper bound
        if (dvar.upperBoundExpressionParseTree != null) {
            dvar.upperBoundValue = visit(dvar.upperBoundExpressionParseTree).getValue().doubleValue();
            if (dvar.upperBoundValue == null) {
                throw new EvaluationErrorException(dvar.fromWresl, dvar.line, "Error in evaluating the upper bound of DVAR " + dvar.name + "!");
            }
        }

        // Return if there is no timearray
        if (dvar.timeArraySizeExpressionParseTree == null) { return; }

        // Otherwise, process time array size
        String dvName = dvar.name;
        int timeArraySize = visit(dvar.timeArraySizeExpressionParseTree).getValue().intValue();
        if (timeArraySize != 0) {
            if (timeArrayDvList.contains(dvName)) {
                timeArrayDvList.add(dvName);

                for (int timeIndex=1; timeIndex<=timeArraySize; timeIndex++) {
                    Dvar newDvar=new Dvar();
                    String newDvarName = dvName + "__fut__" + timeIndex;
                    newDvar.setName(newDvarName);
                    newDvar.setKind(dvar.kind);
                    newDvar.setUnits(dvar.units);
                    newDvar.setInteger(dvar.integer);

                    newDvar.lowerBoundValue = visit(dvar.lowerBoundExpressionParseTree).getValue().doubleValue();
                    if (newDvar.lowerBoundValue == null) {
                        throw new EvaluationErrorException(dvar.fromWresl, dvar.line, "Error in evaluating the lower bound of time array DVAR " + dvar.name + "!");
                    }

                    newDvar.upperBoundValue = visit(dvar.upperBoundExpressionParseTree).getValue().doubleValue();
                    if (newDvar.upperBoundValue == null) {
                        throw new EvaluationErrorException(dvar.fromWresl, dvar.line, "Error in evaluating the upper bound of time array DVAR " + dvar.name + "!");
                    }

                    dvTimeArrayList.add(newDvarName);
                }
            }
        }
    }

    // Process Goals
    private void processGoals(int nThreads, boolean showRunTimeMessage) {
        List<Goal> goalList = INSTANCE.currentModelDataSet.getGoalList();
        int threshold = (int) Math.ceil(goalList.size()/nThreads);
        ForkJoinPool pool = new ForkJoinPool(nThreads);

        // Instantiate parallel work
        ParallelAction<Goal> task = new ParallelAction<>(
                goalList,
                0,
                goalList.size(),
                threshold,
                item -> processGoal(item, showRunTimeMessage)
        );
        pool.invoke(task);
    }

    // Process a single Goal
    private void processGoal(Goal goal, boolean showRunTimeMessage) {
        if (showRunTimeMessage) System.out.println("Processing constraint " + goal.name);

        // Process time array
        if (goal.timeArraySizeParseTree != null) {
            int timeArraySize = visit(goal.timeArraySizeParseTree).getValue().intValue();
            for (int timeIndex=1; timeIndex<=timeArraySize; timeIndex++)  {
                Goal newGoal = new Goal();
                String newGoalName = goal.name + "__fut__" + timeIndex;
                newGoal.setName(newGoalName);

                // Find the case for which we are going to compute goal
                int index = -1;
                for (int caseIndex=0; caseIndex<goal.caseConditionParseTrees.size(); caseIndex++) {
                    if (INSTANCE.evaluateCondition(null,goal.caseConditionParseTrees.get(caseIndex))) {
                        index = caseIndex;
                        break;
                    }
                }
                if (index == -1) {
                    // It is okay not to find a valid case to evaluate; continue to next loop item
                    goal.setSolverData(null);
                    continue;
                }

                // Process goal expression
                GoalEvaluator goalEvaluator = new GoalEvaluator();
                EvalConstraint constraint = goalEvaluator.evaluate(goal.name, goal.fromWresl, goal.line, goal.goalExpressionParseTrees.get(index));
                goal.setSolverData(constraint);

                // Include associated DVARs in the solution
                for (String dvarName : constraint.getMultipliers().keySet()) {
                    INSTANCE.currentModelDataSet.includeDvarInSolution(dvarName);
                }
            }
        }

        // Process goal itself
        // --------------------
        // Find the case for which we are going to compute goal
        int index;
        if (goal.caseConditionParseTrees == null) {
            index = 0;
        } else {
            index = -1;
            for (int caseIndex = 0; caseIndex < goal.caseConditionParseTrees.size(); caseIndex++) {
                if (INSTANCE.evaluateCondition(null, goal.caseConditionParseTrees.get(caseIndex))) {
                    index = caseIndex;
                    break;
                }
            }
            if (index == -1) {
                // It is okay not to find a case to evaluate; simply return
                goal.setSolverData(null);
                return;
            }
        }

        // Process goal expression
        GoalEvaluator goalBuilder = new GoalEvaluator();
        EvalConstraint constraint = goalBuilder.evaluate(goal.name, goal.fromWresl, goal.line, goal.goalExpressionParseTrees.get(index));
        goal.setSolverData(constraint);

        // Include associated DVARs in the solution
        for (String dvarName : constraint.getMultipliers().keySet()) {
            INSTANCE.currentModelDataSet.includeDvarInSolution(dvarName);
        }
    }

    // Process Weights
    private void processWeights(int nThreads, boolean showRunTimeMessage) {
        // Non-conditional weights
        List<WeightElement> weightList = INSTANCE.currentModelDataSet.getWeightList();
        int threshold = (int) Math.ceil(weightList.size()/nThreads);
        ForkJoinPool pool = new ForkJoinPool(nThreads);

        // Instantiate parallel work
        ParallelAction<WeightElement> task = new ParallelAction<>(
                weightList,
                0,
                weightList.size(),
                threshold,
                item -> processWeight(item, showRunTimeMessage)
        );
        pool.invoke(task);

    }

    // Process a single weight
    private void processWeight(WeightElement weight, boolean showRunTimeMessage) {
        if (showRunTimeMessage) System.out.println("Processing weight " + weight.getName());

        // Process time array
        if (weight.timeArraySizeParseTree != null) {
            int timeArraySize = visit(weight.timeArraySizeParseTree).getValue().intValue();
            for (int timeIndex=1; timeIndex<=timeArraySize; timeIndex++)  {
                WeightElement newWeight = new WeightElement();
                String newWeightName = weight.name + "__fut__" + timeArraySize;
                newWeight.setName(newWeightName);
            }
        }

        // Process weight itself (only if parser tree is not null; otherwise, its value has already been computed during initial parsing)
        if (weight.weightParseTree != null) {
            IntDouble data = INSTANCE.visit(weight.weightParseTree);
            weight.setValue(data.getValue().doubleValue());
        }
    }


    // ------------------------------------------------------------
    // --- EVALUATE AN EXPRESSION PROVIDED AS A PARSE TREE
    // ------------------------------------------------------------
    public static IntDouble evaluateExpression(StudyDataSet sds, int modelIndex, int currentDay, int currentMonth, int currentYear, ParseTree expression) {
        INSTANCE.currentDay = currentDay;
        INSTANCE.currentMonth = currentMonth;
        INSTANCE.currentYear = currentYear;

        INSTANCE.sds = sds;
        INSTANCE.currentModelDataSet = sds.getModelDataSet(modelIndex);

        return INSTANCE.visit(expression);
    }


    // ------------------------------------------------------------
    // --- EVALUATE A CONDITION
    // ------------------------------------------------------------
    public static boolean evaluateCondition(StudyDataSet sds, ParseTree expCompareParseTree) {
        // If null ParseTree; that means condition always evaluates to true
        if (expCompareParseTree == null) {return true; }

        // Store StudyDataSet in common memory to be used by visitor methods
        if (sds != null) INSTANCE.sds = sds;

        IntDouble condition = INSTANCE.visit(expCompareParseTree);
        if (condition.getValue().intValue() == Logical.TRUE.value) {
            return true;
        } else {
            return false;
        }
    }


    // ------------------------------------------------------------
    // --- EXPRESSION VISITOR METHODS
    // ------------------------------------------------------------
    @Override
    // expressionComparison
    public IntDouble visitExpressionComparison(wreslParser.ExpressionComparisonContext ctx) {
        // Retrieve left and right values as IntDouble
        IntDouble valueLeft = visit(ctx.expression(0));
        IntDouble valueRight = visit(ctx.expression(1));

        // Retrieve actual numbers from IntDouble as double datatype (since this also covers integers)
        double doubleLeft = valueLeft.getValue().doubleValue();
        double doubleRight = valueRight.getValue().doubleValue();

        // Compare based on comparison operation
        int compareSign = ctx.opCompare().getStart().getType();
        switch (compareSign) {
            // EQUALS_SIGN, DOUBLE_EQUAL
            case wreslLexer.EQUALS_SIGN, wreslLexer.DOUBLE_EQUAL -> {
                if (doubleLeft == doubleRight) {
                    return new IntDouble(Integer.valueOf(Logical.TRUE.value),true);
                }
                else {
                    return new IntDouble(Integer.valueOf(Logical.FALSE.value),true);
                }
            }
            // GREATER_THAN
            case wreslLexer.GREATER_THAN -> {
                if (doubleLeft > doubleRight) {
                    return new IntDouble(Integer.valueOf(Logical.TRUE.value),true);
                }
                else {
                    return new IntDouble(Integer.valueOf(Logical.FALSE.value),true);
                }
            }
            // GREATER_THAN_OR_EQUAL
            case wreslLexer.GREATER_THAN_OR_EQUAL -> {
                if (doubleLeft >= doubleRight) {
                    return new IntDouble(Integer.valueOf(Logical.TRUE.value),true);
                }
                else {
                    return new IntDouble(Integer.valueOf(Logical.FALSE.value),true);
                }
            }
            // LESS_THAN
            case wreslLexer.LESS_THAN -> {
                if (doubleLeft < doubleRight) {
                    return new IntDouble(Integer.valueOf(Logical.TRUE.value),true);
                }
                else {
                    return new IntDouble(Integer.valueOf(Logical.FALSE.value),true);
                }
            }
            // LESS_THAN_OR_EQUAL
            case wreslLexer.LESS_THAN_OR_EQUAL -> {
                if (doubleLeft <= doubleRight) {
                    return new IntDouble(Integer.valueOf(Logical.TRUE.value),true);
                }
                else {
                    return new IntDouble(Integer.valueOf(Logical.FALSE.value),true);
                }
            }
            // NOT_EQUAL
            case wreslLexer.NOT_EQUAL -> {
                if (doubleLeft != doubleRight) {
                    return new IntDouble(Integer.valueOf(Logical.TRUE.value),true);
                }
                else {
                    return new IntDouble(Integer.valueOf(Logical.FALSE.value),true);
                }
            }
            // default; should not need this
            default -> {return null;}

        }
    }

    @Override
    // expressionAddSub
    public IntDouble visitExpressionAddSub(wreslParser.ExpressionAddSubContext ctx) {
        // Retrieve left and right values as IntDouble
        IntDouble valueLeft = visit(ctx.expression(0));
        IntDouble valueRight = visit(ctx.expression(1));

        // Retrieve actual numbers from IntDouble as double datatype (since this also covers integers)
        double doubleLeft = valueLeft.getValue().doubleValue();
        double doubleRight = valueRight.getValue().doubleValue();

        // Are left and right values integer?
        boolean isLeftValueInt = valueLeft.isInt();
        boolean isRightValueInt = valueRight.isInt();

        // Addition
        if (ctx.opAdditionSubtraction().PLUS() != null) {
            // Result of operation is integer
            if (isLeftValueInt && isRightValueInt) {
                Number intResult = Integer.valueOf((int)doubleLeft + (int)doubleRight);
                return new IntDouble(intResult, true);
            }
            // Result of operation is double
            else {
                Number doubleResult = Double.valueOf(doubleLeft + doubleRight);
                return new IntDouble(doubleResult, false);
            }
        }
        // Subtraction
        else {
            // Result of operation is integer
            if (isLeftValueInt && isRightValueInt) {
                Number intResult = Integer.valueOf((int)doubleLeft - (int)doubleRight);
                return new IntDouble(intResult, true);
            }
            // Result of operation is double
            else {
                Number doubleResult = Double.valueOf(doubleLeft - doubleRight);
                return new IntDouble(doubleResult, false);
            }
        }
    }

    @Override
    // expressionMultDiv
    public IntDouble visitExpressionMultDiv(wreslParser.ExpressionMultDivContext ctx) {
        // Retrieve left and right values as IntDouble
        IntDouble valueLeft = visit(ctx.expression(0));
        IntDouble valueRight = visit(ctx.expression(1));

        // Retrieve actual numbers from IntDouble as double datatype (since this also covers integers)
        double doubleLeft = valueLeft.getValue().doubleValue();
        double doubleRight = valueRight.getValue().doubleValue();

        // Are left and right values integer?
        boolean isLeftValueInt = valueLeft.isInt();
        boolean isRightValueInt = valueRight.isInt();

        // Multiplication
        if (ctx.opMultiplicationDivision().MULT() != null) {
            // Result of operation is integer
            if (isLeftValueInt && isRightValueInt) {
                Number intResult = Integer.valueOf((int)doubleLeft * (int)doubleRight);
                return new IntDouble(intResult, true);
            }
            // Result of operation is double
            else {
                Number doubleResult = Double.valueOf(doubleLeft * doubleRight);
                return new IntDouble(doubleResult, false);
            }
        }
        // Division
        else {
            // Result of operation is integer
            if (isLeftValueInt && isRightValueInt) {
                Number intResult = Integer.valueOf((int)doubleLeft / (int)doubleRight);
                return new IntDouble(intResult, true);
            }
            // Result of operation is double
            else {
                Number doubleResult = Double.valueOf(doubleLeft / doubleRight);
                return new IntDouble(doubleResult, false);
            }
        }
    }

    @Override
    // expressionNot
    public IntDouble visitExpressionNot(wreslParser.ExpressionNotContext ctx) throws EvaluationErrorException {
        IntDouble logicalResult = visit(ctx.expression());

        // Check that returned value is understood as a logical value
        if (logicalResult.getValue().intValue()!=Logical.TRUE.value && logicalResult.getValue().intValue()!=Logical.FALSE.value) {
            throw new EvaluationErrorException("Error evaluating expression: " + ctx.getText());
        }

        // Return negated value
        if (logicalResult.getValue().intValue() == Logical.TRUE.value) {
            return new IntDouble(Logical.FALSE.value, true);
        }
        else {
            return new IntDouble(Logical.TRUE.value, true);
        }
    }

    @Override
    // expressionLogical
    public IntDouble visitExpressionLogical(wreslParser.ExpressionLogicalContext ctx) {
        // Process left and right logical operations
        IntDouble valueLeft = visit(ctx.expression(0));
        IntDouble valueRight = visit(ctx.expression(1));
        int intLeft = valueLeft.getValue().intValue();
        int intRight = valueRight.getValue().intValue();

        // Process logical operation
        switch (getWreslText(ctx.opLogical())) {
            // AND
            case ".and." -> {
                if (intLeft==Logical.TRUE.value && intRight==Logical.TRUE.value) {
                    return new IntDouble(Integer.valueOf(Logical.TRUE.value),true);
                }
                else {
                    return new IntDouble(Integer.valueOf(Logical.FALSE.value),true);
                }
            }
            // OR
            case ".or." -> {
                if (intLeft==Logical.TRUE.value || intRight==Logical.TRUE.value) {
                    return new IntDouble(Integer.valueOf(Logical.TRUE.value),true);
                }
                else {
                    return new IntDouble(Integer.valueOf(Logical.FALSE.value),true);
                }
            }
            // default; should not need this
            default -> {return null;}
        }
    }

    @Override
    // expressionSum
    public IntDouble visitExpressionSum(wreslParser.ExpressionSumContext ctx) {
        int iBegin;
        int iEnd;
        int iStep = 1;

        // Retrieve SUM begin index
        IntDouble indexBegin = visit(ctx.sumExpressionBody().sumBegin());
        if (indexBegin == null) {
            return null;
        } else {
            iBegin = indexBegin.getValue().intValue();
        }

        // Retrieve SUM end index
        IntDouble indexEnd = visit(ctx.sumExpressionBody().sumEnd());
        if (indexEnd == null) {
            return null;
        } else {
            iEnd = indexEnd.getValue().intValue();
        }

        // Retrieve SUM step size
        if (ctx.sumExpressionBody().sumStep() != null) {
            IntDouble step = visit(ctx.sumExpressionBody().sumStep());
            if (step == null) {
                return null;
            } else {
                iStep = step.getValue().intValue();
            }
        }

        // Loop through SUM
        String sumIndex = "(" + getWreslText(ctx.sumExpressionBody().OBJECT_NAME()) + ")";
        double sum =0.0;
        IntDouble data;
        for (int i=iBegin; i<=iEnd; i++) {
            // Create a new parse tree for the accumulating expression with the index value specified
            String accumExpression = getWreslText(ctx.sumExpressionBody().accumulatingExpression());
            String accumExpressionMod = accumExpression.replace(sumIndex, "("+i+")");
            ParseTree accumParseTree = generateExpressionParseTree(accumExpressionMod);

            // Retrieve value and add it to sum
            data = visit(accumParseTree);
            if (data == null) { return null; }
            sum = sum + data.getValue().doubleValue();
        }

        return new IntDouble(sum,false);
    }

    @Override
    // expressionSigned
    public IntDouble visitExpressionSigned(wreslParser.ExpressionSignedContext ctx) {
        IntDouble value = visit(ctx.expression());

        // If sign is negative, multiply value with -1
        boolean isNegative = false;
        if (ctx.MINUS() != null) { isNegative = true; }
        if (isNegative) {
            if (value.isInt()) {
                Number valueTemp = Integer.valueOf(-value.getValue().intValue());
                return new IntDouble(valueTemp, true);
            }
            else {
                Number valueTemp = Double.valueOf(-value.getValue().doubleValue());
                return new IntDouble(valueTemp, false);
            }
        }

        // Otherwise, return value as is
        return value;
    }

    @Override
    public IntDouble visitExpressionCall(wreslParser.ExpressionCallContext ctx) throws EvaluationErrorException {
        // Retrieve arguments
        ArrayList<IntDouble> arguments = new ArrayList<>();
        if (ctx.arguments() != null) {
            for (int i=0; i<ctx.arguments().expression().size(); i++) {
                arguments.add(visit(ctx.arguments().expression(i)));
            }
        }

        // If this is a call to a predefined function
        if (ctx.preDefinedFunction() != null) {
            int function = ctx.preDefinedFunction().getStart().getType();
            Number value;
            switch (function) {
                case wreslLexer.F_ABSOLUTE_VALUE -> {
                    if (arguments.get(0).isInt()) {
                        value = Math.abs(arguments.get(0).getValue().intValue());
                        return new IntDouble(value, true);
                    } else {
                        value = Math.abs(arguments.get(0).getValue().doubleValue());
                        return new IntDouble(value, false);
                    }
                }

                case wreslLexer.F_INTEGER -> {
                    value = arguments.get(0).getValue().intValue();
                    return new IntDouble(value, true);
                }

                case wreslLexer.F_REAL -> {
                    value = arguments.get(0).getValue().doubleValue();
                    return new IntDouble(value, false);
                }

                case wreslLexer.F_EXPONENTIAL -> {
                    value = Math.exp(arguments.get(0).getValue().doubleValue());
                    return new IntDouble(value, false);
                }

                case wreslLexer.F_LOG_E -> {
                    value = Math.log(arguments.get(0).getValue().doubleValue());
                    return new IntDouble(value, false);
                }

                case wreslLexer.F_LOG_10 -> {
                    value = Math.log10(arguments.get(0).getValue().doubleValue());
                    return new IntDouble(value, false);
                }

                case wreslLexer.F_SQRT -> {
                    value = Math.sqrt(arguments.get(0).getValue().doubleValue());
                    return new IntDouble(value, false);
                }

                case wreslLexer.F_ROUND -> {
                    value = (int)Math.round(arguments.get(0).getValue().doubleValue());
                    return new IntDouble(value, true);
                }

                case wreslLexer.F_POWER -> {
                    double base = arguments.get(0).getValue().doubleValue();
                    double exponent = arguments.get(1).getValue().doubleValue();
                    value = Math.pow(base, exponent);
                    return new IntDouble(value, false);
                }

                case wreslLexer.F_MODULUS -> {
                    IntDouble dividend = arguments.get(0);
                    IntDouble divisor = arguments.get(1);
                    boolean isDividendInt =  dividend.isInt();
                    boolean isDivisorInt = divisor.isInt();
                    // Make sure divisor is not zero
                    if (divisor.getValue().doubleValue() == 0.0) { throw new EvaluationErrorException("MOD function cannot use 0 as divisor!"); };
                    if (isDividendInt && isDivisorInt) {
                        return new IntDouble(dividend.getValue().intValue() % divisor.getValue().intValue(), true);
                    } else if (!isDividendInt && isDivisorInt) {
                        return new IntDouble(dividend.getValue().doubleValue() % divisor.getValue().intValue(),false);
                    } else if (isDividendInt && !isDivisorInt) {
                        return new IntDouble(dividend.getValue().intValue() % divisor.getValue().doubleValue(),false);
                    } else {
                        return new IntDouble(dividend.getValue().doubleValue() % divisor.getValue().doubleValue(),false);
                    }
                }

                case wreslLexer.F_RANGE -> {
                    boolean isTrue = TimeOperations.range(arguments.get(0).getValue().intValue(), arguments.get(1).getValue().intValue(), arguments.get(2).getValue().intValue());
                    if (isTrue) {
                        return new IntDouble(Logical.TRUE.value, true);
                    } else {
                        return new IntDouble(Logical.FALSE.value, true);
                    }
                }

                case wreslLexer.F_MIN -> {
                     double minValue = Double.MAX_VALUE;
                     boolean intFunction = true;
                     for (IntDouble thisValue : arguments) {
                         if (!thisValue.isInt()) { intFunction = false; }
                         double currentValue = thisValue.getValue().doubleValue();
                         if (currentValue < minValue) {
                             minValue = currentValue;
                         }
                     }
                     if (intFunction) {
                         return new IntDouble(Integer.valueOf((int) minValue), true);
                     } else {
                         return new IntDouble(Double.valueOf(minValue), false);
                     }
                }

                case wreslLexer.F_MAX -> {
                    double maxValue = -Double.MAX_VALUE;
                    boolean intFunction = true;
                    for (IntDouble thisValue : arguments) {
                        if (!thisValue.isInt()) { intFunction = false; }
                        double currentValue = thisValue.getValue().doubleValue();
                        if (currentValue > maxValue) {
                            maxValue = currentValue;
                        }
                    }
                    if (intFunction) {
                        return new IntDouble(Integer.valueOf((int) maxValue), true);
                    } else {
                        return new IntDouble(Double.valueOf(maxValue), false);
                    }
                }

                default -> { throw new EvaluationErrorException("Error in evaluating a function!"); }
            }
        } else {
            // Need to be implemented properly
            return null;
        }
    }

    @Override
    // expressionReference
    public IntDouble visitExpressionReference(wreslParser.ExpressionReferenceContext ctx) {
        return visit(ctx.variableReference());
    }

    @Override
    // expressionParen
    public IntDouble visitExpressionParen(wreslParser.ExpressionParenContext ctx) {
        return visit(ctx.getChild(1));
    }


    // ------------------------------------------------------------
    // --- VARIABLE REFERENCE VISITOR METHODS
    // ------------------------------------------------------------
    @Override
    // objectReference
    public IntDouble visitObjectReference(wreslParser.ObjectReferenceContext ctx) {
        // Variable name
        String varName = getWreslText(ctx.OBJECT_NAME());

        IntDouble varData;

        // If this is a DVAR or ALIAS from a previous cycle
        if (ctx.scope() != null) {
            ModelDataSet prevMds = null;
            // Retrieve scope (i.e. cycle); ignore LOCAL and GLOBAL keywords
            int scope = ctx.scope().scopeBody().getStart().getType();
            if (!(scope == wreslLexer.GLOBAL || scope == wreslLexer.LOCAL)) {
                String prevModel = getWreslText(ctx.scope().scopeBody().expression());
                prevMds = INSTANCE.sds.getModelDataSet(prevModel);
                if (prevMds == null) {
                    throw new EvaluationErrorException(prevModel + " cannot be located in study!");
                }
            }

            // Retrieve DVAR from previous model
            Dvar dvar = prevMds.getDvar(varName);
            if (dvar != null) {
                // Retrieve data from DVAR
                IntDouble value = retrieveDataFromDvar(dvar, ctx.timestepOffset());
                return value;
            }

            // If made it this far, retrieve ALIAS from previous model
            Alias as = prevMds.getAlias(varName);
            if (as != null) {
                // Retrieve data from DVAR
                IntDouble value = retrieveDataFromAlias(as, ctx.timestepOffset());
                return value;
            }
        }

        // This is an SVAR
        Svar var = INSTANCE.currentModelDataSet.getSvar(varName);                       // Is this an Svar?
        if (var != null) {
            varData = var.getData();
            if (varData == null) {
                throw new EvaluationErrorException("Variable " + varName + " is being used before its value is computed!");
            }
            return varData;
        }

        // This is a parameter
        var = INSTANCE.sds.getParameter(varName);
        if (var != null) {
            varData = var.getData();
            if (varData == null) {
                throw new EvaluationErrorException("Variable " + varName + " is being used before its value is computed!");
            }
            return varData;
        }

        // This is a TIMESERIES data
        String tsName = DssOperations.entryNameTS(varName, INSTANCE.currentModelDataSet.getTimeStep());
        Timeseries tsVar = INSTANCE.sds.getSVTimeseries(tsName);
        if (tsVar != null) {
            // Retrieve timestep offset ParallelVars
            ParallelVars prvs = retrieveTimeStepOffsetPRVS("TIMESERIES", varName, tsVar.getTimeStep(), ctx.timestepOffset());

            IntDouble value;
            if (prvs.isEarlierThan(INSTANCE.sds.getStudyStartDate())) {
                // Retrieve from initial data
                Timeseries svInit = INSTANCE.sds.getSVInitTimeseries(tsName);
                if (svInit != null) {
                    value = svInit.retrieveDataForTime(prvs);
                    if (value != null) { return value; }
                }
                // If made it this far, it means initial timeseries data was not read before; try reading it
                svInit = tsVar.copyOf();
                boolean success = svInit.readInitData(INSTANCE.sds.getCacheInit(), INSTANCE.sds.getPartA(), INSTANCE.sds.getPartF_Init(), INSTANCE.currentYear, INSTANCE.currentMonth, INSTANCE.currentDay);
                if (success) {
                    value = svInit.retrieveDataForTime(prvs);
                    if (value != null) {
                        INSTANCE.sds.addSVInitTimeseries(svInit);
                        return value;
                    }
                }
            } else {
                // Retrieve data from Timeseries
                value = tsVar.retrieveDataForTime(prvs);
                if (value != null) { return value; }
            }

            // If made it this far, value was not found; generate error
            throw new EvaluationErrorException(tsVar.fromWresl, tsVar.line, "Was not able to retrieve data from the timeseries data for the provided time index.");
        }

        // This is an ALIAS
        Alias asVar = INSTANCE.currentModelDataSet.asMap.get(varName);
        if (asVar != null) {
            IntDouble value = retrieveDataFromAlias(asVar, ctx.timestepOffset());
            return value;
            }

        // This is a DVAR
        Dvar dvar = INSTANCE.currentModelDataSet.getDvar(varName);
        if (dvar != null) {
            IntDouble value = retrieveDataFromDvar(dvar, ctx.timestepOffset());
            return value;
        }

        // If made it this far, variable was not found; generate error
        throw new EvaluationErrorException("Variable " + varName + " is not defined!");
    }

    @Override
    // daysInMonthReference
    public IntDouble visitDaysInMonthReference(wreslParser.DaysInMonthReferenceContext ctx) {
        String monthName = TimeOperations.monthName(INSTANCE.currentMonth);
        Number daysInMonth = Integer.valueOf(TimeOperations.numberOfDays(INSTANCE.currentMonth, INSTANCE.currentYear));
        return new IntDouble(daysInMonth, true);
    }

    @Override
    // cfstafReference
    public IntDouble visitCfstafReference(wreslParser.CfstafReferenceContext ctx) {
        IntDouble conversion = flowUnitConversion(ctx.timestepOffset(), FlowConversion.CFS_TAF);
        return conversion;
    }

    @Override
    // tafcfsReference
    public IntDouble visitTafcfsReference(wreslParser.TafcfsReferenceContext ctx) {
        IntDouble conversion = flowUnitConversion(ctx.timestepOffset(), FlowConversion.TAF_CFS);
        return conversion;
    }

    @Override
    // cfsafReference
    public IntDouble visitCfsafReference(wreslParser.CfsafReferenceContext ctx) {
        IntDouble conversion = flowUnitConversion(ctx.timestepOffset(), FlowConversion.CFS_AF);
        return conversion;
    }

    @Override
    // afcfsReference
    public IntDouble visitAfcfsReference(wreslParser.AfcfsReferenceContext ctx) {
        IntDouble conversion = flowUnitConversion(ctx.timestepOffset(), FlowConversion.AF_CFS);
        return conversion;
    }

    @Override
    // currentMonthReference
    // Always return value based on water year months (i.e. Oct = 1, Sep =12)
    // INSTANCE.currentMonth is calendar month number
    public IntDouble visitCurrentMonthReference(wreslParser.CurrentMonthReferenceContext ctx) {
        String monthName = TimeOperations.monthName(INSTANCE.currentMonth);
        Number intValue = Integer.valueOf(TimeOperations.waterYearMonthValue(monthName));
        return new IntDouble(intValue, true);
    }

    @Override
    public IntDouble visitWaterYearReference(wreslParser.WaterYearReferenceContext ctx) {
        return new IntDouble(TimeOperations.waterYearValue(INSTANCE.currentMonth, INSTANCE.currentYear), true);
    }

    @Override
    // monthReference
    // Always return value based on water year months (i.e. Oct = 1, Sep =12)
    // INSTANCE.currentMonth is calendar month number
    public IntDouble visitMonthReference(wreslParser.MonthReferenceContext ctx) {
        String month = getWreslText(ctx.MONTH());
        if (month.contains("prev")) {
            // For "prev" month reference, it doesn't matter if we operate on calendar or water year months since returned value is relative to the current month
            month = month.substring(4);
            int currentMonthValue = INSTANCE.currentMonth;
            int monthValue = TimeOperations.monthValue(month);
            if (currentMonthValue > monthValue) {
                return new IntDouble(monthValue-currentMonthValue, true);
            } else if (currentMonthValue < monthValue) {
                return new IntDouble(monthValue-currentMonthValue-12, true);
            } else {
                return new IntDouble (-12, true);
            }
        } else {
            return new IntDouble(TimeOperations.waterYearMonthValue(month), true);
        }
    }

    @Override
    public IntDouble visitFutureArrayIndexReference(wreslParser.FutureArrayIndexReferenceContext ctx) {
        return new IntDouble(INSTANCE.futureArrayIndex, true);
    }

    @Override
    // doubleNumber
    public IntDouble visitDoubleNumber(wreslParser.DoubleNumberContext ctx) {
        Number doubleValue = Double.valueOf(Double.parseDouble(ctx.DOUBLE().getText()));
        return new IntDouble(doubleValue,false);
    }

    @Override
    // intNumber
    public IntDouble visitIntNumber(wreslParser.IntNumberContext ctx) {
        Number intValue = Integer.valueOf(Integer.parseInt(ctx.INT().getText()));
        return new IntDouble(intValue, true);
    }


    // ------------------------------------------------------------
    // --- CASE STATEMENT VISITOR METHODS
    // ------------------------------------------------------------
    @Override
    // caseCondition
    public IntDouble visitCaseCondition(wreslParser.CaseConditionContext ctx) {
        if (ctx.caseConditionExpression().ALWAYS() != null) {
            return new IntDouble(Logical.TRUE.value, true);
        }
        else {
            return visit(ctx.caseConditionExpression().expression());
        }
    }


    // ------------------------------------------------------------
    // --- SELECT
    // ------------------------------------------------------------
    @Override
    public IntDouble visitSelect(wreslParser.SelectContext ctx) {
        // Table name
        String tableName = getWreslText(ctx.OBJECT_NAME());

        // Retrieve SELECT column
        String selectColumn = getWreslText(ctx.columnName());

        // Retrieve GIVEN statement
        HashMap<String, Number> given = new HashMap<>();
        if (ctx.given() != null) {
            String columnName = getWreslText(ctx.given().columnName());
            IntDouble value = visit(ctx.given().expression());
            given.put(columnName, value.getValue());
        }
        else {
            given =null;
        }

        // Retrieve WHERE statement
        HashMap<String, Number> where = new HashMap<>();
        if (ctx.where() != null) {
            for (int i=0; i<ctx.where().columnName().size(); i++) {
                String columnName = getWreslText(ctx.where().columnName(i));
                Number value = visit(ctx.where().expression(i)).getValue();
                where.put(columnName, value);
            }
        }
        else {
            where = null;
        }

        // Retrieve USE statement
        String use = null;
        if (ctx.use() != null) {
            use = getWreslText(ctx.use().interpolation());
            if (use.equals("min")) {
                use = "minimum";
            } else if (use.equals("max")) {
                use = "maximum";
            }
        }

        // Find data in table
        IntDouble result;
        if (where == null) {
            result = findDataInLookupTable(tableName, selectColumn, given, use);
        } else {
            result = findDataInLookupTable(tableName, selectColumn, where, given, use);
        }
        return result;

    }


    // ------------------------------------------------------------
    // --- HELPER METHOD TO CONVERT FLOW UNITS
    // ------------------------------------------------------------
    private IntDouble flowUnitConversion(wreslParser.TimestepOffsetContext ctx, FlowConversion conversionType) {
        // If timestep is not known, return null
        String timeStep = INSTANCE.currentModelDataSet.getTimeStep();
        if (timeStep.equals(Param.undefined)) { return null; }

        // Retrieve time offset
        int timeOffset = 0;
        if (ctx != null) {
            IntDouble value = visit(ctx.expression());
            if (value == null) { return null; }
            timeOffset = value.getValue().intValue();
        }

        // Compute month when time offset is used
        ParallelVars prvs = TimeOperations.findTime(timeStep, timeOffset, INSTANCE.currentYear, INSTANCE.currentMonth, INSTANCE.currentDay);

        // Based on timestep, compute number of days
        int nDays;
        if (TimeOperations.isMonthlyInterval(timeStep)) {
            nDays = TimeOperations.numberOfDays(prvs.dataMonth, prvs.dataYear);
        } else {
            nDays = 1;
        }

        // Based on conversion type, return data
        switch (conversionType) {
            case CFS_TAF -> { return new IntDouble(nDays / 504.1666667, false); }
            case CFS_AF ->  { return new IntDouble(nDays / 504.1666667 * 1000., false); }
            case TAF_CFS -> { return new IntDouble(504.1666667 / nDays, false); }
            case AF_CFS ->  { return new IntDouble(504.1666667 / nDays / 1000., false); }
            default     ->  { return null; }
        }
    }


    // ------------------------------------------------------------
    // --- HELPER METHODS FOR LOOKUP TABLE PROCESSING
    // ------------------------------------------------------------
    // Given conditions (select, where, given, use), find/calculate a value based on data in a lookup table
    private IntDouble findDataInLookupTable(String tableName, String select, Map<String, Number> where, Map<String, Number> given, String use) throws EvaluationErrorException {

        // If table hasn't been copied into memory yet, do so
        if (INSTANCE.tableSeries.get(tableName) == null) {
            cacheLookUpData(tableName);
        }

        // Retrieve table data
        LookUpTable lookupTable = INSTANCE.tableSeries.get(tableName);
        List<Number[]> data = lookupTable.data;
        Map<String, Integer> field = lookupTable.field;
        int fieldSize = field.size();

        // Index of SELECT field
        int selectIndex;
        if (field.containsKey(select)) {
            selectIndex = field.get(select);
        }
        else {
            throw new EvaluationErrorException(select + " in SELECT statement is not a field name in Table " + tableName + "!");
        }

        // Process WHERE part of the SELECT statement
        Set whereSet = where.keySet();
        Iterator iterator = whereSet.iterator();
        int whereSize = where.size();
        int[] whereIndex = new int[whereSize];
        Number[] whereValue = new Number[whereSize];
        int k=0;
        while (iterator.hasNext()) {
            String whereName = (String)iterator.next();
            if (field.containsKey(whereName)) {
                whereIndex[k] = field.get(whereName);
            }
            else {
                throw new EvaluationErrorException(whereName + " in WHERE statement is not a field name in Table " + tableName + "!");
            }
            whereValue[k] = (Number)where.get(whereName);
            k = k+1;
        }

        boolean whereTrue;
        if (whereSize == 0) {
            whereTrue = true;
        }
        else {
            whereTrue = false;
        }

        Number[] values = new Number[fieldSize];
        int i = -1;
        while (i<data.size()-1 && !whereTrue) {
            i++;
            values = data.get(i);
            boolean eachWhereTrue = true;
            k = -1;
            while (k<whereSize-1 && eachWhereTrue) {
                k++;
                if (values[whereIndex[k]].doubleValue() != whereValue[k].doubleValue()) {
                    eachWhereTrue = false;
                }
            }
            if (eachWhereTrue) whereTrue = true;
        }

        if (!whereTrue) {
            String whereError = "";
            for (String key: where.keySet()) {
                whereError = whereError + "(" + key + ": " + where.get(key) + ")";
            }
            throw new EvaluationErrorException("Under WHERE statements " + whereError + ", data could not be found in Table " + tableName + "!");
        }

        Number value = values[selectIndex];
        if (given == null) {
            String valueString = value.toString();
            return new IntDouble(Double.parseDouble(valueString), false);
        }

        int givenIndex;
        Set givenSet = given.keySet();
        iterator = givenSet.iterator();
        String givenName = (String)iterator.next();
        String valueString;
        if (field.containsKey(givenName)) {
            givenIndex = field.get(givenName);
        }
        else {
            throw new EvaluationErrorException(givenName + " in GIVEN statement is not a field name in Table " + tableName + "!");
        }
        Number givenValue = (Number)given.get(givenName);

        List<Number> gVList = new ArrayList<>();
        Map<Number, Number> gVMap = new HashMap<>();
        gVList.add(values[givenIndex]);
        gVMap.put(values[givenIndex], values[selectIndex]);

        while (i<data.size()-1) {
            i++;
            values = data.get(i);
            boolean eachWhereTrue = true;
            k = -1;
            while (k<whereSize-1 && eachWhereTrue) {
                k++;
                if (values[whereIndex[k]].doubleValue() != whereValue[k].doubleValue()) {
                    eachWhereTrue = false;
                }
            }
            if (eachWhereTrue) {
                if (gVList.contains(values[givenIndex])) {
                    throw new EvaluationErrorException("Given value " + values[givenIndex] + " in GIVEN statement is duplicated in Table " + tableName + "!");
                }
                else {
                    gVList.add(values[givenIndex]);
                    gVMap.put(values[givenIndex], values[selectIndex]);
                }
            }
            else {
                eachWhereTrue = true;
            }
        }

        String givenError = "";
        for (String key: given.keySet()) {
            givenError = givenError + "(" + key + ": " + given.get(key) + ")";
        }

        return calculateLookUpTableValue(givenValue, gVList, gVMap, use, tableName, givenError);
    }

    // Given conditions (select, given, use), find/calculate a value based on data in a lookup table
    private IntDouble findDataInLookupTable(String tableName, String select, Map<String, Number> given, String use) throws EvaluationErrorException {
        // If table hasn't been copied into memory yet, do so
        if (INSTANCE.tableSeries.get(tableName) == null) {
            cacheLookUpData(tableName);
        }

        // Retrieve table data
        LookUpTable lookupTable = INSTANCE.tableSeries.get(tableName);
        List<Number[]> data = lookupTable.data;
        Map<String, Integer> field = lookupTable.field;
        int fieldSize = field.size();

        // Index of SELECT field
        int selectIndex;
        if (field.containsKey(select)) {
            selectIndex = field.get(select);
        }
        else {
            throw new EvaluationErrorException(select + " in SELECT statement is not a field name in Table " + tableName + "!");
        }

        int givenIndex;
        Set givenSet = given.keySet();
        Iterator iterator = givenSet.iterator();
        String givenName = (String)iterator.next();
        String valueString;
        if (field.containsKey(givenName)) {
            givenIndex = field.get(givenName);
        }
        else {
            throw new EvaluationErrorException(givenName + " in GIVEN statement is not a field name in Table " + tableName + "!");
        }
        Number givenValue = (Number)given.get(givenName);

        List<Number> gVList = new ArrayList<>();
        Map<Number, Number> gVMap = new HashMap<>();

        Number[] values;
        for (int i=0; i<data.size(); i++) {
            values = data.get(i);
            if (gVList.contains(values[givenIndex])) {
                throw new EvaluationErrorException("Given value " + values[givenIndex] + " in GIVEN statement is duplicated in Table " + tableName + "!");
            } else {
                gVList.add(values[givenIndex]);
                gVMap.put(values[givenIndex], values[selectIndex]);
            }
        }

        String givenError = "";
        for (String key: given.keySet()) {
            givenError = givenError + "(" + key + ": " + given.get(key) + ")";
        }

        return calculateLookUpTableValue(givenValue, gVList, gVMap, use, tableName, givenError);
    }

    // Store lookup data in memory
    private void cacheLookUpData(String tableName) throws EvaluationErrorException {
        // Lookup table filename and data
        String absoluteTableFileName = INSTANCE.absReferencePath + File.separator + "lookup" + File.separator + tableName + ".table";
        LookUpTable lookupTable = new LookUpTable();

        // Set table name
        lookupTable.name = tableName;

        // Open and process file
        try {
            BufferedReader br = new BufferedReader(new FileReader(absoluteTableFileName));
            String strLine;
            int line = 0;
            int fieldSize = 0;
            boolean isFirstEntry = true;
            boolean isDataFound = false;
            while ((strLine = br.readLine()) != null) {
                line = line + 1;

                // Strip comments from line; remove leading and trailing spaces
                if (strLine.indexOf("!") != -1) { strLine = strLine.substring(0, strLine.indexOf("!")); }
                strLine = strLine.strip();

                // if there is nothing left in strLine, continue
                if (strLine.equals("")) {continue;}

                // If first data entry, check that it is the name of the table and read field names right after that
                if (isFirstEntry) {
                    if (!strLine.toLowerCase().equals(tableName)) { throw new EvaluationErrorException("The first line after comments in table " + tableName + ".table should be the file name without extension: " + tableName); }

                    // Second line of entry; read and process field names
                    if ((strLine = br.readLine()) == null) {throw new EvaluationErrorException("No field names were found in table " + tableName + ".table!");}
                    if (strLine.indexOf("!") != -1) { strLine = strLine.substring(0, strLine.indexOf("!")); }
                    strLine = strLine.strip();
                    String[] fieldNames = strLine.toLowerCase().split("\\s+");
                    fieldSize = fieldNames.length;
                    for (int i=0; i<fieldSize; i++) {
                        if (!isFieldNameRight(fieldNames[i])) { throw new EvaluationErrorException("Number " + (i+1) + " field name in table " + tableName + ".table, line " + line + " has a wrong format!"); }
                        lookupTable.field.put(fieldNames[i], i);
                    }

                    // No more first (and second) line of entry
                    isFirstEntry = false;
                    continue;
                }

                // Process and store field values
                String[] values = strLine.split("\\s+");
                if (values.length != fieldSize) { throw new EvaluationErrorException("The number of data in the table " + tableName + ".table, line " + line + " does not agree with the number of the fields!"); }
                Number[] fieldValues = new Number[fieldSize];
                for (int i=0; i<fieldSize; i++) {
                    try {
                        fieldValues[i] = Double.parseDouble(values[i]);
                        isDataFound = true;
                    }
                    catch (NumberFormatException nfe) {
                        throw new EvaluationErrorException("Number " + (i+1) + " data in table " +tableName + ".table, line " + line + " is not numeric!");
                    }
                }
                lookupTable.data.add(fieldValues);
            }

            // If no data was found in the table, generate error
            if (!isDataFound) {
                throw new EvaluationErrorException("No data exists in table " + tableName + ".table!");
            }

            // Add table data to the map
            INSTANCE.tableSeries.put(tableName,lookupTable);

        }
        catch (IOException e) {
            throw new EvaluationErrorException(e.toString());
        }
    }

    // Check correctness of a field name
    private static boolean isFieldNameRight(String fieldName){
        if (Character.isDigit(fieldName.charAt(0))){
            return false;
        }
        Pattern alphaNumberic = Pattern.compile("[A-Za-z0-9_]+");
        Matcher m = alphaNumberic.matcher(fieldName);
        return m.matches();
    }

    // Calculate value from lookup table based on the GIVEN statement
    private static IntDouble calculateLookUpTableValue(Number given, List<Number> gVList, Map<Number, Number> gVMap, String use, String tableName, String givenError) throws EvaluationErrorException{
        double givenValue = given.doubleValue();
        if (gVList.size() == 0) {
            throw new EvaluationErrorException("Under the GIVEN condition of " + givenError + ", data not found in Table " + tableName + "!");
        }
        else if (gVList.size()==1 && use.equals("linear")) {
            Number gV = gVList.get(0);
            if (givenValue == gV.doubleValue()) {
                return new IntDouble(gVMap.get(gV).doubleValue(),false);
            }
            else {
                throw new EvaluationErrorException("Under the GIVEN condition of " + givenError + ", only one value is found for interpolation in Table " + tableName + "!");
            }
        }

        gVList = sortNumberArray(gVList, givenError, tableName);

        for (int i=0; i<gVList.size()-1; i++) {
            int j = i+1;
            Number first = gVList.get(i);
            Number second = gVList.get(j);
            double firstValue = first.doubleValue();
            double secondValue = second.doubleValue();
            if (firstValue <= givenValue && secondValue>=givenValue) {
                if (use.equals("minimum")) {
                    return new IntDouble(gVMap.get(gVList.get(i)).doubleValue(), false);
                }
                else if (use.equals("maximum")) {
                    return new IntDouble(gVMap.get(gVList.get(j)).doubleValue(), false);
                }
                else if (use.equals("linear")) {
                    double value = (givenValue-firstValue)/(secondValue-firstValue)
                            *(gVMap.get(second).doubleValue()-gVMap.get(first).doubleValue())+gVMap.get(first).doubleValue();
                    return new IntDouble(value,false);
                }
                else {
                    throw new EvaluationErrorException("USE statement can only be maximum, minimum, or linear in Table " + tableName + "!");
                }
            }
        }

        if (givenValue < gVList.get(0).doubleValue()) {
            if (use.equals("minimum")) {
                throw new EvaluationErrorException("Under the GIVEN condition of " + givenError + ", data not found in Table " + tableName + "!");
            }
            else if (use.equals("linear")) {
                Number first = gVList.get(0);
                Number second = gVList.get(1);
                double firstValue = first.doubleValue();
                double secondValue = second.doubleValue();
                double value = (givenValue-firstValue)/(secondValue-firstValue)
                        *(gVMap.get(second).doubleValue()-gVMap.get(first).doubleValue())+gVMap.get(first).doubleValue();
                return new IntDouble(value,false);
            }
            else if (use.equals("maximum")) {
                return new IntDouble(gVMap.get(gVList.get(0)).doubleValue(),false);
            }
            else {
                throw new EvaluationErrorException("USE statement can only be maximum, minimum, or linear in Table " + tableName + "!");
            }
        }
        else if (givenValue > gVList.get(gVList.size()-1).doubleValue()) {
            if (use.equals("maximum")) {
                throw new EvaluationErrorException("Under the GIVEN condition of " + givenError + ", data not found in Table " + tableName + "!");
            }
            else if (use.equals("linear")) {
                int size = gVList.size();
                Number first = gVList.get(size-2);
                Number second = gVList.get(size-1);
                double firstValue = first.doubleValue();
                double secondValue = second.doubleValue();
                double value = (givenValue-firstValue)/(secondValue-firstValue)
                        *(gVMap.get(second).doubleValue()-gVMap.get(first).doubleValue())+gVMap.get(first).doubleValue();
                return new IntDouble(value,false);
            }
            else if (use.equals("minimum")) {
                return new IntDouble(gVMap.get(gVList.get(gVList.size()-1)).doubleValue(),false);
            }
            else {
                throw new EvaluationErrorException("USE statement can only be maximum, minimum, or linear in Table " + tableName + "!");
            }
        }

        // If made this far without calculating a value, generate error
        throw new EvaluationErrorException("Under the GIVEN condition of " + givenError + ", only one value is found for interpolation in Table " + tableName + "!");
    }

    // Sort a Number array to be processed for a lookup table
    private static List<Number> sortNumberArray(List<Number> al, String givenError, String tableName) throws EvaluationErrorException {
        for (int i=0; i<al.size(); i++) {
            for (int j=i+1; j<al.size(); j++) {
                Number first = al.get(i);
                Number second = al.get(j);
                if (first.doubleValue() > second.doubleValue()){
                    al.set(i, second);
                    al.set(j, first);
                }
                else if (first.doubleValue() == second.doubleValue()) {
                    throw new EvaluationErrorException("Under the GIVEN condition of " + givenError + ", two data in GIVEN column " + first + ", " + second + " have the same value in Table " + tableName + "!");
                }
            }
        }
        return al;
    }


    // ------------------------------------------------------------
    // --- HELPER METHODS TO RETRIEVE DATA FROM DIFFERENT WRESL OBJECTS
    // ------------------------------------------------------------

    // Retrieve timestep offset
    private ParallelVars retrieveTimeStepOffsetPRVS(String varType, String varName, String timeStep, wreslParser.TimestepOffsetContext timestepOffsetCtx) {
        int timeOffset = 0;
        if (timestepOffsetCtx != null) {
            IntDouble temp = visit(timestepOffsetCtx.expression());
            if (!temp.isInt()) {
                throw new EvaluationErrorException("Timeseries index for " + varType + " " + varName + " must be an integer value.");
            }
            timeOffset = temp.getValue().intValue();
        }
        ParallelVars prvs = TimeOperations.findTime(timeStep, timeOffset, INSTANCE.currentYear, INSTANCE.currentMonth, INSTANCE.currentDay);

        return prvs;
    }

    // Retrieve data from a DVAR
    private IntDouble retrieveDataFromDvar(Dvar dvar, wreslParser.TimestepOffsetContext timestepOffsetCtx) {
        // Retrieve timestep offset parallel vars
        ParallelVars prvs = retrieveTimeStepOffsetPRVS("DVAR", dvar.getName(), dvar.getTimeStep(), timestepOffsetCtx);

        // Retrieve data from Dvar
        IntDouble value;
        value = dvar.retrieveDataForTime(prvs);
        if (value != null) { return value; }

        // If made it this far, dvar did not extend back in time; try to retrieve from initial data
        dvar.setDssBPart(dvar.getName());
        dvar.setTimeStep(INSTANCE.currentModelDataSet.getTimeStep());
        dvar.readInitData(INSTANCE.sds.getCacheInit(), INSTANCE.sds.getPartA(), INSTANCE.sds.getPartF_Init(), INSTANCE.currentYear, INSTANCE.currentMonth, INSTANCE.currentDay);
        value = dvar.retrieveDataForTime(prvs);
        if (value != null ) { return value; }

        // If made it this far, value was not found; generate error
        throw new EvaluationErrorException(dvar.fromWresl, dvar.line, "Was not able to retrieve data for DVAR " + dvar.getName() + " for the provided time index.");

    }

    // Retrieve data from an ALIAS
    private IntDouble retrieveDataFromAlias(Alias as, wreslParser.TimestepOffsetContext timestepOffsetCtx) {
        // Retrieve timestep offset ParallelVars
        ParallelVars prvs = retrieveTimeStepOffsetPRVS("ALIAS", as.getName(), as.getTimeStep(), timestepOffsetCtx);

        // Retrieve data from Alias
        IntDouble value;
        value = as.retrieveDataForTime(prvs);
        if (value != null) { return value; }

        // If made it this far, alias did not extend back in time; try to retrieve from initial data
        as.setDssBPart(as.getName());
        as.setTimeStep(INSTANCE.currentModelDataSet.getTimeStep());
        as.readInitData(INSTANCE.sds.getCacheInit(), INSTANCE.sds.getPartA(), INSTANCE.sds.getPartF_Init(), INSTANCE.currentYear, INSTANCE.currentMonth, INSTANCE.currentDay);
        value = as.retrieveDataForTime(prvs);
        if (value != null ) { return value; }

        // If made it this far, value was not found; generate error
        throw new EvaluationErrorException(as.fromWresl, as.line, "Was not able to retrieve data for ALIAS " + as.name + " for the provided time index.");

    }


    // ------------------------------------------------------------
    // --- CLASS FOR PROCESSING OF GOALS (I.E. CONSTRAINTS)
    // ------------------------------------------------------------
    private class GoalEvaluator extends wreslBaseVisitor<EvalConstraint> {
        // GOAL data that will be built through visit methods below
        private boolean isProcessingLeft = true;
        private EvalConstraint leftOperand;
        private EvalConstraint rightOperand;

        // Source file and line number for the GOAL for error messages
        private String goalName;
        private String fromWresl;
        private int line;

        private static final IntDouble minusOne = new IntDouble(-1.0, false);

        public EvalConstraint evaluate(String goalName, String fromWresl, int line, ParseTree expressionCtx) {
            // Initialize goal data
            this.goalName = goalName;
            this.fromWresl = fromWresl;
            this.line = line;

            // Process; data will be accumulated in left and right common data fields
            EvalConstraint constraint = visit(expressionCtx);

            // Return compiled GOAL data
            return constraint;
        }

        @Override
        // expressionComparison
        // We will accumulate information into following form under the constraintLeft field:
        //  C1xD1 + C2xD2 + C3xD3 + ... + Cn = 0.0 (comaprison sign can also be <, <=, > or >=)
        public EvalConstraint visitExpressionComparison(wreslParser.ExpressionComparisonContext ctx) {
            // Process left and right of the sign
            this.isProcessingLeft = true;
            this.leftOperand = new EvalConstraint();
            this.rightOperand = new EvalConstraint();
            EvalConstraint constraintLeft = visit(ctx.expression(0));
            this.isProcessingLeft = false;
            this.leftOperand = new EvalConstraint();
            this.rightOperand = new EvalConstraint();
            EvalConstraint constraintRight = visit(ctx.expression(1));

            // Proceed based on the type of constraintLeft and constraintRight operands
            if (constraintLeft.isNumeric()) {
                if (constraintRight.isNumeric()) {
                    // Both left and right constraint expressions are values
                    // -----------------------------------------------------
                    throw new EvaluationErrorException(this.fromWresl, this.line, "No DVARs are referenced at GOAL " + this.goalName + "!");
                } else {
                    // Left constraint is value, right constraint is multiplier/value
                    // --------------------------------------------------------------
                    constraintLeft.getConstant().subtract(constraintRight.getConstant());
                    for (IntDouble rightMultiplier : constraintRight.getMultipliers().values()) {
                        rightMultiplier.multiply(this.minusOne);
                        constraintLeft.addMultiplier(rightMultiplier.getArgName(), rightMultiplier);
                    }
                }
            } else {
                if (constraintRight.isNumeric()) {
                    // Left constraint is multiplier/value, right constraint is numeric
                    // ----------------------------------------------------------------
                    constraintLeft.getConstant().subtract(constraintRight.getConstant());
                } else {
                    // Both left and right constraints are multiplier/value
                    constraintLeft.getConstant().subtract(constraintRight.getConstant());
                    for (IntDouble rightMultiplier : constraintRight.getMultipliers().values()) {

                        String dvarName = rightMultiplier.getArgName();
                        IntDouble leftMultiplier = constraintLeft.getMultiplier(dvarName);
                        if (leftMultiplier == null) {
                            rightMultiplier.multiply(this.minusOne);
                            constraintLeft.addMultiplier(dvarName, rightMultiplier);
                        } else {
                            leftMultiplier.subtract(rightMultiplier);
                        }
                    }
                }
            }

            // Retrieve sign; '==' and '!=' are not recognized
            int sign = ctx.opCompare().getStart().getType();
            if (sign == wreslLexer.NOT_EQUAL    ||
                sign == wreslLexer.DOUBLE_EQUAL)  {
                throw new EvaluationErrorException(this.fromWresl, this.line, "'" + sign + "' sign is not allowed in a GOAL statement!");
            }
            constraintLeft.setSign(getWreslText(ctx.opCompare()));

            // Remove multipliers with a coeffcient of zero from the constraint
            constraintLeft.getMultipliers().entrySet().removeIf(entry -> entry.getValue().getValue().doubleValue() == 0.0);

            // Return constraint
            return constraintLeft;
        }

        @Override
        // expressionSigned
        public EvalConstraint visitExpressionSigned(wreslParser.ExpressionSignedContext ctx) {
            // Evaluate expression
            EvalConstraint returnData = visit(ctx.expression());

            // Negative operation, if needed
            if (ctx.MINUS() != null) {
                returnData.multiply(this.minusOne);
            }

            return returnData;
        }

        @Override
        // expressionMultDiv
        public EvalConstraint visitExpressionMultDiv(wreslParser.ExpressionMultDivContext ctx) {
            // Store existing left and right operands to be restored later
            EvalConstraint leftOpStore = this.leftOperand;
            EvalConstraint rightOpStore = this.rightOperand;

            // Temporary data to be used for return value
            EvalConstraint returnData;

            // Compute left and right side of the operation
            this.isProcessingLeft = true;
            this.leftOperand = new EvalConstraint();
            this.leftOperand = visit(ctx.expression(0));
            this.isProcessingLeft = false;
            this.rightOperand = new EvalConstraint();
            this.rightOperand = visit(ctx.expression(1));

            // Which operation?
            boolean isMultiplication = ctx.opMultiplicationDivision().MULT() != null;

            // Proceed based on the type of left operand
            if (this.leftOperand .isNumeric()) {
                if (this.rightOperand.isNumeric()) {
                    // Both left and right operands are numbers
                    // ----------------------------------------
                    IntDouble rightVal = this.rightOperand.getConstant();
                    if (isMultiplication) {
                        // MULTIPLICATION
                        this.leftOperand .getConstant().multiply(rightVal);
                    } else {
                        // DIVISION
                        // No divide-by-zero
                        if (rightVal.getValue().doubleValue() == 0.0) {
                            throw new EvaluationErrorException(this.fromWresl, this.line, "Division-by-zero at GOAL " + this.goalName + "!");
                        }
                        this.leftOperand .getConstant().divide(rightVal);
                    }
                    returnData = this.leftOperand.copyOf();
                } else {
                    // Left operand is number, right operand is multipliers/value
                    // ----------------------------------------------------------
                    IntDouble leftVal = this.leftOperand .getConstant();
                    if (isMultiplication) {
                        // MULTIPLICATION
                        this.rightOperand.getConstant().multiply(leftVal);
                        for (IntDouble rightMultiplier : this.rightOperand.getMultipliers().values()) {
                            rightMultiplier.multiply(leftVal);
                        }
                    } else {
                        // DIVISION
                        // No divide-by-zero
                        if (leftVal.getValue().doubleValue() == 0.0) {
                            throw new EvaluationErrorException(this.fromWresl, this.line, "Division-by-zero at GOAL " + this.goalName + "!");
                        }
                        this.rightOperand.getConstant().divide(leftVal);
                        for (IntDouble rightMultiplier : this.rightOperand.getMultipliers().values()) {
                            rightMultiplier.divide(leftVal);
                        }
                    }
                    returnData = this.rightOperand.copyOf();
                }
            } else {
                if (this.rightOperand.isNumeric()) {
                    // Left operand is multiplier/value, right operand is value
                    // --------------------------------------------------------
                    IntDouble rightVal = this.rightOperand.getConstant();
                    if (isMultiplication) {
                        // MULTIPLICATION
                        this.leftOperand .getConstant().multiply(rightVal);
                        for (IntDouble leftMultiplier : this.leftOperand .getMultipliers().values()) {
                            leftMultiplier.multiply(rightVal);
                        }
                    } else {
                        // DIVISION
                        // No divide-by-zero
                        if (rightVal.getValue().doubleValue() == 0.0) {
                            throw new EvaluationErrorException(this.fromWresl, this.line, "Division-by-zero at GOAL " + this.goalName + "!");
                        }
                        this.leftOperand .getConstant().divide(rightVal);
                        for (IntDouble leftMultiplier : this.leftOperand .getMultipliers().values()) {
                            leftMultiplier.divide(rightVal);
                        }
                    }
                    returnData = this.leftOperand.copyOf();
                } else {
                    // Both left and right operands are multiplier/value
                    // -------------------------------------------------
                    throw new EvaluationErrorException(this.fromWresl, this.line, "Non-linearity detected at GOAL " + this.goalName + "!");
                }
            }

            // Restore original left and right operands
            this.leftOperand = leftOpStore;
            this.rightOperand = rightOpStore;

            // Return data
            return returnData;
        }

        @Override
        // expressionAddSub
        public EvalConstraint visitExpressionAddSub(wreslParser.ExpressionAddSubContext ctx) {
            // Store existing left and right operands to be restored later
            EvalConstraint leftOpStore = this.leftOperand;
            EvalConstraint rightOpStore = this.rightOperand;

            // Temporary data to be used for return value
            EvalConstraint returnData;

            // Compute left and right side of the operation
            this.isProcessingLeft = true;
            this.leftOperand = new EvalConstraint();
            this.leftOperand = visit(ctx.expression(0));
            this.isProcessingLeft = false;
            this.rightOperand = new EvalConstraint();
            this.rightOperand = visit(ctx.expression(1));

            // Which operation is this?
            boolean isAddition = ctx.opAdditionSubtraction().PLUS() != null;

            // Proceed based on format of left and right operands
            if (this.leftOperand.isNumeric()) {
                if (this.rightOperand.isNumeric()) {
                    // Both left and right operands are values
                    // ---------------------------------------
                    if (isAddition) {
                        // ADDITION
                        this.leftOperand.getConstant().add(this.rightOperand.getConstant());
                    } else {
                        // SUBTRACTION
                        this.leftOperand.getConstant().subtract(this.rightOperand.getConstant());
                    }
                    returnData = this.leftOperand.copyOf();
                } else {
                    // Left operand is value, right operand is multiplier/constant
                    // -----------------------------------------------------------
                    if (isAddition) {
                        // ADDITION
                        this.rightOperand.getConstant().add(this.leftOperand.getConstant());
                    } else {
                        // SUBTRACTION
                        this.rightOperand.getConstant().multiply(this.minusOne);
                        this.rightOperand.getConstant().add(this.leftOperand.getConstant());
                        for (IntDouble rightMultiplier : this.rightOperand.getMultipliers().values()) {
                            rightMultiplier.multiply(this.minusOne);
                        }
                    }
                    returnData = this.rightOperand.copyOf();
                }
            } else {
                if (this.rightOperand.isNumeric()) {
                    // Left operand is multiplier/value, right operand is value
                    // --------------------------------------------------------
                    if (isAddition) {
                        // ADDITION
                        this.leftOperand.getConstant().add(this.rightOperand.getConstant());
                    } else {
                        // SUBTRACTION
                        this.leftOperand.getConstant().subtract(this.rightOperand.getConstant());
                    }
                    returnData = this.leftOperand.copyOf();
                } else {
                    // Both left and right operands are multiplier/value
                    // -------------------------------------------------
                    if (isAddition) {
                        // ADDITION
                        this.leftOperand.getConstant().add(this.rightOperand.getConstant());
                        for (IntDouble rightMultiplier : this.rightOperand.getMultipliers().values()) {
                            String dvarName = rightMultiplier.getArgName();
                            IntDouble leftMultiplier = this.leftOperand.getMultiplier(dvarName);
                            if (leftMultiplier == null) {
                                this.leftOperand.addMultiplier(dvarName, rightMultiplier);
                            } else {
                                leftMultiplier.add(rightMultiplier);
                            }
                        }
                    } else {
                        // SUBTRACTION
                        this.leftOperand.getConstant().subtract(this.rightOperand.getConstant());
                        for (IntDouble rightMultiplier : this.rightOperand.getMultipliers().values()) {
                            String dvarName = rightMultiplier.getArgName();
                            IntDouble leftMultiplier = this.leftOperand.getMultiplier(dvarName);
                            if (leftMultiplier == null) {
                                rightMultiplier.multiply(this.minusOne);
                                this.leftOperand.addMultiplier(dvarName, rightMultiplier);
                            } else {
                                leftMultiplier.subtract(rightMultiplier);
                            }
                        }
                    }
                    returnData = this.leftOperand.copyOf();
                }
            }

            // Restore original left and right operands
            this.leftOperand = leftOpStore;
            this.rightOperand = rightOpStore;

            // Return data
            return returnData;
        }

        @Override
        // expressionNot
        public EvalConstraint visitExpressionNot(wreslParser.ExpressionNotContext ctx) {
            throw new SyntaxErrorException(this.fromWresl, this.line, "Logical .NOT. operation is not allowed in GOAL statements!");
        }

        @Override
        // expressionLogical
        public EvalConstraint visitExpressionLogical(wreslParser.ExpressionLogicalContext ctx) {
            throw new SyntaxErrorException(this.fromWresl, this.line, "Logical operations are not allowed in GOAL statements!");
        }

        @Override
        // expressionSum
        public EvalConstraint visitExpressionSum(wreslParser.ExpressionSumContext ctx) {
            IntDouble data = INSTANCE.visitExpressionSum(ctx);
            return new EvalConstraint(data);
        }

        @Override
        // expressionCall
        public EvalConstraint visitExpressionCall(wreslParser.ExpressionCallContext ctx) {
            IntDouble data = INSTANCE.visitExpressionCall(ctx);
            return new EvalConstraint(data);
        }

        @Override
        public EvalConstraint visitExpressionSlice(wreslParser.ExpressionSliceContext ctx) {
            throw new SyntaxErrorException(this.fromWresl, this.line, "Array slice operations are not allowed in GOAL statements!");
        }

        @Override
        // expressionParen
        public EvalConstraint visitExpressionParen(wreslParser.ExpressionParenContext ctx) {
            return visit(ctx.expression());
        }

        @Override
        // expressionReference
        public EvalConstraint visitExpressionReference(wreslParser.ExpressionReferenceContext ctx) {
            // If references objects, use GoalEvaluator's own visit method
            if (ctx.variableReference() instanceof wreslParser.ObjectReferenceContext objRef) {
                return visit(objRef);
            // For everything else, revert back to visit methods of Evaluator class
            } else {
                IntDouble result = INSTANCE.visitExpressionReference(ctx);
                return new EvalConstraint(result);
            }
        }

        @Override
        // objectReference
        public EvalConstraint visitObjectReference(wreslParser.ObjectReferenceContext ctx) {
            // Retrieve object name
            String varName = getWreslText(ctx.OBJECT_NAME());

            // This is a DVAR
            Dvar dvar = INSTANCE.currentModelDataSet.getDvar(varName);
            if (dvar != null) {
                // If there is no time offset return as a multiplier
                if (ctx.timestepOffset() == null) {
                    IntDouble varData = new IntDouble(1.0, false, varName);
                    if (this.isProcessingLeft) {
                        this.leftOperand.addMultiplier(varName, varData);
                        return this.leftOperand;
                    } else {
                        this.rightOperand.addMultiplier(varName, varData);
                        return this.rightOperand;
                    }
                }


                // Retrieve timeseries data from Dvar
                IntDouble value = retrieveDataFromDvar(dvar, ctx.timestepOffset());
                return new EvalConstraint(value);

            }

            // This is an SVAR
            Svar var = INSTANCE.currentModelDataSet.getSvar(varName);                       // Is this an Svar?
            if (var != null) {
                IntDouble varData = var.getData().copyOf();
                if (varData == null) {
                    throw new EvaluationErrorException("Variable " + varName + " is being used before its value is computed!");
                }
                return new EvalConstraint(varData);
            }

            // This is a parameter
            var = INSTANCE.sds.getParameter(varName);
            if (var != null) {
                IntDouble varData = var.getData().copyOf();
                if (varData == null) {
                    throw new EvaluationErrorException("Variable " + varName + " is being used before its value is computed!");
                }
                return new EvalConstraint(varData);
            }

            // This is a TIMESERIES data
            String tsName = DssOperations.entryNameTS(varName, INSTANCE.currentModelDataSet.getTimeStep());
            Timeseries tsVar = INSTANCE.sds.getSVTimeseries(tsName);
            if (tsVar != null) {
                // Retrieve timestep offset ParallelVars
                ParallelVars prvs = retrieveTimeStepOffsetPRVS("TIMESERIES", tsName, tsVar.getTimeStep(), ctx.timestepOffset());

                IntDouble value;
                if (prvs.isEarlierThan(INSTANCE.sds.getStudyStartDate())) {
                    // Retrieve from initial data
                    Timeseries svInit = INSTANCE.sds.getSVInitTimeseries(tsName);
                    if (svInit != null) {
                        value = svInit.retrieveDataForTime(prvs);
                        if (value != null) { return new EvalConstraint(value); }
                    }
                    // If made it this far, it means initial timeseries data was not read before; try reading it
                    svInit = tsVar.copyOf();
                    boolean success = svInit.readInitData(INSTANCE.sds.getCacheInit(), INSTANCE.sds.getPartA(), INSTANCE.sds.getPartF_Init(), INSTANCE.currentYear, INSTANCE.currentMonth, INSTANCE.currentDay);
                    if (success) {
                        value = svInit.retrieveDataForTime(prvs);
                        if (value != null) {
                            INSTANCE.sds.addSVInitTimeseries(svInit);
                            return new EvalConstraint(value);
                        }
                    }
                } else {
                    // Retrieve data from Timeseries
                    value = tsVar.retrieveDataForTime(prvs);
                    if (value != null) { return new EvalConstraint(value); }
                }

                // If made it this far, value was not found; generate error
                throw new EvaluationErrorException(tsVar.fromWresl, tsVar.line, "Was not able to retrieve data from the timeseries data for the provided time index.");
            }

            // This is an ALIAS
            Alias asVar = INSTANCE.currentModelDataSet.asMap.get(varName);
            if (asVar != null) {
                // Retrieve timestep offset and make sure it is an integer number
                IntDouble temp = INSTANCE.visit(ctx.timestepOffset().expression());
                if (!temp.isInt()) {
                    throw new EvaluationErrorException("Timeseries index for ALIAS" + varName + " must be an integer value.");
                }
                int timeOffset = temp.getValue().intValue();
                String timeStep = currentModelDataSet.getTimeStep();
                ParallelVars prvs = TimeOperations.findTime(timeStep, timeOffset, INSTANCE.currentYear, INSTANCE.currentMonth, INSTANCE.currentDay);

                // Retrieve data from Alias
                IntDouble value = retrieveDataFromAlias(asVar, ctx.timestepOffset());
                return new EvalConstraint(value);
            }

            // If made it this far, variable was not found; generate error
            throw new EvaluationErrorException("Variable " + varName + " is not defined!");
        }
    }
}

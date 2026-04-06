package gov.ca.water.wresl.parsing;

import gov.ca.water.wresl.domain.IntDouble;
import gov.ca.water.wresl.domain.StudyDataSet;
import gov.ca.water.wresl.domain.Svar;
import gov.ca.water.wresl.domain.WRESLComponent;
import gov.ca.water.wresl.errors.EvaluationErrorException;
import gov.ca.water.wresl.grammar.wreslBaseVisitor;
import gov.ca.water.wresl.grammar.wreslParser;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static gov.ca.water.wresl.parsing.Utilities.*;

public class Evaluator extends wreslBaseVisitor<IntDouble> {

    private static String absReferencePath = null;                         // Absolute path of the folder that the main WRESL file is located
    private final Map<String,LookUpTable> tableSeries = new HashMap<>();   // Map that stores lookup table data

    // Enumerators
    private enum Logical {
        TRUE(1),
        FALSE(-1);
        private final int value;
        Logical(int value) {this.value = value;}
    }

    // Temporary variables that will be used by multiple methods
    Map<String,Svar> commonSvarsMap;

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
    // --- EVALUATE INITIAL DATA
    // ------------------------------------------------------------
    public static void evaluateInitialData(Map<String, Svar> parameterMap) throws EvaluationErrorException {
        // Copy parameter map into common memory
        INSTANCE.commonSvarsMap = parameterMap;

        // Loop over parameter list and evaluate
        for (String parameter : parameterMap.keySet()) {
            Svar svar = INSTANCE.commonSvarsMap.get(parameter);
            try {
                INSTANCE.commonSvarsMap.get(parameter).setData(INSTANCE.evaluate(svar));
            }
            catch (EvaluationErrorException e) {
                throw new EvaluationErrorException(svar.fromWresl, svar.line, e.getErrorMessage());
            }
        }

        // Update parameter values
        parameterMap = INSTANCE.commonSvarsMap;

        // Clear common memory
        INSTANCE.commonSvarsMap = null;
    }


    // ------------------------------------------------------------
    // --- EVALUATE A CONDITION
    // ------------------------------------------------------------
    public static boolean evaluateCondition(wreslParser.ExpressionContext expCompareParseTree, Map<String, Svar> parameterMap) {
        // Store parameter map in common memory
        INSTANCE.commonSvarsMap = parameterMap;

        IntDouble condition = INSTANCE.visit(expCompareParseTree);
        boolean result;
        if (condition.getValue().intValue() == Logical.TRUE.value) {
            result = true;
        }
        else {
            result = false;
        }

        // Clear scratch memory
        INSTANCE.commonSvarsMap = null;

        return result;
    }


    // ------------------------------------------------------------
    // --- EVALUATE A WRESL COMPONENT (SVAR, DVAR)
    // ------------------------------------------------------------
    public static IntDouble evaluate(WRESLComponent wreslData) throws EvaluationErrorException {
        // Data to store source file and line number for the WRESL component in case there is an evolution error
        String sourceFile = "";
        int line = -1;

        try {
            switch (wreslData) {
                // Evaluate Svar
                case Svar svar -> {
                    sourceFile = svar.fromWresl;
                    line = svar.line;
                    int index = -1;
                    // Process case conditions and figure out which case expression to use
                    for (int i = 0; i < svar.caseName.size(); i++) {
                        // Is case condition "always"?
                        if (svar.caseCondition.get(i).equals("always")) {
                            index = i;
                            break;
                        }
                        // Process case conditions until one of them turns true
                        else if (INSTANCE.evaluateCaseCondition(svar.caseConditionParseTree.get(i))) {
                            index = i;
                            break;
                        }
                    }

                    // If index is still -1, case conditions where not defined properly; generate error
                    if (index == -1) {
                        throw new EvaluationErrorException(sourceFile, line, "A viable condition cannot be found for Svar " + svar.name + " defined in file " + svar.fromWresl + " at line " + svar.line + "!");
                    }

                    // We know which expression to evaluate; evaluate caseExpression
                    return INSTANCE.visit(svar.caseExpressionParseTree.get(index));
                }

                default -> {
                    // Here to complete switch statement but should not need this if overall parsing code is correct
                    throw new EvaluationErrorException("WRESL data is not recognized for evaluation!");
                }
            }
        }
        catch (EvaluationErrorException e) {
            throw new EvaluationErrorException(sourceFile, line, e.getErrorMessage());
        }
    }


    // ------------------------------------------------------------
    // --- EVALUATE CASE CONDITION
    // ------------------------------------------------------------
    private boolean evaluateCaseCondition(ParseTree caseConditionTree) {
        // An IntDouble value of 0 means false, 1 means true
        IntDouble result = INSTANCE.visit(caseConditionTree);
        if (result.getValue().intValue() == 0) {
            return false;
        }
        else {
            return true;
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
        switch (ctx.opCompare().getText()) {
            // EQUALS_SIGN, DOUBLE_EQUAL
            case "=","==" -> {
                if (doubleLeft == doubleRight) {
                    return new IntDouble(Integer.valueOf(Logical.TRUE.value),true);
                }
                else {
                    return new IntDouble(Integer.valueOf(Logical.FALSE.value),true);
                }
            }
            // GREATER_THAN
            case ">" -> {
                if (doubleLeft > doubleRight) {
                    return new IntDouble(Integer.valueOf(Logical.TRUE.value),true);
                }
                else {
                    return new IntDouble(Integer.valueOf(Logical.FALSE.value),true);
                }
            }
            // GREATER_THAN_OR_EQUAL
            case ">=" -> {
                if (doubleLeft >= doubleRight) {
                    return new IntDouble(Integer.valueOf(Logical.TRUE.value),true);
                }
                else {
                    return new IntDouble(Integer.valueOf(Logical.FALSE.value),true);
                }
            }
            // LESS_THAN
            case "<" -> {
                if (doubleLeft < doubleRight) {
                    return new IntDouble(Integer.valueOf(Logical.TRUE.value),true);
                }
                else {
                    return new IntDouble(Integer.valueOf(Logical.FALSE.value),true);
                }
            }
            // LESS_THAN_OR_EQUAL
            case "<=" -> {
                if (doubleLeft <= doubleRight) {
                    return new IntDouble(Integer.valueOf(Logical.TRUE.value),true);
                }
                else {
                    return new IntDouble(Integer.valueOf(Logical.FALSE.value),true);
                }
            }
            // AND
            case ".and." -> {
                if (doubleLeft==(double)Logical.TRUE.value && doubleRight==(double)Logical.TRUE.value) {
                    return new IntDouble(Integer.valueOf(Logical.TRUE.value),true);
                }
                else {
                    return new IntDouble(Integer.valueOf(Logical.FALSE.value),true);
                }
            }
            // OR
            case ".or." -> {
                if (doubleLeft==(double)Logical.TRUE.value || doubleRight==(double)Logical.TRUE.value) {
                    return new IntDouble(Integer.valueOf(Logical.TRUE.value),true);
                }
                else {
                    return new IntDouble(Integer.valueOf(Logical.FALSE.value),true);
                }
            }
            // NOT_EQUAL
            case ".ne." -> {
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
            String function = getWreslText(ctx.preDefinedFunction());
            Number value;
            switch (function) {
                case "abs" -> {
                    value = Math.abs(arguments.get(0).getValue().doubleValue());
                    return new IntDouble(value, false);
                }
                case "int" -> {
                    value = arguments.get(0).getValue().intValue();
                    return new IntDouble(value, true);
                }
               case "real" -> {
                   value = arguments.get(0).getValue().doubleValue();
                   return new IntDouble(value, false);
               }
               case "exp" -> {
                   value = Math.exp(arguments.get(0).getValue().doubleValue());
                   return new IntDouble(value, false);
               }
               case "log" -> {
                   value = Math.log(arguments.get(0).getValue().doubleValue());
                   return new IntDouble(value, false);
               }
               case "log10" -> {
                   value = Math.log10(arguments.get(0).getValue().doubleValue());
                   return new IntDouble(value, false);
               }
               case "sqrt" -> {
                   value = Math.sqrt(arguments.get(0).getValue().doubleValue());
                   return new IntDouble(value, false);
               }
               case "round" -> {
                   value = (int)Math.round(arguments.get(0).getValue().doubleValue());
                   return new IntDouble(value, true);
               }
               case "pow" -> {
                   double base = arguments.get(0).getValue().doubleValue();
                   double exponent = arguments.get(1).getValue().doubleValue();
                   value = Math.pow(base, exponent);
                   return new IntDouble(value, false);
               }
               case "mod" -> {
                   IntDouble dividend = arguments.get(0);
                   IntDouble divisor = arguments.get(1);
                   boolean isDividendInt =  dividend.isInt();
                   boolean isDivisorInt = divisor.isInt();
                   // Make sure divisor is not zero
                   if (divisor.getValue().doubleValue() == 0.0) { throw new EvaluationErrorException("MOD function cannot use 0 as divisor!"); };
                   if (isDividendInt && isDivisorInt) {
                       return new IntDouble(dividend.getValue().intValue() % divisor.getValue().intValue(), true);
                   }
                   else if (!isDividendInt && isDivisorInt) {
                       return new IntDouble(dividend.getValue().doubleValue() % divisor.getValue().intValue(),false);
                   }
                   else if (isDividendInt && !isDivisorInt) {
                       return new IntDouble(dividend.getValue().intValue() % divisor.getValue().doubleValue(),false);
                   }
                   else {
                       return new IntDouble(dividend.getValue().doubleValue() % divisor.getValue().doubleValue(),false);
                   }
                }
               case "range" -> {
                    // NEEDS IMPLEMENTATION!
                   return null;
               }
               case "min" -> {
                    double minValue = Double.MAX_VALUE;
                    boolean intFunction = true;
                    for (int i=0; i<ctx.arguments().expression().size(); i++) {
                        IntDouble thisValue = arguments.get(i);
                        if (!thisValue.isInt()) { intFunction = false; }
                        double currentValue = thisValue.getValue().doubleValue();
                        if (currentValue < minValue) {
                            minValue = currentValue;
                        }
                    }
                    if (intFunction) {
                        return new IntDouble(Integer.valueOf((int) minValue), true);
                    }
                    else {
                        return new IntDouble(Double.valueOf(minValue), false);
                    }
               }
               case "max" -> {
                   double maxValue = Double.MIN_VALUE;
                   boolean intFunction = true;
                   for (int i=0; i<ctx.arguments().expression().size(); i++) {
                       IntDouble thisValue = arguments.get(i);
                       if (!thisValue.isInt()) { intFunction = false; }
                       double currentValue = thisValue.getValue().doubleValue();
                       if (currentValue > maxValue) {
                           maxValue = currentValue;
                       }
                   }
                   if (intFunction) {
                       return new IntDouble(Integer.valueOf((int) maxValue), true);
                   }
                   else {
                       return new IntDouble(Double.valueOf(maxValue), false);
                   }
               }
               default -> { throw new EvaluationErrorException("Error in evaluating a function!"); }
            }
        }
        else {
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

        // Find the variable in common variables
        Svar var = INSTANCE.commonSvarsMap.get(varName);
        if (var == null) {
            throw new EvaluationErrorException("Variable " + varName + " is not defined!");
        }

        // Make sure variable value is already computed
        IntDouble varData = var.getData();
        if (varData == null) {
            throw new EvaluationErrorException("Variable " + varName + " is being used before its value is computed!");
        }

        // Return data
        return varData;
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
        if (ctx.ALWAYS() != null) {
            return new IntDouble(Logical.TRUE.value, true);
        }
        else {
            return visit(ctx.expression());
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
            Number value = visit(ctx.given().expression()).getValue();
            given.put(columnName, value);
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
            }
            else if (use.equals("max")) {
                use = "maximum";
            }
        }

        // Find data in table
        IntDouble result = findDataInLookupTable(tableName, selectColumn, where, given, use);
        return result;

    }


    // ------------------------------------------------------------
    // --- HELPER METHODS FOR LOOKUP TABLE PROCESSING
    // ------------------------------------------------------------

    // Given conditions, find/calculate a value based on data in a lokkup table
    private static IntDouble findDataInLookupTable(String tableName, String select, HashMap<String, Number> where, HashMap<String, Number> given, String use) throws EvaluationErrorException {

        // If table hasn't been copied into memory yet, do so
        if (INSTANCE.tableSeries.get(tableName) == null) {
            cacheLookUpData(tableName);
        }

        // Retrieve table data
        LookUpTable lookupTable = INSTANCE.tableSeries.get(tableName);
        ArrayList<Number[]> data = lookupTable.getData();
        HashMap<String, Integer> field = lookupTable.getField();
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

        ArrayList<Number> gVList = new ArrayList<>();
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

        return calculateValue(givenValue, gVList, gVMap, use, tableName, givenError);
    }

    // Store lookup data in memory
    private static void cacheLookUpData(String tableName) throws EvaluationErrorException {
        // Lookup table filename and data
        String absoluteTableFileName = INSTANCE.absReferencePath + File.separator + "lookup" + File.separator + tableName + ".table";
        LookUpTable lookupTable = new LookUpTable();

        // Set table name
        lookupTable.setName(tableName);

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
                strLine.strip();

                // if there is nothing left in strLine, continue
                if (strLine.equals("")) {continue;}

                // If first data entry, check that it is the name of the table and read field names right after that
                if (isFirstEntry) {
                    if (!strLine.toLowerCase().equals(tableName)) { throw new EvaluationErrorException("The first line after comments in table " + tableName + ".table should be the file name without extension: " + tableName); }

                    // Second line of entry; read and process field names
                    if ((strLine = br.readLine()) == null) {throw new EvaluationErrorException("No field names were found in table " + tableName + ".table!");}
                    if (strLine.indexOf("!") != -1) { strLine = strLine.substring(0, strLine.indexOf("!")); }
                    strLine.strip();
                    String[] fieldNames = strLine.toLowerCase().split("\\s+");
                    fieldSize = fieldNames.length;
                    for (int i=0; i<fieldSize; i++) {
                        if (!isFieldNameRight(fieldNames[i])) { throw new EvaluationErrorException("Number " + (i+1) + " field name in table " + tableName + ".table, line " + line + " has a wrong format!"); }
                        lookupTable.getField().put(fieldNames[i], i);
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
                lookupTable.getData().add(fieldValues);
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
    private static IntDouble calculateValue(Number given, ArrayList<Number> gVList, Map<Number, Number> gVMap, String use, String tableName, String givenError) throws EvaluationErrorException{
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
    public static ArrayList<Number> sortNumberArray(ArrayList<Number> al, String givenError, String tableName) throws EvaluationErrorException {
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


}

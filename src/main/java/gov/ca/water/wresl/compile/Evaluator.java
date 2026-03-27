package gov.ca.water.wresl.compile;

import gov.ca.water.wresl.domain.IntDouble;
import gov.ca.water.wresl.domain.Svar;
import gov.ca.water.wresl.domain.WRESLComponent;
import gov.ca.water.wresl.errors.EvaluationErrorException;
import gov.ca.water.wresl.grammar.wreslBaseVisitor;
import gov.ca.water.wresl.grammar.wreslParser;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static gov.ca.water.wresl.compile.Utilities.*;

public class Evaluator extends wreslBaseVisitor<IntDouble> {

    private static String absReferencePath = null;                         // Absolute path of the folder that the main WRESL file is located
    private final Map<String,LookUpTable> tableSeries = new HashMap<>();   // Map that stores lookup table data

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
    // --- EVALUATE A WRESL COMPONENT (SVAR, DVAR)
    // ------------------------------------------------------------
    public static IntDouble evaluate(WRESLComponent wreslData) {

        switch (wreslData) {
            // Evaluate Svar
            case Svar svar -> {
                int index = -1;
                // Process case conditions and figure out which case expression to use
                for (int i=0; i<svar.caseName.size(); i++) {
                    // Null case condition parse tree means always
                    if (svar.caseConditionTree.get(i) == null) {
                        index = i;
                        break;
                    }
                    // Process case conditions until one of them turns true
                    else if (INSTANCE.evaluateCaseCondition(svar.caseConditionTree.get(i))) {
                        index = i;
                        break;
                    }
                }

                // If index is still -1, case conditions where not defined properly; generate error
                if (index == -1) {
                    throw new EvaluationErrorException("A viable condition cannot be found for Svar " + svar.name + " defined in file " + svar.fromWresl + " at line " + svar.line + "!");
                }

                // We know which expression to evaluate; evaluate caseExpression
                return INSTANCE.visit(svar.caseExpressionTree.get(index));
            }

            default -> {
                // Here to complete switch statement but should not need this if overall parsing code is correct
                throw new EvaluationErrorException("WRESL data is not recognized for evaluation!");
            }
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
    // --- VISITOR METHODS
    // ------------------------------------------------------------
    @Override
    // caseCondition
    public IntDouble visitCaseCondition(wreslParser.CaseConditionContext ctx) {
        return super.visitCaseCondition(ctx);
    }






    // ------------------------------------------------------------
    // --- SELECT
    // ------------------------------------------------------------
    @Override
    public IntDouble visitSelect(wreslParser.SelectContext ctx) {
        // Table name
        String tableName = getWreslText(ctx.OBJECT_NAME());

        // If table hasn't been stored in memory yet, do so
        if (this.tableSeries.get(tableName) == null) {
            retrieveLookUpData(tableName);
        }



        return new IntDouble();
    }


    // ------------------------------------------------------------
    // --- HELPER METHODS FOR LOOKUP TABLE PROCESSING
    // ------------------------------------------------------------

    // Store lookup data in memory
    private void retrieveLookUpData(String tableName) {
        // Lookup table filename and data
        String absoluteTableFileName = this.absReferencePath + File.separator + "lookup" + File.separator + tableName + ".table";
        LookUpTable lookupTable = new LookUpTable();

        // Open and process file
        try {
            BufferedReader br = new BufferedReader(new FileReader(absoluteTableFileName));
            String strLine;
            boolean isFirstEntry = true;
            while ((strLine = br.readLine()) != null) {
                // Strip comments from line; remove leading and trailing spaces
                int index = strLine.indexOf("!");
                if (index != -1) {
                    strLine = strLine.substring(0, index);
                }
                strLine.strip();

                // Remove leading and trailing spaces
            }

        }
        catch (IOException e) {

        }

    }

    // Remove comments from a text read from a Table file
    public static String removeComment(String text){
        int index = text.indexOf("!");
        if (index == -1) {
            return text;
        }
        else {
            return text.substring(0,index);
        }
    }

    // Check correctness of a field name
    public static boolean isFieldNameRight(String fieldName){
        if (Character.isDigit(fieldName.charAt(0))){
            return false;
        }
        Pattern alphaNumberic = Pattern.compile("[A-Za-z0-9_]+");
        Matcher m = alphaNumberic.matcher(fieldName);
        return m.matches();
    }


}

package gov.ca.water.utilities;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;


public final class Utilities {

    // ------------------------------------------------------------
    // --- METHODS TO RETRIEVE LOWERCASE TEXT FROM AN ANTLR TREE AND ITS NODES
    // ------------------------------------------------------------

    // Retrieve lowercase text from ParserRuleContext without stripping whitespaces
    public static String getWreslText(ParserRuleContext ctx) {
        // Start and end of the character stream
        int start = ctx.start.getStartIndex();
        int stop = ctx.stop.getStopIndex();

        // Retrieve text
        return ctx.start.getInputStream().getText(new Interval(start, stop)).toLowerCase();
    }

    // Retrieve lowercase text from TerminalNode without stripping whitespaces
    public static String getWreslText(TerminalNode terminalNode) {
        // Start and end of the character stream
        Token token = terminalNode.getSymbol();
        int start = token.getStartIndex();
        int stop = token.getStopIndex();

        // Retrieve text
        return token.getInputStream().getText(new Interval(start, stop)).toLowerCase();
    }

    // Retrieve lowercase text from ParseTree without stripping whitespaces
    public static String getWreslText(ParseTree tree) {
        // Retrieve text
        return tree.getText().toLowerCase();
    }


    // ------------------------------------------------------------
    // --- UNIT CONVERSIONS
    // ------------------------------------------------------------

     // Convert TAF to CFS, CFS to TAF, AF to CFS or CFS to AF
    public static double tafcfs(String ident, String timeStep, ParallelVars prvs){
        int days = TimeOperations.numberOfDays(prvs.dataMonth, prvs.dataYear);

        if (ident.equals("taf_cfs")) {
            if (TimeOperations.isMonthlyInterval(timeStep)) {
                return 504.1666667 / Double.valueOf(days);
            }
            else {
                return 504.1666667;
            }
        }
        else if (ident.equals("cfs_taf")) {
            if (TimeOperations.isMonthlyInterval(timeStep)) {
                return days / 504.1666667;
            }
            else {
                return 1.0 / 504.1666667;
            }
        }
        else if (ident.equals("af_cfs")) {
            if (TimeOperations.isMonthlyInterval(timeStep)) {
                return 504.1666667 / Double.valueOf(days) / 1000.0;
            }
            else {
                return 504.1666667 / 1000.0;
            }
        }
        else {
            if (TimeOperations.isMonthlyInterval(timeStep)){
                return Double.valueOf(days) / 504.1666667 * 1000.0;
            }
            else {
                return 1.0 / 504.1666667 * 1000.0;
            }
        }
    }
}

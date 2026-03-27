package gov.ca.water.wresl.compile;

import gov.ca.water.wresl.errors.EvaluationErrorException;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Utilities {

    // ------------------------------------------------------------
    // --- METHODS TO RETRIEVE TEXT FROM AN ANTLR TREE ND ITS NODES
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
    // --- COUNT NUMBER OF OCCURANCES OF A SUB-RULE IN A PARENT RULE
    // ------------------------------------------------------------
    public static <W extends ParserRuleContext, T extends ParserRuleContext> int countRuleOccurence(Class<T> findRule, List<W> ruleList) {

        if (ruleList == null) return 0;

        int count = 0;
        for (W rule : ruleList) {
            if (!rule.getRuleContexts(findRule).isEmpty()) {
                count++;
            }
        }
        return count;
    }

}

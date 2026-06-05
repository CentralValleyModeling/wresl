package gov.ca.water.wresl.parsing;

import gov.ca.water.wresl.grammar.wreslLexer;
import gov.ca.water.wresl.grammar.wreslParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

public final class Utilities {

    // Time related data
    private static final Map<String, Integer> daysInMonthMap = Map.ofEntries(
            entry("jan", 31),
            entry("feb", -1),  // Need to decide based on year
            entry("mar", 31),
            entry("apr", 30),
            entry("may", 31),
            entry("jun", 30),
            entry("jul", 31),
            entry("aug", 31),
            entry("sep", 30),
            entry("oct", 31),
            entry("nov", 30),
            entry("dec", 31));


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
    // --- TIME RELATED UTILITIES
    // ------------------------------------------------------------
    // Number of days in a given month
    public static int numberOfDays(String month, int year) {
        int days;
        if (month.equals("feb")) {
            if (isLeapYear(year)) {
                days = 29;
            }
            else {
                days = 28;
            }
        }
        else {
            days = Utilities.daysInMonthMap.get(month);
        }
        return days;
    }

    private static boolean isLeapYear(int year) {
        if (year % 4 == 0) {
            if (year % 100 != 0) {
                return true;
            }else if (year % 400 == 0) {
                return true;
            }else {
                return false;
            }
        }
        else {
            return false;
        }
    }

}

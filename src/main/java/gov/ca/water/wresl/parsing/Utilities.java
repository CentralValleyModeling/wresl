package gov.ca.water.wresl.parsing;

import gov.ca.water.wresl.grammar.wreslLexer;
import gov.ca.water.wresl.grammar.wreslParser;
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
    // --- METHODS TO RETRIEVE SOURCE FILE AND LINE NUMBER OF A RULE FROM A RULE CONTEXT
    // ------------------------------------------------------------

    // Retrieve source file
    public static String getSourceFile(ParserRuleContext ctx) {
        if (ctx == null) {
            return null;
        } else {
            return ctx.getStart().getInputStream().getSourceName();
        }
    }

    // Retrieve line number
    public static int getLine(ParserRuleContext ctx) {
        if (ctx == null) {
            return -1;
        } else {
            return ctx.getStart().getLine();
        }
    }


    // ------------------------------------------------------------
    // --- METHODS TO GENERATE PARSE TREES FROM TEXT
    // ------------------------------------------------------------

    // Generate an Expression parse tree from a string
    public static wreslParser.ExpressionContext generateExpressionParseTree(String expression) {
        CharStream charStream = CharStreams.fromString(expression);
        wreslLexer lexer = new wreslLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        wreslParser parser = new wreslParser(tokenStream);
        return parser.expression();
    }

    // Generate a GoalBody parse tree from a string
    public static wreslParser.GoalBodyContext generateGoalBodyParseTree(String expression) {
        CharStream charStream = CharStreams.fromString(expression);
        wreslLexer lexer = new wreslLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        wreslParser parser = new wreslParser(tokenStream);
        return parser.goalBody();
    }

}

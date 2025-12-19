package gov.ca.water.wresl.errors;

import gov.ca.water.wresl.grammar.wreslLexer;
import gov.ca.water.wresl.grammar.wreslParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;


public class WreslErrorListener implements ANTLRErrorListener {

    public enum Mode {
        DEFAULT,
        SILENT,
    }

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private final Mode mode;


    public WreslErrorListener() {
        this.mode = Mode.DEFAULT;  // default to throwing errors
    }

    public WreslErrorListener(Mode mode) {
        this.mode = mode;
    }

    public WreslErrorListener(String modeStr) {
        mode = Mode.valueOf(modeStr);
        new WreslErrorListener(mode);
    }

    @Override
    public void syntaxError(
            Recognizer<?, ?> recognizer,
            Object offendingSymbol,
            int line,
            int charPositionInLine,
            String msg,
            RecognitionException e
    ) {
        // Create a log message to help with the error. Messages will differ if it was during lexing or parsing
        String logMessage = msg;  // default to simple message
        if (recognizer instanceof wreslLexer) {  // error occurred during Lexing
            String symbolString = recognizer.getInputStream().toString().split("\n")[line - 1]; // `line` is 1 indexed
            logMessage = String.format(
                    "Syntax error while lexing (probably caused by bad characters). Error at line %d, column %d. Line is: \"%s\". Message: %s",
                    line, charPositionInLine, symbolString, msg
            );
        } else if (recognizer instanceof wreslParser) { // error occurred during parsing
            TokenStream stream = ((wreslParser) recognizer).getTokenStream();
            int numTokens = stream.size() - 1;  // Stream includes `EOF`, don't include that
            List<String> lineStrings = new ArrayList<>();
            for (int i = 0; i < numTokens; i++) {
                lineStrings.add(stream.get(i).getText());
            }
            String lineText = String.join(" ", lineStrings);
            logMessage = String.format(
                    "Syntax error while parsing (probably caused by bad structure). Error at line %d, column %d. Line is: \"%s\". Message: %s",
                    line, charPositionInLine, lineText, msg
            );
        }

        switch (mode) {
            case DEFAULT:
                logger.atError().setMessage(logMessage).setCause(e).log();
                throw new RuntimeException(logMessage, e);
            case SILENT:
                break;
        }
    }

    @Override
    public void reportAmbiguity(
            Parser recognizer,
            DFA dfa,
            int startIndex,
            int stopIndex,
            boolean exact,
            BitSet ambigAlts,
            ATNConfigSet configs
    ) {
        String logMessage = String.format("Ambiguity detected between tokens %d and %d", startIndex, stopIndex);
        switch (mode) {
            case DEFAULT:
                logger.atWarn().setMessage(logMessage).log();
            case SILENT:
                break;
        }
    }

    @Override
    public void reportAttemptingFullContext(
            Parser recognizer,
            DFA dfa,
            int startIndex,
            int stopIndex,
            BitSet conflictingAlts,
            ATNConfigSet configs
    ) {

        switch (mode) {
            case DEFAULT:
                logger.atTrace().log("Attempting to resolve ambiguity, reverting to slower LL* parsing");
            case SILENT:
                break;
        }
    }

    @Override
    public void reportContextSensitivity(
            Parser recognizer,
            DFA dfa,
            int startIndex,
            int stopIndex,
            int prediction,
            ATNConfigSet configs
    ) {
        switch (mode) {
            case DEFAULT:
                TokenStream ts = recognizer.getInputStream();
                Token start = ts.get(startIndex);
                String ruleName = recognizer.getRuleNames()[dfa.atnStartState.ruleIndex];
                String text = ts.getText(Interval.of(startIndex, stopIndex));
                String logMessage = String.format(
                        "Context sensitivity resolved. rule: \"%s\", tokens: \"%s\"",
                        ruleName, text
                );
                logger.atTrace().setMessage(logMessage).log();
            case SILENT:
                break;
        }
    }
}


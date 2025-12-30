package gov.ca.water.wresl.errors;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.BitSet;


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
        switch (mode) {
            case DEFAULT:
                logger.atError()
                        .setMessage("WRESL+ Syntax error at line {}, column {}. {}")
                        .addArgument(line)
                        .addArgument(charPositionInLine)
                        .addArgument(msg)
                        .setCause(e)
                        .log();
                throw new SyntaxException(line, charPositionInLine, "WRESL+ Syntax error", e);
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

        switch (mode) {
            case DEFAULT:
                TokenStream tokens = recognizer.getInputStream();
                String inputText = tokensToText(tokens, startIndex, stopIndex);
                String ruleName = recognizer.getRuleNames()[recognizer.getContext().getRuleIndex()];
                logger.atTrace()
                        .setMessage("""
                                Ambiguity detected, details below:
                                ========================================================================
                                rule           \t{}
                                ------------------------------------------------------------------------
                                {}
                                ========================================================================"""
                        )
                        .addArgument(ruleName)
                        .addArgument("\"" + inputText + "\"")
                        .log();
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

    private String tokensToText(TokenStream tokens, int start, int stop) {
        if (tokens instanceof CommonTokenStream) {
            CharStream input = tokens.getTokenSource().getInputStream();
            int startChar = tokens.get(start).getStartIndex();
            int stopChar = tokens.get(stop).getStopIndex();
            return input.getText(Interval.of(startChar, stopChar));
        } else {
            // Fallback: concatenate token text
            StringBuilder sb = new StringBuilder();
            for (int i = start; i <= stop; i++) {
                sb.append(tokens.get(i).getText());
            }
            return sb.toString();
        }
    }
}


package gov.ca.water.wresl.errors;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.ArrayList;
import java.util.List;

public class WRESLErrorListener extends BaseErrorListener {

    private boolean hasSyntaxError = false;
    List<String> errorMessages = new ArrayList<>();

    // ------------------------------------------------------------
    // --- COLLECT SYNTAX ERRORS
    // ------------------------------------------------------------
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        errorMessages.add("Error at line " + line + " character " + (charPositionInLine+1) + ": " + msg);
        this.hasSyntaxError = true;
    }


    // ------------------------------------------------------------
    // --- WERE THERE ANY SYNTAX ERRORS?
    // ------------------------------------------------------------
    public boolean hasSyntaxError() {
        return this.hasSyntaxError;
    }


    // ------------------------------------------------------------
    // --- RETRIEVE ERROR MESSAGES
    // ------------------------------------------------------------
    public List<String> getErrorMessages() {
        return this.errorMessages;
    }
}

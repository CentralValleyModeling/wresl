package gov.ca.water.wresl.errors;

import java.util.ArrayList;
import java.util.List;

public class EvaluationErrorException extends RuntimeException {
    private String sourceFile = "";
    private int line = -1;
    private String errorMessage = "";


    // ------------------------------------------------------------
    // --- CONSTRUCTORS
    // ------------------------------------------------------------
    public EvaluationErrorException(String sourceFile, int line, String errorMessage) {
        this.sourceFile = sourceFile;
        this.line = line;
        this.errorMessage = errorMessage;
    }

    public EvaluationErrorException(String errorMessage) {
        this.errorMessage =  errorMessage;
    }


    // ------------------------------------------------------------
    // --- RETRIEVE ERROR MESSAGES
    // ------------------------------------------------------------
    public String getErrorMessage() {
        return this.errorMessage;
    }


    // ------------------------------------------------------------
    // --- RETRIEVE FILENAME WHERE ERROR OCCURED
    // ------------------------------------------------------------
    public String getSourceFile() {
        return this.sourceFile;
    }


    // ------------------------------------------------------------
    // --- RETRIEVE LINE NUMBER WHERE ERROR OCCURED
    // ------------------------------------------------------------
    public int getLine() {
        return this.line;
    }
}

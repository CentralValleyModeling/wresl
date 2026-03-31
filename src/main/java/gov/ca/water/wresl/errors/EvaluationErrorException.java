package gov.ca.water.wresl.errors;

import java.nio.file.Path;
import java.util.List;

public class EvaluationErrorException extends RuntimeException {
    private String sourceFile = "";
    private int line = -1;
    private String errorMessage;


    // ------------------------------------------------------------
    // --- CONSTRUCTORS
    // ------------------------------------------------------------
    public EvaluationErrorException(String sourceFile, int line, String errorMessage) {
        this.sourceFile = sourceFile;
        this.line = line;
        this.errorMessage =  errorMessage;
    }

    public EvaluationErrorException(String errorMessage) {
        this.errorMessage =  errorMessage;
    }


    // ------------------------------------------------------------
    // --- GETTER
    // ------------------------------------------------------------
    public String getErrorMessage() {
        return this.errorMessage;
    }

    public String getSourceFile() {
        return this.sourceFile;
    }

    public int getLine() {
        return this.line;
    }
}

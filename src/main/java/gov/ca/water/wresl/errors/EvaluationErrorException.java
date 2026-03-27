package gov.ca.water.wresl.errors;

import java.nio.file.Path;
import java.util.List;

public class EvaluationErrorException extends RuntimeException {
    private String errorMessage;

    // ------------------------------------------------------------
    // --- CONSTRUCTOR
    // ------------------------------------------------------------
    public EvaluationErrorException(String errorMessage) {
        this.errorMessage =  errorMessage;
    }

    // ------------------------------------------------------------
    // --- GETTER
    // ------------------------------------------------------------
    public String getErrorMessage() {
        return this.errorMessage;
    }
}

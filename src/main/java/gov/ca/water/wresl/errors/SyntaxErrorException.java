package gov.ca.water.wresl.errors;

import java.nio.file.Path;
import java.util.List;

public class SyntaxErrorException extends RuntimeException {
    private String sourceFile;
    private int line;
    private List<String> errorMessages;

    // ------------------------------------------------------------
    // --- CONSTRUCTOR
    // ------------------------------------------------------------
    public SyntaxErrorException(String sourceFile, int line, List<String> errorMessages) {
        this.sourceFile = sourceFile;
        this.line = line;
        this.errorMessages =  errorMessages;
    }


    // ------------------------------------------------------------
    // --- RETRIEVE ERROR MESSAGES
    // ------------------------------------------------------------
    public List<String> getErrorMessages() {
        return this.errorMessages;
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

package gov.ca.water.wresl.errors;

import java.util.ArrayList;
import java.util.List;

public class SyntaxErrorException extends RuntimeException {
    private String sourceFile = "";
    private int line = -1;
    private List<String> errorMessages = new ArrayList<>();


    // ------------------------------------------------------------
    // --- CONSTRUCTORS
    // ------------------------------------------------------------
    public SyntaxErrorException(String sourceFile, int line, List<String> errorMessages) {
        this.sourceFile = sourceFile;
        this.line = line;
        this.errorMessages =  errorMessages;
    }

    public SyntaxErrorException(String sourceFile, int line, String errorMessage) {
        this.sourceFile = sourceFile;
        this.line = line;
        this.errorMessages.add(errorMessage);
    }

    public SyntaxErrorException(List<String> errorMessages) {
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

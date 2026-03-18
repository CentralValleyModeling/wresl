package gov.ca.water.wresl.errors;

import java.nio.file.Path;
import java.util.List;

public class SyntaxErrorException extends Exception {
    private Path sourceFile;
    private List<String> errorMessages;

    // ------------------------------------------------------------
    // --- CONSTRUCTOR
    // ------------------------------------------------------------
    public SyntaxErrorException(Path sourceFile, List<String> errorMessages) {
        this.sourceFile = sourceFile;
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
    public Path getSourceFile() {
        return this.sourceFile;
    }

 }

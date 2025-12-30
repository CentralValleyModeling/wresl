package gov.ca.water.wresl.errors;

public class SyntaxException extends RuntimeException {
    public int line;
    public int column;

    SyntaxException(int line, int column, String errorMessage) {
        super(errorMessage);
        this.line = line;
        this.column = column;
    }

    SyntaxException(int line, int column, String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.line = line;
        this.column = column;
    }
}

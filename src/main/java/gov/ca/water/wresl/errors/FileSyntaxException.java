package gov.ca.water.wresl.errors;

import java.nio.file.Path;
import java.util.Deque;

public class FileSyntaxException extends RuntimeException {
    public int line;
    public int column;
    public Path source;

    FileSyntaxException(int line, int column, Path source, String errorMessage) {
        super(errorMessage);
        this.line = line;
        this.column = column;
        this.source = source;
    }
    FileSyntaxException(int line, int column, Path source, String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.line = line;
        this.column = column;
        this.source = source;
    }

    public FileSyntaxException(Path source, SyntaxException cause) {
        super(getHumanReadableErrorLocationString(source, cause.line, cause.column), cause);
        this.line = cause.line;
        this.column = cause.column;
        this.source = source;
    }

    private static String getHumanReadableErrorLocationString(Path source, int line, int column) {
        int contextLines = 3;
        try (java.io.BufferedReader reader = java.nio.file.Files.newBufferedReader(source)) {
            // accumulate a section of the file to show in the error
            Deque<String> window = new java.util.ArrayDeque<>(contextLines + 1);
            String current;
            for (int i = 1; i <= line; i++) {
                current = reader.readLine();
                if (current == null) {
                    throw new IllegalArgumentException("Line " + line + " not found");
                }
                if (i > (line - contextLines)) {
                    window.addLast(current);
                }
            }
            // now create the actual error message
            StringBuilder message = new StringBuilder();
            message.append(".".repeat(72)).append(System.lineSeparator());
            for (String l : window) {
                message.append(l).append(System.lineSeparator());
            }
            int caretPos = Math.max(0, column);
            message.append(" ".repeat(caretPos)).append('^').append(System.lineSeparator());
            message.append(".".repeat(72));

            return "Lines that created the exception below:" + System.lineSeparator() + message;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to create message for file " + source +
                            " at line " + line + ", column " + column, e
            );
        }
    }
}

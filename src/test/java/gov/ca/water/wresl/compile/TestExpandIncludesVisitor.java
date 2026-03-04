package gov.ca.water.wresl.compile;

import com.github.valfirst.slf4jtest.LoggingEvent;
import com.github.valfirst.slf4jtest.TestLogger;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import gov.ca.water.wresl.errors.WreslErrorListener;
import gov.ca.water.wresl.grammar.TestLexer;
import gov.ca.water.wresl.grammar.wreslParser;
import org.antlr.v4.runtime.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TestExpandIncludesVisitor {
    TestLogger logger = TestLoggerFactory.getTestLogger(ExpandIncludesListener.class);
    private static final WreslErrorListener errorListener = new WreslErrorListener();

    static wreslParser createParser(List<Token> tokens) {
        ListTokenSource source = new ListTokenSource(tokens);
        CommonTokenStream stream = new CommonTokenStream(source);
        wreslParser parser = new wreslParser(stream);
        parser.addErrorListener(errorListener);

        return parser;
    }

    static wreslParser createParser(Path source) {
        List<Token> tokens = List.of();
        try {
            tokens = TestLexer.getTokens(source);
        } catch (IOException e) {
            fail("IOException encountered, failure by default", e); // Fail on any error
        }
        return createParser(tokens);  // tokens may be empty
    }

    @Test
    public void testLinearInclude() {
        Path root = Path.of("src/test/resources/compiling/expand_includes/linear").toAbsolutePath();
        ExpandIncludesListener visitor = new ExpandIncludesListener();
        Map<Path, wreslParser.StudyContext> trees = visitor.startVisitingFromFile(root.resolve("A.wresl"));
        Set<Path> paths = visitor.getFileSet();
        Set<String> pathNames = paths
                .stream()
                .map(Path::getFileName)
                .map(Path::toString)
                .collect(Collectors.toSet());
        assertEquals(Set.of("A.wresl", "B.wresl", "C.wresl"), pathNames);
        assertEquals(paths, trees.keySet());
    }

    @Test
    public void testCircularInclude() {
        Path root = Path.of("src/test/resources/compiling/expand_includes/circular").toAbsolutePath();
        ExpandIncludesListener visitor = new ExpandIncludesListener();
        Map<Path, wreslParser.StudyContext> trees = visitor.startVisitingFromFile(root.resolve("A.wresl"));
        Set<Path> paths = visitor.getFileSet();
        Set<String> pathNames = paths
                .stream()
                .map(Path::getFileName)
                .map(Path::toString)
                .collect(Collectors.toSet());
        assertEquals(Set.of("A.wresl", "B.wresl", "C.wresl"), pathNames);
        assertEquals(paths, trees.keySet());
        String expectedMessage = "included (circular): {}";
        Set<String> messages = logger.getLoggingEvents().stream().map(LoggingEvent::getMessage).collect(Collectors.toSet());
        assertThat(messages).contains(expectedMessage);
    }

    @Test
    public void testCacheInclude() {
        Path root = Path.of("src/test/resources/compiling/expand_includes/cache").toAbsolutePath();
        ExpandIncludesListener visitor = new ExpandIncludesListener();
        Map<Path, wreslParser.StudyContext> trees = visitor.startVisitingFromFile(root.resolve("A.wresl"));
        Set<Path> paths = visitor.getFileSet();
        Set<String> pathNames = paths
                .stream()
                .map(Path::getFileName)
                .map(Path::toString)
                .collect(Collectors.toSet());
        assertEquals(Set.of("A.wresl", "B.wresl", "C.wresl"), pathNames);
        assertEquals(paths, trees.keySet());
        String expectedMessage = "included (again): {}";
        Set<String> messages = logger.getLoggingEvents().stream().map(LoggingEvent::getMessage).collect(Collectors.toSet());
        assertThat(messages).contains(expectedMessage);
    }

}

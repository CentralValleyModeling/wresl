package gov.ca.water.wresl.grammar;

import gov.ca.water.wresl.errors.WreslErrorListener;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

public class TestLexer {

    private static final Logger logger = LoggerFactory.getLogger(TestLexer.class);
    private static final WreslErrorListener errorListener = new WreslErrorListener();

    static List<Token> getTokens(String str) throws IOException {
        ByteArrayInputStream bStream = new ByteArrayInputStream(str.getBytes());
        CharStream cStream = CharStreams.fromStream(bStream);
        wreslLexer lexer = new wreslLexer(cStream);
        lexer.addErrorListener(errorListener);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        tokenStream.fill();

        return tokenStream.getTokens();
    }

    @Test
    public void testNumberTokens() {
        record TestCase(String expression, int expectedToken) {}

        List<TestCase> expressions = List.of(
                new TestCase("123", wreslLexer.INT), // int != signed int: unsigned ints req in some contexts
                new TestCase("-456", wreslLexer.SIGNED_INT),
                new TestCase("+456", wreslLexer.SIGNED_INT),
                new TestCase("+1.23", wreslLexer.SIGNED_FLOAT),
                new TestCase("4.56", wreslLexer.SIGNED_FLOAT),  // for floats, always parse as signed
                new TestCase("4.56e-78", wreslLexer.SIGNED_FLOAT)  // scientific notation allowed
        );
        // Loop through each expression and test that the number of expectedTokens is what we expect
        for (TestCase tc : expressions) {
            try {
                List<Token> tokens = getTokens(tc.expression());
                assertEquals(2, tokens.size(), "Expected a single token"); // 1 item plus "End of File" token
                assertEquals(tc.expectedToken(), tokens.getFirst().getType(), "Wrong token type returned from the lexer.");
            } catch (IOException e) {
                fail("IOException encountered, failure by default", e); // Fail on any error
            }
        }
    }

    @Test
    public void testIncludeStatementTokens() {
        record TestCase(int expectedTokens, String expression) {}

        List<TestCase> expressions = List.of(
                new TestCase(4, "include group A"), // 4 expectedTokens: include, group, A, and EOF
                new TestCase(3, "include 'file.wresl'"), // 3 expectedTokens: include, file, EOF
                new TestCase(4, "include [local] 'file.wresl'"), // 4 expectedTokens: include, local, file, EOF
                new TestCase(4, "\n\n\ninclude group B"), // 4 expectedTokens: include, group, B, and EOF. Ignores whitespace
                new TestCase(3, "include 'folder/folder/folder/file.wresl'") // 3 expectedTokens: include, file, EOF
        );
        // Loop through each expression and test that the number of expectedTokens is what we expect
        for (TestCase tc : expressions) {
            try {
                List<Token> tokens = getTokens(tc.expression());
                assertEquals(tc.expectedTokens(), tokens.size(), String.format("%s -> %s", tc.expression(), tokens));
                assertEquals(wreslLexer.INCLUDE, tokens.getFirst().getType(), tc.expression());
            } catch (IOException e) {
                fail("IOException encountered, failure by default", e); // Fail on any error
            }
        }
    }

    @Test
    public void testIncludeStatementTokenErrors() {
        record TestCase(String expression, Class<? extends Throwable> exception) {}

        List<TestCase> expressions = List.of(
                new TestCase("include ~", RuntimeException.class), // bad character
                new TestCase("include 'file.wresl", RuntimeException.class) // missing closing quote character
        );

        for (TestCase tc : expressions) {
            assertThrows(
                    tc.exception(),
                    () -> getTokens(tc.expression()),
                    String.format("Expected failure did not occur for expression `%s`", tc.expression())
            );
        }
    }

    @Test
    public void testModelStatementTokens() {
        record TestCase(String expression) {}

        List<TestCase> expressions = List.of(
                new TestCase("model A"),
                new TestCase("model A_VERY_LONG_MODEL_NAME_THAT_TRIES_TO_BREAK_THE_LEN_LIMIT"),
                new TestCase("model A {goal G { B = C}}")
        );

        for (TestCase tc : expressions) {
            try {
                List<Token> tokens = getTokens(tc.expression());
                assertEquals(wreslLexer.MODEL, tokens.getFirst().getType(), tc.expression());
                assertEquals(wreslLexer.OBJECT_NAME, tokens.get(1).getType(), tc.expression());
            } catch (IOException e) {
                fail("IOException encountered, failure by default", e); // Fail on any error
            }
        }
    }

    @Test
    public void testCaseStatementTokens() {
        record TestCase(String expression, List<Integer> expectedTokens) {}
        // only specify as many tokens that you want to check
        List<TestCase> expressions = List.of(
                new TestCase(
                        "case A {condition always value 0 }",
                        List.of(wreslLexer.CASE ,wreslLexer.OBJECT_NAME, wreslLexer.OPEN_BRACE, wreslLexer.CONDITION, wreslLexer.ALWAYS)
                )
        );
        for (TestCase tc : expressions) {
            try {
                List<Token> tokens = getTokens(tc.expression());
                for (int i = 0; i < tc.expectedTokens().size(); i++) {
                    Integer expected = tc.expectedTokens().get(i);
                    Integer observed = tokens.get(i).getType();
                    assertEquals(
                            expected, observed,
                            String.format(
                                    "Token in position %d of expression is was lexed incorrectly: \"%s\"",
                                    i, tc.expression()
                            )
                    );
                }
            } catch (IOException e) {
                fail("IOException encountered, failure by default", e); // Fail on any error
            }
        }
    }
}

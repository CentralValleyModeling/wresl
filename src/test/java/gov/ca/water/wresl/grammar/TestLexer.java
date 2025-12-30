package gov.ca.water.wresl.grammar;

import gov.ca.water.wresl.errors.WreslErrorListener;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class TestLexer {

    private static final Logger logger = LoggerFactory.getLogger(TestLexer.class);
    private static final WreslErrorListener errorListener = new WreslErrorListener();

    static List<Token> getTokens(CharStream stream) {
        wreslLexer lexer = new wreslLexer(stream);
        lexer.addErrorListener(errorListener);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        tokenStream.fill();

        return tokenStream.getTokens();
    }

    static List<Token> getTokens(String str) throws IOException {
        ByteArrayInputStream bStream = new ByteArrayInputStream(str.getBytes());
        CharStream cStream = CharStreams.fromStream(bStream);
        return getTokens(cStream);

    }

    static List<Token> getTokens(Path source) throws IOException {
        CharStream cStream = CharStreams.fromFileName(source.toAbsolutePath().toString());
        return getTokens(cStream);
    }

    @Test
    public void testNumberTokens() {
        record TestCase(String expression, int expectedToken) {}

        List<TestCase> expressions = List.of(
                new TestCase("123", wreslLexer.INT),
                new TestCase("1.23", wreslLexer.FLOAT),
                new TestCase("4.56e-78", wreslLexer.FLOAT)  // scientific notation allowed
        );
        // Loop through each expression and test that the number of expectedTokens is what we expect
        for (TestCase tc : expressions) {
            try {
                List<Token> tokens = getTokens(tc.expression());
                assertEquals(2, tokens.size(), "Expected a single token"); // 1 item plus "End of File" token
                assertEquals(tc.expectedToken(), tokens.getFirst().getType(), "Wrong token type returned from the lexer: %s");
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
                new TestCase(6, "include [local] 'file.wresl'"), // 4 expectedTokens: include, local, file, EOF
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
                ),
                new TestCase(
                        "A+ 3.",
                        List.of(wreslLexer.OBJECT_NAME, wreslLexer.PLUS, wreslLexer.FLOAT)
                ),
                new TestCase(
                        "A+3.",
                        List.of(wreslLexer.OBJECT_NAME, wreslLexer.PLUS, wreslLexer.FLOAT)
                ),
                new TestCase(
                        "sum(i=-1, 2, 1) A(i)",
                        List.of(wreslLexer.SUM, wreslLexer.OPEN_PAREN, wreslLexer.OBJECT_NAME, wreslLexer.EQUALS_SIGN, wreslLexer.MINUS, wreslLexer.INT)
                ),
                new TestCase(
                        "define gt{value TableGen(1,109, UNIMP_OROV(iccs:icce),UNIMP_FOLS(iccs:icce),UNIMP_YUBA(iccs:icce),UNIMP_SHAS(iccs:icce),UNIMP_SRBB(iccs:icce),UNIMP_TRIN(iccs:icce),UNIMP_WH(iccs:icce),UNIMP_ST(iccs:icce),UNIMP_TU(iccs:icce),UNIMP_ME(iccs:icce),UNIMP_SJ(iccs:icce))}",
                        List.of(wreslLexer.DEFINE, wreslLexer.OBJECT_NAME, wreslLexer.OPEN_BRACE, wreslLexer.VALUE, wreslLexer.OBJECT_NAME)
                ),
                new TestCase(
                        "condition SumUD_26S_PU3_sv < .1",
                        List.of(wreslLexer.CONDITION, wreslLexer.OBJECT_NAME, wreslLexer.LESS_THAN, wreslLexer.FLOAT, wreslLexer.EOF)
                ),
                new TestCase(
                        "LOG10(A[B](C))",
                        List.of(wreslLexer.F_LOG_10, wreslLexer.OPEN_PAREN, wreslLexer.OBJECT_NAME, wreslLexer.OPEN_BRACKET, wreslLexer.OBJECT_NAME, wreslLexer.CLOSE_BRACKET)
                )
        );
        for (TestCase tc : expressions) {
            try {
                List<Token> tokens = getTokens(tc.expression());
                int size = Math.min(tc.expectedTokens.size(), tokens.size());
                for (int i = 0; i < size; i++) {
                    Integer expected = tc.expectedTokens().get(i);
                    Integer observed = tokens.get(i).getType();
                    assertEquals(
                            expected, observed,
                            String.format(
                                    "Token in position %d of expression is was lexed incorrectly for expression: \"%s\"",
                                    i, tc.expression()
                            )
                    );
                }
                assertTrue(tc.expectedTokens.size() <= tokens.size(), "Lexer produced too few tokens to fully compare");
            } catch (IOException e) {
                fail("IOException encountered, failure by default", e); // Fail on any error
            }
        }
    }

    @Test
    public void testTokensFromFiles() {
        record TestCase(Path file, List<Integer> expectedTokens) {}
        // only specify as many tokens that you want to check
        List<TestCase> expressions = List.of(
                new TestCase(
                        Path.of("src/test/resources/parsing/include/include.wresl"),
                        List.of(wreslLexer.INCLUDE, wreslLexer.SINGLE_QUOTE_STRING, wreslLexer.EOF)
                ),
                new TestCase(
                        Path.of("src/test/resources/parsing/include/include_group.wresl"),
                        List.of(wreslLexer.INCLUDE, wreslLexer.GROUP, wreslLexer.OBJECT_NAME, wreslLexer.EOF)
                ),
                new TestCase(
                        Path.of("src/test/resources/parsing/include/include_scoped.wresl"),
                        List.of(wreslLexer.INCLUDE, wreslLexer.OPEN_BRACKET, wreslLexer.GLOBAL, wreslLexer.CLOSE_BRACKET, wreslLexer.SINGLE_QUOTE_STRING)
                ),
                new TestCase(
                        Path.of("src/test/resources/parsing/group/group.wresl"),
                        List.of(wreslLexer.GROUP, wreslLexer.OBJECT_NAME, wreslLexer.OPEN_BRACE, wreslLexer.INCLUDE, wreslLexer.SINGLE_QUOTE_STRING, wreslLexer.CLOSE_BRACE, wreslLexer.EOF)
                )
        );
        for (TestCase tc : expressions) {
            try {
                List<Token> tokens = getTokens(tc.file());
                int size = Math.min(tc.expectedTokens.size(), tokens.size());
                for (int i = 0; i < size; i++) {
                    Integer expected = tc.expectedTokens().get(i);
                    Integer observed = tokens.get(i).getType();
                    assertEquals(
                            expected, observed,
                            String.format(
                                    "Token in position %d of file is was lexed incorrectly for file: `%s`",
                                    i, tc.file()
                            )
                    );
                }
                assertTrue(tc.expectedTokens.size() <= tokens.size(), "Lexer produced too few tokens to fully compare");
            } catch (IOException e) {
                fail("IOException encountered, failure by default", e); // Fail on any error
            }
        }
    }
}

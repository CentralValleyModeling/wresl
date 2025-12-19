package gov.ca.water.wresl.grammar;

import gov.ca.water.wresl.errors.WreslErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.ListTokenSource;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

public class TestParser {
    private static final Logger logger = LoggerFactory.getLogger(TestParser.class);
    private static final WreslErrorListener errorListener = new WreslErrorListener();

    static wreslParser createParser(List<Token> tokens) {
        ListTokenSource source = new ListTokenSource(tokens);
        CommonTokenStream stream = new CommonTokenStream(source);
        wreslParser parser = new wreslParser(stream);
        parser.addErrorListener(errorListener);

        return parser;
    }

    static wreslParser createParser(String expression) {
        List<Token> tokens = List.of();
        try {
            tokens = TestLexer.getTokens(expression);
        } catch (IOException e) {
            fail("IOException encountered, failure by default", e); // Fail on any error
        }
        return createParser(tokens);  // tokens may be empty
    }

    @Test
    public void testIncludeStatements() {
        record TestCase(String expression) {}

        // examples of valid expressions
        List<TestCase> expressions = List.of(
                new TestCase("include group A"),
                new TestCase("include [local] 'file.wresl'"),
                new TestCase("include 'file.wresl'"),
                new TestCase("include 'folder/folder/folder/file.wresl'"), // folders should be good too
                new TestCase("\n\n\ninclude  group              B") // lots of extra whitespace should be fine
        );

        // Loop through each expression and make sure they parse
        for (TestCase tc : expressions) {
            wreslParser parser = createParser(tc.expression());
            wreslParser.StartContext ctx = parser.start();
            assertEquals(wreslLexer.INCLUDE, ctx.start.getType());
        }
    }

    @Test
    public void testIncludeStatementErrors() {
        record TestCase(String expression, Class<? extends Throwable> exception) {}

        List<TestCase> cases = List.of(
                new TestCase("include model", RuntimeException.class),
                new TestCase("include group 'file.wresl'", RuntimeException.class)
        );

        for (TestCase tc : cases) {
            wreslParser parser = createParser(tc.expression());
            assertThrows(
                    tc.exception(),
                    parser::start,
                    () -> "Expected failure did not occur for expression `" + tc.expression() + "`"
            );
        }
    }

    @Test
    public void testModelStatements() {
        record TestCase(String expression) {}

        // examples of valid expressions
        List<TestCase> expressions = List.of(
                new TestCase("model A {include group A}"),
                new TestCase("model B {goal G {B = C}}")
        );

        // Loop through each expression and make sure they parse
        for (TestCase tc : expressions) {
            wreslParser parser = createParser(tc.expression());
            wreslParser.StartContext ctx = parser.start();
            assertEquals(wreslLexer.MODEL, ctx.start.getType());
        }
    }

    @Test
    public void testDefineStatementTypes() {
        record TestCase(Class<? extends ParserRuleContext> expectedClass, String expression) {}
        // examples of valid expressions
        List<TestCase> expressions = List.of(
                new TestCase(wreslParser.DvarContext.class, "define A { std kind 'TEST' units 'UNITS'}"),
                new TestCase(wreslParser.DvarContext.class, "dvar   B { lower 0 upper 1 units 'UNITS'}"),
                new TestCase(wreslParser.DvarContext.class, "dvar   C { alias Y + Z units 'UNITS'}"),
                new TestCase(wreslParser.SvarContext.class, "define D { timeseries 'DIFF' kind 'TEST' units 'UNITS'}"),
                new TestCase(wreslParser.SvarContext.class, "define E { timeseries kind 'TEST' units 'UNITS'}"),
                new TestCase(wreslParser.SvarContext.class, "define F { value 3.14}"),
                new TestCase(wreslParser.SvarContext.class, "define G { select COL from TABLE where IDX = ROW}"),
                new TestCase(wreslParser.SvarContext.class, "define H { external 'file.dll'}"),
                new TestCase(wreslParser.SvarContext.class, "define I { case otherwise { condition always value Z } }"),
                new TestCase(wreslParser.SvarContext.class, "define J { sum(i=0, SEP-month, 1) VAR(i) }"),
                new TestCase(wreslParser.DvarContext.class, "define K { integer std units 'TEST' }"),
                new TestCase(wreslParser.DvarContext.class, "define(size) L {std units 'TEST'}"),
                new TestCase(wreslParser.DvarContext.class, "define[local] M {std units 'TEST'}")
        );

        // Loop through each expression and make sure they parse
        for (TestCase tc : expressions) {
            wreslParser parser = createParser(tc.expression());
            wreslParser.StartContext ctx = parser.start();
            assertEquals(tc.expectedClass(), ctx.children.getFirst().getClass());
        }
    }

    @Test
    public void testGoalStatementSimple() {
        String expression = "goal A { Y = Z }";
        wreslParser parser = createParser(expression);
        wreslParser.StartContext ctx = parser.start();

        assertEquals(wreslParser.GoalContext.class, ctx.children.getFirst().getClass());

        wreslParser.GoalContext goal = (wreslParser.GoalContext) ctx.children.getFirst();
        assertEquals("A", goal.OBJECT_NAME().getText());

        wreslParser.GoalBodyContext body = goal.goalBody();
        assertEquals(wreslParser.GoalShortFormContext.class, body.goalShortForm().getClass());

        wreslParser.GoalShortFormContext bodyInner = body.goalShortForm();
        String leftVar = bodyInner.expression(0).getText();
        String comp = bodyInner.opComp().getText();
        String rightVar = bodyInner.expression(1).getText();
        assertEquals("Y", leftVar);
        assertEquals("=", comp);
        assertEquals("Z", rightVar);
    }

    @Test
    public void testGoalStatementViaPenalty() {
        String expression = "goal A { lhs Y rhs Z lhs > rhs penalty P lhs<rhs penalty 9999 }";
        wreslParser parser = createParser(expression);
        wreslParser.StartContext ctx = parser.start();

        assertEquals(wreslParser.GoalContext.class, ctx.children.getFirst().getClass());

        wreslParser.GoalContext goal = (wreslParser.GoalContext) ctx.children.getFirst();
        assertEquals("A", goal.OBJECT_NAME().getText());

        wreslParser.GoalBodyContext body = goal.goalBody();
        assertEquals(wreslParser.GoalViaPenaltyContext.class, body.goalViaPenalty().getClass());

        wreslParser.GoalViaPenaltyContext bodyInner = body.goalViaPenalty();
        String firstSide = bodyInner.SIDE(0).getText();
        String secondSide = bodyInner.SIDE(1).getText();
        assertEquals("lhs", firstSide);
        assertEquals("rhs", secondSide);

        String firstPenalty = bodyInner.penalty(0).penaltyValue().expression().getText();
        String secondPenalty = bodyInner.penalty(1).penaltyValue().expression().getText();
        assertEquals("P", firstPenalty);
        assertEquals("9999", secondPenalty);
    }

}

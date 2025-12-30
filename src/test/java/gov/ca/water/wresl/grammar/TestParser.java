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

import gov.ca.water.wresl.grammar.wreslParser.*;

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
            StartContext ctx = parser.start();
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
            StartContext ctx = parser.start();
            assertEquals(wreslLexer.MODEL, ctx.start.getType());
        }
    }

    @Test
    public void testDefineStatementTypes() {
        record TestCase(Class<? extends ParserRuleContext> expectedClass, String expression) {}
        // examples of valid expressions
        List<TestCase> expressions = List.of(
                new TestCase(DvarContext.class, "define A { std kind 'TEST' units 'UNITS'}"),
                new TestCase(DvarContext.class, "dvar   B { lower 0 upper 1 units 'UNITS'}"),
                new TestCase(DvarContext.class, "dvar   C { alias Y + Z units 'UNITS'}"),
                new TestCase(SvarContext.class, "define D { timeseries 'DIFF' kind 'TEST' units 'UNITS'}"),
                new TestCase(SvarContext.class, "define E { timeseries kind 'TEST' units 'UNITS'}"),
                new TestCase(SvarContext.class, "define F { value 3.14}"),
                new TestCase(SvarContext.class, "define G { select COL from TABLE where IDX = ROW}"),
                new TestCase(SvarContext.class, "define H { external 'file.dll'}"),
                new TestCase(SvarContext.class, "define I { case otherwise { condition always value Z } }"),
                new TestCase(SvarContext.class, "define J { sum(i=0, SEP-month, 1) VAR(i) }"),
                new TestCase(DvarContext.class, "define K { integer std units 'TEST' }"),
                new TestCase(DvarContext.class, "define(size) L {std units 'TEST'}"),
                new TestCase(DvarContext.class, "define[local] M {std units 'TEST'}")
        );

        // Loop through each expression and make sure they parse
        for (TestCase tc : expressions) {
            wreslParser parser = createParser(tc.expression());
            StartContext ctx = parser.start();
            assertEquals(tc.expectedClass(), ctx.getChild(0).getClass());
        }
    }

    @Test
    public void testGoalStatementSimple() {
        String expression = "goal A { Y = Z }";
        wreslParser parser = createParser(expression);
        StartContext ctx = parser.start();

        assertEquals(GoalContext.class, ctx.getChild(0).getClass());

        GoalContext goal = (GoalContext) ctx.getChild(0);
        assertEquals("A", goal.OBJECT_NAME().getText());

        GoalBodyContext body = goal.goalBody();
        assertEquals(GoalShortFormContext.class, body.goalShortForm().getClass());

        GoalShortFormContext bodyInner = body.goalShortForm();
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
        StartContext ctx = parser.start();

        assertEquals(GoalContext.class, ctx.getChild(0).getClass());

        GoalContext goal = (GoalContext) ctx.getChild(0);
        assertEquals("A", goal.OBJECT_NAME().getText());

        GoalBodyContext body = goal.goalBody();
        assertEquals(GoalViaPenaltyContext.class, body.goalViaPenalty().getClass());

        GoalViaPenaltyContext bodyInner = body.goalViaPenalty();
        String firstSide = bodyInner.SIDE(0).getText();
        String secondSide = bodyInner.SIDE(1).getText();
        assertEquals("lhs", firstSide);
        assertEquals("rhs", secondSide);

        String firstPenalty = bodyInner.penalty(0).penaltyValue().expression().getText();
        String secondPenalty = bodyInner.penalty(1).penaltyValue().expression().getText();
        assertEquals("P", firstPenalty);
        assertEquals("9999", secondPenalty);
    }

    @Test
    public void testNestedSumExpression() {
        String expression = "define A {value max(2., min(A+3., C))} ";
        wreslParser parser = createParser(expression);
        StartContext ctx = parser.start();

        assertEquals(SvarContext.class, ctx.getChild(0).getClass());

        SvarContext svar = (SvarContext) ctx.getChild(0);
        assertEquals("A", svar.OBJECT_NAME().getText());

        DefineViaValueContext inner = svar.svarBody().immediateSvarBody().defineViaValue();
        CallExpressionContext call = (CallExpressionContext) inner.expression();
        assertEquals("max", call.getChild(0).getText());
        assertEquals("2.", call.arguments().expression().getFirst().getText());
    }

    @Test
    public void testCallExpressionWithCycleAndTimestepReferences() {
        String expression  = "LOG10(A[B]($m-1))";

        wreslParser parser = createParser(expression);
        CallExpressionContext ctx = (CallExpressionContext) parser.expression();
        assertEquals(PreDefinedFunctionContext.class, ctx.getChild(0).getClass());
        assertEquals(ArgumentsContext.class, ctx.children.get(2).getClass());

        ArgumentsContext args = ctx.arguments();
        assertEquals(ObjectReferenceContext.class, args.getChild(0).getChild(0).getClass());

        ExpressionContext exp = args.expression().getFirst();
        ObjectReferenceContext obj = (ObjectReferenceContext) exp.getChild(0);
        assertEquals("A", obj.OBJECT_NAME().getText());
        assertEquals("B", obj.scope().scopeBody().getText());

        TimestepOffsetContext ts = obj.timestepOffset();
        assertEquals("($m-1)", ts.getText());

        ReferenceExpressionContext m = (ReferenceExpressionContext) ts.expression().getChild(0);
        assertEquals(ArrayMaximumReferenceContext.class, m.variableReference().getClass());
    }
}

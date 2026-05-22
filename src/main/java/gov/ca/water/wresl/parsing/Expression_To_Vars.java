package gov.ca.water.wresl.parsing;

import gov.ca.water.wresl.grammar.wreslBaseVisitor;
import gov.ca.water.wresl.grammar.wreslParser;

import java.util.ArrayList;
import java.util.List;

import static gov.ca.water.wresl.parsing.Utilities.getWreslText;

public class Expression_To_Vars extends wreslBaseVisitor<List<String>> {

    @Override
    // expressionMultDiv
    public List<String> visitExpressionMultDiv(wreslParser.ExpressionMultDivContext ctx) {
        List<String> varList = new ArrayList<>();

        // Left expression
        List<String> leftVars = visit(ctx.expression(0));
        if (leftVars != null) { varList.addAll(leftVars); }

        // Right expression
        List<String> rightVars = visit(ctx.expression(1));
        if (rightVars != null) { varList.addAll(rightVars); }

        return varList;
    }


    @Override
    // expressionAddSub
    public List<String> visitExpressionAddSub(wreslParser.ExpressionAddSubContext ctx) {
        List<String> varList = new ArrayList<>();

        // Left expression
        List<String> leftVars = visit(ctx.expression(0));
        if (leftVars != null) { varList.addAll(leftVars); }

        // Right expression
        List<String> rightVars = visit(ctx.expression(1));
        if (rightVars != null) { varList.addAll(rightVars); }

        return varList;
    }

    @Override
    // expressionComparison
    public List<String> visitExpressionComparison(wreslParser.ExpressionComparisonContext ctx) {
        List<String> varList = new ArrayList<>();

        // Left expression
        List<String> leftVars = visit(ctx.expression(0));
        if (leftVars != null) { varList.addAll(leftVars); }

        // Right expression
        List<String> rightVars = visit(ctx.expression(1));
        if (rightVars != null) { varList.addAll(rightVars); }

        return varList;
    }


    @Override
    // expressionNot
    public List<String> visitExpressionNot(wreslParser.ExpressionNotContext ctx) {
        List<String> varList = visit(ctx.expression());
        return varList;
    }


    @Override
    // expressionSigned
    public List<String> visitExpressionSigned(wreslParser.ExpressionSignedContext ctx) {
        List<String> varList = visit(ctx.expression());
        return varList;
    }


    @Override
    // expressionSum
    public List<String> visitExpressionSum(wreslParser.ExpressionSumContext ctx) {
        List<String> varList = new ArrayList<>();

        List<String> expressionVars;

        expressionVars = visit(ctx.sumExpressionBody().sumBegin().expression());
        if (expressionVars != null) { varList.addAll(expressionVars); }

        expressionVars = visit(ctx.sumExpressionBody().sumEnd().expression());
        if (expressionVars != null) { varList.addAll(expressionVars); }

        expressionVars = visit(ctx.sumExpressionBody().sumStep().expression());
        if (expressionVars != null) { varList.addAll(expressionVars); }

        expressionVars = visit(ctx.sumExpressionBody().accumulatingExpression().expression());
        if (expressionVars != null) { varList.addAll(expressionVars); }

        return varList;
    }


    @Override
    // expressionCall
    public List<String> visitExpressionCall(wreslParser.ExpressionCallContext ctx) {
        List<String> varList = new ArrayList<>();

        if (ctx.OBJECT_NAME() != null) {
            varList.add(getWreslText(ctx.OBJECT_NAME()));
        }

        if (ctx.arguments() != null) {
            for (wreslParser.ExpressionContext expr : ctx.arguments().expression()) {
                List<String> expressionVars = visit(expr);
                if (expressionVars != null) { varList.addAll(visit(expr)); }
            }
        }

        return varList;
    }


    @Override
    // expressionSlice
    public List<String> visitExpressionSlice(wreslParser.ExpressionSliceContext ctx) {
        List<String> varList = new ArrayList<>();

        // Left expression
        List<String> leftVars = visit(ctx.expression(0));
        if (leftVars != null) { varList.addAll(leftVars); }

        // Right expression
        List<String> rightVars = visit(ctx.expression(1));
        if (rightVars != null) { varList.addAll(rightVars); }

        return varList;
    }


    @Override
    // expressionReference
    public List<String> visitObjectReference(wreslParser.ObjectReferenceContext ctx) {
        List<String> varList = new ArrayList<>();
        varList.add(getWreslText(ctx.OBJECT_NAME()));
        return varList;
    }

    @Override
    // expressionParen
    public List<String> visitExpressionParen(wreslParser.ExpressionParenContext ctx) {
        List<String> varList = visit(ctx.expression());
        return varList;
    }
}

package gov.ca.water.wresl.domain;

import gov.ca.water.wresl.grammar.wreslParser.SvarContext;

import java.util.List;

public class Initial extends PreProcessedExpression {
    public final List<SvarContext> svars;

    public Initial(List<SvarContext> svars) {
        this.svars = svars;
    }
}

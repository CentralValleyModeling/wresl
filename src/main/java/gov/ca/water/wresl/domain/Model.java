package gov.ca.water.wresl.domain;

import gov.ca.water.wresl.grammar.wreslParser.ModelBodyContext;

import java.util.List;

public record Model(
        String name,
        List<ModelBodyContext> body
) {
    @Override
    public String toString() {
        return String.format("%s[name=%s]", this.getClass().getSimpleName(), this.name);
    }
}

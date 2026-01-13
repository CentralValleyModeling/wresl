package gov.ca.water.wresl.domain;

import gov.ca.water.wresl.grammar.wreslParser.GroupBodyContext;

import java.util.List;

public record Group(
        String name,
        List<GroupBodyContext> body
) {
    @Override
    public String toString() {
        return String.format("%s[name=%s]", this.getClass().getSimpleName(), this.name);
    }
}

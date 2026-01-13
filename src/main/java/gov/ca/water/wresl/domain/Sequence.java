package gov.ca.water.wresl.domain;

import gov.ca.water.wresl.enums.Timestep;
import gov.ca.water.wresl.grammar.wreslParser.SequenceConditionContext;

public record Sequence(
        String name,
        int order,
        String modelName,
        SequenceConditionContext condition,
        Timestep timestep
) {
    @Override
    public String toString() {
        return String.format("%s[name=%s, order=%d, model=%s]",
                this.getClass().getSimpleName(), this.name, this.order, this.modelName);
    }
}

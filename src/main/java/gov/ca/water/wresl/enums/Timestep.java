package gov.ca.water.wresl.enums;

import gov.ca.water.wresl.grammar.wreslParser;

public enum Timestep {
    DAY,
    MONTH,
    DEFAULT,
    UNKNOWN;

    public static Timestep fromContext(wreslParser.TimestepSpecificationContext ctx) {
        if (ctx == null) {
          return DEFAULT;
        } else if (!ctx.STEP_1DAY().getText().isEmpty()) {
            return DAY;
        } else if (!ctx.STEP_1MON().getText().isEmpty()) {
            return MONTH;
        } else {
            return UNKNOWN;
        }
    }
}

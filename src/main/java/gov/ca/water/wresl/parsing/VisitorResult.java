package gov.ca.water.wresl.parsing;

import gov.ca.water.wresl.domain.WRESLComponent;

import java.util.List;

record VisitorResult(List<WRESLComponent> data) {
    // Constructor for single value returns
    public VisitorResult(WRESLComponent data) {
        this(List.of(data));
    }
}

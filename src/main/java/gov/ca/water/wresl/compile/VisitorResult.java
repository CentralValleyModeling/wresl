package gov.ca.water.wresl.compile;

import gov.ca.water.wresl.domain.WRESLComponent;

import java.util.List;

public record VisitorResult(WRESLComponent data, String name, List<VisitorResult> children) {
    // Constructor for single value returns
    public VisitorResult(WRESLComponent data, String name) {
        this(data, name, List.of());
    }
}

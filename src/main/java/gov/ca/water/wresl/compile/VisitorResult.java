package gov.ca.water.wresl.compile;

import gov.ca.water.wresl.domain.WRIMSComponent;

import java.util.List;

public record VisitorResult(WRIMSComponent data, String name, List<VisitorResult> children) {
    // Constructor for single value returns
    public VisitorResult(WRIMSComponent data, String name) {
        this(data, name, List.of());
    }
}

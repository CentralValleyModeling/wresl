package gov.ca.water.wresl.domain;

import gov.ca.water.wresl.compile.Param;

import java.io.Serializable;

public class External extends WRIMSComponent implements Serializable {
    private static final long serialVersionUID = 1L;

    public String scope = Param.undefined;
    public String type = Param.undefined;

}

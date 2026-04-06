package gov.ca.water.wresl.domain;

import java.io.Serializable;

public class External extends WRESLComponent implements Serializable {
    private static final long serialVersionUID = 1L;

    public String scope = Param.undefined;
    public String type = Param.undefined;

}

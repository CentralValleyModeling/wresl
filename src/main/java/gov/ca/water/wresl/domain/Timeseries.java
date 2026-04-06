package gov.ca.water.wresl.domain;

import java.io.Serializable;

public class Timeseries extends WRESLComponent implements Serializable {
    private static final long serialVersionUID = 1L;

    public String scope = Param.undefined;
    public String dssBPart = Param.undefined;
    public String format = Param.undefined;
    public String kind = Param.undefined;
    public String units = Param.undefined;
    public String convertToUnits = Param.undefined;
    public String fromWresl = Param.undefined;
    public int line = 1;

    private IntDouble data = null;

}

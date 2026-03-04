package gov.ca.water.wresl.domain;

public class Timeseries {
    private static final long serialVersionUID = 1L;

    private String scope;
    private String dssBPart;
    private String format;
    private String kind;
    private String units;
    private String convertToUnits;
    private String fromWresl;
    private int line=1;

    private IntDouble data;

}

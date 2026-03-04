package gov.ca.water.wresl.domain;

import java.io.Serializable;

public class External extends WRIMSComponent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String scope;
    private String type;
    private String fromWresl;
    private int line=1;

}

package gov.ca.water.wresl.domain;

public class WRIMS_String extends WRIMSComponent {
    private String text;

    public WRIMS_String(String text) {
        this.text = text;
    }

    public String getValue() {
        return this.text;
    }
}

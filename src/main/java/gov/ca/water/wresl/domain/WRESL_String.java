package gov.ca.water.wresl.domain;

public class WRESL_String extends WRESLComponent {
    private String text;

    public WRESL_String(String text) {
        this.text = text;
    }

    public String getValue() {
        return this.text;
    }
}

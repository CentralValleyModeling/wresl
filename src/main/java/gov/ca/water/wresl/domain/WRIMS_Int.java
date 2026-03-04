package gov.ca.water.wresl.domain;

public class WRIMS_Int extends WRIMSComponent {
    private int number;

    public WRIMS_Int(int number) {
        this.number = number;
    }

    public int getValue() {
        return this.number;
    }
}

package gov.ca.water.wresl.domain;

public class WRIMS_Float extends WRIMSComponent {
    private float number;

    public WRIMS_Float(float number) {
        this.number = number;
    }

    public float getValue() {
        return this.number;
    }
}

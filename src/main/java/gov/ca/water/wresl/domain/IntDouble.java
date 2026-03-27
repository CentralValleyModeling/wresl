package gov.ca.water.wresl.domain;

public class IntDouble {
    private Number data = null;
    private boolean isInteger = true;
    private String argName = "";
    private int index = 0;


    // ------------------------------------------------------------
    // --- CONSTRUCTORS
    // ------------------------------------------------------------
    public IntDouble() {
    }

    public IntDouble(Number value, boolean isInt) {
        this.data = value;
        this.isInteger = isInt;
        this.argName = "";
    }

    public IntDouble(Number value, boolean isInt, String name, int index) {
        this.data = value;
        this.isInteger = isInt;
        this.argName = name;
        this.index = index;
    }


    // ------------------------------------------------------------
    // --- GETTERS
    // ------------------------------------------------------------
    public Number getValue() {
        return this.data;
    }
}

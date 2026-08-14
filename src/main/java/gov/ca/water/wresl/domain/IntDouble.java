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
    }

    public IntDouble(Number value, boolean isInt, String name) {
        this.data = value;
        this.isInteger = isInt;
        this.argName = name;
    }

    public IntDouble(Number value, boolean isInt, String name, int index) {
        this.data = value;
        this.isInteger = isInt;
        this.argName = name;
        this.index = index;
    }


    // ------------------------------------------------------------
    // --- SETTERS
    // ------------------------------------------------------------
    public void setValue(Number value) { this.data = value; }

    public void setIsInteger(boolean isInt) { this.isInteger = isInt; }

    public void setArgName(String name) { this.argName = name; }

    public void setIndex(int index) {this.index = index; }


    // ------------------------------------------------------------
    // --- GETTERS
    // ------------------------------------------------------------
    public Number getValue() {
        return this.data;
    }

    public IntDouble copyOf(){
        if (this.isInteger){
            return new IntDouble(this.data.intValue(), this.isInteger);
        } else {
            return new IntDouble(this.data.doubleValue(), this.isInteger);
        }
    }

    public boolean isInt(){
        return this.isInteger;
    }

}

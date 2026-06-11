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


    public IntDouble copyOf(){
        IntDouble newIntDouble;
        if (isInteger){
            newIntDouble = new IntDouble(data.intValue(), isInteger);
        }else{
            newIntDouble = new IntDouble(data.doubleValue(), isInteger);
        }
        return newIntDouble;
    }


    // ------------------------------------------------------------
    // --- PREDICATES
    // ------------------------------------------------------------
    public boolean isInt(){
        return this.isInteger;
    }

}

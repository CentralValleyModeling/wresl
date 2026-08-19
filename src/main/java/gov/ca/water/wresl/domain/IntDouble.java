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
    public void setValue(Number value) {
        this.data = value;
    }

    public void setIsInteger(boolean isInt) {
        this.isInteger = isInt;
    }

    public void setArgName(String name) {
        this.argName = name;
    }

    public void setIndex(int index) {
        this.index = index;
    }


    // ------------------------------------------------------------
    // --- GETTERS
    // ------------------------------------------------------------
    public Number getValue() {
        return this.data;
    }

    public String getArgName() {
        return this.argName;
    }

    public IntDouble copyOf() {
        if (this.isInteger) {
            return new IntDouble(this.data.intValue(), this.isInteger);
        } else {
            return new IntDouble(this.data.doubleValue(), this.isInteger);
        }
    }

    public boolean isInt() {
        return this.isInteger;
    }


    // ------------------------------------------------------------
    // --- MISC. METHODS
    // ------------------------------------------------------------

    // Add two IntDouble variables, right onto left
    public void add(IntDouble value) {
        if (this.data == null) { return; }
        if (value.data == null) { return; }

        if (this.isInteger) {
            if (value.isInteger) {
                this.data = this.data.intValue() + value.data.intValue();
            } else {
                this.data = this.data.intValue() +value.data.doubleValue();
                this.isInteger = false;
            }
        } else {
            if (value.isInteger) {
                this.data = this.data.doubleValue() + value.data.intValue();
            } else {
                this.data = this.data.doubleValue() + value.data.doubleValue();
            }
        }
    }

    // Subtract two IntDouble variables, right from left
    public void subtract(IntDouble subt) {
        if (this.data == null) { return; }
        if (subt.data == null) { return; }

        if (this.isInteger) {
            if (subt.isInteger) {
                this.data = this.data.intValue() - subt.data.intValue();
            } else {
                this.data = this.data.intValue() - subt.data.doubleValue();
                this.isInteger = false;
            }
        } else {
            if (subt.isInteger) {
                this.data = this.data.doubleValue() - subt.data.intValue();
            } else {
                this.data = this.data.doubleValue() - subt.data.doubleValue();
            }
        }
    }

    // Multiply two IntDouble variables, modify left's data
    public void multiply(IntDouble multiplier) {
        if (this.data == null) { return; }
        if (multiplier.data == null) { return; }

        if (this.isInteger) {
            if (multiplier.isInteger) {
                this.data =this.data.intValue() * multiplier.data.intValue();
            } else {
                this.data = this.data.intValue() * multiplier.data.doubleValue();
                this.isInteger = false;
            }
        } else {
            if (multiplier.isInteger) {
                this.data = this.data.doubleValue() * multiplier.data.intValue();
            } else {
                this.data = this.data.doubleValue() * multiplier.data.doubleValue();
            }
        }
    }

    // Divide two IntDouble variables, modify left's data
    public void divide(IntDouble divisor) {
        if (this.data == null) { return; }
        if (divisor.data == null) { return; }

        if (this.isInteger) {
            if (divisor.isInteger) {
                this.data = this.data.intValue() / divisor.data.intValue();
            } else {
                this.data = this.data.intValue() / divisor.data.doubleValue();
                this.isInteger = false;
            }
        } else {
            if (divisor.isInteger) {
                this.data = this.data.doubleValue() / divisor.data.intValue();
            } else {
                this.data = this.data.doubleValue() / divisor.data.doubleValue();
            }
        }
    }
}
package gov.ca.water.wresl.domain;

import java.util.LinkedHashMap;

// ------------------------------------------------------------
// --- CLASS TO HOLD GOAL DATA
// ------------------------------------------------------------
public class EvalConstraint {
    private IntDouble constant = new IntDouble(0.0, false);
    private LinkedHashMap<String, IntDouble> multipliers = null;
    private String sign = "";

    // Constructor 1
    public EvalConstraint() {}

    // Constructor 2
    public EvalConstraint(IntDouble constant) {
        this.constant = constant;
    }

    // Add a multiplier
    public void addMultiplier(String name, IntDouble data) {
        // Instantiate multiplier if needed
        if (this.multipliers == null) {
            this.multipliers = new LinkedHashMap<>();
        }

        // If multiplier already exists, accumulate coefficeint
        if (this.multipliers.containsKey(name)) {
            IntDouble dataOld = this.multipliers.get(name);
            dataOld.add(data);
            // Otherwise, create new multiplier
        } else {
            this.multipliers.put(name, data);
        }
    }

    // Add constant
    public void addConstant(IntDouble data) {
        this.constant.add(data);
    }

    // Get a multiplier
    public IntDouble getMultiplier(String name) {
        return this.multipliers.get(name);
    }

    // Get all multipliers
    public LinkedHashMap<String, IntDouble> getMultipliers() { return this.multipliers; }

    // Get constant
    public IntDouble getConstant() { return this.constant; }

    // Get sign
    public String getSign() { return this.sign; }

    // Set sign
    public void setSign(String sign) { this.sign = sign; }

    // Check if multipliers are defined
    public boolean isNumeric() {
        if (this.multipliers == null)     { return true; }
        if (this.multipliers.size() == 0) { return true; }
        return false;
    }
}

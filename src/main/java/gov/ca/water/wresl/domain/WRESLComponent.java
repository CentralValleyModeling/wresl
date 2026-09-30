package gov.ca.water.wresl.domain;

import gov.ca.water.utilities.Param;

public abstract class WRESLComponent {
    public String name = "";
    public String fromWresl = Param.undefined;
    public int line = 0;

    // --------------------
    // --- SETTERS
    // --------------------
    public void setName (String name) { this.name = name; }

    public void setFromWresl(String sourceFile) { this.fromWresl = sourceFile; }

    public void setLine(int line) { this.line = line; }


    // --------------------
    // --- GETTERS
    // --------------------
    public String getName() { return this.name; }


    // --------------------
    // ---MISC. METHODS
    // --------------------
    public WRESLComponent copyOf() {
        try {
            WRESLComponent component = this.getClass().getDeclaredConstructor().newInstance();
            component.name = this.name;
            component.fromWresl = this.fromWresl;
            component.line = this.line;
            return component;
        } catch (Exception e) {
            throw new RuntimeException("Failed to make a copy of " + this.name + "!");
        }
    }
}

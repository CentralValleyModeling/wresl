package gov.ca.water.wresl.compile;

import java.util.ArrayList;
import java.util.HashMap;

public class LookUpTable {
    private String name = null;
    private HashMap<String, Integer> field = new HashMap<>();
    private ArrayList<Number[]> data=new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HashMap<String, Integer> getField() {
        return field;
    }

    public void setField(HashMap<String, Integer> field) {
        this.field = field;
    }

    public ArrayList<Number[]> getData() {
        return data;
    }
}

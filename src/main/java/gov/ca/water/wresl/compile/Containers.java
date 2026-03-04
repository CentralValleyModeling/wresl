package gov.ca.water.wresl.compile;

import gov.ca.water.wresl.domain.Group;
import gov.ca.water.wresl.domain.Initial;
import gov.ca.water.wresl.domain.ModelDataSet;
import gov.ca.water.wresl.domain.Sequence;

import java.util.HashMap;
import java.util.Map;

/// A class that holds all the container objects for a single study; this includes:
/// - At most, 1 `Initial` object
/// - All `Model` objects
/// - All `Sequence` objects
/// - All `Group` objects
public class Containers {
    public Initial initial = null;
    public Map<String, ModelDataSet> models = new HashMap<>();
    public Map<String, Sequence> sequences = new HashMap<>();
    public Map<String, Group> groups = new HashMap<>();

    public ModelDataSet getModel(Sequence s) {
        return this.models.get(s.name());
    }
}

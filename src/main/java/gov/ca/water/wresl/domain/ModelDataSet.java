package gov.ca.water.wresl.domain;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ModelDataSet extends WRESLComponent implements Serializable {
    private static final long serialVersionUID = 1L;

    // Weight table   // <objName,  <itemName, value>>
    public List<String> wtList = new ArrayList<>();
    public List<String> wtTimeArrayList = new ArrayList<>();
    public List<String> wtSlackSurplusList = new ArrayList<>();
    public CopyOnWriteArrayList<String> usedWtSlackSurplusList = new CopyOnWriteArrayList<>();
    public CopyOnWriteArrayList<String> usedWtSlackSurplusDvList = new CopyOnWriteArrayList<>();
    public Map<String, WeightElement> wtMap = new HashMap<>();
    public Map<String, WeightElement> wtSlackSurplusMap = new HashMap<>();

    // External function structure
    public List<String> exList = new ArrayList<>();
    public Map<String, External> exMap = new HashMap<>();

    // Svar timeseries data structure
    public ArrayList<String> tsList = new ArrayList<>();
    public Map<String, Timeseries> tsMap = new HashMap<>();

    // Svar data structure
    public Set<String> svSet_unknown = new HashSet<>();
    public List<String> svList = new ArrayList<>();
    public Map<String, Svar> svMap = new HashMap<>();
    public Map<String, Svar> svFutMap = new HashMap<>();

    // Dvar data structure
    public List<String> dvList = new ArrayList<>();
    public List<String> dvList_deviationSlackSurplus = new ArrayList<>();
    public Map<String,Double> deviationSlackSurplus_toleranceMap = new HashMap<>();
    public List<String> dvTimeArrayList = new ArrayList<>();
    public List<String> timeArrayDvList = new ArrayList<>();
    public List<String> dvSlackSurplusList = new ArrayList<>();
    public Map<String, Dvar> dvMap = new HashMap<>();
    public Map<String, Dvar> dvSlackSurplusMap = new HashMap<>();

    // Alias data structure
    public Set<String> asSet_unknown = new HashSet<>();
    public List<String> asList = new ArrayList<>();
    public Map<String, Alias> asMap = new HashMap<>();
    public Map<String, Alias> asFutMap = new HashMap<>();

    // Goal data structure
    public List<String> gList = new ArrayList<>();
    public List<String> gTimeArrayList = new ArrayList<>();
    public Map<String, Goal> gMap = new HashMap<>();

    public List<String> incFileList=new ArrayList<>();

    public Set<String> varUsedByLaterCycle = new HashSet<>();

    public Set<String> dvarUsedByLaterCycle = new HashSet<>();
    public Set<String> dvarTimeArrayUsedByLaterCycle = new HashSet<>();
    public Set<String> svarUsedByLaterCycle = new HashSet<>();
    public Set<String> aliasUsedByLaterCycle = new HashSet<>();


    // ------------------------------------------------------------
    // --- METHODS
    // ------------------------------------------------------------

    // Append data from another model
    public void appendModelDataSet(ModelDataSet mds) {
        this.wtList.addAll(mds.wtList);
        this.wtTimeArrayList.addAll(mds.wtTimeArrayList);
        this.wtSlackSurplusList.addAll(mds.wtSlackSurplusList);
        this.usedWtSlackSurplusList.addAll(mds.usedWtSlackSurplusList);
        this.usedWtSlackSurplusDvList.addAll(mds.usedWtSlackSurplusDvList);
        this.wtMap.putAll(mds.wtMap);
        this.wtSlackSurplusMap.putAll(mds.wtSlackSurplusMap);

        this.exList.addAll(mds.exList);
        this.exMap.putAll(mds.exMap);

        this.tsList.addAll(mds.tsList);
        this.tsMap.putAll(mds.tsMap);

        this.svSet_unknown.addAll(mds.svSet_unknown);
        this.svList.addAll(mds.svList);
        this.svMap.putAll(mds.svMap);
        this.svFutMap.putAll(mds.svFutMap);

        this.dvList.addAll(mds.dvList);
        this.dvList_deviationSlackSurplus.addAll(mds.dvList_deviationSlackSurplus);
        this.deviationSlackSurplus_toleranceMap.putAll(mds.deviationSlackSurplus_toleranceMap);
        this.dvTimeArrayList.addAll(mds.dvTimeArrayList);
        this.timeArrayDvList.addAll(mds.timeArrayDvList);
        this.dvSlackSurplusList.addAll(mds.dvSlackSurplusList);
        this.dvMap.putAll(mds.dvMap);
        this.dvSlackSurplusMap.putAll(mds.dvSlackSurplusMap);

        this.asSet_unknown.addAll(mds.asSet_unknown);
        this.asList.addAll(mds.asList);
        this.asMap.putAll(mds.asMap);
        this.asFutMap.putAll(mds.asFutMap);

        this.gList.addAll(mds.gList);
        this.gTimeArrayList.addAll(mds.gTimeArrayList);
        this.gMap.putAll(mds.gMap);

        this.incFileList.addAll(mds.incFileList);

        this.varUsedByLaterCycle.addAll(mds.varUsedByLaterCycle);

        this.dvarUsedByLaterCycle.addAll(mds.dvarUsedByLaterCycle);
        this.dvarTimeArrayUsedByLaterCycle.addAll(mds.dvarTimeArrayUsedByLaterCycle);
        this.svarUsedByLaterCycle.addAll(mds.svarUsedByLaterCycle);
        this.aliasUsedByLaterCycle.addAll(mds.aliasUsedByLaterCycle);
    }
}

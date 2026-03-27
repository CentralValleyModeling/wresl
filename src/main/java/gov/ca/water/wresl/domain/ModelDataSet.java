package gov.ca.water.wresl.domain;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ModelDataSet extends WRESLComponent implements Serializable {
    private static final long serialVersionUID = 1L;

    // Weight table   // <objName,  <itemName, value>>
    public ArrayList<String> wtList = new ArrayList<>();
    public ArrayList<String> wtTimeArrayList = new ArrayList<>();
    public ArrayList<String> wtSlackSurplusList = new ArrayList<>();
    public CopyOnWriteArrayList<String> usedWtSlackSurplusList = new CopyOnWriteArrayList<>();
    public CopyOnWriteArrayList<String> usedWtSlackSurplusDvList = new CopyOnWriteArrayList<>();
    public Map<String, WeightElement> wtMap = new HashMap<>();
    public Map<String, WeightElement> wtSlackSurplusMap = new HashMap<>();

    // External function structure
    public ArrayList<String> exList = new ArrayList<>();
    public ArrayList<String> exList_global = new ArrayList<>();
    public ArrayList<String> exList_local = new ArrayList<>();
    public Map<String, External> exMap = new HashMap<>();

    // Svar timeseries data structure
    public ArrayList<String> tsList = new ArrayList<>();
    public ArrayList<String> tsList_global = new ArrayList<>();
    public ArrayList<String> tsList_local = new ArrayList<>();
    public Map<String, Timeseries> tsMap = new HashMap<>();

    // Svar data structure
    public Set<String> svSet_unknown = new HashSet<>();
    public ArrayList<String> svList = new ArrayList<>();
    public ArrayList<String> svList_global = new ArrayList<>();
    public ArrayList<String> svList_local = new ArrayList<>();
    public Map<String, Svar> svMap = new HashMap<>();
    public Map<String, Svar> svFutMap = new HashMap<>();

    // Dvar data structure
    public ArrayList<String> dvList = new ArrayList<>();
    public ArrayList<String> dvList_deviationSlackSurplus = new ArrayList<>();
    public Map<String,Double> deviationSlackSurplus_toleranceMap = new HashMap<>();
    public ArrayList<String> dvTimeArrayList = new ArrayList<>();
    public ArrayList<String> timeArrayDvList = new ArrayList<>();
    public ArrayList<String> dvSlackSurplusList = new ArrayList<>();
    public ArrayList<String> dvList_global = new ArrayList<>();
    public ArrayList<String> dvList_local = new ArrayList<>();
    public Map<String, Dvar> dvMap = new HashMap<>();
    public Map<String, Dvar> dvSlackSurplusMap = new HashMap<>();

    // Alias data structure
    public Set<String> asSet_unknown = new HashSet<>();
    public ArrayList<String> asList = new ArrayList<>();
    public ArrayList<String> asList_global = new ArrayList<>();
    public ArrayList<String> asList_local = new ArrayList<>();
    public Map<String, Alias> asMap = new HashMap<>();
    public Map<String, Alias> asFutMap = new HashMap<>();

    // Goal data structure
    public ArrayList<String> gList = new ArrayList<>();
    public ArrayList<String> gTimeArrayList = new ArrayList<>();
    public ArrayList<String> gList_global = new ArrayList<>();
    public ArrayList<String> gList_local = new ArrayList<>();
    public Map<String, Goal> gMap = new HashMap<>();

    public ArrayList<String> incFileList=new ArrayList<>();
    public ArrayList<String> incFileList_global=new ArrayList<>();
    public ArrayList<String> incFileList_local=new ArrayList<>();

    public Set<String> varUsedByLaterCycle = new HashSet<>();

    public Set<String> dvarUsedByLaterCycle = new HashSet<>();
    public Set<String> dvarTimeArrayUsedByLaterCycle = new HashSet<>();
    public Set<String> svarUsedByLaterCycle = new HashSet<>();
    public Set<String> aliasUsedByLaterCycle = new HashSet<>();
}

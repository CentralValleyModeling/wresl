package gov.ca.water.wresl.domain;

import org.antlr.v4.runtime.tree.ParseTree;

import java.io.Serializable;
import java.util.*;

public class StudyDataSet extends WRESLComponent implements Serializable  {
    private static final long serialVersionUID = 1L;

    private String absMainFilePath;

    private List<String> parameterList = new ArrayList<>();
    private LinkedHashMap<String, Svar> parameterMap = new LinkedHashMap<>();

    private List<String> modelList = new ArrayList<>();
    private List<String> modelConditionList = new ArrayList<>();
    private List<String> modelTimeStepList = new ArrayList<>();
    private List<ParseTree> modelConditionParseTrees = new ArrayList<>();

    ///  < timeseries name, timeseries object >
    private Map<String, Timeseries> timeseriesMap = new HashMap<>();
    private Map<String, List<String>> timeseriesTimeStepMap = new HashMap<>();

    ///  < modelName, modelDataSet >
    private Map<String, ModelDataSet> modelDataSetMap = new HashMap<>();

    /// this map contains value of vars needed for WRESL syntax: varName[cycleName]
    /// < VarName, < CycleName, Value >>
    private Map<String, Map<String, IntDouble>> varCycleValueMap = new HashMap<>();
    private Map<String, Map<String, IntDouble>> varTimeArrayCycleValueMap = new HashMap<>();
    private Map<String, Map<String, IntDouble>> varCycleIndexValueMap = new HashMap<>();
    private List<String> varCycleIndexList = new ArrayList<>();
    private List<String> dvarTimeArrayCycleIndexList = new ArrayList<>();

    public LinkedHashSet<String> allIntDv=new LinkedHashSet<>();
    public Map<Integer,LinkedHashSet<String>> cycIntDvMap=new HashMap<>();


    // -----------------
    // Methods
    // -----------------
    public List<String> getParameterList() {
        return new ArrayList<String>(this.parameterList);
    }

    public void setParameterList(List<String> parameterList) {
        this.parameterList = parameterList;
    }

    public LinkedHashMap<String, Svar> getParameterMap() {
        return new LinkedHashMap<String, Svar>(this.parameterMap);
    }

    public void setParameterMap(LinkedHashMap<String, Svar> parameterMap) {
        this.parameterMap = parameterMap;
    }

    public Map<String, Timeseries> getTimeseriesMap() {
        return new HashMap<String, Timeseries>(this.timeseriesMap);
    }

    public void setTimeseriesMap(Map<String, Timeseries> timeseriesMap) {
        this.timeseriesMap = timeseriesMap;
    }

    public Map<String, List<String>> getTimeseriesTimeStepMap() {
        return new HashMap<String, List<String>>(this.timeseriesTimeStepMap);
    }

    public void setTimeseriesTimeStepMap(Map<String, List<String>> timeseriesTimeStepMap) {
        this.timeseriesTimeStepMap = timeseriesTimeStepMap;
    }

    public String getAbsMainFilePath() {
        return new String(absMainFilePath);
    }

    public void setAbsMainFilePath(String absMainFilePath) {
        this.absMainFilePath = absMainFilePath;
    }

    public List<String> getModelList() {
        return new ArrayList<String>(this.modelList);
    }

    public void setModelList(List<String> modelList) {
        this.modelList = modelList;
    }

    public ArrayList<String> getModelConditionList() {
        return new ArrayList<String>(modelConditionList);
    }

    public List<String> getModelTimeStepList() {
        return this.modelTimeStepList;
    }

    public void setModelConditionList(List<String> modelConditionList) {
        this.modelConditionList = modelConditionList;
    }

    public void setModelTimeStepList(List<String> modelTimeStepList) {
        this.modelTimeStepList = modelTimeStepList;
    }

    public List<ParseTree> getModelConditionParseTrees() {
        return this.modelConditionParseTrees;
    }

    public void setModelConditionParseTrees(List<ParseTree> modelConditionParseTrees) {
        this.modelConditionParseTrees = modelConditionParseTrees;
    }

    public Map<String, ModelDataSet> getModelDataSetMap() {
        return new HashMap<String, ModelDataSet>(this.modelDataSetMap);
    }

    public void setModelDataSetMap(Map<String, ModelDataSet> modelDataSetMap) {
        this.modelDataSetMap = modelDataSetMap;
    }

    public Map<String, Map<String, IntDouble>> getVarCycleValueMap() {
        return this.varCycleValueMap;
    }

    public void setVarCycleValueMap(Map<String, Map<String, IntDouble>> varCycleValueMap) {
        this.varCycleValueMap = varCycleValueMap;
    }

    public Map<String, Map<String, IntDouble>> getVarTimeArrayCycleValueMap() {
        return this.varTimeArrayCycleValueMap;
    }

    public Map<String, Map<String, IntDouble>> getVarCycleIndexValueMap() {
        return this.varCycleIndexValueMap;
    }

    public void setVarCycleIndexValueMap(Map<String, Map<String, IntDouble>> varCycleIndexValueMap) {
        this.varCycleIndexValueMap = varCycleIndexValueMap;
    }

    public void clearVarTimeArrayCycleValueMap(){
        this.varTimeArrayCycleValueMap = new HashMap<String, Map<String, IntDouble>>();
    }

    public void clearVarCycleIndexByTimeStep(){
        this.varCycleIndexValueMap = new HashMap<String, Map<String, IntDouble>>();
        this.dvarTimeArrayCycleIndexList = new ArrayList<String> ();
    }

    public List<String> getVarCycleIndexList(){
        return this.varCycleIndexList;
    }

    public void setVarCycleIndexList(List<String> varCycleIndexList){
        this.varCycleIndexList = varCycleIndexList;
    }

    public List<String> getDvarTimeArrayCycleIndexList(){
        return this.dvarTimeArrayCycleIndexList;
    }
}

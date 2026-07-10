package gov.ca.water.wresl.domain;

import gov.ca.water.wresl.errors.SyntaxErrorException;
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


    // ------------------------------------------------------------
    // --- GETTERS
    // ------------------------------------------------------------

    public Svar getParameter(String parameterName) {
        return this.parameterMap.get(parameterName);
    }

    public List<String> getParameterList() {
        return this.parameterList;
    }

    public LinkedHashMap<String, Svar> getParameterMap() {
        return this.parameterMap;
    }

    public Timeseries getTimeseries(String tsName) {
        return this.timeseriesMap.get(tsName);
    }

    public Map<String, Timeseries> getTimeseriesMap() {
        return new HashMap<String, Timeseries>(this.timeseriesMap);
    }

    public Map<String, List<String>> getTimeseriesTimeStepMap() {
        return new HashMap<String, List<String>>(this.timeseriesTimeStepMap);
    }

    public String getAbsMainFilePath() {
        return new String(absMainFilePath);
    }

    public List<String> getModelList() {
        return new ArrayList<String>(this.modelList);
    }

    public List<String> getModelConditionList() {
        return new ArrayList<String>(modelConditionList);
    }

    public List<String> getModelTimeStepList() {
        return this.modelTimeStepList;
    }

    public ParseTree getModelConditionParseTree(int modelIndex) {
        return this.modelConditionParseTrees.get(modelIndex);
    }

    public List<ParseTree> getModelConditionParseTrees() {
        return this.modelConditionParseTrees;
    }

    public ModelDataSet getModelDataSet(int modelIndex) {
        return this.modelDataSetMap.get(this.modelList.get(modelIndex));
    }

    public Map<String, ModelDataSet> getModelDataSetMap() {
        return new HashMap<String, ModelDataSet>(this.modelDataSetMap);
    }

    public Map<String, Map<String, IntDouble>> getVarCycleValueMap() {
        return this.varCycleValueMap;
    }

    public Map<String, Map<String, IntDouble>> getVarTimeArrayCycleValueMap() {
        return this.varTimeArrayCycleValueMap;
    }

    public Map<String, Map<String, IntDouble>> getVarCycleIndexValueMap() {
        return this.varCycleIndexValueMap;
    }

    public List<String> getVarCycleIndexList(){
        return this.varCycleIndexList;
    }

    public List<String> getDvarTimeArrayCycleIndexList(){
        return this.dvarTimeArrayCycleIndexList;
    }


    // ------------------------------------------------------------
    // --- SETTERS
    // ------------------------------------------------------------

    public void setParameterList(List<String> parameterList) {
        this.parameterList = parameterList;
    }

    public void setParameterMap(LinkedHashMap<String, Svar> parameterMap) {
        this.parameterMap = parameterMap;
    }

    public void setTimeseriesMap(Map<String, Timeseries> timeseriesMap) {
        this.timeseriesMap = timeseriesMap;
    }

    public void setTimeseriesTimeStepMap(Map<String, List<String>> timeseriesTimeStepMap) {
        this.timeseriesTimeStepMap = timeseriesTimeStepMap;
    }

    public void setAbsMainFilePath(String absMainFilePath) {
        this.absMainFilePath = absMainFilePath;
    }

    public void setModelList(List<String> modelList) {
        this.modelList = modelList;
    }

    public void setModelConditionList(List<String> modelConditionList) {
        this.modelConditionList = modelConditionList;
    }

    public void setModelTimeStepList(List<String> modelTimeStepList) {
        this.modelTimeStepList = modelTimeStepList;
    }

    public void setModelConditionParseTrees(List<ParseTree> modelConditionParseTrees) {
        this.modelConditionParseTrees = modelConditionParseTrees;
    }

    public void setModelDataSetMap(Map<String, ModelDataSet> modelDataSetMap) {
        this.modelDataSetMap = modelDataSetMap;
    }

    public void setVarCycleValueMap(Map<String, Map<String, IntDouble>> varCycleValueMap) {
        this.varCycleValueMap = varCycleValueMap;
    }

    public void setVarCycleIndexValueMap(Map<String, Map<String, IntDouble>> varCycleIndexValueMap) {
        this.varCycleIndexValueMap = varCycleIndexValueMap;
    }

    public void setVarCycleIndexList(List<String> varCycleIndexList){
        this.varCycleIndexList = varCycleIndexList;
    }


    // ------------------------------------------------------------
    // --- MISC. METHODS
    // ------------------------------------------------------------
    public void addParameter(Svar parameter) throws SyntaxErrorException {
        // Check that parameter is not defined more than once
        if (this.parameterList.contains(parameter.name)) {
            throw new SyntaxErrorException(parameter.fromWresl, parameter.line,"Initial parameter "+parameter.name+" is defined more than once!");
        }
        this.parameterList.add(parameter.name);
        this.parameterMap.put(parameter.name, parameter);
    }

    public void clearVarTimeArrayCycleValueMap(){
        this.varTimeArrayCycleValueMap = new HashMap<String, Map<String, IntDouble>>();
    }

    public void clearVarCycleIndexByTimeStep(){
        this.varCycleIndexValueMap = new HashMap<String, Map<String, IntDouble>>();
        this.dvarTimeArrayCycleIndexList = new ArrayList<String> ();
    }
}

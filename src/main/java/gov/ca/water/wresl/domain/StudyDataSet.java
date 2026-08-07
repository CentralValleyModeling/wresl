package gov.ca.water.wresl.domain;

import gov.ca.water.io.DSS.CondensedReferenceCacheAndRead;
import gov.ca.water.io.DSS.DssOperations;
import gov.ca.water.io.HDF5.HDF5Reader;
import gov.ca.water.utilities.MiscUtilities;
import gov.ca.water.utilities.ParallelVars;
import gov.ca.water.utilities.TimeOperations;
import gov.ca.water.wresl.errors.SyntaxErrorException;
import hec.heclib.dss.HecDss;
import hec.heclib.util.HecTime;
import hec.io.TimeSeriesContainer;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.*;
import java.util.*;

public class StudyDataSet extends WRESLComponent implements Serializable  {
    private static final long serialVersionUID = 1L;

    private String absMainFilePath;

    private List<String> parameterList = new ArrayList<>();
    private LinkedHashMap<String, Svar> parameterMap = new LinkedHashMap<>();

    private List<String> modelList = new ArrayList<>();
    private List<String> modelConditionList = new ArrayList<>();
    private List<ParseTree> modelConditionParseTrees = new ArrayList<>();
    private Map<String, ModelDataSet> modelDataSetMap = new HashMap<>();  // <modelName, ModelDataSet>

    ///  < timeseries name, timeseries object >
    private Map<String, Timeseries> svTimeseriesMap = new HashMap<>();         // Actual Timeseries map that holds the data read from SV file
    private Map<String, Timeseries> svInitTimeseriesMap = new HashMap<>();     // Timeseries that are read from INIT file to initialize timeseries Svars
    private Map<String, Timeseries> dvAliasTS = new HashMap<>();
    private Map<String, Timeseries> dvAliasInit = new HashMap<>();
 //   private Map<String, List<String>> timeseriesTimeStepMap = new HashMap<>(); // Map that holds timesteps for timeseries data

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

    public Timeseries getSVTimeseries(String tsName) {
        return this.svTimeseriesMap.get(tsName);
    }

    public Timeseries getSVInitTimeseries(String tsName) {
        return this.svInitTimeseriesMap.get(tsName);
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

    public String getModelTimeStep(int modelIndex) {
        String modelName = this.modelList.get(modelIndex);
        ModelDataSet mds = this.modelDataSetMap.get(modelName);
        return mds.getTimeStep();
    }

    public List<String> getModelTimeStepList() {
        List<String> timeStepList = new ArrayList<>();

        for (int i=0; i<this.modelList.size(); i++) {
            timeStepList.add(this.getModelTimeStep(i));
        }

        return timeStepList;
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

    public void setSVTimeseriesMap(Map<String, Timeseries> svTimeseriesMap) {
        this.svTimeseriesMap = svTimeseriesMap;
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

    public void clearVarTimeArrayCycleValueMap() {
        this.varTimeArrayCycleValueMap = new HashMap<String, Map<String, IntDouble>>();
    }

    public void clearVarCycleIndexByTimeStep() {
        this.varCycleIndexValueMap = new HashMap<String, Map<String, IntDouble>>();
        this.dvarTimeArrayCycleIndexList = new ArrayList<String> ();
    }

    // Read timeseries data from SV and INIT files
    public void readTimeSeriesData(CondensedReferenceCacheAndRead.CondensedReferenceCache cacheTS,
                                   CondensedReferenceCacheAndRead.CondensedReferenceCache cacheTS2,
                                   CondensedReferenceCacheAndRead.CondensedReferenceCache cacheInitTS,
                                   String svFileName_HDF,
                                   String initFileName_HDF,
                                   String partA,
                                   String partF_SV,
                                   String partF_Init,
                                   int studyStartYear,
                                   int studyStartMonth,
                                   int studyStartDay) {

        // Flags
        boolean isSVFileDSS = true;
        boolean isInitFileDSS = true;
        if (cacheTS == null) { isSVFileDSS = false; }
        if (cacheInitTS == null) { isInitFileDSS = false; }

        // Both SV and INIT files are DSS
        if (isSVFileDSS && isInitFileDSS) {
            // Loop through timeseries and read data
            this.svTimeseriesMap.forEach((tsName, ts) -> {
                boolean success;

                // Read SV data
                success = ts.readTimeseries_DSS(cacheTS, partA, partF_SV, studyStartYear, studyStartMonth, studyStartDay);
                if (cacheTS2 != null) {
                    success = ts.readTimeseries_DSS(cacheTS2, partA, partF_SV, studyStartYear, studyStartMonth, studyStartDay);
                }

                // Read initial data and add it to our list if reading is successful
                Timeseries tsInit = ts.copyOf();
                success = tsInit.readTimeseries_DSS(cacheInitTS, partA, partF_Init, studyStartYear, studyStartMonth, studyStartDay);
                if (success) { this.svInitTimeseriesMap.put(tsName, tsInit); }
            });
            return;
        }

        // SV file is DSS, INIT file is HDF
        if (isSVFileDSS && !isInitFileDSS) {
            // Loop through SV timeseries and read data
            boolean success;
            for (Timeseries ts : this.svTimeseriesMap.values()) {
                success = ts.readTimeseries_DSS(cacheTS, partA, partF_SV, studyStartYear, studyStartMonth, studyStartDay);
                if (cacheTS2 != null) {
                    success = ts.readTimeseries_DSS(cacheTS2, partA, partF_SV, studyStartYear, studyStartMonth, studyStartDay);
                }
            }

            // Read INIT timeseries
            // First, open file
            HDF5Reader.openInitFile(initFileName_HDF, partA, partF_Init);

            // Loop through timeseries and read data
            this.svTimeseriesMap.forEach((tsName, ts) -> {
                boolean success1;
                Timeseries tsInit = ts.copyOf();
                success1 = tsInit.readTimeseries_HDF(studyStartYear, studyStartMonth, studyStartDay);

                // Add Timeseries to the list of initial SV timeseries, if read successfully
                if (success1) { this.svInitTimeseriesMap.put(tsName, tsInit); }
            });

            // Close HDF5 file
            HDF5Reader.closeInitFile();

            return;
        }

        // SV file is HDF, INIT file is DSS
        if (!isSVFileDSS && isInitFileDSS) {
            // First read the SV timeseries data
            // First, open file
            HDF5Reader.openSVFile(svFileName_HDF, partA, partF_SV);

            // Loop through timeseries and read data
            boolean success;
            for (Timeseries ts : this.svTimeseriesMap.values()) {
                success = ts.readTimeseries_HDF(studyStartYear, studyStartMonth, studyStartDay);
            }

            // Close HDF5 file
            HDF5Reader.closeSVFile();

            // Next, read the INIT data
            this.svTimeseriesMap.forEach((tsName, ts) -> {
                boolean success1;
                Timeseries tsInit = ts.copyOf();
                success1 = tsInit.readTimeseries_DSS(cacheInitTS, partA, partF_Init, studyStartYear, studyStartMonth, studyStartDay);

                // Add Timeseries to the list of initial SV timeseries, if read successfully
                if (success1) { this.svInitTimeseriesMap.put(tsName, tsInit); }
            });
            return;
        }

        // If, made it this far, both SV and INIT files are HDF
        // First, open files
        HDF5Reader.openSVFile(svFileName_HDF, partA, partF_SV);
        HDF5Reader.openInitFile(initFileName_HDF, partA, partF_Init);

        // Loop through timeseries and read data
        this.svTimeseriesMap.forEach((tsName, ts) -> {
            boolean success;
            // read SV data
            success = ts.readTimeseries_HDF(studyStartYear, studyStartMonth, studyStartDay);

            // Read INIT data, and if succesful, add it to our list
            Timeseries tsInit = ts.copyOf();
            success = tsInit.readTimeseries_HDF(studyStartYear, studyStartMonth, studyStartDay);
            if (success) { this.svInitTimeseriesMap.put(tsName, tsInit); }
        });

        // Close HDF5 files
        HDF5Reader.closeSVFile();
        HDF5Reader.closeInitFile();
    }


    // ------------------------------------------------------------
    // --- DATA SAVERS
    // ------------------------------------------------------------

    public void saveSvarTSData(HecDss dss, String fileName, String timeStep, String partA, String partF) {
        System.out.println("Write svar timeseries to "+fileName);
        Set svTsSet = this.svTimeseriesMap.keySet();
        Iterator iterator = svTsSet.iterator();
        Map<String, Timeseries> allTsMap = this.svTimeseriesMap;
        while(iterator.hasNext()){
            String svTsName=(String)iterator.next();
            String svName=DssOperations.getTSName(svTsName);
            String ctu = "none";
            String units="none";
            if (allTsMap.containsKey(svName)){
                Timeseries ts=allTsMap.get(svName);
                units = ts.units;
                ctu=ts.convertToUnits;
            }
            Timeseries dds=this.svTimeseriesMap.get(svTsName);
            List<Double> values=dds.getData();
            TimeSeriesContainer dc = new TimeSeriesContainer();
            dc.type="PER-AVER";
            int size=values.size();
            dc.numberValues=size;
            dc.units=dds.getUnits().toUpperCase();
            dc.values=new double[size];
            Date startDate=dds.getStartTime();
            Calendar startCalendar=Calendar.getInstance();
            Date startDate1 = new Date(startDate.getYear(), startDate.getMonth(), startDate.getDate(), 24, 0);
            startCalendar.setTime(startDate1);
            dc.setStartTime(new HecTime(startCalendar));
            //startDate.setTime(startDate.getTime()-1*24*60*60);
            int year=startDate.getYear()+1900;
            int month=startDate.getMonth()+1;
            int day=startDate.getDate();
            //String startDateStr=TimeOperation.dssTimeEndDay(year, month, day);
            //long startJulmin = TimeFactory.getInstance().createTime(startDateStr).getTimeInMinutes();
            if (units.equals("taf") && ctu.equals("cfs")) {
                for (int i=0; i<size; i++){
                    Double value=values.get(i);
                    if (value == null) {
                        dc.values[i]=-901.0;
                    } else {
                        if (value == -901.0 || value == -902.0) {
                            dc.values[i]=value;
                        } else {
                            ParallelVars prvs=TimeOperations.findTime(timeStep, i, year, month, day);
                            dc.values[i]=value/ MiscUtilities.tafcfs("taf_cfs", timeStep, prvs);
                        }
                    }
                }
            } else if (units.equals("cfs") && ctu.equals("taf")) {
                for (int i=0; i<size; i++){
                    Double value=values.get(i);
                    if (value == null){
                        dc.values[i]=-901.0;
                    }else{
                        if (value == -901.0 || value == -902.0){
                            dc.values[i]=value;
                        }else{
                            ParallelVars prvs=TimeOperations.findTime(timeStep, i, year, month, day);
                            dc.values[i]=value/ MiscUtilities.tafcfs("cfs_taf", timeStep, prvs);
                        }
                    }
                }
            }else{
                for (int i=0; i<size; i++){
                    Double value=values.get(i);
                    dc.values[i]=value;
                }
            }
            //boolean storeFlags = false;
            dc.setName("/"+partA+"/"+svName+"/"+dds.getKind()+"//"+dds.getTimeStep()+"/"+partF+"/");
            dc.setStoreAsDoubles(true);
            try {
                dss.put(dc);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //writer.storeTimeSeriesData(pathName, startJulmin, dd, storeFlags);
        }
        System.out.println("Svar file saved.");
    }


    // ------------------------------------------------------------
    // --- METHODS TO WRITE DATA TO A CSV FILE
    // ------------------------------------------------------------
    public void outputCSV(String csvLocalPath,
                          int scenarioIndex,
                          int ovOption,
                          String ovFileName,
                          boolean isSimOutput,
                          boolean writeInitToDVOutput,
                          String partA,
                          String partF) {

        // Initialize
        String slackPrefix="slack__";
        String surplusPrefix="surplus__";
        Map<String, String> ovPartBC=new HashMap<>();

        if (ovOption != 0){
            procOVFile(ovOption, ovFileName, ovPartBC);
        }

        try {
            System.out.println("Writing data to CSV file...");

            File csvFile= new File(csvLocalPath);
            csvFile.getParentFile().mkdirs();
            FileWriter fw = new FileWriter(csvFile);
            BufferedWriter bw = new BufferedWriter(fw, 8192);
            String line="id,PartA,PartF,Timestep,Units,Date_Time,Variable,Kind,Value\n";
            bw.write(line);
            Set<String> keys = this.dvAliasTS.keySet();
            Iterator<String> it = keys.iterator();
            while (it.hasNext()){
                String name=it.next();
                String nameUp= DssOperations.getTSName(name).toUpperCase();
                Timeseries dds = this.dvAliasTS.get(name);
                String origKindName=dds.getKind();
                boolean isWritten=false;
                if (ovOption==0){
                    isWritten=true;
                }else{
                    if (ovPartBC.containsKey(nameUp)){
                        if (ovPartBC.get(nameUp).equals(origKindName.toUpperCase())){
                            isWritten = true;
                        }
                    }
                }
                if (isWritten && !nameUp.startsWith(slackPrefix) && !nameUp.startsWith(surplusPrefix)){
                    String timestep=dds.getTimeStep().toUpperCase();
                    Date date = dds.getStartTime();
                    String unitsName=formUnitsName(dds.getUnits());
                    String variableName=formVariableName(nameUp);
                    String kindName=formKindName(origKindName);
                    List<Double> data = dds.getData();
                    if (timestep.equals("1DAY")){
                        if (!isSimOutput) date= TimeOperations.backOneDay(date);
                        for (int i=0; i<data.size(); i++){
                            double value = data.get(i);
                            if (value != -901.0 && value !=-902.0){
                                line = scenarioIndex+","+partA+","+partF+",1DAY,"+unitsName+","+formDateData(date)+","+variableName+","+kindName+","+ value +"\n";
                                bw.write(line);
                            }else{
                                if (isSimOutput){
                                    line = scenarioIndex+","+partA+","+partF+",1DAY,"+unitsName+","+formDateData(date)+","+variableName+","+kindName+","+ value +"\n";
                                    bw.write(line);
                                }
                            }
                            date= TimeOperations.addOneDay(date);
                        }
                    }else{
                        if (!isSimOutput) date= TimeOperations.backOneMonth(date);
                        for (int i=0; i<data.size(); i++){
                            double value = data.get(i);
                            if (value != -901.0 && value !=-902.0){
                                line = scenarioIndex+","+partA+","+partF+",1MON,"+unitsName+","+formDateData(date)+","+variableName+","+kindName+","+ value +"\n";
                                bw.write(line);
                            }else{
                                if (isSimOutput){
                                    line = scenarioIndex+","+partA+","+partF+",1MON,"+unitsName+","+formDateData(date)+","+variableName+","+kindName+","+ value +"\n";
                                    bw.write(line);
                                }
                            }
                            date= TimeOperations.addOneMonth(date);
                        }
                    }
                }
            }
            Set<String> svKeys = this.svTimeseriesMap.keySet();
            it = svKeys.iterator();
            while (it.hasNext()){
                String name=it.next();
                String nameUp=DssOperations.getTSName(name).toUpperCase();
                Timeseries dds = this.svTimeseriesMap.get(name);
                String origKindName = dds.getKind();
                boolean isWritten=false;
                if (ovOption==0){
                    isWritten=true;
                }else{
                    if (ovPartBC.containsKey(nameUp)){
                        if (ovPartBC.get(nameUp).equals(origKindName.toUpperCase())){
                            isWritten = true;
                        }
                    }
                }
                if (isWritten && !nameUp.startsWith(slackPrefix) && !nameUp.startsWith(surplusPrefix)){
                    String timestep=dds.getTimeStep().toUpperCase();
                    String units=dds.getUnits();
                    String unitsName=formUnitsName(units);
                    String convertToUnits = dds.getConvertToUnits();
                    Date date = dds.getStartTime();
                    String variableName=formVariableName(nameUp);
                    String kindName=formKindName(origKindName);
                    List<Double> data = dds.getData();
                    if (timestep.equals("1DAY")){
                        //date=TimeOperation.backOneDay(date);
                        for (int i=0; i<data.size(); i++){
                            double value = data.get(i);
                            if (value != -901.0 && value !=-902.0){
                                line = scenarioIndex+","+partA+","+partF+",1DAY,"+unitsName+","+formDateData(date)+","+variableName+","+kindName+","+ convertValue(value, units, convertToUnits, date, timestep) +"\n";
                                bw.write(line);
                            }
                            date= TimeOperations.addOneDay(date);
                        }
                    }else{
                        //date=TimeOperation.backOneMonth(date);
                        for (int i=0; i<data.size(); i++){
                            double value = data.get(i);
                            if (value != -901.0 && value !=-902.0){
                                line = scenarioIndex+","+partA+","+partF+",1MON,"+unitsName+","+formDateData(date)+","+variableName+","+kindName+","+convertValue(value, units, convertToUnits, date, timestep)+"\n";
                                bw.write(line);
                            }
                            date= TimeOperations.addOneMonth(date);
                        }
                    }
                }
            }
            if (writeInitToDVOutput){
                keys = this.dvAliasInit.keySet();
                it = keys.iterator();
                while (it.hasNext()){
                    String name=it.next();
                    if (isSimOutput || svKeys.contains(name)){
                        String nameUp=DssOperations.getTSName(name).toUpperCase();
                        Timeseries dds = this.dvAliasInit.get(name);
                        String origKindName=dds.getKind();
                        boolean isWritten=false;
                        if (ovOption==0){
                            isWritten=true;
                        }else{
                            if (ovPartBC.containsKey(nameUp)){
                                if (ovPartBC.get(nameUp).equals(origKindName.toUpperCase())){
                                    isWritten = true;
                                }
                            }
                        }
                        if (isWritten && !nameUp.startsWith(slackPrefix) && !nameUp.startsWith(surplusPrefix)){
                            String timestep=dds.getTimeStep().toUpperCase();
                            Date date = dds.getStartTime();
                            String units=dds.getUnits();
                            String unitsName=formUnitsName(units);
                            String convertToUnits=dds.getConvertToUnits();
                            String variableName=formVariableName(nameUp);
                            String kindName=formKindName(origKindName);
                            List<Double> data = dds.getData();
                            if (timestep.equals("1DAY")){
                                date= TimeOperations.backOneDay(date);
                                for (int i=0; i<data.size(); i++){
                                    double value = data.get(i);
                                    if (value != -901.0 && value !=-902.0){
                                        line = scenarioIndex+","+partA+","+partF+",1DAY,"+unitsName+","+formDateData(date)+","+variableName+","+kindName+","+convertValue(value, units, convertToUnits, date, timestep)+"\n";
                                        bw.write(line);
                                    }
                                    date= TimeOperations.addOneDay(date);
                                }
                            }else{
                                date= TimeOperations.backOneMonth(date);
                                for (int i=0; i<data.size(); i++){
                                    double value = data.get(i);
                                    if (value != -901.0 && value !=-902.0){
                                        line = scenarioIndex+","+partA+","+partF+",1MON,"+unitsName+","+formDateData(date)+","+variableName+","+kindName+","+convertValue(value, units, convertToUnits, date, timestep)+"\n";
                                        bw.write(line);
                                    }
                                    date= TimeOperations.addOneMonth(date);
                                }
                            }
                        }
                    }
                }
            }
            if (isSimOutput && writeInitToDVOutput){
                keys = this.svInitTimeseriesMap.keySet();
                it = keys.iterator();
                while (it.hasNext()){
                    String name=it.next();
                    String nameUp=DssOperations.getTSName(name).toUpperCase();
                    Timeseries dds = this.svInitTimeseriesMap.get(name);
                    String origKindName=dds.getKind();
                    boolean isWritten=false;
                    if (ovOption==0){
                        isWritten=true;
                    }else{
                        if (ovPartBC.containsKey(nameUp)){
                            if (ovPartBC.get(nameUp).equals(origKindName.toUpperCase())){
                                isWritten = true;
                            }
                        }
                    }
                    if (isWritten && !nameUp.startsWith(slackPrefix) && !nameUp.startsWith(surplusPrefix)){
                        String timestep=dds.getTimeStep().toUpperCase();
                        Date date = dds.getStartTime();
                        String units=dds.getUnits();
                        String unitsName=formUnitsName(units);
                        String convertToUnits=dds.getConvertToUnits();
                        String variableName=formVariableName(nameUp);
                        String kindName=formKindName(origKindName);
                        List<Double> data = dds.getData();
                        if (timestep.equals("1DAY")){
                            date= TimeOperations.backOneDay(date);
                            for (int i=0; i<data.size(); i++){
                                double value = data.get(i);
                                if (value != -901.0 && value !=-902.0){
                                    line = scenarioIndex+","+partA+","+partF+",1DAY,"+unitsName+","+formDateData(date)+","+variableName+","+kindName+","+convertValue(value, units, convertToUnits, date, timestep)+"\n";
                                    bw.write(line);
                                }
                                date= TimeOperations.addOneDay(date);
                            }
                        }else{
                            date= TimeOperations.backOneMonth(date);
                            for (int i=0; i<data.size(); i++){
                                double value = data.get(i);
                                if (value != -901.0 && value !=-902.0){
                                    line = scenarioIndex+","+partA+","+partF+",1MON,"+unitsName+","+formDateData(date)+","+variableName+","+kindName+","+convertValue(value, units, convertToUnits, date, timestep)+"\n";
                                    bw.write(line);
                                }
                                date= TimeOperations.addOneMonth(date);
                            }
                        }
                    }
                }
            }
            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Wrote data to CSV file");
    }

    private void procOVFile(int ovOption, String ovFileName, Map<String, String> ovPartBC) {
        File ovFile = new File (ovFileName);
        if (!ovFile.exists()){
            System.out.println("Output variable file doesn't exist. All the timeseries will be written to the csv file.");
            ovOption=0;
            return;
        }
        try {
            FileInputStream fs = new FileInputStream(ovFile.getAbsolutePath());
            BufferedReader br = new BufferedReader(new InputStreamReader(fs));
            String line=br.readLine();
            if (br == null) {
                System.out.println("Output variable file doesn't contain data. All the timeseries will be written to the csv file.");
            };
            while((line=br.readLine()) !=null){
                line=line.replace(" ", "").replace("\t",  "").toUpperCase();
                if (line.equals("")) return;
                String[] parts = line.split(",");
                if (parts.length>=2){
                    ovPartBC.put(parts[0], parts[1]);
                }
            }
            br.close();
            fs.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("Output variable file doesn't exist. All the timeseries will be written to the csv file.");
            ovOption=0;
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Output variable file has errors. All the timeseries will be written to the csv file.");
            ovOption=0;
        }
    }

    private String formKindName(String name){
        String kindName = name.replaceAll("-", "_");
        return kindName;
    }

    private String formDateData(Date date){
        int year=date.getYear()+1900;
        int month=date.getMonth()+1;
        int day = date.getDate();
        return year+"-"+TimeOperations.monthNameNumeric(month)+"-"+TimeOperations.dayName(day)+" 00:00:00";
    }

    private String formUnitsName(String units){
        String newUnits=units.replaceAll("/", "_").replaceAll("-", "_");
        return newUnits;
    }

    private String formVariableName(String name){
        String variableName = name.replaceAll("-", "_");
        return variableName;
    }

    private double convertValue(double value, String units, String convertToUnits, Date date, String timestep) {
        if (units.equalsIgnoreCase("cfs") && convertToUnits.equalsIgnoreCase("taf")) {
            return value*factorTafToCfs(date, timestep);
        }else if (units.equalsIgnoreCase("taf") && convertToUnits.equalsIgnoreCase("cfs")) {
            return value*factorCfsToTaf(date, timestep);
        } else {
            return value;
        }
    }

    private double factorCfsToTaf(Date date, String timestep) {
        if (TimeOperations.isMonthlyInterval(timestep)) {
            int year=date.getYear()+1900;
            int month=date.getMonth()+1;
            int daysInMonth=TimeOperations.numberOfDays(month, year);
            return daysInMonth / 504.1666667;
        } else {
            return 1 / 504.1666667;
        }
    }

    private double factorTafToCfs(Date date, String timestep){
        if (TimeOperations.isMonthlyInterval(timestep)) {
            int year=date.getYear()+1900;
            int month=date.getMonth()+1;
            int daysInMonth=TimeOperations.numberOfDays(month, year);
            return 504.1666667 / daysInMonth;
        } else {
            return 504.1666667;
        }
    }



}

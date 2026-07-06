package gov.ca.water.wrims.engine.core.components;

import gov.ca.water.utilities.Param;
import gov.ca.water.utilities.TimeOperations;
import gov.ca.water.wresl.domain.ModelDataSet;
import gov.ca.water.wresl.domain.StudyDataSet;
import gov.ca.water.wresl.errors.EvaluationErrorException;
import gov.ca.water.wresl.errors.SyntaxErrorException;
import gov.ca.water.wresl.parsing.Evaluator;
import gov.ca.water.wresl.parsing.Study;
import gov.ca.water.wrims.engine.core.config.ConfigUtils;
import gov.ca.water.wrims.engine.core.evaluator.AssignPastCycleVariable;
import gov.ca.water.wrims.engine.core.evaluator.CsvOperation;
import gov.ca.water.wrims.engine.core.evaluator.DssOperation;
import gov.ca.water.wrims.engine.core.evaluator.WeightEval;
import gov.ca.water.wrims.engine.core.fromWrims2.StudyUtils;
import gov.ca.water.wrims.engine.core.hdf5.HDF5Writer;
import gov.ca.water.wrims.engine.core.ilp.ILP;
import gov.ca.water.wrims.engine.core.launch.LaunchConfiguration;
import gov.ca.water.wrims.engine.core.solver.*;
import gov.ca.water.wrims.engine.core.sql.DataBaseProfile;
import gov.ca.water.wrims.engine.core.sql.MySQLCWriter;
import gov.ca.water.wrims.engine.core.sql.MySQLRWriter;
import gov.ca.water.wrims.engine.core.sql.SQLServerRWriter;
import gov.ca.water.wrims.engine.core.tools.General;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.*;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ControllerBatch {
    private boolean enableProgressLog = false;
    private boolean enableConfigProgress = false;
    private boolean runCompleted = false;
    private MySQLCWriter mySQLCWriter;
    private MySQLRWriter mySQLRWriter;
    private SQLServerRWriter sqlServerRWriter;

    public void ControllerBatch(String[] args) {
        long startTimeInMillis = Calendar.getInstance().getTimeInMillis();
        new DataBaseProfile(args);
        processArgs(args);
        if (ILP.loggingUsageMemeory) General.getPID();
        connectToDataBase();
        if (enableConfigProgress) {
            try {
                FileWriter progressFile= new FileWriter(StudyUtils.configFilePath+".prgss");
                PrintWriter pw = new PrintWriter(progressFile);
                pw.println("Parsing and preprocessing the model ...");
                pw.close();
                progressFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        StudyDataSet sds = new StudyDataSet();
        try {
            Study study = new Study();
            sds = study.compile(FilePaths.fullMainPath);
        }
        catch (SyntaxErrorException e) {
            System.err.println("WRESL+ syntax error(s) encountered in file "+e.getSourceFile());
            List<String> syntaxErrors = e.getErrorMessages();
            for (int i=0; i<syntaxErrors.size(); i++) {
                System.err.println(syntaxErrors.get(i));
            }
        }
        catch (EvaluationErrorException e) {
            System.err.println("Evaluation error: " + e.getErrorMessage());
            System.err.println("                  " +"File " + e.getSourceFile() + ", line " + e.getLine());
        }
        long afterParsing = Calendar.getInstance().getTimeInMillis();
        ControlData.t_parse=(int) (afterParsing-startTimeInMillis);
        System.out.println("Parsing Time is "+ControlData.t_parse/60000+"min"+Math.round((ControlData.t_parse/60000.0-ControlData.t_parse/60000)*60)+"sec");

        new PreRunModel(sds);

        ILP.getIlpDir();
        ILP.setVarDir();
        ILP.createNoteFile();
        ILP.setMaximumFractionDigits();

        runModel(sds);
        if (ControlData.showTimeUsage) TimeUsage.showTimeUsage();
        long endTimeInMillis = Calendar.getInstance().getTimeInMillis();
        int runPeriod=(int) (endTimeInMillis-startTimeInMillis);
        System.out.println("=================Run Time is "+runPeriod/60000+"min"+Math.round((runPeriod/60000.0-runPeriod/60000)*60)+"sec====");
        ILP.writeNoteLn("Total time", "(sec): "+                        Math.round(runPeriod/1000.0));
        ILP.writeNoteLn("Total time", "(min): "+                        Math.round(runPeriod/1000.0/60));
        ILP.writeNoteLn("Total time", "(sec): "+                        Math.round(runPeriod/1000.0), ILP._noteFile_timeusage);
        ILP.writeNoteLn("Total time", "(min): "+                        Math.round(runPeriod/1000.0/60), ILP._noteFile_timeusage);
        runCompleted = true;
    }


    public void processArgs(String[] args){

        if(args[0].startsWith("-")) {
            if (args[0].toLowerCase().startsWith("-launch")){
                procLaunch(args);
            }else{
                ConfigUtils.loadArgs(args);
            }
            if (args[0].toLowerCase().endsWith(".launch.config")){
                enableConfigProgress=true;
            }
            if (ControlData.enableProgressLog){
                enableProgressLog=true;
            }
        } else {
            setControlData(args);
        }

    }


    public void procLaunch(String[] args){
        String launchFilePath = args[0].substring(args[0].indexOf("=") + 1, args[0].length());
        new LaunchConfiguration(launchFilePath);
    }


    public void setControlData(String[] args){
        FilePaths.groundwaterDir=args[0];
        FilePaths.setMainFilePaths(args[1]);
        FilePaths.setSvarFilePaths(args[2]);
        FilePaths.setInitFilePaths(args[3]);
        FilePaths.setDvarFilePaths(args[4]);
        ControlData.svDvPartF=args[5];
        ControlData.initPartF=args[6];
        ControlData.partA = args[7];
        ControlData.defaultTimeStep = args[8];
        ControlData.startYear=Integer.parseInt(args[9]);
        ControlData.startMonth=Integer.parseInt(args[10]);
        ControlData.startDay=Integer.parseInt(args[11]);
        ControlData.endYear=Integer.parseInt(args[12]);
        ControlData.endMonth=Integer.parseInt(args[13]);
        ControlData.endDay=Integer.parseInt(args[14]);
        ControlData.solverName=args[15];
        FilePaths.csvFolderName = args[16];
        ControlData.currYear=ControlData.startYear;
        ControlData.currMonth=ControlData.startMonth;
        ControlData.currDay=ControlData.startDay;
    }


    public void connectToDataBase(){
        if (ControlData.outputType==2){
            mySQLCWriter=new MySQLCWriter();
        }else if (ControlData.outputType==3){
            mySQLRWriter=new MySQLRWriter();
        }else if (ControlData.outputType==4){
            sqlServerRWriter=new SQLServerRWriter();
        }
    }


    public void runModel(StudyDataSet sds){
        System.out.println("==============Run Study Start============");

        runModelILP(sds);

        WeightEval.outputWtTableAR();

        if (Error.getTotalError()>0){
            System.out.println("=================Run ends with errors====");
            System.exit(1);
        } else {
            System.out.println("=================Run ends!================");
        }
    }


    public void runModelILP(StudyDataSet sds) {

        ILP.initializeIlp();

        List<String> modelList = sds.getModelList();
        Map<String, ModelDataSet> modelDataSetMap = sds.getModelDataSetMap();

        if (ControlData.solverName.equalsIgnoreCase("clp0")) {
            ControlData.solverType = Param.SOLVER_CLP0;
            // initiate clp0
            Clp0Solver.init();
        } else if (ControlData.solverName.equalsIgnoreCase("clp1")) {
            ControlData.solverType = Param.SOLVER_CLP1;
            // initiate clp
            ClpSolver.init(true);
        } else if (ControlData.solverName.equalsIgnoreCase("clp")) {
            ControlData.solverType = Param.SOLVER_CLP;
            // initiate clp
            ClpSolver.init(false);
        } else if (ControlData.solverName.equalsIgnoreCase("cbc0")) {
            ControlData.solverType = Param.SOLVER_CBC0;
            // initiate cbc0
            Cbc0Solver.init();
        } else if (ControlData.solverName.equalsIgnoreCase("cbc1")) {
            ControlData.solverType = Param.SOLVER_CBC1;
            // initiate cbc file passing jni
            CbcSolver.init(true, sds);
        } else if (ControlData.solverName.equalsIgnoreCase("cbc")) {
            ControlData.solverType = Param.SOLVER_CBC;
            // initiate cbc file passing jni
            CbcSolver.init(false, sds);
        } else if (ControlData.solverName.equalsIgnoreCase("lpsolve")) {
            ControlData.solverType = Param.SOLVER_LPSOLVE;
            // initiate lpsolve
        } else if (ControlData.solverName.toLowerCase().contains("xa")) {
            ControlData.solverType = Param.SOLVER_XA; //default
            new InitialXASolver();
        } else {
            Error.addConfigError("Solver name not recognized: " + ControlData.solverName);
            Error.writeErrorLog();
        }

        ControlData.initOutputDate();
        ControlData.initMemDate();

        List<ParseTree> modelConditionParsers = sds.getModelConditionParseTrees();
        boolean noError = true;
        VariableTimeStep.initialCurrTimeStep(modelList);
        VariableTimeStep.initialCycleStartDate();
        VariableTimeStep.setCycleEndDate(sds);
        int sectionI = 0;
        time_marching:
        while (VariableTimeStep.checkEndDate(ControlData.cycleStartDay, ControlData.cycleStartMonth, ControlData.cycleStartYear, ControlData.endDay, ControlData.endMonth, ControlData.endYear) <= 0 && noError) {
            if (ControlData.solverType == Param.SOLVER_XA && ControlData.solverName.toLowerCase().contains("xalog")) SetXALog.enableXALog();
            ClearValue.clearValues(modelList, modelDataSetMap);
            sds.clearVarTimeArrayCycleValueMap();
            sds.clearVarCycleIndexByTimeStep();
            int i=0;
            while (i<modelList.size() && noError){
                int cycleI=i+1;
                String strCycleI=cycleI+"";
                boolean isSelectedCycleOutput=General.isSelectedCycleOutput(strCycleI);

                String model=modelList.get(i);
                ModelDataSet mds=modelDataSetMap.get(model);
                ControlData.currModelDataSet=mds;
                ControlData.currCycleName=model;
                ControlData.currCycleIndex=i;
                VariableTimeStep.setCycleTimeStep(sds);
                VariableTimeStep.setCurrentDate(sds, ControlData.cycleStartDay, ControlData.cycleStartMonth, ControlData.cycleStartYear);

                while(VariableTimeStep.checkEndDate(ControlData.currDay, ControlData.currMonth, ControlData.currYear, ControlData.cycleEndDay, ControlData.cycleEndMonth, ControlData.cycleEndYear)<0 && noError) {
                    ParseTree modelCondition = modelConditionParsers.get(i);
                    boolean condition;
                    try {
                        condition = Evaluator.evaluateCondition(ControlData.currDay, ControlData.currMonth, ControlData.currYear, modelCondition);
                    } catch (Exception e) {
                        Error.addEvaluationError("Model condition evaluation has error.");
                        condition = false;
                    }
                    
                    if (condition){
                        ClearValue.clearCycleLoopValue(modelList, modelDataSetMap);
                        ControlData.currSvMap=mds.svMap;
                        ControlData.currSvFutMap=mds.svFutMap;
                        ControlData.currDvMap=mds.dvMap;
                        ControlData.currDvSlackSurplusMap=mds.dvSlackSurplusMap;
                        ControlData.currAliasMap=mds.asMap;
                        ControlData.currGoalMap=mds.gMap;
                        ControlData.currTsMap=mds.tsMap;
                        ControlData.isPostProcessing=false;
                        mds.processModel();
                    } else {
                        if (ControlData.outputType==1){
                            if (ControlData.isOutputCycle && isSelectedCycleOutput){
                                HDF5Writer.skipOneCycle(mds, cycleI);
                            }
                        }
                        System.out.println("Cycle "+cycleI+" in "+ControlData.currYear+"/"+ControlData.currMonth+"/"+ControlData.currDay+" Skipped. ("+model+")");
                        new AssignPastCycleVariable();
                        ControlData.currTimeStep.set(ControlData.currCycleIndex, ControlData.currTimeStep.get(ControlData.currCycleIndex)+1);
                        if (TimeOperations.isMonthlyInterval(ControlData.timeStep)){
                            VariableTimeStep.currTimeAddOneMonth();
                        }else{
                            VariableTimeStep.currTimeAddOneDay();
                        }
                    }
                }
                i=i+1;
            }
            Date date1= new Date(ControlData.currYear-1900, ControlData.currMonth-1, ControlData.currDay);
            Date date2= new Date(ControlData.outputYear-1900, ControlData.outputMonth-1, ControlData.outputDay);
            Date date3= new Date(ControlData.endYear-1900, ControlData.endMonth-1, ControlData.endDay);
            if (ControlData.yearOutputSection>0 && (date1.after(date2) || date1.after(date3))){
                if (ControlData.writeInitToDVOutput && sectionI==0){
                    DssOperation.writeInitDvarAliasToDSS();
                }
                sectionI++;
                DssOperation.writeDVAliasToDSS();
                ControlData.setMemDate();
                DssOperation.shiftData();
                ControlData.setOutputDate();
            }
            VariableTimeStep.setCycleStartDate(ControlData.cycleEndDay, ControlData.cycleEndMonth, ControlData.cycleEndYear);
            VariableTimeStep.setCycleEndDate(sds);
        }
        if (ControlData.solverType == Param.SOLVER_LPSOLVE) {
            //ControlData.lpssolver.deleteLp();
        } else if (ControlData.solverType == Param.SOLVER_CLP0) {
            // close clp exe
        } else if (ControlData.solverType == Param.SOLVER_CBC0) {
            // close cbc exe
        } else if (ControlData.solverType == Param.SOLVER_CBC || ControlData.solverType == Param.SOLVER_CBC1) {
            CbcSolver.close();
        } else if (ControlData.solverType == Param.SOLVER_CLP1 || ControlData.solverType == Param.SOLVER_CLP) {
            ClpSolver.close();
        } else {
            ControlData.xasolver.close();
        }

        if (ControlData.yearOutputSection<0 && ControlData.writeInitToDVOutput) DssOperation.writeInitDvarAliasToDSS();
        if (ControlData.yearOutputSection<0) DssOperation.writeDVAliasToDSS();
        ControlData.dvDss.close();
        if (ControlData.outputType==1){
            HDF5Writer.createDvarAliasLookup();
            HDF5Writer.writeTimestepData();
            HDF5Writer.writeCyclesDvAlias();
            HDF5Writer.closeDataStructure();
        }else if (ControlData.outputType==2){
            mySQLCWriter.process();
        }else if (ControlData.outputType==3){
            mySQLRWriter.process();
        }else if (ControlData.outputType==4){
            sqlServerRWriter.process();
        }else if (ControlData.outputType==5){
            CsvOperation co = new CsvOperation();
            co.ouputCSV(FilePaths.fullCsvPath, 0);
        }

        // write complete or fail
        if (enableProgressLog || enableConfigProgress) {
            try {
                FileWriter progressFile;
                if (enableConfigProgress){
                    progressFile= new FileWriter(StudyUtils.configFilePath+".prgss");
                }else{
                    progressFile= new FileWriter(FilePaths.mainDirectory + "progress.txt", true);
                }
                PrintWriter pw = new PrintWriter(progressFile);
                if (Error.getTotalError() > 0) {
                    pw.println("Run failed.");
                } else {
                    pw.println("Run completed.");
                }
                pw.close();
                progressFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

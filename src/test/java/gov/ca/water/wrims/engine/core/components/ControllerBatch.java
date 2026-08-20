package gov.ca.water.wrims.engine.core.components;

import gov.ca.water.solverdata.SolverData;
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
import gov.ca.water.wrims.engine.core.evaluator.DssOperation;
import gov.ca.water.wrims.engine.core.evaluator.WeightEval;
import gov.ca.water.wrims.engine.core.fromWrims2.ErrorCheck;
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
import java.util.*;

public class ControllerBatch {
    private int infeasCyclIndex= -100;
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
            ControlData.cycWarmStart = sds.getCycWarmStart();
            ControlData.cycWarmStop = sds.getCycWarmStop();
            ControlData.cycWarmUse = sds.getCycWarmUse();
        }
        catch (SyntaxErrorException e) {
            System.err.println("WRESL+ syntax error(s) encountered in file "+e.getSourceFile());
            List<String> syntaxErrors = e.getErrorMessages();
            for (int i=0; i<syntaxErrors.size(); i++) {
                System.err.println(syntaxErrors.get(i));
            }
            return;
        }
        catch (EvaluationErrorException e) {
            System.err.println("Evaluation error: " + e.getErrorMessage());
            if (e.getLine() > 0 ) {
                System.err.println("                  " + "File " + e.getSourceFile() + ", line " + e.getLine());
            }
            return;
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

    private void enableInfeasibilityLogging(int cycleIndex) {
        // dump error to file
        Error.writeSolvingErrorFile("Error_solving.txt");
        Error.writeErrorLog();
        // keep track of when infeasibility occurs
        infeasCyclIndex = cycleIndex;
        Error.error_solving = new ArrayList();
        // Enable logging for infeasibility
        ILP.loggingLpSolve = false;
        ILP.loggingCplexLp = true;
        ILP.loggingAllCycles = true;
        ILP.logging = true;
        ILP.loggingVariableValue = true;
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
            while (i<modelList.size() && noError) {
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

                while (VariableTimeStep.checkEndDate(ControlData.currDay, ControlData.currMonth, ControlData.currYear, ControlData.cycleEndDay, ControlData.cycleEndMonth, ControlData.cycleEndYear)<0 && noError) {
                    boolean modelProcessed = Evaluator.processModel(sds, i, ControlData.currDay, ControlData.currMonth, ControlData.currYear, ControlData.nThreads, ControlData.showRunTimeMessage);
                    if (modelProcessed) {
                        // Compile solver data
                        SolverData.compile(sds, i);

                        if (ILP.logging && (isSelectedCycleOutput || ILP.loggingAllCycles)) {

                            long beginT = System.currentTimeMillis();
                            ILP.setIlpFile();
                            ILP.writeIlp();
                            long endT = System.currentTimeMillis();
                            double time_second = (endT-beginT)/1000.;
                            ControlData.lpFileWritingTime += time_second;


                            if (ILP.loggingVariableValue) {
                                ILP.setVarFile();
                                ILP.writeSvarValue();
                            }
                        }

                        if (Error.error_evaluation.size()>=1){
                            Error.writeEvaluationErrorFile("Error_evaluation.txt");
                            Error.writeErrorLog();
                            noError=false;break time_marching;
                        }

                        // choose solver to solve. TODO: this is not efficient. need to be done outside ILP
                        if (ControlData.solverType == Param.SOLVER_LPSOLVE.intValue()) {
                            LPSolveSolver.setLP(ILP.lpSolveFilePath);
                            LPSolveSolver.solve(sds, i);
                            if (Error.error_solving.size()<1) {
                                if (ILP.logging)  {
                                    ILP.writeObjValue_LPSOLVE();
                                    if (ILP.loggingVariableValue) ILP.writeDvarValue_LPSOLVE();
                                }
                            }
                        // for cbc0
                        } else if (ControlData.solverType == Param.SOLVER_CBC0.intValue()){

                            ILP.closeCplexLpFile(); // prevent double-locked by both core and ilp

                            // send lp file path to cbc
                            Cbc0Solver.setLP(ILP.cplexLpFilePath);

                            // call cbc solve
                            Cbc0Solver.solve(sds, i);

                            // check solving errors and put them in Error.error_solving
                            if (Error.error_solving.size()<1) {
                                if (ILP.logging) {

                                    ILP.reOpenCplexLpFile(true);
                                    // write objValue in lp file
                                    ILP.writeObjValue_Clp0_Cbc0();
                                    if (ILP.loggingVariableValue) {
                                        // TODO: write solution
                                        ILP.writeDvarValue_Clp0_Cbc0(Cbc0Solver.varDoubleMap);
                                    }
                                }
                            }
                        // for clp
                        } else if (ControlData.solverType == Param.SOLVER_CLP0.intValue()){

                            ILP.closeCplexLpFile(); // prevent double-locked by both core and ilp

                            // send lp file path to clp
                            Clp0Solver.setLP(ILP.cplexLpFilePath);

                            // call clp solve
                            Clp0Solver.solve(sds, i);

                            // check solving errors and put them in Error.error_solving
                            if (Error.error_solving.size()<1) {
                                if (ILP.logging)  {

                                    ILP.reOpenCplexLpFile(true);
                                    // write objValue in lp file
                                    ILP.writeObjValue_Clp0_Cbc0();
                                    if (ILP.loggingVariableValue) {
                                        // TODO: write solution
                                        ILP.writeDvarValue_Clp0_Cbc0(Clp0Solver.varDoubleMap);
                                    }
                                }
                            }
                        } else if (ControlData.solverType == Param.SOLVER_CBC1.intValue()||ControlData.solverType == Param.SOLVER_CBC.intValue()){

                            if(!ControlData.useCplexLpString) ILP.closeCplexLpFile(); // prevent double-locked by both core and ilp

                            CbcSolver.newProblem(sds, i);

                            // check solving errors and put them in Error.error_solving
                            if (Error.error_solving.size()<1) {
                                if (ILP.logging)  {

                                    if(!ControlData.useCplexLpString) ILP.reOpenCplexLpFile(true);
                                    // write objValue in lp file
                                    ILP.writeObjValue_Clp0_Cbc0();
                                    if(ControlData.saveCplexLpStringToFile) ILP.saveCplexLpStringToFile();
                                    if (ILP.loggingVariableValue) {
                                        // TODO: write solution
                                        ILP.writeDvarValue_Clp0_Cbc0(CbcSolver.varDoubleMap);
                                    }
                                }
                            }

                        } else if (ControlData.solverType == Param.SOLVER_CLP1.intValue()){

                            ILP.closeCplexLpFile(); // prevent double-locked by both core and ilp

                            // send lp file path to clp
                            ClpSolver.newProblem(ILP.cplexLpFilePath, true, sds, i);

                            // check solving errors and put them in Error.error_solving
                            if (Error.error_solving.size()<1) {
                                if (ILP.logging)  {

                                    ILP.reOpenCplexLpFile(true);
                                    // write objValue in lp file
                                    ILP.writeObjValue_Clp0_Cbc0();
                                    if (ILP.loggingVariableValue) {
                                        // TODO: write solution
                                        ILP.writeDvarValue_Clp0_Cbc0(ClpSolver.varDoubleMap);
                                    }
                                }
                            }

                        } else {

                            new XASolver(sds, i);

                            if (ILP.logging) {
                                ILP.writeObjValue_XA();
                                if (ILP.loggingVariableValue) ILP.writeDvarValue_XA();
                            }
                        }

                        ILP.closeIlpFile();

                        // check monitored dvar list. they are slack and surplus generated automatically
                        // from the weight group deviation penalty
                        // give error if they are not zero or greater than a small tolerance.
                        noError = !ErrorCheck.checkDeviationSlackSurplus(mds.deviationSlackSurplus_toleranceMap, mds.dvMap);

                        if (ControlData.showRunTimeMessage) System.out.println("Solving Done.");
                        if (Error.error_solving.size()<1) {
                            ControlData.isPostProcessing=true;
                            Evaluator.processAliases(sds, i, ControlData.showRunTimeMessage);
                            if (ControlData.showRunTimeMessage) System.out.println("Assign Alias Done.");
                        } else if (infeasCyclIndex==i) {
                            noError=false;
                        } else {
                            enableInfeasibilityLogging(i);
                            Error.writeErrorLog();
                            noError=true;
                            i=-1;
                        }
                        if (ControlData.outputType==1){
                            if (ControlData.isOutputCycle && isSelectedCycleOutput){
                                HDF5Writer.writeOneCycleSv(mds, cycleI);
                            }
                        }
                        if (ILP.loggingUsageMemeory) ILP.logUsageMemory(ControlData.currYear, ControlData.currMonth, ControlData.currDay, ControlData.currCycleIndex);
                        //ILP.logUsageMemory(ControlData.currYear, ControlData.currMonth, ControlData.currDay, ControlData.currCycleIndex);
                        System.out.println("Cycle "+cycleI+" in "+ControlData.currYear+"/"+ControlData.currMonth+"/"+ControlData.currDay+" Done. ("+model+")");
                        if (Error.error_evaluation.size()>=1) noError=false;
                        try{
                            if (enableConfigProgress) {
                                FileWriter progressFile = new FileWriter(StudyUtils.configFilePath+".prgss");
                                PrintWriter pw = new PrintWriter(progressFile);
                                pw.println("Run to "+ControlData.currYear +"/"+ ControlData.currMonth +"/"+ ControlData.currDay);
                                pw.close();
                                progressFile.close();
                            } else if(enableProgressLog) {
                                FileWriter progressFile= new FileWriter(FilePaths.mainDirectory + "progress.txt");
                                PrintWriter pw = new PrintWriter(progressFile);
                                int cy = 0;
                                if (ControlData.currYear > cy) {
                                    cy = ControlData.currYear;
                                    pw.println(ControlData.startYear + " " + ControlData.endYear + " " + ControlData.currYear +" "+ ControlData.currMonth);
                                    pw.close();
                                    progressFile.close();
                                }
                            }
                        } catch(IOException e) {
                            e.printStackTrace();
                        }

    //                    if (CbcSolver.intLog && ControlData.solverType == Param.SOLVER_CBC.intValue()) {
    //                        CbcSolver.logIntCheck(sds);
    //                    }
//
    //                    if (ControlData.solverType == Param.SOLVER_CBC1.intValue()||ControlData.solverType == Param.SOLVER_CBC.intValue()) { CbcSolver.resetModel();}

                        ControlData.currTimeStep.set(ControlData.currCycleIndex, ControlData.currTimeStep.get(ControlData.currCycleIndex)+1);
                        if (TimeOperations.isMonthlyInterval(ControlData.timeStep)) {
                            VariableTimeStep.currTimeAddOneMonth();
                        } else {
                            VariableTimeStep.currTimeAddOneDay();
                        }
                    } else {
                        if (ControlData.outputType==1){
                            if (ControlData.isOutputCycle && isSelectedCycleOutput) {
                                HDF5Writer.skipOneCycle(mds, cycleI);
                            }
                        }
                        System.out.println("Cycle "+cycleI+" in "+ControlData.currYear+"/"+ControlData.currMonth+"/"+ControlData.currDay+" Skipped. ("+model+")");
                        new AssignPastCycleVariable();
                        ControlData.currTimeStep.set(ControlData.currCycleIndex, ControlData.currTimeStep.get(ControlData.currCycleIndex)+1);
                        if (TimeOperations.isMonthlyInterval(ControlData.timeStep)) {
                            VariableTimeStep.currTimeAddOneMonth();
                        } else {
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
        if (ControlData.outputType==1) {
            HDF5Writer.createDvarAliasLookup();
            HDF5Writer.writeTimestepData();
            HDF5Writer.writeCyclesDvAlias();
            HDF5Writer.closeDataStructure();
        } else if (ControlData.outputType==2) {
            mySQLCWriter.process();
        } else if (ControlData.outputType==3) {
            mySQLRWriter.process();
        } else if (ControlData.outputType==4) {
            sqlServerRWriter.process();
        } else if (ControlData.outputType==5) {
            sds.outputCSV(FilePaths.fullCsvPath,
                       0,
                          ControlData.ovOption,
                          ControlData.ovFile,
                          ControlData.isSimOutput,
                          ControlData.writeInitToDVOutput,
                          ControlData.partA,
                          ControlData.svDvPartF);
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

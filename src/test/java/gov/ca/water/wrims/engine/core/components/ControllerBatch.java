package gov.ca.water.wrims.engine.core.components;

import gov.ca.water.wresl.domain.StudyDataSet;
import gov.ca.water.wresl.errors.EvaluationErrorException;
import gov.ca.water.wresl.errors.SyntaxErrorException;
import gov.ca.water.wresl.parsing.Study;
import gov.ca.water.wrims.engine.core.components.ControlData;
import gov.ca.water.wrims.engine.core.config.ConfigUtils;
import gov.ca.water.wrims.engine.core.launch.LaunchConfiguration;
import gov.ca.water.wrims.engine.core.sql.DataBaseProfile;
import org.antlr.runtime.RecognitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.List;

public class ControllerBatch {
    public boolean enableProgressLog = false;
    public boolean enableConfigProgress = false;

    public ControllerBatch(String[] args) {
        long startTimeInMillis = Calendar.getInstance().getTimeInMillis();
        try {
            new DataBaseProfile(args);
            processArgs(args);
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



}

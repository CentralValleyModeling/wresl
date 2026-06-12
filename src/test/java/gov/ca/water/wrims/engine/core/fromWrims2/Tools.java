package gov.ca.water.wrims.engine.core.fromWrims2;

import gov.ca.water.wresl.domain.Goal;
import gov.ca.water.wresl.domain.ModelDataSet;
import gov.ca.water.wrims.engine.core.components.ControlData;
import gov.ca.water.wrims.engine.core.components.FilePaths;

import java.io.*;

public class Tools {

    public static PrintWriter openFile(String dirPath, String fileName) throws IOException {

        File f = new File(dirPath, fileName);
        File dir = new File(f.getParent());
        dir.mkdirs();
        f.createNewFile();

        return new PrintWriter(new BufferedWriter(new FileWriter(f)));
    }

    public static PrintWriter openFile(String dirPath, String fileName, boolean isAppend) throws IOException {

        File f = new File(dirPath, fileName);
        File dir = new File(f.getParent());
        dir.mkdirs();
        f.createNewFile();

        return new PrintWriter(new BufferedWriter(new FileWriter(f,isAppend)));
    }

    public static void quickLog(String fn, String x) {
        quickLog(fn, x, false);
    }

    public static void quickLog(String fn, String x, boolean isAppend) {

        File ilpRootDir = new File(FilePaths.mainDirectory, "=ILP=");
        File ilpDir = new File(ilpRootDir, StudyUtils.configFileName);

        try {
            PrintWriter quickLogFile = Tools.openFile(ilpDir.getAbsolutePath(), fn, isAppend);
            quickLogFile.println(x);
            quickLogFile.close();
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public static String findGoalLocation(String goalName){
        ModelDataSet mds = ControlData.currModelDataSet;
        String sourceLocation = "";
        if (mds.gMap.containsKey(goalName)){
            Goal goal=mds.gMap.get(goalName);
            sourceLocation="("+goal.fromWresl+":"+goal.line+")";
        }
        return sourceLocation;
    }

    public static String noZerofmt(double d)
    {
        if(d == (long) d)
            return String.format("%d",(long)d);
        else
            return String.format("%s",d);
    }
}

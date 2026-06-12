package gov.ca.water.wrims.engine.core.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import gov.ca.water.wrims.engine.core.fromWrims2.StudyUtils;
import org.antlr.runtime.RecognitionException;

import gov.ca.water.wresl.domain.ModelDataSet;
import gov.ca.water.wrims.engine.core.components.ControlData;
import gov.ca.water.wrims.engine.core.components.FilePaths;


public class InfeasibilityAnalysis {
	private final static String ifsExt=".ifs";
	private final static String wExt=".wresl";
	private final static String cPref="c: ";
	private final static String fPref="f: ";
	public static LinkedHashSet<String> constraintSetMixedCases = new LinkedHashSet<String>();
	public static LinkedHashSet<String> constraintSet = new LinkedHashSet<String>();
	
	public static String procRelativePath(String path, String folderPath){
		String absPath=folderPath+"\\"+path;
		return absPath;
	}
	
	public static LinkedHashSet<String> procFolderToFiles(String absPath, ModelDataSet mds){
		LinkedHashSet<String> subFpSet = new LinkedHashSet<String>();
		File[] files = new File(absPath).listFiles(); 

		if (files !=null){
			for (File file : files) {
				if (file.isFile()) {
					String fp = file.getAbsolutePath().toLowerCase();
					if (fp.endsWith(wExt) && isStudyInclFile(fp, mds)){
						subFpSet.add(fp);
					}
				}else if (file.isDirectory()){
					String fp = file.getAbsolutePath().toLowerCase();
					subFpSet.addAll(procFolderToFiles(fp, mds));
				}
			}
		}
		return subFpSet;
	}
	
	public static boolean isStudyInclFile(String fp, ModelDataSet mds){
		boolean isIncluded=false;	
		List<String> ifl = mds.incFileList;
		for (int i=0; i<ifl.size(); i++){
			String ifp = ifl.get(i).toLowerCase();
			if (fp.equals(ifp)){
				isIncluded=true;
			}
		}
		return isIncluded;
	}
	
	public static void convertToLowerCase(){
		constraintSet = new LinkedHashSet<String>();
		Iterator<String> it = constraintSetMixedCases.iterator();
		while (it.hasNext()){
			constraintSet.add(it.next().toLowerCase());
		}
	}
}

package gov.ca.water.wrims.engine.core.evaluator;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import gov.ca.water.wresl.domain.WeightElement;
import gov.ca.water.wrims.engine.core.ilp.ILP;

public class WeightEval {
	private static String farstr = "WeightTableAR.csv";
	
	private static CopyOnWriteArrayList<String> wListAR=new CopyOnWriteArrayList<String>();
	private static ConcurrentHashMap<String, WeightElement> wMapAR=new ConcurrentHashMap<String, WeightElement>();


	public static void outputWtTableAR(){
		try {
			File file = new File(ILP.getIlpDir(), farstr);
			if (!file.exists()){
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file.getAbsolutePath());
			PrintWriter out = new PrintWriter(fw);
		
			Collections.sort(wListAR);
		
			out.println("Name"+","+"Min-Weight"+","+"Timestep-Cycle"+","+"Max-Weight"+","+"Timestep-Cycle");
			for (int i=0; i<wListAR.size(); i++){
				String wtName = wListAR.get(i);
				if (wMapAR.containsKey(wtName)){
					WeightElement wt = wMapAR.get(wtName);
					out.println(wtName+","+wt.min+","+wt.minTC+","+wt.max+","+wt.maxTC);
				}
			}
			out.close();
			fw.close();
		}catch (Exception e){
			e.printStackTrace();
		}
	}
}

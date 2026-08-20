package gov.ca.water.wrims.engine.core.fromWrims2;

import gov.ca.water.wresl.domain.Dvar;
import gov.ca.water.wrims.engine.core.components.Error;

import java.util.*;

public class ErrorCheck {
	
	
	private ErrorCheck(){}

	// these dvars are from slack and surplus of weight group deviation penalty
	public static boolean checkDeviationSlackSurplus(Map<String,Double> deviationSS_toleranceMap, Map<String, Dvar> dvMap) {
		
		ArrayList<String> errorList = new ArrayList<String>();
		
		for (String x : deviationSS_toleranceMap.keySet()){
			
			double v = dvMap.get(x).getLastData().getValue().doubleValue();
			
			if (v > deviationSS_toleranceMap.get(x)) {
				
				errorList.add(x);
				Error.addDeviationError( "Tolerance of ["+ deviationSS_toleranceMap.get(x) +"] exceeded by deviation slack and surplus: ["+x+"]"); 
				
			}			
			
		}
		
		if (errorList.size()>0) {
			
			Error.writeDeviationErrorFile("Error_deviation.txt");
			Error.writeErrorLog();
			return true;
			
		}
		
		return false;
			
	}

}
	

package gov.ca.water.solverdata;

import gov.ca.water.wresl.domain.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;




public class SolverData {
	private static ConcurrentHashMap<String, EvalConstraint> constraintDataMap;
	private static ConcurrentHashMap<String, Dvar> dvarMap;
	private static ConcurrentHashMap<String, WeightElement> weightMap;
	private static  ConcurrentHashMap<String, WeightElement> weightSlackSurplusMap;



	public static ConcurrentHashMap<String, EvalConstraint> getConstraintDataMap(){
		return constraintDataMap;
	}

	public static ConcurrentHashMap<String, Dvar> getDvarMap(){
		return dvarMap;
	}

	public static ConcurrentHashMap<String, WeightElement> getWeightMap(){
		return weightMap;
	}

	public static  ConcurrentHashMap<String, WeightElement> getWeightSlackSurplusMap(){
		return weightSlackSurplusMap;
	}

	public static void addDvar(Dvar dvar) {
		dvarMap.put(dvar.getName(), dvar);
	}

	public static void addWeight(WeightElement weight) {
		weightMap.put(weight.getName(), weight);
	}

	public static void addWeightSlackSurplus(WeightElement weight) {
		weightSlackSurplusMap.put(weight.getName(), weight);
	}

	public static void compile(StudyDataSet sds, int modelIndex) {
        // Retrieve GOAL data from model and add them to SolverData
		constraintDataMap = new ConcurrentHashMap<>();
        Map<String, EvalConstraint> constraintMap = sds.getConstraintMap(modelIndex);
		constraintDataMap.putAll(constraintMap);

		// Retrieve DVAR data from  model and add them to SolverData
		dvarMap = new ConcurrentHashMap<>();
		Map<String, Dvar> dvMap = sds.getDvarMap(modelIndex);
		dvarMap.putAll(dvMap);

		// Retrieve WEIGHT data from  model and add them to SolverData
		weightMap = new ConcurrentHashMap<>();
		Map<String, WeightElement> wtMap = sds.getWeightMap(modelIndex);
		weightMap.putAll(wtMap);

		// Retrieve slack/surplus WEIGHT data from  model and add them to SolverData
		weightSlackSurplusMap = new ConcurrentHashMap<>();
		Map<String, WeightElement> wtSlackSurplusMap = sds.getWeightSlackSurplusMap(modelIndex);
		weightSlackSurplusMap.putAll(wtSlackSurplusMap);
	}
}

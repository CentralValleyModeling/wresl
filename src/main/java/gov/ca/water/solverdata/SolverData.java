package gov.ca.water.solverdata;

import java.util.concurrent.ConcurrentHashMap;

import gov.ca.water.wresl.domain.Dvar;
import gov.ca.water.wresl.domain.Goal;
import gov.ca.water.wresl.domain.WeightElement;


public class SolverData {
	private static ConcurrentHashMap<String, Goal> constraintDataMap;
	private static ConcurrentHashMap<String, Dvar> dvarMap;
	private static ConcurrentHashMap<String, WeightElement> weightMap;
	private static  ConcurrentHashMap<String, WeightElement> weightSlackSurplusMap;



	public static ConcurrentHashMap<String, Goal> getConstraintDataMap(){
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

	public static void addConstraint(Goal goal) {
		constraintDataMap.put(goal.getName(), goal);
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

	public static void clearSolverData() {
		constraintDataMap = new ConcurrentHashMap<>();
		dvarMap = new ConcurrentHashMap<>();
		weightMap = new ConcurrentHashMap<>();
		weightSlackSurplusMap = new ConcurrentHashMap<>();
	}
}

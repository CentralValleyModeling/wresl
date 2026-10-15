package gov.ca.water.wrims.engine.core.evaluator;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import gov.ca.water.io.DSS.DssOperations;
import gov.ca.water.wresl.domain.Dvar;
import gov.ca.water.utilities.TimeOperations;
import gov.ca.water.wrims.engine.core.components.ControlData;

public class DataTimeSeries {
	public static HashMap<String, DssDataSet> svTS = new HashMap<String, DssDataSet> ();
	public static HashMap<String, DssDataSetFixLength> dvAliasTS = new HashMap<String, DssDataSetFixLength> ();
	public static ArrayList<HashMap<String, DssDataSetFixLength>> dvAliasTSCycles = new ArrayList<HashMap<String, DssDataSetFixLength>> ();
	public static HashMap<String, DssDataSet> svInit = new HashMap<String, DssDataSet> ();
	public static HashMap<String, DssDataSet> dvAliasInit = new HashMap<String, DssDataSet> ();



}

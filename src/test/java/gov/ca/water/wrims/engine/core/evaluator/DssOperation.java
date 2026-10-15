package gov.ca.water.wrims.engine.core.evaluator;

import gov.ca.water.utilities.TimeOperations;
import gov.ca.water.wrims.engine.core.components.ControlData;
import gov.ca.water.wrims.engine.core.tools.General;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class DssOperation {

	public static void shiftData(){
		Date outputDate=new Date(ControlData.outputYear-1900, ControlData.outputMonth-1, ControlData.outputDay);
		shiftDvAliasData(ControlData.prevMemDate, ControlData.memStartDate, outputDate);
		shiftDvAliasCycleData(ControlData.prevMemDate, ControlData.memStartDate, outputDate);
	}

	public static void shiftDvAliasData(Date prevMemDate, Date memStartDate, Date outputDate){
		Set dvAliasSet=DataTimeSeries.dvAliasTS.keySet();
		Iterator iterator = dvAliasSet.iterator();
		while(iterator.hasNext()){
			String dvAliasName=(String)iterator.next();
			DssDataSetFixLength ddsfl=DataTimeSeries.dvAliasTS.get(dvAliasName);
			double[] values=ddsfl.getData();
			String timestep=ddsfl.getTimeStep();
			int nTimeStep1 = TimeOperations.getNumberOfTimestep(prevMemDate, memStartDate, timestep)-1;
			int nTimeStep2 = TimeOperations.getNumberOfTimestep(prevMemDate, outputDate, timestep)-1;
			int size = nTimeStep2-nTimeStep1+1;
			//double[] values1=new double[values.length];
			for (int i=0; i<size; i++){
				//values1[i]=values[i+nTimeStep1];
				values[i]=values[i+nTimeStep1];
				values[i+nTimeStep1]=-901.0;
			}
			//ddsfl.data=null;
			//values=null;
			//ddsfl.setData(values1);
			ddsfl.setStartTime(memStartDate);
		}
		System.gc();
	}

	public static void shiftDvAliasCycleData(Date prevMemDate, Date memStartDate, Date outputDate){
		int totalCycleNumber=ControlData.currStudyDataSet.getModelList().size();

		for (int i=0; i<totalCycleNumber; i++){
			int cycleI=i+1;
			String strCycleI=cycleI+"";
			if (General.isSelectedCycleOutput(strCycleI)){
				HashMap<String, DssDataSetFixLength> dvAliasTSCycle = DataTimeSeries.dvAliasTSCycles.get(i);
				Set dvAliasSet=dvAliasTSCycle.keySet();
				Iterator iterator = dvAliasSet.iterator();
				while(iterator.hasNext()){
					String dvAliasName=(String)iterator.next();
					DssDataSetFixLength ddsfl=dvAliasTSCycle.get(dvAliasName);
					double[] values=ddsfl.getData();
					String timestep=ddsfl.getTimeStep();
					int nTimeStep1 = TimeOperations.getNumberOfTimestep(prevMemDate, memStartDate, timestep)-1;
					int nTimeStep2 = TimeOperations.getNumberOfTimestep(prevMemDate, outputDate, timestep)-1;
					int size = nTimeStep2-nTimeStep1+1;
					//double[] values1=new double[values.length];
					for (int j=0; j<size; j++){
						//values1[j]=values[j+nTimeStep1];
						values[j]=values[j+nTimeStep1];
					}
					//ddsfl.data=null;
					//values=null;
					//ddsfl.setData(values1);
					ddsfl.setStartTime(memStartDate);
				}
			}
		}
		System.gc();
	}
}

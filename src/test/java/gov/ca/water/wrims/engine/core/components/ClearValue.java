package gov.ca.water.wrims.engine.core.components;

import gov.ca.water.wresl.domain.Alias;
import gov.ca.water.wresl.domain.Dvar;
import gov.ca.water.wresl.domain.ModelDataSet;
import gov.ca.water.wresl.domain.Svar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClearValue {

	public static void clearCycleLoopValue(List<String> modelList, Map<String, ModelDataSet> modelDataSetMap){
			String model=modelList.get(ControlData.currCycleIndex);
			ModelDataSet mds=modelDataSetMap.get(model);
			List<String> dvList = mds.dvList;
			Map<String, Dvar> dvMap =mds.dvMap;
			for (String dvName: dvList){
				Dvar dvar=dvMap.get(dvName);
				dvar.setData(null);
			}
			List<String> svList = mds.svList;
			Map<String, Svar> svMap =mds.svMap;
			for (String svName: svList){
				Svar svar=svMap.get(svName);
				svar.setData(null);
			}
			List<String> asList = mds.asList;
			Map<String, Alias> asMap =mds.asMap;
			for (String asName: asList){
				Alias alias=asMap.get(asName);
				alias.setData(null);
			}
	}
	
	public static void 	clearValues(List<String> modelList, Map<String, ModelDataSet> modelDataSetMap){
		for (int i=0; i<modelList.size(); i++){
			String model=modelList.get(i);
			ModelDataSet mds=modelDataSetMap.get(model);
			List<String> dvList = mds.dvList;
			Map<String, Dvar> dvMap =mds.dvMap;
			for (String dvName: dvList){
				Dvar dvar=dvMap.get(dvName);
				dvar.setData(null);
			}
			List<String> svList = mds.svList;
			Map<String, Svar> svMap =mds.svMap;
			for (String svName: svList){
				Svar svar=svMap.get(svName);
				svar.setData(null);
			}
			List<String> asList = mds.asList;
			Map<String, Alias> asMap =mds.asMap;
			for (String asName: asList){
				Alias alias=asMap.get(asName);
				alias.setData(null);
			}
			
			mds.clearFutureSvMap();
			mds.clearFutureAsMap();
		}
	}
}

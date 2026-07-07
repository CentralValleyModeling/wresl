package gov.ca.water.wresl.domain;

import gov.ca.water.utilities.ParallelVars;
import gov.ca.water.utilities.Param;
import gov.ca.water.utilities.TimeOperations;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class Timeseries extends WRESLComponent implements Serializable {
    private static final ForkJoinPool pool = new ForkJoinPool();
    private static final long serialVersionUID = 1L;

    public String dssBPart = Param.undefined;
    public String format = Param.undefined;
    public String kind = Param.undefined;
    public String units = Param.undefined;
    public String convertToUnits = Param.undefined;

    private IntDouble data = null;


    public void processTimeseries(List<String> tsList, Map<String, Timeseries> tsMap, int nThreads, boolean showRunTimeMessage, String timeStep, int currYear, int currMonth, int currDay) {
        ProcessTimeseries pt = new ProcessTimeseries(tsList, tsMap, 0, tsList.size()-1, nThreads, showRunTimeMessage, timeStep, currYear, currMonth, currDay);
        pool.invoke(pt);
    }

    // ------------------------------------------------------------
    // --- HELPER CLASS AND METHODS TO PROCESS TIMESERIES DATA IN PARALLEL
    // ------------------------------------------------------------
    private class ProcessTimeseries extends RecursiveTask<Integer> {
        private int threshold;
        private int start;
        private int end;
        private int nThreads;
        private List<String> tList;
        private Map<String, Timeseries> tMap;
        private boolean showRunTimeMessage;
        private String timeStep;
        private int currYear;
        private int currMonth;
        private int currDay;

        private ProcessTimeseries(List<String> tList, Map<String, Timeseries> tMap, int start, int end, int nThreads, boolean showRunTimeMessage, String timeStep, int currYear, int currMonth, int currDay) {
            this.start              = start;
            this.end                = end;
            this.nThreads           = nThreads;
            this.tList              = tList;
            this.tMap               = tMap;
            this.threshold          = (int) Math.ceil(tList.size()*1.0/nThreads);
            this.showRunTimeMessage = showRunTimeMessage;
            this.timeStep           = timeStep;
            this.currYear           = currYear;
            this.currMonth          = currMonth;
            this.currDay            = currDay;
        }

        @Override
        protected Integer compute() {
            if (this.end - this.start < this.threshold) {
                return computeDirectly();
            } else {
                List<ProcessTimeseries> subTasks=new ArrayList<>(this.nThreads);

                for (int i=0; i<this.nThreads; i++){
                    int subStart, subEnd;
                    subStart=i*this.threshold;
                    if (i==this.nThreads-1){
                        subEnd=this.end;
                    }else{
                        subEnd=Math.min(this.end, (i+1)*this.threshold-1);
                    }
                    subTasks.add(new ProcessTimeseries(this.tList,
                                                       this.tMap,
                                                       subStart,
                                                       subEnd,
                                                       this.nThreads,
                                                       this.showRunTimeMessage,
                                                       this.timeStep,
                                                       this.currYear,
                                                       this.currMonth,
                                                       this.currDay));
                }

                for(ProcessTimeseries subtask : subTasks){
                    subtask.fork();
                }

                int sum=0;
                for (int i=0; i<this.nThreads; i++){
                    sum=sum+subTasks.get(i).join();
                }
                return sum;
            }
        }

        protected int computeDirectly() {
            for (int ii=this.start; ii<=this.end; ii++){
                String tsName=this.tList.get(ii);
                if (this.showRunTimeMessage) System.out.println("Processing timeseries "+tsName);
                Timeseries ts=tMap.get(tsName);
                ParallelVars prvs = TimeOperations.findTime(this.timeStep, 0, this.currYear, this.currMonth, this.currDay);
                ts.data = new IntDouble(svarTimeSeries(tsName, 0, prvs),false);
            }
            return 1;
        }

        private double svarTimeSeries(String ident, int idValue, ParallelVars prvs){
    //        int index;
    //        String entryNameTS=DssOperation.entryNameTS(ident, this.timeStep);
    //        if (DataTimeSeries.svTS.containsKey(entryNameTS)){
    //            DssDataSet dds=DataTimeSeries.svTS.get(entryNameTS);
    //            index =timeSeriesIndex(dds, prvs);
    //            ArrayList<Double> data=dds.getData();
    //            if (index>=0 && index<data.size() && index>=dds.getStudyStartIndex()){
    //                double value=data.get(index);
    //                if (dds.fromDssFile()){
    //                    if (value != -901.0 && value != -902.0){
    //                        return value;
    //                    }
    //                }else{
    //                    return value;
    //                }
    //            }
    //        }
    //        if (DataTimeSeries.svInit.containsKey(entryNameTS)){
    //            DssDataSet dds=DataTimeSeries.svInit.get(entryNameTS);
    //            index =timeSeriesIndex(dds, prvs);
    //            ArrayList<Double> data=dds.getData();
    //            if (index>=0 && index<data.size()){
    //                double value=data.get(index);
    //                if (value !=-901.0){
    //                    return value;
    //                }
    //            }
    //        }else{
    //            DataTimeSeries.lookInitDss.add(entryNameTS);
    //            if (getSVInitTimeseries(ident)){
    //                DssDataSet dds=DataTimeSeries.svInit.get(entryNameTS);
    //                prvs=TimeOperation.findTime(idValue);
    //                index =timeSeriesIndex(dds, prvs);
    //                ArrayList<Double> data=dds.getData();
    //                if (index>=0 && index<data.size()){
    //                    double value=data.get(index);
    //                    if (value !=-901.0){
    //                        return value;
    //                    }
    //                }
    //            }
    //        }
    //        if (ControlData.allowSvTsInit && DataTimeSeries.svTS.containsKey(entryNameTS)){
    //            DssDataSet dds=DataTimeSeries.svTS.get(entryNameTS);
    //            index =timeSeriesIndex(dds, prvs);
    //            ArrayList<Double> data=dds.getData();
    //            if (index>=0 && index<data.size() && index<dds.getStudyStartIndex()){
    //                double value=data.get(index);
    //                if (dds.fromDssFile()){
    //                    if (value != -901.0 && value != -902.0){
    //                        return value;
    //                    }
    //                }else{
    //                    return value;
    //                }
    //            }
    //        }
    //        Error.addEvaluationError("The data requested for timeseries "+ident+" does not match the entries in the dss file. Please check the name, kind, unit, and requested time period.");
            return 1.0;
        }
    }



}

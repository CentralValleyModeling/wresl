package gov.ca.water.wresl.domain;

import gov.ca.water.io.DSS.CondensedReferenceCacheAndRead;
import gov.ca.water.io.DSS.DssOperations;
import gov.ca.water.io.HDF5.HDF5Reader;
import gov.ca.water.utilities.MiscUtilities;
import gov.ca.water.utilities.ParallelVars;
import gov.ca.water.utilities.Param;
import gov.ca.water.utilities.TimeOperations;
import hec.heclib.util.HecTime;
import hec.io.TimeSeriesContainer;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Timeseries extends WRESLComponent implements Serializable {
    private static final long serialVersionUID = 1L;

    public String dssBPart = Param.undefined;
    public String kind = Param.undefined;
    public String units = Param.undefined;
    public String convertToUnits = Param.undefined;
    public String timeStep = "";
    public Date startTime;
    public int studyStartIndex = -1;

    private List<Double> data = new ArrayList<>();


    // --------------------
    // --- SETTERS
    // --------------------
    public void setDssBPart(String dssBPart) { this.dssBPart = dssBPart; }

    public void setKind(String kind) { this.kind = kind; }

    public void setUnits(String units) { this.units = units; }

    public void setConvertToUnits(String convertToUnits) { this.convertToUnits = convertToUnits; }


    // --------------------
    // --- GETTERS
    // --------------------
    public String getKind() {
        return this.kind;
    }

    public String getTimeStep(){
        return this.timeStep;
    }

    public Date getStartTime(){
        return this.startTime;
    }

    public List<Double> getData(){
        return this.data;
    }

    public String getUnits(){
        return this.units;
    }

    public String getConvertToUnits(){
        return this.convertToUnits;
    }


    // --------------------
    // --- MISC. METHODS
    // --------------------

    // Create a copy of the Timeseries data
    public Timeseries copyOf() {
        Timeseries tsCopy = new Timeseries();

        tsCopy.dssBPart = this.dssBPart;
        tsCopy.kind = this.kind;
        tsCopy.units = this.units;
        tsCopy.convertToUnits = this.convertToUnits;
        tsCopy.timeStep = this.timeStep;
        tsCopy.startTime = this.startTime;
        tsCopy.studyStartIndex = this.studyStartIndex;
        tsCopy.data = this.data;

        return tsCopy;
    }

    // Read timeseries data from DSS file
    public boolean readTimeseries(CondensedReferenceCacheAndRead.CondensedReferenceCache cacheTS, String partA, String partF, String timeStep, int studyStartYear, int studyStartMonth, int studyStartDay) {
        // Read data
        TimeSeriesContainer tsc;
        tsc = DssOperations.readTimeSeriesData(cacheTS, this.units, timeStep, partA, this.dssBPart, this.kind, "", partF);

        // Return "false" if data was not read
        if (tsc == null) {return false;}

        // DSS data time related info
        HecTime startTime = tsc.getStartTime();
        int tsStartYear = startTime.year();
        int tsStartMonth = startTime.month();
        int tsStartDay = startTime.day();

        // Data that is read
        List<Double> dataArray = new ArrayList<>();
        double[] values = tsc.values;
        if (this.units.equals("taf") && this.convertToUnits.equals("cfs")) {
            // Convert taf to cfs
            int i = 0;
            for (double dataEntry : values) {
                if (dataEntry == -901.0) {
                    dataArray.add(-901.0);
                } else if (dataEntry == -902.0) {
                    dataArray.add(-902.0);
                } else {
                    ParallelVars prvs = TimeOperations.findTime(timeStep, i, tsStartYear, tsStartMonth, tsStartDay);
                    double dataEntryValue = dataEntry * MiscUtilities.tafcfs("taf_cfs", timeStep, prvs);
                    dataArray.add(dataEntryValue);
                }
                i = i + 1;
            }
        } else if (this.units.equals("cfs") && this.convertToUnits.equals("taf")) {
            // Convert cfs to taf
            int i = 0;
            for (double dataEntry : values) {
                if (dataEntry == -901.0) {
                    dataArray.add(-901.0);
                } else if (dataEntry == -902.0) {
                    dataArray.add(-902.0);
                } else {
                    ParallelVars prvs = TimeOperations.findTime(timeStep, i, tsStartYear, tsStartMonth, tsStartDay);
                    double dataEntryValue = dataEntry * MiscUtilities.tafcfs("cfs_taf", timeStep, prvs);
                    dataArray.add(dataEntryValue);
                }
                i = i + 1;
            }
        } else {
            // No unit conversion
            for (double dataEntry :  values){
                dataArray.add(dataEntry);
            }
        }

        // Store data in timeseries
        this.timeStep = timeStep;
        this.data = dataArray;
        this.startTime = new Date(tsStartYear-1900, tsStartMonth-1, tsStartDay);
        this.generateStudyStartIndex(studyStartYear, studyStartMonth, studyStartDay);

        // If made it to this point, successful read
        return true;
    }

    // Read timeseries data from HDF5 file
    public boolean readTimeseries(String timeStep, int studyStartYear, int studyStartMonth, int studyStartDay) {
        // Read data
        double[] values;
        Date tsStartDate = new Date(21, 9, 31, 24, 0);
        values = HDF5Reader.readTimeSeriesData(this.dssBPart, this.kind, this.units, timeStep, tsStartDate);
        if (values == null) {return false; }

        // Data time related info
        int tsStartYear = tsStartDate.getYear();
        int tsStartMonth = tsStartDate.getMonth();
        int tsStartDay = tsStartDate.getDay();

        // Data that is read
        List<Double> dataArray = new ArrayList<>();
        if (this.units.equals("taf") && this.convertToUnits.equals("cfs")) {
            // Convert taf to cfs
            int i = 0;
            for (double dataEntry : values) {
                if (dataEntry == -901.0) {
                    dataArray.add(-901.0);
                } else if (dataEntry == -902.0) {
                    dataArray.add(-902.0);
                } else {
                    ParallelVars prvs = TimeOperations.findTime(timeStep, i, tsStartYear+1900, tsStartMonth, tsStartDay);
                    double dataEntryValue = dataEntry * MiscUtilities.tafcfs("taf_cfs", timeStep, prvs);
                    dataArray.add(dataEntryValue);
                }
                i = i + 1;
            }
        } else if (this.units.equals("cfs") && this.convertToUnits.equals("taf")) {
            // Convert cfs to taf
            int i = 0;
            for (double dataEntry : values) {
                if (dataEntry == -901.0) {
                    dataArray.add(-901.0);
                } else if (dataEntry == -902.0) {
                    dataArray.add(-902.0);
                } else {
                    ParallelVars prvs = TimeOperations.findTime(timeStep, i, tsStartYear+1900, tsStartMonth, tsStartDay);
                    double dataEntryValue = dataEntry * MiscUtilities.tafcfs("cfs_taf", timeStep, prvs);
                    dataArray.add(dataEntryValue);
                }
                i = i + 1;
            }
        } else {
            // No unit conversion
            for (double dataEntry :  values){
                dataArray.add(dataEntry);
            }
        }

        // Store data in timeseries
        this.timeStep = timeStep;
        this.data = dataArray;
        this.startTime = tsStartDate;
        this.generateStudyStartIndex(studyStartYear, studyStartMonth, studyStartDay);

        // If made it to this point, successful read
        return true;
    }

    // Retrieve data for a time
    public Double retrieveDataForTime(ParallelVars prvs, boolean isInit) {
        int index = timeSeriesIndex(prvs);
        if (index >= 0) {
            if (index < this.data.size()) {
                Double value = null;
                if (isInit) {
                    value = this.data.get(index);
                } else {
                    if (index > this.studyStartIndex) {
                        value = this.data.get(index);
                    }
                }
                if (value == null) { return null; }
                if (value.doubleValue() != -901.0) {
                    if (value.doubleValue() != -902.0) {
                        return value;
                    }
                }
            }
        }

        // If made it this, the time index within the timeseries data was not found; return null
        return null;
    }

    // Generate study start index
    private void generateStudyStartIndex(int studyStartYear, int studyStartMonth, int studyStartDay) {
        Date st = this.startTime;
        int sYear = st.getYear() + 1900;
        int sMonth = st.getMonth() + 1; //Originally it should be getMonth()-1. However, dss data store at 24:00 Jan31, 1921 is considered to store at 0:00 Feb 1, 1921
        Date studyStart = new Date(studyStartYear-1900, studyStartMonth-1, studyStartDay);
        if (TimeOperations.isMonthlyInterval(this.timeStep)) {
            this.studyStartIndex = studyStartYear*12 + studyStartMonth-(sYear*12+sMonth);
        } else {
            Calendar c1 = Calendar.getInstance();
            c1.setTime(st);
            Calendar c2 = Calendar.getInstance();
            c2.setTime(studyStart);
            long indexValue = Duration.between(c1.toInstant(), c2.toInstant()).toDays();
            this.studyStartIndex = (int)indexValue + 1;
        }
    }

    private int timeSeriesIndex(ParallelVars prvs) {
        Date st = this.startTime;
        int sYear = st.getYear() + 1900;
        int sMonth = st.getMonth() + 1; //HEC DSS7 uses getMonth()+1. However, Vista/HecDSS6 uses getMonth()bbecause dss data store at 24:00 Jan31, 1921 is considered to store at 0:00 Feb 1, 1921
        Date dataDate = new Date(prvs.dataYear-1900, prvs.dataMonth-1, prvs.dataDay);
        int index;
        if (TimeOperations.isMonthlyInterval(this.timeStep)) {
            index = prvs.dataYear*12+prvs.dataMonth-(sYear*12+sMonth);
        } else {
            Calendar c1=Calendar.getInstance();
            c1.setTime(st);
            Calendar c2=Calendar.getInstance();
            c2.setTime(dataDate);
            long indexValue = Duration.between(c1.toInstant(), c2.toInstant()).toDays();
            index=(int)indexValue+1;  //HEC DSS7 uses indexValue+1; Vista/Hec DSS6 uses indexValue+2
        }

        return index;
    }
}


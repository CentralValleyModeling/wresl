package gov.ca.water.io.DSS;

import gov.ca.water.utilities.TimeOperations;
import gov.ca.water.wresl.domain.IntDouble;
import hec.heclib.dss.HecDss;
import hec.heclib.util.HecTime;
import hec.io.TimeSeriesContainer;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class DssOperations {

    // Read DSS data
    public static TimeSeriesContainer readTimeSeriesData(CondensedReferenceCacheAndRead.CondensedReferenceCache cacheSvar, String units, String timeStep, String partA, String partB, String partC, String partD, String partF) {
        TimeSeriesContainer tsc;

        // Try reading the data as a DSS6 data
        String path = createPath(partA.toUpperCase(), partB.toUpperCase(), partC.toUpperCase(), partD.toUpperCase(), timeStep.toUpperCase(), partF.toUpperCase());
        tsc = cacheSvar.readFullRecord(path);

        // If unsuccessful, try DSS7 data
        if (tsc == null) {
            if (TimeOperations.isMonthlyInterval(timeStep)) {
                path = createPath(partA.toUpperCase(), partB.toUpperCase(), partC.toUpperCase(), partD.toUpperCase(), "1Month", partF.toUpperCase());
                tsc = cacheSvar.readFullRecord(path);
                if (tsc == null) { return null; }
            }
        }

        // Check that units of the data read is the same as the units described for the timeseries data
        if (!tsc.getUnits().toUpperCase().equals(units.toUpperCase())) { return null; }

        return tsc;
    }

    // Create DSS pathname from parts
    public static String createPath(String partA, String partB, String partC, String partD, String partE, String partF){
        String path = "/" + partA
                + "/" + partB
                + "/" + partC
                + "/" + partD
                + "/" + partE
                + "/" + partF + "/";
        return path;
    }

    public static String regularExp(String part){
        return "^"+part+"$";
    }

    public static String entryNameTS(String name, String timeStep) {
        return name+"@"+timeStep;
    }

    public static String getTSName(String entryNameTS){
        String[] entry=entryNameTS.split("@");
        return entry[0];
    }

    // Print a timeseries data to a given DSS file
    public static void printTimeseriesData(int vHeclib, HecDss dssFile, Date dataStartDate, String tsName, String timeStep, String dataType, String dataUnits, String dataKind, String partA, String partF, List<IntDouble> tsData) {
        TimeSeriesContainer dc = new TimeSeriesContainer();

        String timeStepUsed = timeStep;
        if (vHeclib == 7) {
            if (timeStepUsed.toUpperCase().equals("1MON")) { timeStepUsed = "1Month"; }
        }

        Calendar dataStartCalendar = Calendar.getInstance();
        dataStartCalendar.setTime(dataStartDate);

        double[] dataValues = new double[tsData.size()];
        for (int i=0; i<tsData.size(); i++) {
            IntDouble value = tsData.get(i);
            if (value == null) {
                dataValues[i] = -901.0;
            } else {
                dataValues[i] = value.getValue().doubleValue();
            }
        }

        dc.setStartTime(new HecTime(dataStartCalendar));
        dc.setType(dataType);
        dc.setUnits(dataUnits);
        dc.setValues(dataValues);
        dc.setName(createPath(partA, tsName, dataKind, "", timeStepUsed, partF));
        dc.setStoreAsDoubles(true);

        try {
            dssFile.put(dc);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

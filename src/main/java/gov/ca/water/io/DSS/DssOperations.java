package gov.ca.water.io.DSS;

import gov.ca.water.utilities.TimeOperations;
import hec.io.TimeSeriesContainer;

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
}

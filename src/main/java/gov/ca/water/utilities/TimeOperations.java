package gov.ca.water.utilities;

import mil.army.usace.hec.metadata.Interval;
import mil.army.usace.hec.metadata.IntervalFactory;

import java.time.Duration;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import static java.util.Map.entry;

public final class TimeOperations {

    // Time related lookup data
    private static final Map<String, Integer> daysInMonthMap_String = Map.ofEntries(
            entry("jan", 31),
            entry("feb", -1),  // Need to decide based on year
            entry("mar", 31),
            entry("apr", 30),
            entry("may", 31),
            entry("jun", 30),
            entry("jul", 31),
            entry("aug", 31),
            entry("sep", 30),
            entry("oct", 31),
            entry("nov", 30),
            entry("dec", 31));
    private static final Map<Integer, Integer> daysInMonthMap_Integer = Map.ofEntries(
            entry(1, 31),
            entry(2, -1),  // Need to decide based on year
            entry(3, 31),
            entry(4, 30),
            entry(5, 31),
            entry(6, 30),
            entry(7, 31),
            entry(8, 31),
            entry(9, 30),
            entry(10, 31),
            entry(11, 30),
            entry(12, 31));
    private static final Map<String, Integer> monthNameNumberMap = Map.ofEntries(
            entry("jan", 1),
            entry("feb", 2),  // Need to decide based on year
            entry("mar", 3),
            entry("apr", 4),
            entry("may", 5),
            entry("jun", 6),
            entry("jul", 7),
            entry("aug", 8),
            entry("sep", 9),
            entry("oct", 10),
            entry("nov", 11),
            entry("dec", 12));

    // Number of days in a given month (month is given as String)
    public static int numberOfDays(String month, int year) {
        int days;
        if (month.equals("feb")) {
            if (isLeapYear(year)) {
                days = 29;
            } else {
                days = 28;
            }
        } else {
            days = daysInMonthMap_String.get(month);
        }
        return days;
    }

    // Number of days in a given month (month is given as int)
    public static int numberOfDays(int month, int year) {
        int days;
        if (month == 2) {
            if (isLeapYear(year)) {
                days = 29;
            } else {
                days = 28;
            }
        } else {
            days = daysInMonthMap_Integer.get(month);
        }
        return days;
    }

    private static boolean isLeapYear(int year) {
        if (year % 4 == 0) {
            if (year % 100 != 0) {
                return true;
            } else if (year % 400 == 0) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static Date backOneDay(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DATE, -1);
        Date newDate = c.getTime();
        return newDate;
    }

    public static Date addOneDay(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DATE, 1);
        Date newDate = c.getTime();
        return newDate;
    }

    public static Date backOneMonth(Date date) {
        int month = date.getMonth() - 1;
        int year = date.getYear();
        if (month < 0) {
            month = month + 12;
            year = year - 1;
        }
        int day = TimeOperations.numberOfDays(month + 1, year + 1900);
        Date newDate = new Date(year, month, day);
        return newDate;
    }

    public static Date addOneMonth(Date date) {
        int month = date.getMonth() + 1;
        int year = date.getYear();
        if (month > 11) {
            month = month - 12;
            year = year + 1;
        }
        int day = TimeOperations.numberOfDays(month + 1, year + 1900);
        Date newDate = new Date(year, month, day);
        return newDate;
    }

    public static String monthNameNumeric(int month) {
        if (month < 10) {
            return "0" + Integer.toString(month);
        } else {
            return Integer.toString(month);
        }
    }

    public static String dayName(int day) {
        if (day < 10) {
            return "0" + Integer.toString(day);
        } else {
            return Integer.toString(day);
        }
    }

    public static int getNumberOfTimestep(Date dateA, Date dateB, String timeStep) {
        if (TimeOperations.isMonthlyInterval(timeStep)) {
            int monthA = dateA.getMonth();
            int yearA = dateA.getYear();
            int monthB = dateB.getMonth();
            int yearB = dateB.getYear();
            int diff = (yearB - yearA) * 12 + (monthB - monthA) + 1;
            if (diff <= 0) diff = 0;
            return diff;
        } else {
            Calendar c1 = Calendar.getInstance();
            c1.setTime(dateA);
            Calendar c2 = Calendar.getInstance();
            c2.setTime(dateB);
            int diff = (int) Duration.between(c1.toInstant(), c2.toInstant()).toDays() + 1;
            if (diff <= 0) diff = 0;
            return diff;
        }
    }

    public static boolean isMonthlyInterval(String intervalName) {
        return IntervalFactory.findAllDss(IntervalFactory.equalsName(intervalName)).stream()
                .anyMatch(Interval::isMonthly);
    }

    public static boolean range(int dataMonth, String m1, String m2) {
        int mon1 = monthNameNumberMap.get(m1);
        int mon2 = monthNameNumberMap.get(m2);

        if (mon1 <= mon2) {
            if (dataMonth >= mon1 && dataMonth <= mon2) {
                return true;
            } else {
                return false;
            }
        } else {
            if (dataMonth >= mon1 || dataMonth <= mon2) {
                return true;
            } else {
                return false;
            }
        }
    }

    public static ParallelVars findTime(String timeStep, int value, int year, int month, int day) {
        ParallelVars prvs = new ParallelVars();
        if (isMonthlyInterval(timeStep)) {
            int detYear = value / 12;
            int detMonth = value % 12;
            prvs.dataMonth = month + detMonth;
            prvs.dataYear = year + detYear;
            if (prvs.dataMonth < 1) {
                prvs.dataMonth = prvs.dataMonth + 12;
                prvs.dataYear = prvs.dataYear - 1;
            } else if (prvs.dataMonth > 12) {
                prvs.dataMonth = prvs.dataMonth - 12;
                prvs.dataYear = prvs.dataYear + 1;
            }
            int days = numberOfDays(prvs.dataMonth, prvs.dataYear);
            if (day <= days) {
                prvs.dataDay = day;
            } else {
                prvs.dataDay = days - numberOfDays(month, year) + day;
            }
        } else if (timeStep.equals("1DAY")) {
            Date thisDate = new Date(year - 1900, month - 1, day);
            Calendar c = Calendar.getInstance();
            c.setTime(thisDate);
            c.add(Calendar.DATE, value);
            Date dataDate = c.getTime();
            prvs.dataDay = dataDate.getDate();
            prvs.dataMonth = dataDate.getMonth() + 1;
            prvs.dataYear = dataDate.getYear() + 1900;
        }
        return prvs;
    }

    public static int monthValue(String month) {
        return monthNameNumberMap.get(month);
    }

}

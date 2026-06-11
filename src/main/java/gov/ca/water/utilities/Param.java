package gov.ca.water.utilities;

public class Param {
    public static final String undefined = "undefined";
    public static final String always = "always";
    public static final String defaultCaseName = "default";
    public static final String no =  "n";
    public static final String yes = "y";

    public static final String zero= "0";
    public static final String dv_std_lowerBound = "0";
    public static final String dv_std_upperBound= "1e38";
    public static final String dv_std_integer_lowerBound= "0";
    public static final String dv_std_integer_upperBound= "1";
    public static final String dv_lower_unbounded= "-1e38";
    public static final String dv_upper_unbounded= "1e38";
    public static final String lower_unbounded = "lower_unbounded";
    public static final String upper_unbounded = "upper_unbounded";
    public static final double lower_unbounded_double= -1e23;
    public static final double upper_unbounded_double= 1e23;


    public static final double inf_assumed = 10e30;
    public static final double inf = Double.POSITIVE_INFINITY;

    public static final Integer SOLVER_XA = 10;
    public static final Integer SOLVER_LPSOLVE = 20;
    public static final Integer SOLVER_GUROBI = 40;
    public static final Integer SOLVER_CLP0 = 50;
    public static final Integer SOLVER_CLP1 = 60;
    public static final Integer SOLVER_CLP = 70;
    public static final Integer SOLVER_CBC0 = 80;  // exe by file
    public static final Integer SOLVER_CBC1 = 90;  // jni by file
    public static final Integer SOLVER_CBC = 100;  // jni
    public static final Integer cbcMinIntNumber = 2;  // minimum integer number for warm start

}

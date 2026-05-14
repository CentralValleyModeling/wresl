package gov.ca.water.wresl.parsing;

import gov.ca.water.wresl.domain.Param;
import org.antlr.v4.runtime.tree.ParseTree;

public class Sequence {
    public String sequenceName = Param.undefined;
    public String modelName = Param.undefined;
    public int order = 0;
    public String condition = Param.always;
    public ParseTree conditionParseTree = null;
    public String timeStep = Param.undefined;

}

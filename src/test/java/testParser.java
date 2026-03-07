import gov.ca.water.wresl.compile.*;
import gov.ca.water.wresl.domain.StudyDataSet;
import org.antlr.v4.runtime.tree.ParseTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;

public class testParser {
    private static final Logger logger = LoggerFactory.getLogger(testParser.class);

    public static void main(String[] args) {
        Path mainWRESL = Path.of(args[0]).normalize();
        logger.atInfo().setMessage("mainWresl={}").addArgument(mainWRESL).log();
        Study study = new Study("TEST", mainWRESL);
        study.compile();
    }
}

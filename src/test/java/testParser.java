import gov.ca.water.wresl.parsing.*;
import gov.ca.water.wresl.errors.EvaluationErrorException;
import gov.ca.water.wresl.errors.SyntaxErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

public class testParser {
    private static final Logger logger = LoggerFactory.getLogger(testParser.class);


    public static void main(String[] args) {
        Path mainWRESL = Path.of(args[0]).normalize();
        logger.atInfo().setMessage("mainWresl={}").addArgument(mainWRESL).log();
        Study study = new Study("TEST", mainWRESL);
        try {
            study.compile();
        }
        catch (SyntaxErrorException e) {
            System.err.println("WRESL+ syntax error(s) encountered in file "+e.getSourceFile());
            List<String> syntaxErrors = e.getErrorMessages();
            for (int i=0; i<syntaxErrors.size(); i++) {
                System.err.println(syntaxErrors.get(i));
            }
        }
        catch (EvaluationErrorException e) {
            System.err.println("Evaluation error: " + e.getErrorMessage());
            System.err.println("                  " +"File " + e.getSourceFile() + ", line " + e.getLine());
        }
    }
}

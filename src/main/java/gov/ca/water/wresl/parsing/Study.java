package gov.ca.water.wresl.parsing;

import gov.ca.water.utilities.ParallelVars;
import gov.ca.water.wresl.domain.StudyDataSet;
import org.antlr.v4.runtime.tree.ParseTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;

public class Study {
    private static final Logger logger = LoggerFactory.getLogger(Study.class);

    private StudyDataSet sds;
    private Evaluator evaluator;


    // Parse WRIMS data from WRESL files
    public void parse(String mainFile, int startDay, int startMon, int startYear) {
        // Track time
        long start = System.currentTimeMillis();

        // Path for main WRESL file
        Path mainFilePath = Path.of(mainFile).normalize();

        // PASS 1
        // Collect all the trees that are included
        Map<Path, WRESLFile> treesByFile = collectTrees(mainFilePath);

        // PASS 2
        // Retrieve parse tree for the main file as the starting tree
        ParseTree studyTree = treesByFile.get(mainFilePath).getParseTree();

        // Parse WRESL input into WRIMS objects
        this.sds = new StudyDataSet();
        this.evaluator = new Evaluator();
        this.evaluator.setStudyDataSet(this.sds);
        Antlr_To_WRIMS parse = new Antlr_To_WRIMS(mainFilePath, treesByFile, this.sds, this.evaluator);
        VisitorResult dummy = parse.visit(studyTree);

        // Store study name and WRESl file details
        this.sds.fromWresl = mainFilePath.toString();
        this.sds.line = 1;

        // Set study start date
        ParallelVars startDate = new ParallelVars(startDay, startMon, startYear);
        this.sds.setStudyStartDate(startDate);

        // Report total compile time
        long end = System.currentTimeMillis();
        float durationTotal= (float) (end - start) / 1_000L;
        logger.atInfo()
                .setMessage("total parse + compile time: {} seconds")
                .addArgument(durationTotal)
                .log();
        //    logger.atInfo().setMessage("{}").addArgument(containers.sequences.get("CYCLE01")).log();

    }


    // Retrieve study dataset
    public StudyDataSet getStudyDataSet() {
        return this.sds;
    }


    // Process a model
    public boolean processModel(int modelIndex, int currentDay, int currentMonth, int currentYear, int nThreads, boolean showRunTimeMessage) {
        return this.evaluator.processModel(modelIndex, currentDay, currentMonth, currentYear, nThreads, showRunTimeMessage);
    }


    // Process aliasses
    public void processAliases(int modelIndex, boolean showRunTimeMessage) {
        this.evaluator.processAliases(modelIndex, showRunTimeMessage);
    }

    // Collect study WRESL files, compile their parse trees as well as their parent and child files
    private static Map<Path, WRESLFile> collectTrees(Path entryFile) {
        // Begin by parsing all the files into separate trees
        long start = System.currentTimeMillis();
        WRESLFileCollector studyFileCollector = new WRESLFileCollector();
        studyFileCollector.collect(entryFile);

        // Otherwise, report parse time
        long end = System.currentTimeMillis();
        float duration = (float) (end - start) / 1_000L;
        logger.atInfo()
                .setMessage("{} files parsed in {} seconds")
                .addArgument(studyFileCollector.getFiles().size())
                .addArgument(duration)
                .log();

        // Return files
        return studyFileCollector.getFiles();
    }



}

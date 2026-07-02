package gov.ca.water.wresl.parsing;

import gov.ca.water.wresl.domain.StudyDataSet;
import org.antlr.v4.runtime.tree.ParseTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;

public class Study {
    private static final Logger logger = LoggerFactory.getLogger(Study.class);


    // ------------------------------------------------------------
    // --- COMPILE WRIMS DATA FROM WRESL FILES
    // ------------------------------------------------------------
    public StudyDataSet compile(String mainFile) {
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
        Antlr_To_WRIMS parse = new Antlr_To_WRIMS(mainFilePath, treesByFile);
        VisitorResult study = parse.visit(studyTree);
        StudyDataSet sds = (StudyDataSet) study.data().get(0);

        // Store study name and WRESl file details
        sds.fromWresl = mainFilePath.toString();
        sds.line = 1;

        // Report total compile time
        long end = System.currentTimeMillis();
        float durationTotal= (float) (end - start) / 1_000L;
        logger.atInfo()
                .setMessage("total parse + compile time: {} seconds")
                .addArgument(durationTotal)
                .log();
        //    logger.atInfo().setMessage("{}").addArgument(containers.sequences.get("CYCLE01")).log();

        return sds;
    }


    // ------------------------------------------------------------
    // --- COLLECT STUDY WRESL FILES, COMPILE THEIR PARSE TREES AS WELL AS THEIR PARENT AND CHILD FILES
    // ------------------------------------------------------------
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

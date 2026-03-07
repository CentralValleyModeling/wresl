package gov.ca.water.wresl.compile;

import gov.ca.water.wresl.domain.StudyDataSet;
import org.antlr.v4.runtime.tree.ParseTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;

public class Study {
    private static final Logger logger = LoggerFactory.getLogger(Study.class);
    private String name;
    private Path mainFileFolder;
    private Path mainFilePath;

    // Constructor
    public Study(String name, Path mainFilePath) {
        this.name = name;
        this.mainFilePath = mainFilePath;
    }

    // Collect study WRESL files, compile their parse trees as well as their parent and child files
    private static Map<Path, WRESLFile> collectTrees(Path entryFile) {
        // begin by parsing all the files into separate trees
        long start = System.currentTimeMillis();
        WRESLFileCollector studyFileCollector = new WRESLFileCollector();
        studyFileCollector.collect(entryFile);
        long end = System.currentTimeMillis();
        float duration = (float) (end - start) / 1_000L;
        logger.atInfo()
                .setMessage("{} files parsed in {} seconds")
                .addArgument(studyFileCollector.getFiles().size())
                .addArgument(duration)
                .log();
        return studyFileCollector.getFiles();
    }


    public void compile() {
        // Track time
        long start = System.currentTimeMillis();

        // PASS 1
        // Collect all the trees that are included
        Map<Path, WRESLFile> treesByFile = collectTrees(this.mainFilePath);

        // PASS 2
        // Retrieve parse tree for the main file as the starting tree
        ParseTree studyTree = treesByFile.get(this.mainFilePath).getParseTree();

        // Parse WRESL input into WRIMS objects
        Antlr_To_WRIMS parse = new Antlr_To_WRIMS(this.mainFilePath, treesByFile);
        VisitorResult study = parse.visit(studyTree);
        StudyDataSet sds = (StudyDataSet)study.data();

        // next, find the sequences, models, groups, and initial constructs
        //Containers containers = compileTreesToContainers(treesByFile);
        // next combine the trees, organized by each sequence
        // TODO
        // next, compile each sequence into package objects
        // TODO
        // Report total compile time
        long end = System.currentTimeMillis();
        float durationTotal= (float) (end - start) / 1_000L;
        logger.atInfo()
                .setMessage("total parse + compile time: {} seconds")
                .addArgument(durationTotal)
                .log();
    //    logger.atInfo().setMessage("{}").addArgument(containers.sequences.get("CYCLE01")).log();
    }
}

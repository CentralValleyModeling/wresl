package gov.ca.water.wresl.containers;

import gov.ca.water.wresl.compile.*;
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

    private static Containers compileTreesToContainers(Map<Path, WRESLFile> trees) {
        long start = System.currentTimeMillis();
//        CollectContainersListener containerCollector = new CollectContainersListener();
//        Containers containers = containerCollector.collectContainersFromTrees(trees.values());
        long end = System.currentTimeMillis();
        float durationCompile= (float) (end - start) / 1_000L;
        logger.atDebug()
                .setMessage("containers collected in {} seconds")
                .addArgument(durationCompile)
                .log();
//        return containers;
        return null;
    }

    private void compile() {
        // track the time
        long start = System.currentTimeMillis();

        // First, collect all the trees that are included
        Map<Path, WRESLFile> treesByFile = collectTrees(this.mainFilePath);

        // Retrieve parse tree for the main file as the starting tree
        ParseTree studyTree = treesByFile.get(this.mainFilePath).getParseTree();

        // Parse WRESL input into WRIMS objects
        Antlr_To_WRIMS parse = new Antlr_To_WRIMS(this.mainFilePath, treesByFile);
        try {
            VisitorResult study = parse.visit(studyTree);
            StudyDataSet sds = (StudyDataSet)study.data();
        } catch (Exception e) {
            System.out.println(e);
        }

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

    public static void main(String[] args) {
        Path mainWRESL = Path.of(args[0]).normalize();
        logger.atInfo().setMessage("mainWresl={}").addArgument(mainWRESL).log();
        Study study = new Study("TEST", mainWRESL);
        study.compile();
    }
}

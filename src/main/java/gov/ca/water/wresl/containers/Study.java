package gov.ca.water.wresl.containers;

import gov.ca.water.wresl.compile.CollectContainersListener;
import gov.ca.water.wresl.compile.CollectedContainers;
import gov.ca.water.wresl.compile.ExpandIncludesListener;
import gov.ca.water.wresl.configuration.StudyConfiguration;
import gov.ca.water.wresl.grammar.wreslParser.StartContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;

public class Study {
    private static final Logger logger = LoggerFactory.getLogger(Study.class);
    public String name;
    public StudyConfiguration config;

    public Study(String name, Path svPath, Path dvPath, Path initPath, Path mainFile) {
        StudyConfiguration cfg = new StudyConfiguration(svPath, dvPath, initPath, mainFile);
        new Study(name, cfg);
    }

    public Study(String name, StudyConfiguration studyConfiguration) {
        this.name = name;
        this.config = studyConfiguration;
    }

    private static Map<Path, StartContext> collectTrees(Path entryFile) {
        // begin by parsing all the files into separate trees
        long start = System.currentTimeMillis();
        ExpandIncludesListener expandIncludes = new ExpandIncludesListener();
        expandIncludes.startVisitingFromFile(entryFile);  // find and parse files
        long end = System.currentTimeMillis();
        float duration = (float) (end - start) / 1_000L;
        logger.atInfo()
                .setMessage("{} files parsed in {} seconds")
                .addArgument(expandIncludes.getFileSet().size())
                .addArgument(duration)
                .log();
        return expandIncludes.includedTrees;
    }

    private static CollectedContainers compileTreesToContainers(Map<Path, StartContext> trees) {
        long start = System.currentTimeMillis();
        CollectContainersListener containerCollector = new CollectContainersListener();
        CollectedContainers containers = containerCollector.collectContainersFromTrees(trees.values());
        long end = System.currentTimeMillis();
        float durationCompile= (float) (end - start) / 1_000L;
        logger.atDebug()
                .setMessage("containers collected in {} seconds")
                .addArgument(durationCompile)
                .log();
        return containers;
    }

    private void compile() {
        // track the time
        long start = System.currentTimeMillis();
        // first, collect all the trees that are included
        Map<Path, StartContext> treesByFile = collectTrees(this.config.mainFile);
        // next, find the sequences, models, groups, and initial constructs
        CollectedContainers containers = compileTreesToContainers(treesByFile);
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
        logger.atInfo().setMessage("{}").addArgument(containers.sequences.get("CYCLE01")).log();
    }

    public static void main(String[] args) {
        Path mainWresl = Path.of(args[0]).normalize();
        Path svPath = Path.of("SV-PLACEHOLDER");
        Path dvPath = Path.of("DV-PLACEHOLDER");
        logger.atInfo().setMessage("mainWresl={}").addArgument(mainWresl).log();
        StudyConfiguration cfg = new StudyConfiguration(svPath, dvPath, svPath, mainWresl);
        Study study = new Study("TEST", cfg);
        study.compile();
    }
}

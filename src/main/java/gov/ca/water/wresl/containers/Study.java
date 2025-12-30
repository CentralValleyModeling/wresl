package gov.ca.water.wresl.containers;

import gov.ca.water.wresl.configuration.StudyConfiguration;
import gov.ca.water.wresl.domain.Group;
import gov.ca.water.wresl.domain.Model;
import gov.ca.water.wresl.domain.Sequence;
import gov.ca.water.wresl.visitors.ExpandIncludesVisitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Study {
    private static final Logger logger = LoggerFactory.getLogger(Study.class);
    public String name;
    public StudyConfiguration config;
    public Set<Path> paths;
    public Map<String, Model> models;
    public Map<String, Sequence> sequences;
    public Map<String, Group> groups;

    public Study(String name, Path svPath, Path dvPath, Path initPath, Path mainFile) {
        StudyConfiguration cfg = new StudyConfiguration(svPath, dvPath, initPath, mainFile);
        new Study(name, cfg);
    }

    public Study(String name, StudyConfiguration studyConfiguration) {
        this.name = name;
        this.config = studyConfiguration;
        this.paths = new HashSet<>();
        this.models = new HashMap<>();
        this.sequences = new HashMap<>();
        this.groups = new HashMap<>();
    }

    private ExpandIncludesVisitor loadStudyFiles() {
        ExpandIncludesVisitor visitor = new ExpandIncludesVisitor(this.config.mainFile.getParent()); // assume the root is the same location as the main file
        long start = System.currentTimeMillis();
        visitor.startVisitingFromFile(this.config.mainFile);
        long end = System.currentTimeMillis();
        float duration = (float) (end - start) / 1_000L;
        logger.atInfo()
                .setMessage("{} files parsed in {} seconds")
                .addArgument(visitor.getFileSet().size())
                .addArgument(duration)
                .log();
        return visitor;
    }

    public String toString(){
        return String.format("%s(name=\"%s\")", this.getClass().getSimpleName(), this.name);
    }

    public static void main(String[] args) {
        Path mainWresl = Path.of(args[0]).normalize();
        Path svPath = Path.of("SV-PLACEHOLDER");
        Path dvPath = Path.of("DV-PLACEHOLDER");
        logger.atInfo().setMessage("mainWresl={}").addArgument(mainWresl).log();
        StudyConfiguration cfg = new StudyConfiguration(svPath, dvPath, svPath, mainWresl);
        Study study = new Study("TEST", cfg);
        ExpandIncludesVisitor visitor = study.loadStudyFiles();
    }
}

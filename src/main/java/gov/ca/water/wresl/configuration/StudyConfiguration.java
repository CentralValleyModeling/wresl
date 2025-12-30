package gov.ca.water.wresl.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;

public class StudyConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(StudyConfiguration.class);
    public Path svPath;
    public Path initPath;
    public Path dvPath;
    private Map<String, Path> externalPaths;
    public Path mainFile;

    public StudyConfiguration(Path svPath, Path dvPath, Path initPath, Path mainFile) {
        this.svPath = svPath;
        this.dvPath = dvPath;
        this.initPath = initPath;
        this.mainFile = mainFile;
    }

    public String toString() {
        return String.format(
                "%s(svPath=\"%s\", dvPath=\"%s\", initPath=\"%s\")",
                this.getClass().getSimpleName(), this.svPath.toString(), this.dvPath.toString(), this.initPath.toString()
        );
    }
}

package gov.ca.water.wresl.compile;

import gov.ca.water.wresl.errors.SyntaxErrorException;
import gov.ca.water.wresl.errors.WRESLErrorListener;
import gov.ca.water.wresl.grammar.wreslLexer;
import gov.ca.water.wresl.grammar.wreslParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class WRESLFileCollector {
    private static final Logger logger = LoggerFactory.getLogger(WRESLFileCollector.class);

    private final Map<Path, WRESLFile> mapStudyFiles;

    // ------------------------------------------------------------
    // --- CONSTRUCTOR
    // ------------------------------------------------------------
    public WRESLFileCollector() {
        this.mapStudyFiles = new HashMap<>();
    }


    // ------------------------------------------------------------
    // --- COLLECT STUDY WRESL FILES AND THEIR TREES
    // ------------------------------------------------------------
    public void collect(Path mainFile) throws SyntaxErrorException {
        // Variables
        Set<Path> processedFiles = new HashSet<>();
        Queue<Path> filesToBeProcessed = new LinkedList<>();

        // Add lower case main file to the queue to be processed
        filesToBeProcessed.add(Path.of(mainFile.toString().toLowerCase()));

        while (!filesToBeProcessed.isEmpty()) {
            Path currentFile = filesToBeProcessed.poll();

            // Skip if we've already parsed this file
            if (processedFiles.contains(currentFile)) continue;

            // Skip if file doesn't exist
            if (!currentFile.toFile().exists()) {continue;}

            // Find the path to starting point where parent file point to include file lives
            Path startingPath = currentFile.getParent();

            // Inform user the file is processed
            // Logger is refusing to show naything on the console; using System.out.Println for now
            System.out.println("Processing " + currentFile);
            // Delete above line when logger decides to cooporate
            logger.atInfo()
                    .setMessage("Processing " + currentFile)
                    .log();

            try {
                // ANTLR Parsing Pipeline
                CharStream input = CharStreams.fromFileName(currentFile.toString());
                wreslLexer lexer = new wreslLexer(input);
                CommonTokenStream tokens = new CommonTokenStream(lexer);
                wreslParser parser = new wreslParser(tokens);

                // Replace default error generator with ours
                WRESLErrorListener customErrorListener = new WRESLErrorListener();
                parser.removeErrorListeners();
                parser.addErrorListener(customErrorListener);

                // Generate parse trees for the main file and include files from different roots
                ParseTree tree;
                if (currentFile.toString().equals(mainFile.toString())) {
                    tree = parser.study();
                } else {
                    tree = parser.includeStart();
                }

                // Report syntax errors if any
                if (customErrorListener.hasSyntaxError()) {
                    throw new SyntaxErrorException(currentFile,customErrorListener.getErrorMessages());
                }

                // Create new file data
                WRESLFile wreslFile = new WRESLFile(tree);

                // Run the Visitor on the current file
                WRESLFileFinder includeFileFinder = new WRESLFileFinder();
                includeFileFinder.visit(tree);

                // Add newly found files to the queue and as children to the file we are working with
                for (String childFilename : includeFileFinder.getListFoundFiles()) {
                    File childFile = new File(startingPath.toString(), childFilename);
                    Path childFilePath = Path.of(childFile.getCanonicalPath().toLowerCase()); //startingPath.resolve(childFilename);
                    wreslFile.addChild(childFilePath);
                    if (!processedFiles.contains(childFilename)) {
                        filesToBeProcessed.add(childFilePath);
                    }
                }

                // Mark this file as done
                processedFiles.add(currentFile);

                // Add the WRESL file data to the list
                this.mapStudyFiles.put(currentFile,wreslFile);

            } catch (IOException e) {
                // Do nothing at this point since this error is catostropic at this point;
                // The file may be not needed becuase of teh results of an IF statement
            }
        }

        // Compile parent files for each WRESL file
        this.mapStudyFiles.forEach((key, value) -> {
            Set<Path> childrenFiles = value.getChildrenFiles();
            for (Path childFile : childrenFiles) {
                WRESLFile childWRESLFile = this.mapStudyFiles.get(childFile);
                if (childWRESLFile == null) {continue;}
                childWRESLFile.addParent(key);
                this.mapStudyFiles.put(childFile, childWRESLFile);
            }
        });
    }


    // ------------------------------------------------------------
    // --- RETRIEVE PROCESSED FILES, THEIR PARSE TREES, PARENTS AND CHILDREN
    // ------------------------------------------------------------
    public Map<Path,WRESLFile> getFiles() {
        return this.mapStudyFiles;
    }

}

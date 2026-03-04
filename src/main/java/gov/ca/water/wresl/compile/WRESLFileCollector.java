package gov.ca.water.wresl.compile;

import gov.ca.water.wresl.grammar.wreslLexer;
import gov.ca.water.wresl.grammar.wreslParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class WRESLFileCollector {
    private final Map<Path, WRESLFile> mapStudyFiles;


    // Constructor
    public WRESLFileCollector() {
        this.mapStudyFiles = new HashMap<>();
    }


    // Collect study WRESL files and their trees
    public void collect(Path mainFile) {
        // Variables
        Set<Path> processedFiles = new HashSet<>();
        Queue<Path> filesToBeProcessed = new LinkedList<>();

        // Add the main file to the queue to be processed
        filesToBeProcessed.add(mainFile);

        while (!filesToBeProcessed.isEmpty()) {
            Path currentFile = filesToBeProcessed.poll();

            // Skip if we've already parsed this file
            if (processedFiles.contains(currentFile)) continue;

            System.out.println("Processing "+currentFile);

            // Find the path to starting point where parent file point to include file lives
            Path startingPath = currentFile.getParent();

            try {
                // ANTLR Parsing Pipeline
                CharStream input = CharStreams.fromFileName(currentFile.toString());
                wreslLexer lexer = new wreslLexer(input);
                CommonTokenStream tokens = new CommonTokenStream(lexer);
                wreslParser parser = new wreslParser(tokens);
                // Generate parse trees for the main file and include files from different roots
                ParseTree tree;
                if (currentFile == mainFile) {
                    tree = parser.study();
                } else {
                    tree = parser.includeStart();
                }
                // Create new file data
                WRESLFile wreslFile = new WRESLFile(tree);

                // Run the Visitor on the current file
                WRESLFileFinder includeFileFinder = new WRESLFileFinder();
                includeFileFinder.visit(tree);

                // Add newly found files to the queue and as children to the file we are working with
                for (String childFile : includeFileFinder.getListFoundFiles()) {
                    Path childFilePath = startingPath.resolve(childFile);
                    wreslFile.addChild(childFilePath);
                    if (!processedFiles.contains(childFile)) {
                        filesToBeProcessed.add(childFilePath);
                    }
                }

                // Mark this file as done
                processedFiles.add(currentFile);

                // Add the WRESL file data to the list
                this.mapStudyFiles.put(currentFile,wreslFile);

            } catch (IOException e) {
                System.err.println("Skipping file (not found): " + currentFile);
            }
        }

        // Compile parent files for each WRESL file
        this.mapStudyFiles.forEach((key, value) -> {
            Set<Path> childrenFiles = value.getChildrenFiles();
            for (Path childFile : childrenFiles) {
                WRESLFile childWRESLFile = this.mapStudyFiles.get(childFile);
                childWRESLFile.addParent(key);
                this.mapStudyFiles.put(childFile, childWRESLFile);
            }
        });
    }
    
    // Retrieve processed files, their parse trees, parents and children
    public Map<Path,WRESLFile> getFiles() {
        return this.mapStudyFiles;
    }
}

package gov.ca.water.wresl.compile;

import gov.ca.water.wresl.domain.WRIMSComponent;
import org.antlr.v4.runtime.tree.ParseTree;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class WRESLFile extends WRIMSComponent {
    private ParseTree parseTree;
    private Set<Path> parentFiles;
    private Set<Path> childrenFiles;

    // Constructor
    public WRESLFile(ParseTree tree) {
        this.parseTree = tree;
        this.parentFiles = new HashSet<>();
        this.childrenFiles = new HashSet<>();
    }

    // Add a new child file path
    public void addChild(Path childFilePath) {
        this.childrenFiles.add(childFilePath);
    }

    // Add a new parent file path
    public void addParent(Path parentFilePath) {
        this.parentFiles.add(parentFilePath);
    }

    // Retrieve list of child files as a Set
    public Set<Path> getChildrenFiles() {
        return this.childrenFiles;
    }

    // Retrieve list of parent files as a Set
    public Set<Path> getParentFiles() {
        return this.parentFiles;
    }

    // Retrieve parse tree
    public ParseTree getParseTree() {
        return this.parseTree;
    }

}

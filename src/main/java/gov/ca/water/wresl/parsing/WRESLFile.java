package gov.ca.water.wresl.parsing;

import org.antlr.v4.runtime.tree.ParseTree;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

// Package-private class
class WRESLFile  {
    private ParseTree parseTree;
    private Set<Path> parentFiles;
    private Set<Path> childFiles;

    // Constructor
    public WRESLFile(ParseTree tree) {
        this.parseTree = tree;
        this.parentFiles = new HashSet<>();
        this.childFiles = new HashSet<>();
    }

    // Add a new child file path
    public void addChild(Path childFilePath) {
        this.childFiles.add(childFilePath);
    }

    // Add a new parent file path
    public void addParent(Path parentFilePath) {
        this.parentFiles.add(parentFilePath);
    }

    // Retrieve list of child files as a Set
    public Set<Path> getChildFiles() {
        return this.childFiles;
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

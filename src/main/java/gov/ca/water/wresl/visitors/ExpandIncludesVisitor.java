package gov.ca.water.wresl.visitors;

import gov.ca.water.wresl.errors.FileSyntaxException;
import gov.ca.water.wresl.errors.SyntaxException;
import gov.ca.water.wresl.errors.WreslErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.CharStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.ca.water.wresl.grammar.wreslBaseVisitor;
import gov.ca.water.wresl.grammar.wreslParser;
import gov.ca.water.wresl.grammar.wreslLexer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class ExpandIncludesVisitor extends wreslBaseVisitor<ParseTree> {
    private static final Logger logger = LoggerFactory.getLogger(ExpandIncludesVisitor.class);

    /**
     * The starting file for this visitor, typically the "main" file for a study.
     * This is used to determine the location on disk for relative paths.
     */
    public final Path root;

    /**
     * A cache of included files, used to avoid reparsing files already seen.
     */
    private final Map<Path, ParseTree> includedFileCache;

    /**
     * A set of files that are currently being parsed (used to avoid infinite parsing in circular includes).
     */
    private final Set<Path> allExpandingPaths;

    /**
     * A map of java.nio.file.Path objects keyed by their String representation.
     * This saves time on resolving relative paths on disk.
     */
    private final Map<String, Path> filePaths;


    public ExpandIncludesVisitor(Path root) {
        super();
        this.root = root.normalize();
        this.includedFileCache = new HashMap<>();
        this.allExpandingPaths = new HashSet<>();
        this.filePaths = new HashMap<>();
    }

    public void setNewExpansionTarget(Path target) {
        this.allExpandingPaths.add(target);
    }

    public void resolveExpansion(Path target, ParseTree tree) {
        this.includedFileCache.put(target, tree);
        this.allExpandingPaths.remove(target);
    }

    @Override
    public ParseTree visitIncludeFile(wreslParser.IncludeFileContext ctx) {
        String fileName = strip(ctx.specificationString());
        Path target = getCanonicalRelativePath(fileName);
        ParseTree tree;
        if (this.includedFileCache.containsKey(target)) {  // check to see if we've already parsed this file
            logger.atDebug()
                    .setMessage("included (again): {}")
                    .addArgument(target)
                    .log();
            tree = this.includedFileCache.get(target);
        } else if (this.allExpandingPaths.contains(target)) {  //  check for circular
            logger.atWarn()
                    .setMessage("included (circular): {} ")
                    .addArgument(target)
                    .log();
            // Do nothing, we are currently expanding this one
            tree = ctx;
        } else {  // otherwise, go ahead and parse the tree
            logger.atInfo()
                    .setMessage("included: {}")
                    .addArgument(target)
                    .log();
            wreslParser parser = getParserForFile(target);
            this.setNewExpansionTarget(target);
            try {
                tree = parser.start();
            } catch (SyntaxException e) {
                throw new FileSyntaxException(target, e);
            }
            this.resolveExpansion(target, tree);
        }
        return tree;
    }

    @Override
    public ParseTree visitGroup(wreslParser.GroupContext ctx) {
        logger.atTrace()
                .setMessage("visiting group definition: {}")
                .addArgument(ctx.OBJECT_NAME())
                .log();
        return visitChildren(ctx);
    }

    private String strip(wreslParser.SpecificationStringContext ctx) {
        String pattern = (ctx.DOUBLE_QUOTE_STRING() == null) ? "^'|'$" : "^\"|\"$";
        return ctx.getText().replaceAll(pattern, "");
    }

    public wreslParser getParserForFile(Path source) {
        CharStream stream;
        try {
            stream = CharStreams.fromFileName(source.toString());
        } catch (IOException e) {
            logger.atError()
                    .setMessage("IOException while reading main WRESL file: {}")
                    .addArgument(source)
                    .setCause(e)
                    .log();
            throw new RuntimeException("IOException while reading main WRESL file.", e);
        }
        wreslLexer lexer = new wreslLexer(stream);
        wreslParser parser = new wreslParser(new CommonTokenStream(lexer));
        WreslErrorListener listener = new WreslErrorListener();
        parser.addErrorListener(listener);
        return parser;
    }

    public void startVisitingFromFile(Path mainFile) {
        logger.atInfo()
                .setMessage("starting parsing from file: {}")
                .addArgument(mainFile)
                .log();
        Path mainFileFull = getCanonicalRelativePath(mainFile);
        wreslParser parser = getParserForFile(mainFileFull);
        ParseTree tree = parser.start();
        this.includedFileCache.put(mainFile, tree);
        this.visit(tree);

    }

    public Path getCanonicalRelativePath(String source) {
        if (this.filePaths.containsKey(source)) {
            return this.filePaths.get(source);
        } else {
            Path result = getCanonicalRelativePath(Path.of(source));
            this.filePaths.put(source, result); // cache
            return result;
        }
    }

    public Path getCanonicalRelativePath(Path source) {
        try {
            String pathStr = this.root.resolve(source).toFile().getCanonicalPath();
            return Path.of(pathStr);
        } catch (IOException e) {
            String msg = String.format("Couldn't find path on disk: %s", source);
            logger.atError().setMessage(msg).setCause(e).log();
            throw new RuntimeException(msg, e);
        }
    }

    public Set<Path> getFileSet() {
        return this.includedFileCache.keySet();
    }

    public ParseTree getTree(String sourceString) throws IllegalArgumentException {
        return getTree(Path.of(sourceString));
    }

    public ParseTree getTree(Path source) throws IllegalArgumentException {
        if (this.includedFileCache.containsKey(source)) {
            return this.includedFileCache.get(source);
        }
        Path resolvedSource = getCanonicalRelativePath(source);
        if (this.includedFileCache.containsKey(resolvedSource)) {
            return this.includedFileCache.get(resolvedSource);
        } else {
            String msg = String.format("Could not find \"%s\" (or \"%s\") in file cache", source, resolvedSource);
            logger.atError().setMessage(msg).log();
            throw new IllegalArgumentException(msg);
        }

    }

}

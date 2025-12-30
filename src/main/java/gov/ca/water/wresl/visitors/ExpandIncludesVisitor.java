package gov.ca.water.wresl.visitors;

import gov.ca.water.wresl.errors.FileSyntaxException;
import gov.ca.water.wresl.errors.SyntaxException;
import gov.ca.water.wresl.errors.WreslErrorListener;
import gov.ca.water.wresl.grammar.wreslBaseVisitor;
import gov.ca.water.wresl.grammar.wreslLexer;
import gov.ca.water.wresl.grammar.wreslParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class ExpandIncludesVisitor extends wreslBaseVisitor<ParseTree> {
    private static final Logger logger = LoggerFactory.getLogger(ExpandIncludesVisitor.class);
    /**
     * An array of the current files being expanded, where the last file in the list is the current target.
     */
    private final ArrayList<Path> currentExpansion;

    /**
     * A cache of included files, used to avoid reparsing files already seen.
     */
    private final Map<Path, ParseTree> includedTrees;

    /**
     * A map of java.nio.file.Path objects keyed by their String representation.
     * This saves time on resolving relative paths on disk.
     */
    private final Map<String, Path> filePathCache;

    /**
     * ExpandIncludesVisitor is used to traverse files looking for include statements, and will traverse those included
     * files in a depth first search.
     */
    public ExpandIncludesVisitor() {
        super();
        // optimized for larger models, like CalSim3 (although the performance impact is <1 sec)
        this.currentExpansion = new ArrayList<>(10);  // CalSim3 max is    5 on 2025-12-30
        this.includedTrees = new HashMap<>(1500);     // CalSim3 max is 1009 on 2025-12-30
        this.filePathCache = new HashMap<>(1500);     // CalSim3 max is 1038 on 2025-12-30
    }

    /**
     * Read the main file, and perform a depth first search of the files included by other files.
     * @param mainFile The file to begin reading from.
     * @return All the parsed trees, keyed by the Path of the file.
     */
    public Map<Path, ParseTree> startVisitingFromFile(Path mainFile) {
        logger.atInfo()
                .setMessage("starting parsing from file: {}")
                .addArgument(mainFile)
                .log();
        ParseTree tree = this.getTreeForFile(mainFile);
        this.includedTrees.put(mainFile, tree);  // track the main file
        return this.includedTrees;
    }

    public ParseTree getTreeForFile(Path target) {
        this.currentExpansion.addLast(target); // track the new file being traversed
        wreslParser parser = getParserForFile(target);
        ParseTree tree;
        try {
            tree = parser.start();
            this.visit(tree);  // go one level deeper
        } catch (SyntaxException e) {
            throw new FileSyntaxException(target, e);
        }
        this.currentExpansion.removeLast(); // no longer resolving this target
        return tree;
    }

    public Set<Path> getFileSet() {
        return this.includedTrees.keySet();
    }

    public ParseTree getTree(String sourceString) throws IllegalArgumentException {
        return getTree(Path.of(sourceString));
    }

    /**
     * Nearly the default behavior of the parent class method, but this implementation modifies the state of the
     * currentExpansion, and includedTrees attributes of this subclass. This also triggers the depth first traversal
     * of other files.
     *
     * @param ctx The context of the node
     * @return The default behavior from visitChildren
     */
    @Override
    public ParseTree visitIncludeFile(wreslParser.IncludeFileContext ctx) {
        String fileName = strip(ctx.specificationString());
        Path target = getFullPathFromWreslReference(fileName);
        if (this.includedTrees.containsKey(target)) {
            // we've already finished parsing this file
            logger.atDebug().setMessage("included (again): {}").addArgument(target).log();
        } else if (this.currentExpansion.contains(target)) {
            // circular include, we aren't done parsing this
            logger.atWarn().setMessage("included (circular): {}").addArgument(target).log();
        } else {
            // new file, go ahead and parse the tree
            logger.atInfo().setMessage("included: {}").addArgument(target).log();
            ParseTree tree = this.getTreeForFile(target);
            this.includedTrees.put(target, tree);  // modify this object in place
        }
        return visitChildren(ctx);
    }

    private ParseTree getTree(Path source) throws IllegalArgumentException {
        if (this.includedTrees.containsKey(source)) {
            return this.includedTrees.get(source);
        }
        Path resolvedSource = getFullPathFromWreslReference(source);
        if (this.includedTrees.containsKey(resolvedSource)) {
            return this.includedTrees.get(resolvedSource);
        } else {
            String msg = String.format("Could not find \"%s\" (or \"%s\") in file cache", source, resolvedSource);
            logger.atError().setMessage(msg).log();
            throw new IllegalArgumentException(msg);
        }
    }

    private String strip(wreslParser.SpecificationStringContext ctx) {
        String pattern = (ctx.DOUBLE_QUOTE_STRING() == null) ? "^'|'$" : "^\"|\"$";
        return ctx.getText().replaceAll(pattern, "");
    }

    private wreslParser getParserForFile(Path source) {
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

    private Path getFullPathFromWreslReference(String source) {
        if (this.filePathCache.containsKey(source)) {
            return this.filePathCache.get(source);
        } else {
            Path result = getFullPathFromWreslReference(Path.of(source));
            this.filePathCache.put(source, result); // cache
            return result;
        }
    }

    private Path getFullPathFromWreslReference(Path source) {
        try {
            String pathStr;
            if (this.currentExpansion.isEmpty()) {
                pathStr = source.toAbsolutePath().toFile().getCanonicalPath();  // first path should be absolute, or relative to CWD
            } else {
                source = removeLeadingSlashFromPath(source);
                Path currentTarget = this.currentExpansion.getLast();
                pathStr = currentTarget.getParent().resolve(source).toFile().getCanonicalPath();
            }
            return Path.of(pathStr);
        } catch (IOException e) {
            String msg = String.format("Couldn't find path on disk: %s", source);
            logger.atError().setMessage(msg).setCause(e).log();
            throw new RuntimeException(msg, e);
        }
    }

    private static Path removeLeadingSlashFromPath(Path source) {
        String sourceStr = source.toString();
        if (sourceStr.startsWith("\\") || sourceStr.startsWith("/")) {
            sourceStr = sourceStr.substring(1);
            return Path.of(sourceStr);
        }
        return source;
    }

}

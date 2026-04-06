package gov.ca.water.wresl.parsing;

import gov.ca.water.wresl.grammar.wreslBaseVisitor;
import gov.ca.water.wresl.grammar.wreslParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class WRESLFileFinder extends wreslBaseVisitor<String> {
    private static final Logger logger = LoggerFactory.getLogger(WRESLFileFinder.class);
    private List<String> listFoundFiles;


    // Constructor
    public WRESLFileFinder() {
        this.listFoundFiles = new ArrayList<>();
    }

    @Override
    // Discover included files
    public String visitIncludeFile(wreslParser.IncludeFileContext ctx) {
        // Retrieve filename
        String sFileName = visit(ctx.specificationString()).toLowerCase();
        this.listFoundFiles.add(sFileName);
        return null;
    }

    // Strip filenames from single or double quotes
    @Override
    public String visitSpecificationString(wreslParser.SpecificationStringContext ctx) {
        String cleanFileName = ctx.getText().substring(0,ctx.getText().length()-1).substring(1);   // Remove first and last character
        return cleanFileName;
    }

    // Retrieve the list of include files discovered in a file
    public List<String> getListFoundFiles() {
        return listFoundFiles;
    }
}

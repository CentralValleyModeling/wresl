package gov.ca.water.wresl.compile;

import gov.ca.water.wresl.domain.*;
import gov.ca.water.wresl.grammar.wreslBaseVisitor;
import gov.ca.water.wresl.grammar.wreslParser;
import org.antlr.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Antlr_To_WRIMS extends wreslBaseVisitor<VisitorResult> {
    private final Path mainFilePath;
    private final Path startingFolder;
    private final Map<Path, WRESLFile> wreslInput;

    private String currentFile;

    // Containers
    private Map<String,Initial> initialData;
    private Map<String, Group> groups;
    private Map<String, ModelDataSet> models;
    private StudyDataSet sds;


    // Constructor
    public Antlr_To_WRIMS(Path mainFilePath, Map<Path, WRESLFile> wreslInput) {
        this.mainFilePath = mainFilePath;
        this.startingFolder = mainFilePath.getParent();
        this.wreslInput = wreslInput;

        this.initialData = new HashMap<>();
        this.groups = new HashMap<>();
        this.models = new HashMap<>();
        this.sds = new StudyDataSet();
    }


    // --------------------------------------
    // --- WRESL FILE PARSING ENTRY METHODS
    // --------------------------------------
    @Override
    // ROOT VISITOR FOR THE MAIN FILE
    public VisitorResult visitStudy(wreslParser.StudyContext ctx) {
        // Current WRESL file we are working with
        this.currentFile = this.mainFilePath.toString();

        // File related data
        this.sds.setAbsMainFilePath(this.mainFilePath.toString());

        // Loop through Study children nodes
        for (int i = 0; i <= ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);

            // EOF?
            if (child instanceof TerminalNode terminalNode) {
                if (terminalNode.getSymbol().getType() == org.antlr.v4.runtime.Token.EOF) continue;
            }

            // Visit child
            VisitorResult data = visit(child);
            if (data == null) continue;

            // Proceed based on child node type
        }

        return new VisitorResult(sds,null);
    }

    @Override
    // ROOT VISITOR FOR INCLUDE FILES
    public VisitorResult visitIncludeStart(wreslParser.IncludeStartContext ctx) {
        // Array to store multiple return data
        List<VisitorResult> compiledData = new ArrayList<>();

      // Loop through Include file children nodes
      for (int i = 0; i <= ctx.getChildCount()-1; i++) {
          ParseTree child = ctx.getChild(i);

          // EOF?
          if (child instanceof TerminalNode terminalNode) {
              if (terminalNode.getSymbol().getType() == Token.EOF) continue;
          }

          // Visit child
          VisitorResult data = visit(ctx.getChild(i));
          if (data == null) continue;

          // We got a single data
          if (data.children().size() == 0) {
              compiledData.add(data);
          // We got multiple data
          } else {
              compiledData.addAll(data.children());
          }
      }

      // Return compiled data
      return new VisitorResult(null, null, compiledData);
    }


    // --------------------------------------
    // --- VISITORS TO COMPILE CONTAINERS
    // --------------------------------------
    @Override
    // INITIAL
    public VisitorResult visitInitial(wreslParser.InitialContext ctx) {
        return null;
    }

    @Override
    // SEQUENCE
    public VisitorResult visitSequence(wreslParser.SequenceContext ctx) {
        return null;
    }

    @Override
    // MODEL
    public VisitorResult visitModel(wreslParser.ModelContext ctx) {
        ModelDataSet mds = new ModelDataSet();
        String modelName = ctx.OBJECT_NAME().getText();

        // Visit modelBody
        for (int i = 0; i <= ctx.modelBody().size(); i++) {
            VisitorResult returnedData = visit(ctx.modelBody(i));

            // Copy returned data into ModelDataSet
            List<VisitorResult> dataList;
            if (returnedData.children().size() == 0) {
                dataList = List.of(returnedData); }
            else {
                dataList = returnedData.children();
            }
            for (int j=0; j<=dataList.size()-1; j++) {
                WRIMSComponent data = dataList.get(j).data();
                if (data == null) continue;
                String name = dataList.get(j).name();
                switch (data) {
                    case Svar     svar     -> {
                        mds.svList.add(name);
                        mds.svMap.put(name,(Svar)data);
                    }
                    case Dvar     dvar     -> {}
                    case Goal     goal     -> {}
                    case Alias    alias    -> {}
                    case External external -> {}
                    default      -> System.out.println("error");
                }
            }
        }

        // Store the data for the model
        this.models.put(modelName, mds);

        // Return null; we have already collected all the data into "models" field
        return null;
    }


    @Override
    // GROUP
    public VisitorResult visitGroup(wreslParser.GroupContext ctx) {
        return null;
    }


    //----------------
    // INCLUDE
    //----------------
    @Override
    // includeFile
    public VisitorResult visitIncludeFile(wreslParser.IncludeFileContext ctx) {
        // Retrieve filename and create file path
        VisitorResult result = visit(ctx.specificationString());
        String includeFileName = null;
        if (result.data() instanceof WRIMS_String fileName) {
            includeFileName = fileName.getValue();
        }
        Path includeFilePath = this.startingFolder.resolve(includeFileName);

        // Set current file
        this.currentFile = includeFilePath.toString();

        // Parse tree corresponding to the file
        ParseTree includeFileTree = this.wreslInput.get(includeFilePath).getParseTree();

        // Visit include files's parse tree
        VisitorResult data = visit(includeFileTree);
        return data;
    }


    //----------------
    // SVAR
    //----------------
    @Override
    public VisitorResult visitSvar(wreslParser.SvarContext ctx) {
        // Collect data from svarBody; this will visit all possible Svar definitions
        return visit(ctx.svarBody());
    }

    @Override
    // svarCase
    public VisitorResult visitSvarCase(wreslParser.SvarCaseContext ctx) {
        Svar svar = new Svar();

        // Loop over case statements and case information
        for (int i=0; i<=ctx.getChildCount()-1; i++) {
            VisitorResult caseDataVisit = visit(ctx.getChild(i));
            WRIMS_CaseData caseData = (WRIMS_CaseData)caseDataVisit.data();
            svar.addCaseData(caseDataVisit.name().toLowerCase(), caseData.caseCondition, caseData.caseExpression);
        }

        // Walk up the tree to access parent (svar) data
        if (ctx.parent instanceof wreslParser.SvarContext svarCtx) {
            // WRESL file related data
            svar.setFileData(this.currentFile, svarCtx.OBJECT_NAME().getSymbol().getLine());

            // Is this a future array?
            if (svarCtx.arraySizeDefinition() != null) {
                svar.needVarFromEarlierCycle = true;
                if (visit(svarCtx.arraySizeDefinition()).data() instanceof WRIMS_String timeArraySize) {
                    svar.timeArraySize = timeArraySize.getValue();
                }
            }

            String name = svarCtx.OBJECT_NAME().getText();
            return new VisitorResult(svar, name.toLowerCase());
        }

        // This should not happen
        return null;
    }

    @Override
    // svarLookup
    public VisitorResult visitSvarLookup(wreslParser.SvarLookupContext ctx) {
        Svar svar = new Svar();

        // Set case condition

        // Walk up the tree to access parent (svar) data for svar name, filename and lline number in file
        if (ctx.parent instanceof wreslParser.SvarContext svarCtx) {
            // WRESL file related data
            svar.setFileData(this.currentFile, svarCtx.OBJECT_NAME().getSymbol().getLine());

            // Is this a future array?
            if (svarCtx.arraySizeDefinition() != null) {
                svar.needVarFromEarlierCycle = true;
                if (visit(svarCtx.arraySizeDefinition()).data() instanceof WRIMS_String timeArraySize) {
                    svar.timeArraySize = timeArraySize.getValue();
                }
            }

            String name = svarCtx.OBJECT_NAME().getText();
            return new VisitorResult(svar, name.toLowerCase());
        }

        // This should not happen
        return null;
    }

    @Override
    // svarExternal
    public VisitorResult visitSvarExternal(wreslParser.SvarExternalContext ctx) {
        return null;
    }

    @Override
    // svarSum
    public VisitorResult visitSvarSum(wreslParser.SvarSumContext ctx) {
        return null;
    }

    @Override
    // svarValue
    public VisitorResult visitSvarValue(wreslParser.SvarValueContext ctx) {
        Svar svar = new Svar();

        // Set case condition
        svar.addCaseData(Param.defaultCaseName, Param.always, ctx.expression().getText());

        // Walk up the tree to access parent (svar) data for svar name, filename and lline number in file
        if (ctx.parent instanceof wreslParser.SvarContext svarCtx) {
            // WRESL file related data
            svar.setFileData(this.currentFile, svarCtx.OBJECT_NAME().getSymbol().getLine());

            // Is this a future array?
            if (svarCtx.arraySizeDefinition() != null) {
                svar.needVarFromEarlierCycle = true;
                if (visit(svarCtx.arraySizeDefinition()).data() instanceof WRIMS_String timeArraySize) {
                    svar.timeArraySize = timeArraySize.getValue();
                }
            }

            String name = svarCtx.OBJECT_NAME().getText();
            return new VisitorResult(svar, name.toLowerCase());
        }

        // This should not happen
        return null;
    }

    @Override
    // svarTimeseries (returns a Timeseries object)
    public VisitorResult visitSvarTimeseries(wreslParser.SvarTimeseriesContext ctx) {
        Timeseries ts = new Timeseries();

        // Collect data

        // Walk up the tree to access parent (svar) data
        if (ctx.parent instanceof wreslParser.SvarContext svarCtx) {
            // WRESL file related data
            ts.fromWresl = this.currentFile;
            ts.line = svarCtx.OBJECT_NAME().getSymbol().getLine();

            String name = svarCtx.OBJECT_NAME().getText();
            return new VisitorResult(ts, name.toLowerCase());
        }

        // This should not happen
        return null;
    }


    //----------------
    // EXPRESSIONS
    //----------------
    @Override
    // comparisonExpression
    public VisitorResult visitComparisonExpression(wreslParser.ComparisonExpressionContext ctx) {
        WRIMS_String expression = new WRIMS_String(ctx.getText());
        return new VisitorResult(expression, null);
    }

    @Override
    // MultDivExpression
    public VisitorResult visitMultDivExpression(wreslParser.MultDivExpressionContext ctx) {
        // Store expression to be computed later during run
        WRIMS_String expression = new WRIMS_String(ctx.getText());
        return new VisitorResult(expression,null);
     }

    @Override
    public VisitorResult visitAddSubExpression(wreslParser.AddSubExpressionContext ctx) {
        // Store expression to be computed later during run
        WRIMS_String expression = new WRIMS_String(ctx.getText());
        return new VisitorResult(expression,null);
    }


    //----------------
    // MISCELLANEOUS
    //----------------
    @Override
    // caseStatement
    public VisitorResult visitCaseStatement(wreslParser.CaseStatementContext ctx) {
        // Case name
        String caseName = ctx.caseName().getText();

        // Case condition
        String caseCondition;
        if (ctx.caseCondition() != null) {
            caseCondition = ctx.caseCondition().getChild(1).getText(); }
        else {
            caseCondition = Param.always;
        }

        // Case expression
        String caseExpression;
        wreslParser.CaseBodyContext caseBody = ctx.caseBody();
        if (caseBody.caseViaValue() != null) {
            caseExpression = caseBody.caseViaValue().getChild(1).getText(); }
        else if (caseBody.goalCase() != null) {
            caseExpression = "needs implementation"; }
        else if (caseBody.caseViaSelect() != null) {
            caseExpression = "needs implementation"; }
        else if (caseBody.caseViaExpression() != null) {
            caseExpression = caseBody.caseViaExpression().expression().getText(); }
        else {
            caseExpression = null;
        }

        WRIMS_CaseData caseData = new WRIMS_CaseData(caseCondition, caseExpression);

        return new VisitorResult(caseData,caseName.toLowerCase());
    }

    @Override
    // arraySizeDefinition
    public VisitorResult visitArraySizeDefinition(wreslParser.ArraySizeDefinitionContext ctx) {
        WRIMS_String expr = new WRIMS_String(ctx.expression().getText());
        return new VisitorResult(expr,null);
    }

    @Override
    // specificationString
    public VisitorResult visitSpecificationString(wreslParser.SpecificationStringContext ctx) {
        WRIMS_String data = new WRIMS_String(ctx.getText().substring(0, ctx.getText().length() - 1).substring(1));   // Remove first and last character;
        return new VisitorResult(data, null);
    }
}
package gov.ca.water.wresl.compile;

import gov.ca.water.wresl.domain.*;
import gov.ca.water.wresl.grammar.wreslBaseVisitor;
import gov.ca.water.wresl.grammar.wreslParser;
import org.antlr.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;

public class Antlr_To_WRIMS extends wreslBaseVisitor<VisitorResult> {
    private static final Logger logger = LoggerFactory.getLogger(Antlr_To_WRIMS.class);

    private final Path mainFilePath;
    private final Path startingFolder;
    private final Map<Path, WRESLFile> wreslInput;

    private String currentFile;

    // Containers
    private Map<String,Svar> initialData;
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
        this.currentFile = this.mainFilePath.toString().toLowerCase();

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
    // INITIAL; Svars listed under INITIAL statement are stored as parameters in StudyDataSet
    public VisitorResult visitInitial(wreslParser.InitialContext ctx) {
        ArrayList<String> parameterList = new ArrayList<>();
        LinkedHashMap<String,Svar> parameterMap = new LinkedHashMap<>();

        // Loop through children; they should all be SVARs
        for (int i=0; i<=ctx.children.size()-1; i++) {
            // Skip anything that is not Svar definition
            if (!(ctx.getChild(i) instanceof wreslParser.SvarContext svarCtx)) {continue;}

            VisitorResult returnedData = visit(ctx.getChild(i));
            WRIMSComponent data = returnedData.data();
            String name = returnedData.name();

            // WRIMS component must be an SVAR; generate error otherwise
            switch (data) {
                case Svar svar -> {
                    parameterList.add(name);
                    parameterMap.put(name,svar);
                }
                default -> {
                    logger.atError()
                            .setMessage("INITIAL statement can only include SVARs!%n " +
                                        "Variable " + name + " in file " + data.fromWresl + " at line " + data.line)
                            .log();
                }
            }
        }

        // Store parameter data in permanently
        this.sds.setParameterList(parameterList);
        this.sds.setParameterMap(parameterMap);

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
        String modelName = ctx.OBJECT_NAME().getText().toLowerCase();

        // Visit modelBody
        for (int i = 0; i <= ctx.modelBody().size()-1; i++) {
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
                    case Svar svar -> {
                        mds.svList.add(name);
                        mds.svMap.put(name,(Svar)data);
                    }
                    case Dvar dvar -> {}
                    case Timeseries ts -> {
                        mds.tsList.add(name);
                        mds.tsMap.put(name,(Timeseries)data);
                    }
                    case Goal goal -> {}
                    case Alias alias    -> {}
                    case External external -> {
                        mds.exList.add(name);
                        mds.exMap.put(name,(External)data);
                    }
                    default -> System.out.println("error");
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
        String includeFileName = visitorResultToString(result);
        Path includeFilePath = this.startingFolder.resolve(includeFileName);

        // Set current file
        this.currentFile = includeFilePath.toString().toLowerCase();

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
            VisitorResult result = visit(ctx.getChild(i));
            WRIMS_CaseData caseData = (WRIMS_CaseData)result.data();
            svar.addCaseData(result.name().toLowerCase(), caseData.caseCondition, caseData.caseExpression);
        }

        // Walk up the tree to access parent (svar) data
        if (ctx.parent instanceof wreslParser.SvarContext svarCtx) {
            // WRESL file related data
            svar.setFileData(this.currentFile, svarCtx.OBJECT_NAME().getSymbol().getLine());

            // Is this a future array?
            if (svarCtx.arraySizeDefinition() != null) {
                svar.needVarFromEarlierCycle = true;
                VisitorResult result = visit(svarCtx.arraySizeDefinition());
                svar.timeArraySize = visitorResultToString(result);
            }

            String name = svarCtx.OBJECT_NAME().getText().toLowerCase();
            return new VisitorResult(svar, name);
        }

        // This should not happen
        return null;
    }

    @Override
    // svarLookup
    public VisitorResult visitSvarLookup(wreslParser.SvarLookupContext ctx) {
        Svar svar = new Svar();

        // Walk up the tree to access parent (svar) data for svar name, filename and line number in file
        if (ctx.parent instanceof wreslParser.SvarContext svarCtx) {
            // WRESL file related data
            svar.setFileData(this.currentFile, svarCtx.OBJECT_NAME().getSymbol().getLine());

            // Is this a future array?
            if (svarCtx.arraySizeDefinition() != null) {
                svar.needVarFromEarlierCycle = true;
                VisitorResult result = visit(svarCtx.arraySizeDefinition());
                svar.timeArraySize = visitorResultToString(result);
            }

            String name = svarCtx.OBJECT_NAME().getText().toLowerCase();
            return new VisitorResult(svar, name);
        }

        // This should not happen
        return null;
    }

    @Override
    // svarExternal; returns an External object
    public VisitorResult visitSvarExternal(wreslParser.SvarExternalContext ctx) {
        External ex = new External();

        // Walk up the tree to access parent (svar) data for svar name, filename and lline number in file
        if (ctx.parent instanceof wreslParser.SvarContext svarCtx) {
            // WRESL file related data
            ex.fromWresl = this.currentFile;
            ex.line =  svarCtx.OBJECT_NAME().getSymbol().getLine();
            wreslParser.ExternalTargetContext externalTargetCtx = ctx.externalTarget();
            if (externalTargetCtx.specificationString() != null) {
                ex.type = visitorResultToString(visit(externalTargetCtx.specificationString()));
            } else {
                ex.type = externalTargetCtx.unescapedTargetString().OBJECT_NAME(0).getText().toLowerCase();
                if (externalTargetCtx.unescapedTargetString().OBJECT_NAME(01) != null) {
                    ex.type = ex.type + "." + externalTargetCtx.unescapedTargetString().OBJECT_NAME(01);
                }
            }

            String name = svarCtx.OBJECT_NAME().getText().toLowerCase();
            return new VisitorResult(ex, name);

        }

        // This should be treated as error
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
        svar.addCaseData(Param.defaultCaseName, Param.always, ctx.expression().getText().toLowerCase());

        // Walk up the tree to access parent (svar) data for svar name, filename and lline number in file
        if (ctx.parent instanceof wreslParser.SvarContext svarCtx) {
            // WRESL file related data
            svar.setFileData(this.currentFile, svarCtx.OBJECT_NAME().getSymbol().getLine());

            // Is this a future array?
            if (svarCtx.arraySizeDefinition() != null) {
                svar.needVarFromEarlierCycle = true;
                VisitorResult result = visit(svarCtx.arraySizeDefinition());
                svar.timeArraySize = visitorResultToString(result);
            }

            String name = svarCtx.OBJECT_NAME().getText().toLowerCase();
            return new VisitorResult(svar, name);
        }

        // This should not happen
        return null;
    }


    //----------------
    // TIMESERIES
    //----------------
    // WRESL+ type
    @Override
    public VisitorResult visitTimeSeriesTypeTS(wreslParser.TimeSeriesTypeTSContext ctx) {
        Timeseries ts = new Timeseries();

        // Retrieve ts name
        String tsName = ctx.OBJECT_NAME().getText().toLowerCase();

        // Collect data
        ts.fromWresl = this.currentFile;
        ts.line = ctx.TIMESERIES().getSymbol().getLine();
        ts.dssBPart = tsName;
        ts.kind = visitorResultToString(visit(ctx.kind().specificationString()));
        ts.units = visitorResultToString(visit(ctx.units().specificationString()));
        if (ctx.convert() != null) {
            ts.convertToUnits = visitorResultToString(visit(ctx.convert().specificationString()));
        }

        return new VisitorResult(ts, tsName);
    }

    @Override
    // WRESL type
    public VisitorResult visitTimeSeriesTypeDef(wreslParser.TimeSeriesTypeDefContext ctx) {
        Timeseries ts = new Timeseries();

        // Retrieve ts name
        String tsName = ctx.OBJECT_NAME().getText().toLowerCase();

        // Collect data
        ts.fromWresl = this.currentFile;
        ts.line = ctx.DEFINE().getSymbol().getLine();
        if (ctx.optionalBPart() != null) {
            ts.dssBPart = visitorResultToString(visit(ctx.optionalBPart().specificationString())); }
        else {
            ts.dssBPart = tsName;
        }
        ts.kind = visitorResultToString(visit(ctx.kind().specificationString()));
        ts.units = visitorResultToString(visit(ctx.units().specificationString()));
        if (ctx.convert() != null) {
            ts.convertToUnits = visitorResultToString(visit(ctx.convert().specificationString()));
        }

        return new VisitorResult(ts, tsName);
    }


    //----------------
    // EXPRESSIONS
    //----------------
    @Override
    // expressionComparison
    public VisitorResult visitExpressionComparison(wreslParser.ExpressionComparisonContext ctx) {
        WRIMS_String expression = new WRIMS_String(ctx.getText().toLowerCase());
        return new VisitorResult(expression, null);
    }

    @Override
    // expressionMultDiv
    public VisitorResult visitExpressionMultDiv(wreslParser.ExpressionMultDivContext ctx) {
        // Store expression to be computed later during run
        WRIMS_String expression = new WRIMS_String(ctx.getText().toLowerCase());
        return new VisitorResult(expression,null);
     }

    @Override
    // expressionAddSub
    public VisitorResult visitExpressionAddSub(wreslParser.ExpressionAddSubContext ctx) {
        // Store expression to be computed later during run
        WRIMS_String expression = new WRIMS_String(ctx.getText().toLowerCase());
        return new VisitorResult(expression,null);
    }

    @Override
    // ExpressionCall
    public VisitorResult visitExpressionCall(wreslParser.ExpressionCallContext ctx) {
        // Return function name if it is not a predefined function
        if (ctx.preDefinedFunction() != null) {
            return new VisitorResult(null, null); }
        else {
            WRIMS_String functionName = new WRIMS_String(ctx.OBJECT_NAME().getText().toLowerCase());
            return new VisitorResult(functionName, null);
        }
    }


    //-----------------------------
    // MISCELLANEOUS VISIT METHODS
    //-----------------------------
    @Override
    // caseStatement
    public VisitorResult visitCaseStatement(wreslParser.CaseStatementContext ctx) {
        // Case name
        String caseName = ctx.caseName().getText().toLowerCase();

        // Case condition
        String caseCondition;
        if (ctx.caseCondition() != null) {
            caseCondition = ctx.caseCondition().getChild(1).getText().toLowerCase(); }
        else {
            caseCondition = Param.always;
        }

        // Case expression
        String caseExpression;
        wreslParser.CaseBodyContext caseBody = ctx.caseBody();
        if (caseBody.caseViaValue() != null) {
            caseExpression = caseBody.caseViaValue().getChild(1).getText().toLowerCase();
        } else if (caseBody.goalCase() != null) {
            caseExpression = "needs implementation";
        } else if (caseBody.caseViaSelect() != null) {
            caseExpression = "needs implementation";
        } else if (caseBody.caseViaExpression() != null) {
            caseExpression = caseBody.caseViaExpression().expression().getText().toLowerCase();
        } else {
            caseExpression = null;
        }

        WRIMS_CaseData caseData = new WRIMS_CaseData(caseCondition, caseExpression);

        return new VisitorResult(caseData,caseName);
    }

    @Override
    // arraySizeDefinition
    public VisitorResult visitArraySizeDefinition(wreslParser.ArraySizeDefinitionContext ctx) {
        WRIMS_String expr = new WRIMS_String(ctx.expression().getText().toLowerCase());
        return new VisitorResult(expr,null);
    }

    @Override
    // specificationString
    public VisitorResult visitSpecificationString(wreslParser.SpecificationStringContext ctx) {
        WRIMS_String data = new WRIMS_String(ctx.getText().substring(0, ctx.getText().length() - 1).substring(1).toLowerCase());   // Remove first and last character;
        return new VisitorResult(data, null);
    }


    // ----------------------------
    // --- HELPER METHODS
    // ----------------------------

    // Convert visitor result to string
    private String visitorResultToString(VisitorResult result) {
        String stringData;

        if (result.data() instanceof WRIMS_String data) {
            stringData = data.getValue(); }
        else {
            stringData = null;
        }

        return stringData;
    }


}
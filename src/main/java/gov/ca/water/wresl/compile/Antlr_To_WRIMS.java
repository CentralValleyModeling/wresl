package gov.ca.water.wresl.compile;

import gov.ca.water.wresl.domain.*;
import gov.ca.water.wresl.errors.SyntaxErrorException;
import gov.ca.water.wresl.grammar.wreslBaseVisitor;
import gov.ca.water.wresl.grammar.wreslParser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

import static gov.ca.water.wresl.compile.Utilities.getWreslText;

public class Antlr_To_WRIMS extends wreslBaseVisitor<VisitorResult> {
    private static final Logger logger = LoggerFactory.getLogger(Antlr_To_WRIMS.class);

    private final Path mainFilePath;
    private final Path absReferencePath;
    private final Map<Path, WRESLFile> wreslFilesMap;

    private String currentFile;

    // Containers (data defined under INITIAL will be stored as "parameters" under sds
    private Map<Integer,Sequence> sequenceData;
    private Map<String, ModelDataSet> groups;  // Store GROUPs as ModelDataSet to be included in actual ModelDataSets
    private Map<String, ModelDataSet> models;
    private StudyDataSet sds;


    // ------------------------------------------------------------
    // --- CONSTRUCTOR
    // ------------------------------------------------------------
    public Antlr_To_WRIMS(Path mainFilePath, Map<Path, WRESLFile> wreslFilesMap) {
        this.mainFilePath = mainFilePath;
        this.absReferencePath = mainFilePath.getParent();
        this.wreslFilesMap = wreslFilesMap;

        this.sequenceData = new HashMap<>();
        this.groups = new HashMap<>();
        this.models = new HashMap<>();
        this.sds = new StudyDataSet();
    }


    // ------------------------------------------------------------
    // --- WRESL FILE PARSING ENTRY METHODS
    // ------------------------------------------------------------
    @Override
    // ROOT VISITOR FOR THE MAIN FILE
    public VisitorResult visitMainStart(wreslParser.MainStartContext ctx) {
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
          VisitorResult result = visit(ctx.getChild(i));
          if (result == null) continue;

          // We got a single data
          if (result.children().size() == 0) {
              compiledData.add(result);
          // We got multiple data
          } else {
              compiledData.addAll(result.children());
          }
      }

      // Return compiled data
      return new VisitorResult(null, null, compiledData);
    }


    // ------------------------------------------------------------
    // --- VISITORS TO COMPILE CONTAINERS
    // ------------------------------------------------------------
    @Override
    // INITIAL; Svars listed under INITIAL statement are stored as parameters in StudyDataSet
    public VisitorResult visitInitial(wreslParser.InitialContext ctx) {
        ArrayList<String> parameterList = new ArrayList<>();
        LinkedHashMap<String,Svar> parameterMap = new LinkedHashMap<>();

        // Loop through children; they should all be SVARs
        for (int i=0; i<=ctx.children.size()-1; i++) {
            // Skip anything that is not Svar definition
            if (!(ctx.getChild(i) instanceof wreslParser.SvarContext svarCtx)) {continue;}

            VisitorResult result = visit(ctx.getChild(i));
            WRESLComponent data = result.data();
            String name = result.name();

            // WRESL component can only be an SVAR as dictated by the grammar
            switch (data) {
                case Svar svar -> {
                    svar.setData(Evaluator.evaluate(data));
                    parameterList.add(name);
                    parameterMap.put(name,svar);
                }
                default -> {
                    // Do nothing since we are already skipping any non-Svar context
                }
            }
        }

        // Now, evaluate parameter values
        for (String parameterName : parameterList) {
            // TO DO: implement evaluation and store results in the IntDouble data field of svar associated with parameter
        }


        // Store parameter data in permanently
        this.sds.setParameterList(parameterList);
        this.sds.setParameterMap(parameterMap);


        return null;
    }

    @Override
    // SEQUENCE
    public VisitorResult visitSequence(wreslParser.SequenceContext ctx) {
        Sequence sq = new Sequence();

        // Sequence name
        sq.sequenceName = getWreslText(ctx.OBJECT_NAME());

        // Retrieve sequenceBody context
        wreslParser.SequenceBodyContext sqBodyCtx = ctx.sequenceBody();

        // Model name and order
        sq.modelName = getWreslText(sqBodyCtx.OBJECT_NAME());
        sq.order = Integer.parseInt(getWreslText(sqBodyCtx.INT()));

        // Source file
        sq.fromWresl = this.currentFile;

        // Condition, if exists
        if (sqBodyCtx.sequenceCondition() != null) {
            sq.condition = getWreslText(sqBodyCtx.sequenceCondition().expression()).toLowerCase();
        }

        // Timestep, if exists
        if (sqBodyCtx.timestepSpecification() != null) {
            sq.timeStep = getWreslText(sqBodyCtx.timestepSpecification().getChild(1));
        }

        // Add sequence to map
        this.sequenceData.put(Integer.valueOf(sq.order),sq);

        return null;
    }

    @Override
    // MODEL
    public VisitorResult visitModel(wreslParser.ModelContext ctx) {
        ModelDataSet mds = new ModelDataSet();
        String modelName = getWreslText(ctx.OBJECT_NAME());

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
                WRESLComponent data = dataList.get(j).data();
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
                    case Alias alias -> {
                        mds.asList.add(name);
                        mds.asMap.put(name,(Alias)data);
                    }
                    case External external -> {
                        mds.exList.add(name);
                        mds.exMap.put(name,(External)data);
                    }
                    default -> System.err.println("error");
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
        ModelDataSet mds = new ModelDataSet();
        String groupName = getWreslText(ctx.OBJECT_NAME());

        // Visit groupBody
        for (int i = 0; i <= ctx.groupBody().size()-1; i++) {
            VisitorResult result = visit(ctx.groupBody(i));

            // Copy returned data into ModelDataSet
            List<VisitorResult> dataList;
            if (result.children().size() == 0) {
                dataList = List.of(result); }
            else {
                dataList = result.children();
            }
            for (int j=0; j<=dataList.size()-1; j++) {
                WRESLComponent data = dataList.get(j).data();
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
                    case Alias alias    -> {
                        mds.asList.add(name);
                        mds.asMap.put(name,(Alias)data);
                    }
                    case External external -> {
                        mds.exList.add(name);
                        mds.exMap.put(name,(External)data);
                    }
                    default -> System.out.println("error");
                }
            }
        }

        // Store the data for the model
        this.groups.put(groupName, mds);

        // Return null; we have already collected all the data into "groups" field
        return null;
    }


    // ------------------------------------------------------------
    // --- INCLUDE
    // ------------------------------------------------------------
    @Override
    // includeFile
    public VisitorResult visitIncludeFile(wreslParser.IncludeFileContext ctx) {
        // On entry: file pointing to include file
        String parentFile = this.currentFile;

        // Retrieve filename and create file path
        String includeFileName = visitorResultToString(visit(ctx.specificationString()));
        File absIncludeFile = new File(Path.of(parentFile).getParent().toString(), includeFileName);
        Path includeFilePath = null;
        try {
            includeFilePath = Path.of(absIncludeFile.getCanonicalPath().toLowerCase());
        }
        catch (IOException e) {
            // Redundent catch
            // Do nothing since this is the second pass we are visiting this file
            // Anny errors would have been caught in the first pass
        }

        // Set current file to include file to be used by its children
        this.currentFile = includeFilePath.toString().toLowerCase();

        // Parse tree corresponding to the file
        WRESLFile thisFile = this.wreslFilesMap.get(includeFilePath);
        if (thisFile == null) {
            int can =0;
        }
        ParseTree includeFileTree = thisFile.getParseTree();

        // Visit include file's parse tree and return collected data
        VisitorResult result = visit(includeFileTree);

        // On exit: restore current file to parent file
        this.currentFile = parentFile;

        // Return collected data
        return result;
    }


    // ------------------------------------------------------------
    // --- SVAR
    // ------------------------------------------------------------
    @Override
    // Gateway to SVAR visitors
    public VisitorResult visitSvar(wreslParser.SvarContext ctx) {
        Svar svar = new Svar();

        // Collect data from svarBody; this will visit all possible Svar definitions
        VisitorResult result = visit(ctx.svarBody());
        if (result.data() instanceof Svar tempSvar) {
            svar = tempSvar;
        }

        // Retrieve name, source file and line number of Svar
        svar.name = getWreslText(ctx.OBJECT_NAME());
        svar.fromWresl = this.currentFile;
        svar.line = ctx.OBJECT_NAME().getSymbol().getLine();

        // Is this a future array?
        if (ctx.arraySizeDefinition() != null) {
            svar.needVarFromEarlierCycle = true;
            VisitorResult result1 = visit(ctx.arraySizeDefinition());
            svar.timeArraySize = visitorResultToString(result1);
        }

        // Return data
        return new VisitorResult(svar, svar.name);
    }

    @Override
    // svarCase
    public VisitorResult visitSvarCase(wreslParser.SvarCaseContext ctx) {
        Svar svar = new Svar();

        // Loop over case statements and case information
        for (int i=0; i<=ctx.getChildCount()-1; i++) {
            VisitorResult result = visit(ctx.getChild(i));
            WRESL_CaseData caseData = (WRESL_CaseData)result.data();
            svar.addCaseData(result.name().toLowerCase(), caseData.caseCondition, caseData.caseExpression, caseData.caseConditionTree, caseData.caseExpressionTree);
        }

        // Return data
        return new VisitorResult(svar, null);

    }

    @Override
    // svarLookup
    public VisitorResult visitSvarLookup(wreslParser.SvarLookupContext ctx) {
        Svar svar = new Svar();

        // Set case condition
        svar.addCaseData(Param.defaultCaseName, Param.always, getWreslText(ctx.select()).toLowerCase(),null,ctx.select());

        // Return data
        return new VisitorResult(svar, null);
    }

    @Override
    // svarSum
    public VisitorResult visitSvarSum(wreslParser.SvarSumContext ctx) {
        Svar svar = new Svar();

        // Set case condition
        svar.addCaseData(Param.defaultCaseName, Param.always, getWreslText(ctx.sumExpressionBody()), null,ctx.sumExpressionBody());

        // Return data
        return new VisitorResult(svar, null);
    }

    @Override
    // svarValue
    public VisitorResult visitSvarValue(wreslParser.SvarValueContext ctx) {
        Svar svar = new Svar();

        // Set case condition
        svar.addCaseData(Param.defaultCaseName, Param.always, getWreslText(ctx.expression()), null, ctx.expression());

        // Return data
        return new VisitorResult(svar, null);
    }


    // ------------------------------------------------------------
    // --- DVAR
    // ------------------------------------------------------------
    @Override
    // Gateway to devar visitors
    public VisitorResult visitDvar(wreslParser.DvarContext ctx) {
        Dvar dvar = new Dvar();

        // Collect data from dvarBody; this will visit all possible Dvar definitions
        //VisitorResult result = visit(ctx.dvarBody());
        //if (result.data() instanceof Dvar tempDvar) {
        //    dvar = tempDvar;
        //}

        // Is this a future array?
        if (ctx.arraySizeDefinition() != null) {
            dvar.timeArraySize = visitorResultToString(visit(ctx.arraySizeDefinition()));
        }

        // Source file and line number
        dvar.fromWresl = this.currentFile;
        dvar.line = ctx.OBJECT_NAME().getSymbol().getLine();

        // Dvar name
        String name = getWreslText(ctx.OBJECT_NAME());

        // Return data
        return new VisitorResult(dvar, name);
    }


    // ------------------------------------------------------------
    // --- EXTERNAL
    // ------------------------------------------------------------
    @Override
    public VisitorResult visitExternal(wreslParser.ExternalContext ctx) {
        External ex = new External();

        // Retrieve external function call
        wreslParser.ExternalTargetContext externalTargetCtx = ctx.externalTarget();
        if (externalTargetCtx.specificationString() != null) {
            ex.type = visitorResultToString(visit(externalTargetCtx.specificationString()));
        } else {
            ex.type = getWreslText(externalTargetCtx.unescapedTargetString().OBJECT_NAME(0));
            if (externalTargetCtx.unescapedTargetString().OBJECT_NAME(01) != null) {
                ex.type = ex.type + "." + getWreslText(externalTargetCtx.unescapedTargetString().OBJECT_NAME(01));
            }
        }

        // Filename and line number
        ex.fromWresl = this.currentFile;
        ex.line = ctx.OBJECT_NAME().getSymbol().getLine();

        // External name
        String name = getWreslText(ctx.OBJECT_NAME());

        // Return data
        return new VisitorResult(ex, name);
    }


    // ------------------------------------------------------------
    // --- ALIAS
    // ------------------------------------------------------------
    @Override
    public VisitorResult visitAlias(wreslParser.AliasContext ctx) {
        Alias as = new Alias();
        List<String> errorMessages = new ArrayList<>();
        int count;

        // Alias name
        String name = getWreslText(ctx.OBJECT_NAME());

        // Source file and line number
        as.fromWresl = this.currentFile;
        as.line = ctx.OBJECT_NAME().getSymbol().getLine();

        // Process KIND keyword, if exists there must be only one
        count = ctx.kind().size();
        if (count > 1) {
            errorMessages.add("There cannot be more than one KIND keyword declared in an ALIAS statement!");
            throw new SyntaxErrorException(Path.of(as.fromWresl), as.line, errorMessages);
        }
        else if (count == 1) {
            as.kind = visitorResultToString(visit(ctx.kind().get(0).specificationString()));
        }

        // Process UNITS keyword, if exists there must be only one
        count = ctx.units().size();
        if (count > 1) {
            errorMessages.add("There cannot be more than one UNITS keyword declared in an ALIAS statement!");
            throw new SyntaxErrorException(Path.of(as.fromWresl), as.line, errorMessages);
        }
        else if (count == 1) {
            as.units = visitorResultToString(visit(ctx.units().get(0).specificationString()));
        }

        // Is this a future array?
        if (ctx.arraySizeDefinition() != null) {
            as.needVarFromEarlierCycle = true;
            VisitorResult result = visit(ctx.arraySizeDefinition());
            as.timeArraySize = visitorResultToString(result);
        }

        // Retrieve expression
        as.expression = getWreslText(ctx.expression());

        // Return data
        return new VisitorResult(as, name);
    }

    // ------------------------------------------------------------
    // --- TIMESERIES
    // ------------------------------------------------------------
    // WRESL+ type
    @Override
    public VisitorResult visitTimeSeriesTypeTS(wreslParser.TimeSeriesTypeTSContext ctx) {
        Timeseries ts = new Timeseries();

        List<String> errorMessages = new ArrayList<>();

        // Retrieve ts name
        String name = getWreslText(ctx.OBJECT_NAME());
        ts.dssBPart = name;

        // Source file and line number
        ts.fromWresl = this.currentFile;
        ts.line = ctx.TIMESERIES().getSymbol().getLine();

        // Process KIND; check that only one exists
        if (ctx.kind().size() != 1) {
            errorMessages.add("There must be one and only one KIND keyword in a TIMESERIES statement!");
            throw new SyntaxErrorException(Path.of(this.currentFile), ts.line, errorMessages);
        }
        else {
            ts.kind = visitorResultToString(visit(ctx.kind().get(0).specificationString()));
        }

        // Process UNITS; check that one exists
        if (ctx.units().size() != 1) {
            errorMessages.add("There must be one and only one UNITS keyword in a TIMESERIES statement!");
            throw new SyntaxErrorException(Path.of(this.currentFile), ts.line, errorMessages);
        }
        else {
            ts.units = visitorResultToString(visit(ctx.units().get(0).specificationString()));
        }

        // Process CONVERT; it is optional and if exists there should be only one
        if (!ctx.convert().isEmpty()) {
            if (ctx.convert().size() != 1) {
                errorMessages.add("There must be one and only one CONVERT keyword in a TIMESERIES statement!");
                throw new SyntaxErrorException(Path.of(this.currentFile), ts.line, errorMessages);
            }
            else {
                ts.convertToUnits = visitorResultToString(visit(ctx.convert().get(0).specificationString()));
            }
        }

        return new VisitorResult(ts, name);
    }

    @Override
    // WRESL type
    public VisitorResult visitTimeSeriesTypeDef(wreslParser.TimeSeriesTypeDefContext ctx) {
        Timeseries ts = new Timeseries();
        List<String> errorMessages = new ArrayList<>();

        // Retrieve ts name
        String name = getWreslText(ctx.OBJECT_NAME());

        // Source file and line number
        ts.fromWresl = this.currentFile;
        ts.line = ctx.DEFINE().getSymbol().getLine();

        // Process optional B part; if exists there should be only one
        if (!ctx.optionalBPart().isEmpty()) {
            if (ctx.optionalBPart().size() != 1) {
                errorMessages.add("There must be one and only one optional B part defined in a TIMESERIES statement!");
                throw new SyntaxErrorException(Path.of(ts.fromWresl), ts.line, errorMessages);
            } else {
                ts.dssBPart = visitorResultToString(visit(ctx.optionalBPart().get(0).specificationString()));
            }
        }
        else {
            ts.dssBPart = name;
        }

        // Process KIND; check that only one exists
        if (ctx.kind().size() != 1) {
            errorMessages.add("There must be one and only one KIND keyword in a TIMESERIES statement!");
            throw new SyntaxErrorException(Path.of(this.currentFile), ts.line, errorMessages);
        }
        else {
            ts.kind = visitorResultToString(visit(ctx.kind().get(0).specificationString()));
        }

        // Process UNITS; check that one exists
        if (ctx.units().size() != 1) {
            errorMessages.add("There must be one and only one UNITS keyword in a TIMESERIES statement!");
            throw new SyntaxErrorException(Path.of(this.currentFile), ts.line, errorMessages);
        }
        else {
            ts.units = visitorResultToString(visit(ctx.units().get(0).specificationString()));
        }

        // Process CONVERT; it is optional and if exists there should be only one
        if (!ctx.convert().isEmpty()) {
            if (ctx.convert().size() != 1) {
                errorMessages.add("There must be one and only one CONVERT keyword in a TIMESERIES statement!");
                throw new SyntaxErrorException(Path.of(this.currentFile), ts.line, errorMessages);
            }
            else {
                ts.convertToUnits = visitorResultToString(visit(ctx.convert().get(0).specificationString()));
            }
        }

        return new VisitorResult(ts, name);
    }


    // ------------------------------------------------------------
    // --- EXPRESSIONS
    // ------------------------------------------------------------
    @Override
    // expressionComparison
    public VisitorResult visitExpressionComparison(wreslParser.ExpressionComparisonContext ctx) {
        WRESL_String expression = new WRESL_String(getWreslText(ctx));
        return new VisitorResult(expression, null);
    }

    @Override
    // expressionMultDiv
    public VisitorResult visitExpressionMultDiv(wreslParser.ExpressionMultDivContext ctx) {
        // Store expression to be computed later during run
        WRESL_String expression = new WRESL_String(getWreslText(ctx));
        return new VisitorResult(expression,null);
     }

    @Override
    // expressionAddSub
    public VisitorResult visitExpressionAddSub(wreslParser.ExpressionAddSubContext ctx) {
        // Store expression to be computed later during run
        WRESL_String expression = new WRESL_String(getWreslText(ctx));
        return new VisitorResult(expression,null);
    }

    @Override
    // ExpressionCall
    public VisitorResult visitExpressionCall(wreslParser.ExpressionCallContext ctx) {
        // Return function name if it is not a predefined function
        if (ctx.preDefinedFunction() != null) {
            return new VisitorResult(null, null); }
        else {
            WRESL_String functionName = new WRESL_String(getWreslText(ctx.OBJECT_NAME()));
            return new VisitorResult(functionName, null);
        }
    }


    // ------------------------------------------------------------
    // --- CASE STATEMENT
    // ------------------------------------------------------------
    @Override
    // caseStatement - Gateway to all CASE visit methods
    public VisitorResult visitCaseStatement(wreslParser.CaseStatementContext ctx) {
        // Case name
        String caseName = getWreslText(ctx.caseName());

        // Case condition
        String caseCondition;
        ParseTree caseConditionTree;
        if (ctx.caseCondition() != null) {
            caseCondition = getWreslText(ctx.caseCondition().getChild(1));
            caseConditionTree = ctx.caseCondition();
        }
        else {
            caseCondition = Param.always;
            caseConditionTree = null;
        }

        // Case expression
        String caseExpression = visitorResultToString(visit(ctx.caseBody()));
        ParseTree caseExpressionTree = ctx.caseBody();

        WRESL_CaseData caseData = new WRESL_CaseData(caseCondition, caseExpression, caseConditionTree, caseExpressionTree);

        return new VisitorResult(caseData,caseName);
    }

    @Override
    // caseViaValue
    public VisitorResult visitCaseViaValue(wreslParser.CaseViaValueContext ctx) {
        return new VisitorResult(new WRESL_String(getWreslText(ctx.expression())), null);
    }

    @Override
    // caseViaGoal
    public VisitorResult visitCaseViaGoal(wreslParser.CaseViaGoalContext ctx) {
        return new VisitorResult(new WRESL_String("needs implementation"), null);
    }

    @Override
    // caseViaSelect
    public VisitorResult visitCaseViaSelect(wreslParser.CaseViaSelectContext ctx) {
        return new VisitorResult(new WRESL_String(getWreslText(ctx.select())), null);
    }

    @Override
    // caseViaExpression
    public VisitorResult visitCaseViaExpression(wreslParser.CaseViaExpressionContext ctx) {
        return new VisitorResult(new WRESL_String(getWreslText(ctx.expression())), null);
    }


    // ------------------------------------------------------------
    // --- MISCELLANEOUS VISIT METHODS
    // ------------------------------------------------------------
    @Override
    // arraySizeDefinition
    public VisitorResult visitArraySizeDefinition(wreslParser.ArraySizeDefinitionContext ctx) {
        WRESL_String expr = new WRESL_String(getWreslText(ctx.expression()));
        return new VisitorResult(expr,null);
    }

    @Override
    // specificationString
    public VisitorResult visitSpecificationString(wreslParser.SpecificationStringContext ctx) {
        String tempString = getWreslText(ctx);
//        WRIMS_String data = new WRIMS_String(ctx.getText().substring(0, ctx.getText().length() - 1).substring(1).toLowerCase());   // Remove first and last character;
        WRESL_String data = new WRESL_String(tempString.substring(0, tempString.length() - 1).substring(1));   // Remove first and last character;
        return new VisitorResult(data, null);
    }


    // ----------------------------
    // --- HELPER METHODS
    // ----------------------------

    // Convert visitor result to string
    private String visitorResultToString(VisitorResult result) {
        String stringData;

        if (result.data() instanceof WRESL_String data) {
            stringData = data.getValue(); }
        else {
            stringData = null;
        }

        return stringData;
    }





}
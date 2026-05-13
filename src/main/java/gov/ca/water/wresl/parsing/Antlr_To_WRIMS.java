package gov.ca.water.wresl.parsing;

import gov.ca.water.wresl.domain.*;
import gov.ca.water.wresl.errors.EvaluationErrorException;
import gov.ca.water.wresl.errors.SyntaxErrorException;
import gov.ca.water.wresl.grammar.wreslBaseVisitor;
import gov.ca.water.wresl.grammar.wreslLexer;
import gov.ca.water.wresl.grammar.wreslParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

import static gov.ca.water.wresl.parsing.Utilities.getWreslText;

public class Antlr_To_WRIMS extends wreslBaseVisitor<VisitorResult> {
    private static final Logger log = LoggerFactory.getLogger(Antlr_To_WRIMS.class);
    // Main WRESL file, absolute folder that it resides, and list of WRESL files
    private final Path mainFilePath;
    private final Path absReferencePath;
    private final Map<Path, WRESLFile> wreslFilesMap;

    // Containers (data defined under INITIAL will be stored as "parameters" under sds
    private Map<Integer,Sequence> sequenceData;
    private Map<String, ModelDataSet> groups;  // Store GROUPs as ModelDataSet to be included in actual ModelDataSets
    private Map<String, ModelDataSet> models;
    private StudyDataSet sds;

    // Scratch memory used for data that needs to be access by multiple methods
    private String currentFile;                                   // WRESL file that's being parsed
    private Map<String, ArrayList<String>> modelsWithinModelsMap; // List of models included in each model/group
    private String currentModelOrGroupName = "";                  // Name of model or group that is currently being parsed
    private LinkedHashMap<String, Svar> tempParameterMap;         // Temporary map of Svars defined in the INITIAL statement

    // Static parameters needed in the class
    private static final String f_InsertLHSHere = "insertLHS";


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

        this.modelsWithinModelsMap = new HashMap<>();
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

        // Instantiate Evaluator
        Evaluator.setReferencePath(this.absReferencePath.toString());

        // Loop through Study children nodes
        for (int i = 0; i <= ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);

            // EOF?
            if (child instanceof TerminalNode terminalNode) {
                if (terminalNode.getSymbol().getType() == org.antlr.v4.runtime.Token.EOF) break;
            }

            // Visit child
            VisitorResult data = visit(child);
            if (data == null) continue;
        }

        // Process SEQUENCE data, compile ordered modelList in StudyDataSet
        ArrayList<String> modelList = new ArrayList<>();
        ArrayList<String> modelConditionList = new ArrayList<>();
        ArrayList<String> modelTimeStepList = new ArrayList<>();
        for (int i=0; i<this.sequenceData.size(); i++) {
            Sequence sq = this.sequenceData.get(i+1);
            // Check that a model is not refernced in multiple SEQUENCEs
            if (modelList.contains(sq.modelName)) {
                throw new EvaluationErrorException("Each SEQUENCE must define a unique model. Model '" + sq.modelName + "' is used in multiple SEQUENCEs!");
            }
            modelList.add(sq.modelName);
            modelConditionList.add(sq.condition);
            modelTimeStepList.add(sq.timeStep);
        }
        this.sds.setModelList(modelList);
        this.sds.setModelConditionList(modelConditionList);
        this.sds.setModelTimeStepList(modelTimeStepList);

        // Check that models/groups included in other models/groups exist
        for (String thisModel: this.modelsWithinModelsMap.keySet()) {
            List<String> includedModelsList = this.modelsWithinModelsMap.get(thisModel);
            for (String includedModel: includedModelsList) {
                if (!this.models.containsKey(includedModel)) {
                    if (!this.groups.containsKey(includedModel)) {
                        throw new EvaluationErrorException("Model/group " + includedModel + " referenced from model " + thisModel + " is not defined!");
                    }
                }
            }
        }

        // Check for duplicate Dvars within each model

        // Check for duplicate Svars within each model

        // Check for duplicate Goals within each model

        // Check for duplicate aliases within each model

        // Check for duplicate External within each model

        // Check for duplicate Timeseries within each model

        // Check for duplicate weight tables within each model


        // Clear data that is no longer needed
        this.sequenceData = null;
        this.groups = null;
        this.models = null;

        return new VisitorResult(this.sds,null);
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
        ArrayList<String> tempParameterList = new ArrayList<>();
        this.tempParameterMap = new LinkedHashMap<>();

        // Loop through children; they should all be SVARs
        for (int i = 0; i <= ctx.children.size() - 1; i++) {
            // Skip anything that is not Svar definition
            if (!(ctx.getChild(i) instanceof wreslParser.SvarContext svarCtx)) {
                continue;
            }

            VisitorResult result = visit(ctx.getChild(i));
            WRESLComponent data = result.data();
            String name = result.name();

            // WRESL component can only be an SVAR as dictated by the grammar
            switch (data) {
                case Svar svar -> {
                    tempParameterList.add(name);
                    this.tempParameterMap.put(name, svar);
                }
                default -> {
                    // Do nothing since we are already skipping any non-Svar context
                }
            }
        }

        // Process parameters (Svars)
        Evaluator.evaluateInitialData(this.tempParameterMap);

        // Store parameter data in permanently
        this.sds.setParameterList(tempParameterList);
        this.sds.setParameterMap(this.tempParameterMap);

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
    public VisitorResult visitModel(wreslParser.ModelContext ctx) throws EvaluationErrorException {
        ModelDataSet mds = new ModelDataSet();
        this.currentModelOrGroupName = getWreslText(ctx.OBJECT_NAME());

        // On entry: store file referiing to Model context
        String parentFile = this.currentFile;

        // Check that model is not defined more than once
        if (this.models.get(this.currentModelOrGroupName) != null) {
            throw new EvaluationErrorException(this.currentFile, ctx.OBJECT_NAME().getSymbol().getLine(), "Model " + this.currentModelOrGroupName + " is defined more than once!");
        }

        // Visit modelBody
        for (int i = 0; i < ctx.modelBody().size(); i++) {
            VisitorResult returnedData = visit(ctx.modelBody(i));

            // Continue if returnedData is null, for instance, after evaluation of an IF statement
            if (returnedData == null) { continue; }

            // Copy returned data into ModelDataSet
            List<VisitorResult> dataList;
            if (returnedData.children().size() == 0) {
                dataList = List.of(returnedData);
            } else {
                dataList = returnedData.children();
            }
            for (int j = 0; j <= dataList.size() - 1; j++) {
                WRESLComponent data = dataList.get(j).data();
                if (data == null) continue;
                String name = dataList.get(j).name();
                switch (data) {
                    case Svar svar -> {
                        mds.svList.add(name);
                        mds.svMap.put(name, (Svar) data);
                    }
                    case Dvar dvar -> {
                        mds.dvList.add(name);
                        mds.dvMap.put(name, (Dvar) data);
                    }
                    case Timeseries ts -> {
                        mds.tsList.add(name);
                        mds.tsMap.put(name, (Timeseries) data);
                    }
                    case Goal goal -> {
                        mds.gList.add(name);
                        mds.gMap.put(name, (Goal) data);
                    }
                    case Alias alias -> {
                        mds.asList.add(name);
                        mds.asMap.put(name, (Alias) data);
                    }
                    case External external -> {
                        mds.exList.add(name);
                        mds.exMap.put(name, (External) data);
                    }
                    default -> System.err.println("error");
                }
            }
        }

        // Store the data for the model
        this.models.put(this.currentModelOrGroupName, mds);

        // On exit: restore filename from which Model context was referred to
        this.currentFile = parentFile;

        // Return null; we have already collected all the data into "models" field
        return null;
    }

    @Override
    // GROUP
    public VisitorResult visitGroup(wreslParser.GroupContext ctx) throws EvaluationErrorException {
        ModelDataSet mds = new ModelDataSet();
        this.currentModelOrGroupName = getWreslText(ctx.OBJECT_NAME());

        // Visit groupBody
        for (int i = 0; i <= ctx.groupBody().size()-1; i++) {
            VisitorResult result = visit(ctx.groupBody(i));

            // Continue if null value is returned as VisitorResult (such as the result of an IF statement)
            if (result == null) { continue; }

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
                        mds.svMap.put(name,svar);
                    }
                    case Dvar dvar -> {
                        mds.dvList.add(name);
                        mds.dvMap.put(name, dvar);
                    }
                    case Timeseries ts -> {
                        mds.tsList.add(name);
                        mds.tsMap.put(name, ts);
                    }
                    case Goal goal -> {
                        mds.gList.add(name);
                        mds.gMap.put(name, goal);
                    }
                    case Alias alias    -> {
                        mds.asList.add(name);
                        mds.asMap.put(name, alias);
                    }
                    case External external -> {
                        mds.exList.add(name);
                        mds.exMap.put(name, external);
                    }
                    default -> throw new EvaluationErrorException("Error in processing WRESL data for group " + this.currentModelOrGroupName);
                }
            }
        }

        // Store the data for the model
        this.groups.put(this.currentModelOrGroupName, mds);

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
            return null;
        }
        ParseTree includeFileTree = thisFile.getParseTree();

        // Visit include file's parse tree and return collected data
        VisitorResult result = visit(includeFileTree);

        // On exit: restore current file to parent file
        this.currentFile = parentFile;

        // Return collected data
        return result;
    }

    @Override
    // includeModel
    public VisitorResult visitIncludeModel(wreslParser.IncludeModelContext ctx) {
        // Included model name
        String includedModel = getWreslText(ctx.modelReference().OBJECT_NAME());

        // Add included model to list of models referenced by a model
        if (this.currentModelOrGroupName.contains(this.currentModelOrGroupName)) {
            this.modelsWithinModelsMap.get(this.currentModelOrGroupName).add(includedModel);
        }
        else {
            ArrayList<String> includedModels = new ArrayList<>(List.of(includedModel));
            this.modelsWithinModelsMap.put(this.currentModelOrGroupName, includedModels);
        }

        // Return null since we have already stored the necessary data in modelsWithinModelsMap data
        return null;
    }

    @Override
    public VisitorResult visitIncludeGroup(wreslParser.IncludeGroupContext ctx) {
        // Included model name
        String includedModel = getWreslText(ctx.groupReference().OBJECT_NAME());

        // Add included model to list of models referenced by a model
        if (this.modelsWithinModelsMap.containsKey(this.currentModelOrGroupName)) {
            this.modelsWithinModelsMap.get(this.currentModelOrGroupName).add(includedModel);
        }
        else {
            ArrayList<String> includedModels = new ArrayList<>(List.of(includedModel));
            this.modelsWithinModelsMap.put(this.currentModelOrGroupName, includedModels);
        }

        // Return null since we have already stored the necessary data in modelsWithinModelsMap data
        return null;
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
            svar.addCaseData(result.name().toLowerCase(),
                             caseData.caseCondition,
                             caseData.caseExpressionList.get(0),
                             caseData.caseConditionTree,
                             caseData.caseExpressionTreeList.get(0));
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
    // Gateway for DVAR visitors
    public VisitorResult visitDvar(wreslParser.DvarContext ctx) {
        Dvar dvar = new Dvar();
        String errorMessage;

        // Dvar name
        String name = getWreslText(ctx.OBJECT_NAME());

        // Integer dvar, and bounds
        WRESLComponent data = visit(ctx.defineBoundLimits()).data();
        dvar = (Dvar)data;

        // Is this a future array?
        if (ctx.arraySizeDefinition() != null) {
            dvar.timeArraySize = visitorResultToString(visit(ctx.arraySizeDefinition()));
        }

        // Source file and line number
        dvar.fromWresl = this.currentFile;
        dvar.line = ctx.OBJECT_NAME().getSymbol().getLine();

        // Process KIND and UNITS keyword; there has to be only one KIND and one UNITS keyword
        int countKind = 0;
        int countUnits = 0;
        for (wreslParser.DefinitionSpecificsContext defSpec : ctx.definitionSpecifics()) {
            // Process KIND keyword
            if (defSpec.kind() != null) {
                countKind = countKind + 1;
                dvar.kind = visitorResultToString(visit(defSpec.kind().specificationString()));
            }
            // Process UNITS keyword
            if (defSpec.units() != null) {
                countUnits = countUnits + 1;
                dvar.units = visitorResultToString(visit(defSpec.units().specificationString()));
            }
        }
        if (countKind != 1) {
            errorMessage = "There must be one and only one KIND keyword declared in a DVAR statement!";
            throw new SyntaxErrorException(dvar.fromWresl, dvar.line, errorMessage);
        }
        if (countUnits != 1) {
            errorMessage = "There must be one and only one UNITS keyword declared in a DVAR statement!";
            throw new SyntaxErrorException(dvar.fromWresl, dvar.line, errorMessage);
        }

        // Return data
        return new VisitorResult(dvar, name);
    }

    @Override
    // defineBoundLimits
    public VisitorResult visitDefineBoundLimits(wreslParser.DefineBoundLimitsContext ctx) {
        Dvar dvar = new Dvar();
        String errorMessage;

        // Integer DVAR?
        if (ctx.INTEGER() != null) {
            dvar.integer = Param.yes;
        }

        // STD type bounds
        if (ctx.STD() != null) {
            if (ctx.INTEGER() != null) {
                dvar.lowerBound = Param.dv_std_lowerBound;
                dvar.lowerBoundValue = Integer.valueOf(dvar.lowerBound);
                dvar.upperBound = Param.dv_std_integer_upperBound;
                dvar.upperBoundValue = Integer.valueOf(dvar.upperBound);
            }
            else {
                dvar.lowerBound = Param.dv_std_lowerBound;
                dvar.lowerBoundValue = Double.valueOf(dvar.lowerBound);
                dvar.upperBound = Param.dv_std_upperBound;
                dvar.upperBoundValue = Double.valueOf(Param.dv_upper_unbounded);
            }

            // That's it! Return the value
            return new VisitorResult(dvar, null);
        }

        // Initially assume standard bounds
        if (ctx.INTEGER() != null) {
            dvar.lowerBound = Param.dv_std_integer_lowerBound;
            dvar.lowerBoundValue = Integer.valueOf(dvar.lowerBound);
            dvar.upperBound = Param.dv_std_integer_upperBound;
            dvar.upperBoundValue = Integer.valueOf(dvar.upperBound);
        } else {
            dvar.lowerBound = Param.dv_std_lowerBound;
            dvar.lowerBoundValue = Double.valueOf(dvar.lowerBound);
            dvar.upperBound = Param.dv_std_upperBound;
            dvar.upperBoundValue = Double.valueOf(dvar.upperBound);
        }

        // Process bounds
        int iLowerCount = 0;
        int iUpperCount = 0;
        for (wreslParser.DefineBoundUpperLowerContext defBound : ctx.defineBoundUpperLower()) {
            // Process LOWER BOUND
            if (defBound.defineLowerBound() != null) {
                iLowerCount = iLowerCount + 1;

                // Cannot have more than 1 lower bound
                if (iLowerCount > 1) {
                    errorMessage = "There cannot be more than one lower bound defined for a decision variable!";
                    throw new SyntaxErrorException(dvar.fromWresl, dvar.line, errorMessage);
                }

                // Lower unbounded
                if (defBound.defineLowerBound().boundType().UNBOUNDED() != null) {
                    dvar.lowerBound = Param.dv_lower_unbounded;
                    dvar.lowerBoundValue = Double.valueOf(dvar.lowerBound);
                }
                // Lower bounded
                else {
                    dvar.lowerBound = defBound.defineLowerBound().boundType().expression().getText();
                }
                dvar.lowerBoundExpressionParseTree = Utilities.generateParseTree(dvar.lowerBound, "expression");

                // Check that integer Dvar has proper lower bound
                if (ctx.INTEGER() != null) {
                    if (dvar.lowerBound != Param.dv_std_lowerBound) {
                        errorMessage = "An integer decision variable can only have 0  as the lower bound!";
                        throw new SyntaxErrorException(dvar.fromWresl, dvar.line, errorMessage);
                    }
                }
            }

            // Process UPPER BOUND
            if (defBound.defineUpperBound() != null) {
                iUpperCount = iUpperCount + 1;

                // Cannot have more than 1 upper bound
                if (iUpperCount > 1) {
                    errorMessage = "There cannot be more than one upper bound defined for a decision variable!";
                    throw new SyntaxErrorException(dvar.fromWresl, dvar.line, errorMessage);
                }

                // Upper unbounded
                if (defBound.defineUpperBound().boundType().UNBOUNDED() != null) {
                    // Do nothing; this is how upper bound was initialized anyways
                }
                // Upper bounded
                else {
                    dvar.upperBound = defBound.defineUpperBound().boundType().expression().getText();
                }
                dvar.upperBoundExpressionParseTree = Utilities.generateParseTree(dvar.upperBound, "expression");

                // Check that integer Dvar has proper lower bound
                if (ctx.INTEGER() != null) {
                    if (dvar.lowerBound != Param.dv_std_lowerBound) {
                        errorMessage = "An integer decision variable can only have 0 as the lower bound!";
                        throw new SyntaxErrorException(dvar.fromWresl, dvar.line, errorMessage);
                    }
                    if (dvar.upperBound != Param.dv_std_upperBound) {
                        errorMessage = "An integer decision variable can only have 1 as the upper bound!";
                        throw new SyntaxErrorException(dvar.fromWresl, dvar.line, errorMessage);
                    }
                }
            }
        }

        // Return a dvar object only for the upper and lower bounds and if it is an integer or not
        return new VisitorResult(dvar, null);
    }


    // ------------------------------------------------------------
    // --- GOAL
    // ------------------------------------------------------------
    @Override
    // Gateway to GOAL-related visitor methods
    public VisitorResult visitGoal(wreslParser.GoalContext ctx) {
        // Visit goal body
        VisitorResult result = visit(ctx.goalBody());
        Goal goal = (Goal) result.data();

        // Goal name
        String name = getWreslText(ctx.OBJECT_NAME());
        goal.name = name;

        // Source filename and line number
        goal.fromWresl = this.currentFile;
        goal.line = ctx.OBJECT_NAME().getSymbol().getLine();

        return new VisitorResult(goal, name);
    }

    @Override
    // goalShortForm
    public VisitorResult visitGoalShortForm(wreslParser.GoalShortFormContext ctx) {
        Goal goal = new Goal();

        // Set case related stuff
        goal.caseName.add(Param.defaultCaseName);
        goal.caseCondition.add(Param.always);
        goal.caseExpression.add(getWreslText(ctx));
        goal.caseExpressionParseTrees.add(ctx);

        return new VisitorResult(goal, null);
    }

    @Override
    // goalViaCase
    public VisitorResult visitGoalViaCase(wreslParser.GoalViaCaseContext ctx) {
        Goal goal = new Goal();

        // Loop through case statements
        for (int i=0; i<ctx.goalCaseStatement().size(); i++) {
            VisitorResult result = visit(ctx.goalCaseStatement(i));
            WRESL_CaseData caseData = (WRESL_CaseData) result.data();

            // Update counters for surplus/slack variables
            int index = -1;
            for (String slackSurplusDvar : caseData.slackSurplusDvarList) {
                index = index + 1;
                if (slackSurplusDvar.contains("surplus")) {
                    String tempDvar = slackSurplusDvar.substring(0,slackSurplusDvar.lastIndexOf("_")+1) + (i+1);
                    String tempCaseExpression = caseData.caseExpressionList.get(0).replace(slackSurplusDvar,tempDvar);
                    caseData.caseExpressionList.set(0, tempCaseExpression);
                    caseData.slackSurplusDvarList.set(index, tempDvar);
                    String weight = caseData.slackSurplusDvarWeightMap.get(slackSurplusDvar);
                    if (weight != null) {
                        caseData.slackSurplusDvarWeightMap.remove(slackSurplusDvar);
                        caseData.slackSurplusDvarWeightMap.put(tempDvar, weight);
                    }
                }
                if (slackSurplusDvar.contains("slack")) {
                    String tempDvar = slackSurplusDvar.substring(0,slackSurplusDvar.lastIndexOf("_")+1) + (i+1);
                    String tempCaseExpression = caseData.caseExpressionList.get(0).replace(slackSurplusDvar,tempDvar);
                    caseData.caseExpressionList.set(0, tempCaseExpression);
                    caseData.slackSurplusDvarList.set(index, tempDvar);
                    String weight = caseData.slackSurplusDvarWeightMap.get(slackSurplusDvar);
                    if (weight != null) {
                        caseData.slackSurplusDvarWeightMap.remove(slackSurplusDvar);
                        caseData.slackSurplusDvarWeightMap.put(tempDvar, weight);
                    }
                }
            }

            // Copy data from caseData into goal
            goal.caseName.add(result.name());
            goal.caseCondition.add(caseData.caseCondition);
            goal.caseConditionParseTrees.add(caseData.caseConditionTree);
            goal.caseExpression.addAll(caseData.caseExpressionList);
            goal.caseExpressionParseTrees.addAll(caseData.caseExpressionTreeList);
            goal.dvarSlackSurplusList.add(caseData.slackSurplusDvarList);
            goal.dvarWeightMapList.add(caseData.slackSurplusDvarWeightMap);
        }

        return new VisitorResult(goal, null);
    }

    @Override
    // goalViaPenalty
    public VisitorResult visitGoalViaPenalty(wreslParser.GoalViaPenaltyContext ctx) {
        Goal goal = new Goal();

        // Penalty related data
        if (ctx.goalPenalties() != null) {
            VisitorResult result = visit(ctx.goalPenalties());
            Goal goalTemp = (Goal) result.data();
            goal = goalTemp;
        } else {
            // Retrieve LHS and RHS expressions
            String lhsExpression = getWreslText(ctx.expression(0));
            String rhsExpression = getWreslText(ctx.expression(1));
            String caseExpression = lhsExpression + "=" + rhsExpression;
            goal.caseExpression.add(caseExpression);
            goal.caseExpressionParseTrees.add(getExpressionParseTree(caseExpression));
        }

        // Default case name and condition
        goal.caseName.add(Param.defaultCaseName);
        goal.caseCondition.add(Param.always);

        // Return goal
        return new VisitorResult(goal, null);
    }

    @Override
    // goalCaseStatement
    public VisitorResult visitGoalCaseStatement(wreslParser.GoalCaseStatementContext ctx) {
        List<String> caseExpressionList = new ArrayList<>();
        List<ParseTree> caseExpressionTreeList = new ArrayList<>();
        List<String> caseSlackSurplusDvarList = new ArrayList<>();
        Map<String,String> caseSlackSurplusDvarWeightMap = new HashMap<>();

         // Retrieve case name
        String caseName = getWreslText(ctx.goalCaseName());

        // Case condition
        String caseCondition;
        ParseTree caseConditionTree;
        if (ctx.goalCaseCondition() != null) {
            caseCondition = getWreslText(ctx.goalCaseCondition().getChild(1));
            caseConditionTree = ctx.goalCaseCondition().caseConditionExpression();
        }
        else {
            caseCondition = Param.always;
            caseConditionTree = null;
        }

        // Retrieve penalty-related information (constraint expressions, new dvars and weights)
        if (ctx.goalPenalties() != null) {
            VisitorResult result = visit(ctx.goalPenalties());
            Goal goalTemp = (Goal) result.data();
            caseExpressionList.addAll(goalTemp.caseExpression);
            caseExpressionTreeList.addAll(goalTemp.caseExpressionParseTrees);
            caseSlackSurplusDvarList.addAll(goalTemp.dvarSlackSurplusList.get(0));
            caseSlackSurplusDvarWeightMap.putAll(goalTemp.dvarWeightMapList.get(0));
        } else {
            // Walk back up the parse tree and retrieve goal name and LHS expression
            ParserRuleContext goalCtx = ctx.getParent().getParent().getParent();
            wreslParser.GoalContext goal = (wreslParser.GoalContext) goalCtx;  // This is coded to fail if grammar is changed for GOAL statement so we can catch the issue quickly
            String lhsExpression = getWreslText(goal.goalBody().goalViaCase().expression());
            String rhsExpression = getWreslText(ctx.expression());
            String caseExpression = lhsExpression + "=" + rhsExpression;
            caseExpressionList.add(caseExpression);
            caseExpressionTreeList.add(getExpressionParseTree(caseExpression));
        }

        // Return case data
        WRESL_CaseData caseData = new WRESL_CaseData(caseCondition,
                                                     caseConditionTree,
                                                     caseExpressionList,
                                                     caseExpressionTreeList,
                                                     caseSlackSurplusDvarList,
                                                     caseSlackSurplusDvarWeightMap);
        return new VisitorResult(caseData,caseName);
    }

    @Override
    // penalties
    // Note: If surplus/slack variables are genereted, their count numbers
    //       will need to be updated in the calling method since we can't
    //       determine the count in this method
    public VisitorResult visitGoalPenalties(wreslParser.GoalPenaltiesContext ctx) {
        Goal goal = new Goal();
        List<String> dvarSlackSurplusList = new ArrayList<>();
        Map<String,String> dvarWeightMap = new HashMap<>();

        // Flags describing which constrains are defined and how
        boolean bGTExists = ctx.penaltyGT() != null;
        boolean bGTPenaltyExists = false;
        boolean bGTConstrainExists = false;
        String penaltyGT = null;
        Double numberPenaltyGT = -99.9;
        if (bGTExists) {
            if (ctx.penaltyGT().penaltyValue().PENALTY() != null) {
                bGTPenaltyExists = true;
                penaltyGT = getWreslText(ctx.penaltyGT().penaltyValue().expression());
                try {
                    numberPenaltyGT = Double.valueOf(penaltyGT);
                } catch (NumberFormatException e) {
                    // Do nothing
                }
            }
            if (ctx.penaltyGT().penaltyValue().CONSTRAIN() != null) { bGTConstrainExists = true; }
        }
        boolean bLTExists = ctx.penaltyLT() != null;
        boolean bLTPenaltyExists = false;
        boolean bLTConstrainExists = false;
        String penaltyLT = null;
        Double numberPenaltyLT = -99.9;
        if (bLTExists) {
            if (ctx.penaltyLT().penaltyValue().PENALTY() != null) {
                bLTPenaltyExists = true;
                penaltyLT = getWreslText(ctx.penaltyLT().penaltyValue().expression());
                try {
                    numberPenaltyLT = Double.valueOf(penaltyLT);
                } catch (NumberFormatException e) {
                    // Do nothing
                }
            }
            if (ctx.penaltyLT().penaltyValue().CONSTRAIN() != null) { bLTConstrainExists = true; }
        }

        // Retrieve RHS expression from parent context, and line number in WRESL file in case an error needs to be reported
        // Also, goal name
        int lineNumber;
        String rhsExpression;
        String lhsExpression;
        String goalName;
        ParserRuleContext parentCtx = ctx.getParent();
        wreslParser.GoalContext goalTemp;
        if (parentCtx instanceof wreslParser.GoalCaseStatementContext) {
            wreslParser.GoalCaseStatementContext workCtx = (wreslParser.GoalCaseStatementContext) parentCtx;
            rhsExpression = getWreslText(workCtx.expression());
            goalTemp = (wreslParser.GoalContext) workCtx.getParent().getParent().getParent();
            lhsExpression = getWreslText(goalTemp.goalBody().goalViaCase().expression());
        } else {
            wreslParser.GoalViaPenaltyContext workCtx = (wreslParser.GoalViaPenaltyContext) parentCtx;
            rhsExpression = getWreslText(workCtx.expression(1));
            goalTemp = (wreslParser.GoalContext) workCtx.getParent().getParent();
            lhsExpression = getWreslText(goalTemp.goalBody().goalViaPenalty().expression(0));
        }
        lineNumber = goalTemp.OBJECT_NAME().getSymbol().getLine();
        goalName = getWreslText(goalTemp.OBJECT_NAME());

        // Process penalty statements based on individual statements provided
        if (bGTExists) {
            // "LHS > RHS" statement exists
            if (bLTExists) {
                // "LHS < RHS" statement also exist
                if (bGTConstrainExists) {
                    if (bLTConstrainExists) {
                        // LHS > RHS CONSTRAIN
                        // LHS < RHS CONSTRAIN
                        goal.caseExpression.add(lhsExpression + "=" + rhsExpression);
                    } else {
                        if (numberPenaltyLT == 0.0) {
                            // LHS > RHS CONSTRAIN
                            // LHS < RHS PENALTY 0
                            goal.caseExpression.add(lhsExpression + "<" + rhsExpression);
                        } else {
                            // LHS > RHS CONSTRAIN
                            // LHS < RHS PENALTY (non-zero value)
                            String slackDvar = "slack__" + goalName + "_1";
                            String weight = "-(" + penaltyLT + ")";
                            goal.caseExpression.add(lhsExpression + "+"+ slackDvar + "=" + rhsExpression);
                            dvarSlackSurplusList.add(slackDvar);
                            dvarWeightMap.put(slackDvar,weight);
                        }
                    }
                } else {
                    if (numberPenaltyGT == 0.0) {
                        if (bLTConstrainExists) {
                            // LHS > RHS PENALTY 0
                            // LHS < RHS CONSTRAIN
                            goal.caseExpression.add(lhsExpression + ">" + rhsExpression);
                        } else {
                            if (numberPenaltyLT == 0.0) {
                                // LHS > RHS PENALTY 0
                                // LHS < RHS PENALTY 0
                                goal.caseExpression.add("1 > 0"); // Copied from WRIMS2
                            } else {
                                // LHS > RHS PENALTY 0
                                // LHS < RHS PENALTY (non-zero value)
                                String slackDvar = "slack__" + goalName + "_1";
                                String weight = "-(" + penaltyLT + ")";
                                goal.caseExpression.add(lhsExpression + "+" + slackDvar + ">" + rhsExpression);
                                dvarSlackSurplusList.add(slackDvar);
                                dvarWeightMap.put(slackDvar, weight);
                            }
                        }
                    } else {
                        if (bLTConstrainExists) {
                            // LHS > RHS PENALTY (non-zero value)
                            // LHS < RHS CONSTRAIN
                            String surplusDvar = "surplus__" + goalName + "_1";
                            String weight = "-(" + penaltyGT + ")";
                            goal.caseExpression.add(lhsExpression + "-" + surplusDvar + "=" + rhsExpression);
                            dvarSlackSurplusList.add(surplusDvar);
                            dvarWeightMap.put(surplusDvar, weight);
                        } else {
                            if (numberPenaltyLT == 0.0) {
                                // LHS > RHS PENALTY (non-zero value)
                                // LHS < RHS PENALTY 0
                                String surplusDvar = "surplus__" + goalName + "_1";
                                String weight = "-(" + penaltyGT + ")";
                                goal.caseExpression.add(lhsExpression + "-" + surplusDvar + "<" + rhsExpression);
                                dvarSlackSurplusList.add(surplusDvar);
                                dvarWeightMap.put(surplusDvar, weight);
                            } else {
                                // LHS > RHS PENALTY (non-zero value)
                                // LHS < RHS PENALTY (non-zero value)
                                String surplusDvar = "surplus__" + goalName + "_1";
                                String surplusWeight = "-(" + penaltyGT + ")";
                                String slackDvar = "slack__" + goalName + "_1";
                                String slackWeight = "-(" + penaltyLT + ")";
                                goal.caseExpression.add(lhsExpression + "-" + surplusDvar + "+" + slackDvar + "=" + rhsExpression);
                                dvarSlackSurplusList.add(slackDvar);
                                dvarSlackSurplusList.add(surplusDvar);
                                dvarWeightMap.put(slackDvar, slackWeight);
                                dvarWeightMap.put(surplusDvar, surplusWeight);
                            }
                        }
                    }
                }
            } else {
                // Only "LHS > RHS" statement exists
                if (bGTConstrainExists) {
                    // LHS > RHS CONSTRAIN
                    goal.caseExpression.add(lhsExpression + "=" + rhsExpression);
                } else  {
                    if (numberPenaltyGT == 0.0) {
                        // LHS > RHS PENALTY 0.0
                        goal.caseExpression.add(lhsExpression + ">" + rhsExpression);
                    } else {
                        // LHS > RHS PENALTY (non-zero value)
                        String surplusDvar = "surplus__" + goalName + "_1";
                        String weight = "-(" + penaltyGT + ")";
                        goal.caseExpression.add(lhsExpression + "-"+ surplusDvar + "=" + rhsExpression);
                        dvarSlackSurplusList.add(surplusDvar);
                        dvarWeightMap.put(surplusDvar,weight);
                    }
                }
            }
        } else {
            // Only "LHS < RHS" statement exists
            if (bLTExists) {
                if (bLTConstrainExists) {
                    // LHS < RHS CONSTRAIN
                    goal.caseExpression.add(lhsExpression + "=" + rhsExpression);
                }
                if (bLTPenaltyExists) {
                    if (numberPenaltyLT == 0.0) {
                        // LHS < RHS PENALTY 0.0
                        goal.caseExpression.add(lhsExpression + "<" + rhsExpression);
                    } else {
                        // LHS < RHS PENALTY (non-zero value)
                        String slackDvar = "slack__" + goalName + "_1";
                        String weight = "-(" + penaltyLT + ")";
                        goal.caseExpression.add(lhsExpression + "+"+ slackDvar + "=" + rhsExpression);
                        dvarSlackSurplusList.add(slackDvar);
                        dvarWeightMap.put(slackDvar,weight);
                    }
                }
            }
        }

        // Generate parser tree for case expression
        goal.caseExpressionParseTrees.add(getExpressionParseTree(goal.caseExpression.get(0)));

        // Add slack/surplus data to goal
        goal.dvarSlackSurplusList.add(dvarSlackSurplusList);
        goal.dvarWeightMapList.add(dvarWeightMap);

        return new VisitorResult(goal, null);
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
            throw new SyntaxErrorException(as.fromWresl, as.line, errorMessages);
        }
        else if (count == 1) {
            as.kind = visitorResultToString(visit(ctx.kind().get(0).specificationString()));
        }

        // Process UNITS keyword, if exists there must be only one
        count = ctx.units().size();
        if (count > 1) {
            errorMessages.add("There cannot be more than one UNITS keyword declared in an ALIAS statement!");
            throw new SyntaxErrorException(as.fromWresl, as.line, errorMessages);
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
            throw new SyntaxErrorException(this.currentFile, ts.line, errorMessages);
        }
        else {
            ts.kind = visitorResultToString(visit(ctx.kind().get(0).specificationString()));
        }

        // Process UNITS; check that one exists
        if (ctx.units().size() != 1) {
            errorMessages.add("There must be one and only one UNITS keyword in a TIMESERIES statement!");
            throw new SyntaxErrorException(this.currentFile, ts.line, errorMessages);
        }
        else {
            ts.units = visitorResultToString(visit(ctx.units().get(0).specificationString()));
        }

        // Process CONVERT; it is optional and if exists there should be only one
        if (!ctx.convert().isEmpty()) {
            if (ctx.convert().size() != 1) {
                errorMessages.add("There must be one and only one CONVERT keyword in a TIMESERIES statement!");
                throw new SyntaxErrorException(this.currentFile, ts.line, errorMessages);
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
                throw new SyntaxErrorException(ts.fromWresl, ts.line, errorMessages);
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
            throw new SyntaxErrorException(this.currentFile, ts.line, errorMessages);
        }
        else {
            ts.kind = visitorResultToString(visit(ctx.kind().get(0).specificationString()));
        }

        // Process UNITS; check that one exists
        if (ctx.units().size() != 1) {
            errorMessages.add("There must be one and only one UNITS keyword in a TIMESERIES statement!");
            throw new SyntaxErrorException(this.currentFile, ts.line, errorMessages);
        }
        else {
            ts.units = visitorResultToString(visit(ctx.units().get(0).specificationString()));
        }

        // Process CONVERT; it is optional and if exists there should be only one
        if (!ctx.convert().isEmpty()) {
            if (ctx.convert().size() != 1) {
                errorMessages.add("There must be one and only one CONVERT keyword in a TIMESERIES statement!");
                throw new SyntaxErrorException(this.currentFile, ts.line, errorMessages);
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
            // Check that number of arguments match the requirements of the function
            int nArguments = ctx.arguments().expression().size();
            String function = getWreslText(ctx.preDefinedFunction());
            switch (function) {
                case "abs", "real", "int", "exp", "log", "log10", "sqrt", "round"-> {
                    if (nArguments != 1) { throw new SyntaxErrorException(this.currentFile,ctx.OPEN_PAREN().getSymbol().getLine(), List.of(function.toUpperCase() + " function requires only 1 argument!")); }
                }
                case "pow", "mod" -> {
                    if (nArguments != 2) { throw new SyntaxErrorException(this.currentFile,ctx.OPEN_PAREN().getSymbol().getLine(), List.of(function.toUpperCase() + " requires 2 arguments!")); }
                }
                case "max", "min" -> {
                    if (nArguments < 2) { throw new SyntaxErrorException(this.currentFile,ctx.OPEN_PAREN().getSymbol().getLine(), List.of(function.toUpperCase() + " requires at least 2 arguments!")); }
                }
            }
            return new VisitorResult(null, null);
        }
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
            caseConditionTree = ctx.caseCondition().caseConditionExpression();
        }
        else {
            caseCondition = Param.always;
            caseConditionTree = null;
        }

        // Case expression and expression tree
        VisitorResult result = visit(ctx.caseBody());
        String caseExpression = visitorResultToString(result);
        ParseTree caseExpressionTree = ctx.caseBody();

        // Return data
        WRESL_CaseData caseData = new WRESL_CaseData(caseCondition,
                                                     caseConditionTree,
                                                     List.of(caseExpression),
                                                     List.of(caseExpressionTree),
                                 null,
                            null);
        return new VisitorResult(caseData,caseName);
    }

    @Override
    // caseViaValue
    public VisitorResult visitCaseViaValue(wreslParser.CaseViaValueContext ctx) {
        WRESL_String caseExpression = new WRESL_String(getWreslText(ctx.expression()));
        return new VisitorResult(caseExpression, null);
    }

//@Override
//// caseViaGoal
//public VisitorResult visitCaseViaGoal(wreslParser.CaseViaGoalContext ctx) {
//    List<String> caseExpressionList = new ArrayList<>();
//    List<String> caseSlackSurplusDvarList = new ArrayList<>();
//    List<String> caseSlackSurplusDvarWeightList = new ArrayList<>();

//    // Make sure RHS expression for goal is defined and retrieve expression
//    if (!getWreslText(ctx.SIDE()).equals("rhs")) {
//        String errorMessage = "Syntax error: RHS expression for goal must be defined, instead of LHS expression!";
//        throw new SyntaxErrorException(this.currentFile,ctx.SIDE().getSymbol().getLine(),errorMessage);
//    }
//    String rhs = getWreslText(ctx.expression());

//    // Loop through penalty statements, generate constraining expressions and slack/surplus dvars and their weights
//    for (wreslParser.PenaltyContext penalty : ctx.penalty()) {
//        // Make sure LHS and RHS keywords are defined on the proper side of the comparison operator
//        String errorMessage;
//        if (getWreslText(penalty.SIDE(0)).equals("rhs") || getWreslText(penalty.SIDE(1)).equals("lhs")) {
//            errorMessage = "LHS and RHS keywords in the PENALTY statement are reversed!";
//            throw new SyntaxErrorException(this.currentFile, penalty.SIDE(0).getSymbol().getLine(), errorMessage);
//        }

//        // Replace RHS keyword with expression
//        String rhsPenalty = getWreslText(penalty.SIDE(1)).replace("rhs", rhs);

//        // If LHS > RHS, create a surplus Dvar
//        if (!(penalty.GREATER_THAN() == null)) {

//        }
//    }

//    WRESL_CaseData caseData = new WRESL_CaseData(null, null, caseExpressionList, null, caseSlackSurplusDvarList, caseSlackSurplusDvarWeightList);
//    return new VisitorResult(caseData, null);
//}

    @Override
    // caseViaSelect
    public VisitorResult visitCaseViaSelect(wreslParser.CaseViaSelectContext ctx) {
        WRESL_String caseExpression = new WRESL_String(getWreslText(ctx.select()));
        return new VisitorResult(caseExpression, null);
    }

    @Override
    // caseViaExpression
    public VisitorResult visitCaseViaExpression(wreslParser.CaseViaExpressionContext ctx) {
        WRESL_String caseExpression = new WRESL_String(getWreslText(ctx.expression()));
        return new VisitorResult(caseExpression, null);
    }


    // ------------------------------------------------------------
    // --- IF STATEMENT (CAN ONLY USE SVARs DEFINED IN INITIAL STATEMENT)
    // ------------------------------------------------------------
    @Override
    // ifStatement
    public VisitorResult visitIfStatement(wreslParser.IfStatementContext ctx) {
        VisitorResult result;

        // Process first IF clause
        if (Evaluator.evaluateCondition(ctx.ifClause().expression(), this.tempParameterMap)) {
            return visit(ctx.ifClause().ifBlock());
        }

        // Process ELSE IF clauses
        for (int i=0; i<ctx.elseIfClause().size(); i++) {
            if (Evaluator.evaluateCondition(ctx.elseIfClause(i).expression(), this.tempParameterMap)) {
                return visit(ctx.elseIfClause(i).ifBlock());
            }
        }

        // If made it this far, check that ELSE clause exists, and return result
        if (ctx.elseClause() != null) {
            return visit(ctx.elseClause().ifBlock());
        }

        // If made this far, none of the IF statements were evaluated as true; return null
        return null;

    }

    @Override
    // ifBlock
    public VisitorResult visitIfBlock(wreslParser.IfBlockContext ctx) {
        List<VisitorResult> ifBlockContents = new ArrayList<>();
        List<VisitorResult> dataList;
        for (int i=1; i<ctx.getChildCount()-1; i++) {
            // Visit child
            VisitorResult result = visit(ctx.getChild(i));

            // Accumulate returned data into return variable
            // ... when only one data is returned
            if (result.children().size() == 0) {
                dataList = List.of(result);
            }
            // ... when multiple data are returned
            else {
                dataList = result.children();
            }
            ifBlockContents.addAll(dataList);
        }

        return new VisitorResult(null, null, ifBlockContents);
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


    // Generate a Expression parse tree froma string
    private wreslParser.ExpressionContext getExpressionParseTree(String expression) {
        CharStream charStream = CharStreams.fromString(expression);
        wreslLexer lexer = new wreslLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        wreslParser parser = new wreslParser(tokenStream);
        return parser.expression();
    }
}
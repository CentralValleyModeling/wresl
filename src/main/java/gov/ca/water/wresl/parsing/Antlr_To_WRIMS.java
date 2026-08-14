package gov.ca.water.wresl.parsing;

import gov.ca.water.io.DSS.DssOperations;
import gov.ca.water.utilities.Param;
import gov.ca.water.wresl.domain.*;
import gov.ca.water.wresl.errors.EvaluationErrorException;
import gov.ca.water.wresl.errors.SyntaxErrorException;
import gov.ca.water.wresl.grammar.wreslBaseVisitor;
import gov.ca.water.wresl.grammar.wreslParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

import static gov.ca.water.wresl.parsing.Utilities.*;

// Package-private class
class Antlr_To_WRIMS extends wreslBaseVisitor<VisitorResult> {
    private static final Logger log = LoggerFactory.getLogger(Antlr_To_WRIMS.class);
    // Main WRESL file, absolute folder that it resides, and list of WRESL files
    private Path mainFilePath;
    private Path absReferencePath;
    private Map<Path, WRESLFile> wreslFilesMap;

    // Containers (data defined under INITIAL will be stored as "parameters" under sds
    private Map<Integer,Sequence> sequenceData;
    private Map<String, ModelDataSet> modelsAndGroups;
    private StudyDataSet sds;

    // Scratch memory used for data that needs to be access by multiple methods
    private String currentFile;                                   // WRESL file that's being parsed
    private String currentModelOrGroupName = "";                  // Name of model or group that is currently being parsed
    private List<String> includeFileList;                         // List of include files refernced by a model

    // ------------------------------------------------------------
    // --- CONSTRUCTOR
    // ------------------------------------------------------------
    public Antlr_To_WRIMS(Path mainFilePath, Map<Path, WRESLFile> wreslFilesMap) {
        this.mainFilePath = mainFilePath;
        this.absReferencePath = mainFilePath.getParent();
        this.wreslFilesMap = wreslFilesMap;

        this.sequenceData = new HashMap<>();
        this.modelsAndGroups = new HashMap<>();
        this.sds = new StudyDataSet();
    }




    // ------------------------------------------------------------
    // ------------------------------------------------------------
    // --- VISITOR METHODS
    // ------------------------------------------------------------
    // ------------------------------------------------------------

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
        }

        // Process SEQUENCE data, compile ordered modelList and related data in StudyDataSet
        List<String> modelList = new ArrayList<>();
        List<String> modelConditionList = new ArrayList<>();
        List<ParseTree> modelConditionParseTreeList = new ArrayList<>();
        Map<String, ModelDataSet> modelDataSetMap = new HashMap<>();
        for (int i=0; i<this.sequenceData.size(); i++) {
            Sequence sq = this.sequenceData.get(i+1);
            String modelName = sq.modelName;
            // Check that a model is not refernced in multiple SEQUENCEs
            if (modelList.contains(modelName)) {
                throw new EvaluationErrorException("Each SEQUENCE must define a unique model. Model '" + modelName + "' is used in multiple SEQUENCEs!");
            }
            ModelDataSet mds = this.modelsAndGroups.get(modelName);
            mds.setTimeStep(sq.timeStep);
            modelList.add(modelName);
            modelDataSetMap.put(modelName, mds);
            modelConditionList.add(sq.condition);
            modelConditionParseTreeList.add(sq.conditionParseTree);
        }
        this.sds.setModelList(modelList);
        this.sds.setModelConditionList(modelConditionList);
        this.sds.setModelConditionParseTrees(modelConditionParseTreeList);
        this.sds.setModelDataSetMap(modelDataSetMap);

        // Loop through models and process data, check for errors
        Map<String, Timeseries> svTSMap = new HashMap<>();
        for (String modelName : this.sds.getModelList()) {
            ModelDataSet mds = modelDataSetMap.get(modelName);

            // Compile SV timeseries map and timseries timesteps
            String modelTimeStep = mds.getTimeStep();
            mds.tsMap_Temp.forEach((tsName, ts) -> {
                ts.setTimeStep(modelTimeStep);
                String svTSName = DssOperations.entryNameTS(tsName, modelTimeStep);
                svTSMap.put(svTSName, ts);
            });
            mds.clearTempTSMap();  // Clear memory for the temporary Timeseries map that we just utilized and now are done with

            // Evaluate weights when possible (i.e. when they don't depend on some dynamic value such a taf-cfs)
            for (String weightName : mds.wtList) {
                WeightElement weight = mds.wtMap.get(weightName);
                try {
                    // Evaluate weight value and set parse tree to null to indicate this value is already evaluated
                    weight.value = Evaluator.evaluateExpression(0, 0, 0, weight.weightParseTree).getValue().doubleValue();
                    weight.weightParseTree = null;
                } catch (EvaluationErrorException | NullPointerException e) {
                    // Do nothing at this point since this error is likely due to a dynamic variable within the expression
                }
            }

            // Convert ALIASes referenced in GOALs, and other ALIASes referenced from these ALIASes, to DVARs and GOALs
            mds = convertAliasToGoal(mds);
        }
        this.sds.setSVTimeseriesMap(svTSMap);


        // Check for duplicate Dvars within each model

        // Check for duplicate Svars within each model

        // Check for duplicate Goals within each model

        // Check for duplicate aliases within each model

        // Check for duplicate External within each model

        // Check for duplicate Timeseries within each model

        // Check for duplicate weight tables within each model


        // Clear scratch memory that is no longer needed
        clearMemory();

        return new VisitorResult(this.sds);
    }

    @Override
    // ROOT VISITOR FOR INCLUDE FILES
    public VisitorResult visitIncludeStart(wreslParser.IncludeStartContext ctx) {
        // Array to store multiple return data
        List<WRESLComponent> compiledData = new ArrayList<>();

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
            compiledData.addAll(result.data());

        }

        // Return compiled data
        return new VisitorResult(compiledData);
    }


    // ------------------------------------------------------------
    // --- VISITORS TO COMPILE CONTAINERS
    // ------------------------------------------------------------
    @Override
    // INITIAL; Svars listed under INITIAL statement are stored as parameters in StudyDataSet
    public VisitorResult visitInitial(wreslParser.InitialContext ctx) {
        // Loop through children; they should all be SVARs
        for (int i = 0; i <= ctx.children.size() - 1; i++) {
            // Skip anything that is not Svar definition
            if (!(ctx.getChild(i) instanceof wreslParser.SvarContext svarCtx)) {
                continue;
            }

            VisitorResult result = visit(ctx.getChild(i));

            // WRESL component can only be an SVAR as dictated by the grammar
            Svar parameter = (Svar) result.data().get(0);
            this.sds.addParameter(parameter);
        }

        // Process parameters (Svars)
        Evaluator.processSvars(this.sds, this.sds.getParameterList(), this.sds.getParameterMap(), false);

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

        // Condition, if exists; create parse tree based on lower case of condition
        if (sqBodyCtx.sequenceCondition() != null) {
             sq.condition = getWreslText(sqBodyCtx.sequenceCondition().expression());
             sq.conditionParseTree = generateExpressionParseTree(sq.condition);
        }

        // Timestep, if exists
        if (sqBodyCtx.timestepSpecification() == null) {
            // Default timestep = 1MON
            String timeStep = wreslParser.VOCABULARY.getLiteralName(wreslParser.STEP_1MON);
            sq.timeStep = timeStep.replace("'","").toLowerCase();
        } else {
            sq.timeStep = getWreslText(sqBodyCtx.timestepSpecification().getChild(1));
        }

        // Add sequence to map
        this.sequenceData.put(Integer.valueOf(sq.order),sq);

        return null;
    }

    @Override
    // MODEL
    public VisitorResult visitModel(wreslParser.ModelContext ctx) throws EvaluationErrorException, SyntaxErrorException {
        ModelDataSet mds = new ModelDataSet();
        this.currentModelOrGroupName = getWreslText(ctx.OBJECT_NAME());

        // Name, WRESL file and line
        mds.name = this.currentModelOrGroupName;
        mds.fromWresl = this.currentFile;
        mds.line = ctx.OBJECT_NAME().getSymbol().getLine();

        // On entry: store file referiing to Model context
        String parentFile = this.currentFile;

        // Instantiate the list of include files
        this.includeFileList = new ArrayList<>();

        // Check that model is not defined more than once
        if (this.modelsAndGroups.get(this.currentModelOrGroupName) != null) {
            throw new EvaluationErrorException(this.currentFile, ctx.OBJECT_NAME().getSymbol().getLine(), "Model " + this.currentModelOrGroupName + " is defined more than once!");
        }

        // Visit modelBody
        for (int i = 0; i < ctx.modelBody().size(); i++) {
            VisitorResult result = visit(ctx.modelBody(i));

            // Continue if returnedData is null, for instance, after evaluation of an IF statement
            if (result == null) { continue; }

            // Copy returned data into ModelDataSet
            for (int j = 0; j <= result.data().size() - 1; j++) {
                WRESLComponent data = result.data().get(j);
                String name = data.name;
                switch (data) {
                    case Svar svar -> {
                        // Make sure svar is not defined as a parameter
                        if (this.sds.getParameter(name) != null) {
                            throw new SyntaxErrorException(svar.fromWresl, svar.line, "Svar '"+name+"' is previously defined as an initial parameter!");
                        }
                        // Make sure svar is not defined more than once
                        if (mds.svList.contains(name)) {
                            throw new SyntaxErrorException(svar.fromWresl, svar.line, "Svar '"+name+"' is defined more than once in model '"+mds.name+"'!");
                        }
                        mds.svList.add(name);
                        mds.svMap.put(name, svar);
                    }
                    case Dvar dvar -> {
                        // Make sure dvar is not defined more than once
                        if (mds.dvMap.containsKey(name)) {
                            throw new SyntaxErrorException(dvar.fromWresl, dvar.line, "Dvar '"+name+"' is defined more than once in model '"+mds.name+"'!");
                        }
                        mds.dvList.add(name);
                        mds.dvMap.put(name, dvar);
                    }
                    case WeightElement weight -> {
                        mds.wtList.add(name);
                        mds.wtMap.put(name, weight);
                    }
                    case Goal goal -> {
                        // Make sure goal is not defined more than once
                        if (mds.gList.contains(name)) {
                            throw new SyntaxErrorException(goal.fromWresl, goal.line, "Goal '"+name+"' is defined more than once in model '"+mds.name+"'!");
                        }
                        mds.gList.add(name);
                        mds.gMap.put(name, goal);
                    }
                    case Alias alias -> {
                        // Make sure alias is not defined more than once
                        if (mds.asList.contains(name)) {
                            throw new SyntaxErrorException(alias.fromWresl, alias.line, "Alias '"+name+"' is defined more than once in model '"+mds.name+"'!");
                        }
                        mds.asList.add(name);
                        mds.asMap.put(name, alias);
                    }
                    case Timeseries ts -> {
                        // Make sure ts is not defined more than once
                        if (mds.tsMap_Temp.containsKey(name)) {
                            throw new SyntaxErrorException(ts.fromWresl, ts.line, "Timeseries '"+name+"' is defined more than once in model '"+mds.name+"'!");
                        }
                        mds.tsMap_Temp.put(name, ts);
                    }
                    case External external -> {
                        mds.exList.add(name);
                        mds.exMap.put(name, external);
                    }
                    case WRESL_String includedGroupModel -> {
                        String incGroupModel = includedGroupModel.text;
                        // Make sure included group or model is already defined
                        if (!this.modelsAndGroups.containsKey(incGroupModel)) {
                            throw new EvaluationErrorException("Model/group " + incGroupModel + " referenced from model " + mds.name + " is not defined!");
                        }
                        // Insert model/group data into referencing model
                        ModelDataSet mdsIncluded = this.modelsAndGroups.get(incGroupModel);
                        mds.appendModelDataSet(mdsIncluded);
                    }
                    default -> {
                        throw new EvaluationErrorException("Error in processing WRESL data for model " + this.currentModelOrGroupName);
                    }
                }
            }
        }

        // Store list of include files refernced by the model
        mds.incFileList = this.includeFileList;

        // Store the data for the model
        this.modelsAndGroups.put(this.currentModelOrGroupName, mds);

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

        // Instantiate list of files refernced by group
        this.includeFileList = new ArrayList<>();

        // Visit groupBody
        for (int i = 0; i <= ctx.groupBody().size()-1; i++) {
            VisitorResult result = visit(ctx.groupBody(i));

            // Continue if null value is returned as VisitorResult (such as the result of an IF statement)
            if (result == null) { continue; }

            // Copy returned data into ModelDataSet
            for (int j=0; j<=result.data().size()-1; j++) {
                WRESLComponent data = result.data().get(j);
                String name = data.name;
                switch (data) {
                    case Svar svar -> {
                        mds.svList.add(name);
                        mds.svMap.put(name,svar);
                    }
                    case Dvar dvar -> {
                        mds.dvList.add(name);
                        mds.dvMap.put(name, dvar);
                    }
                    case WeightElement weight -> {
                        mds.wtList.add(name);
                        mds.wtMap.put(name, weight);
                    }
                    case Goal goal -> {
                        mds.gList.add(name);
                        mds.gMap.put(name, goal);
                    }
                    case Alias alias    -> {
                        mds.asList.add(name);
                        mds.asMap.put(name, alias);
                    }
                    case Timeseries ts -> {
                        // Make sure ts is not defined more than once
                        if (mds.tsMap_Temp.containsKey(name)) {
                            throw new SyntaxErrorException(ts.fromWresl, ts.line, "Timeseries '"+name+"' is defined more than once in group '"+mds.name+"'!");
                        }
                        mds.tsMap_Temp.put(name, ts);
                    }
                    case External external -> {
                        mds.exList.add(name);
                        mds.exMap.put(name, external);
                    }
                    default -> {
                        throw new EvaluationErrorException("Error in processing WRESL data for group " + this.currentModelOrGroupName);
                    }
                }
            }
        }

        // Store the list of refrenced files from group
        mds.incFileList = this.includeFileList;

        // Store the data for the model
        this.modelsAndGroups.put(this.currentModelOrGroupName, mds);

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

        // Set current file to include file to be used by its children; also add it to the list of files refernced by a model
        this.currentFile = includeFilePath.toString().toLowerCase();
        if (this.currentFile != null) { this.includeFileList.add(this.currentFile); }

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

        // Return the name of the included model to be appended to the model being compiled
        return new VisitorResult(new WRESL_String(includedModel));
    }

    @Override
    public VisitorResult visitIncludeGroup(wreslParser.IncludeGroupContext ctx) {
        // Included model name
        String includedModel = getWreslText(ctx.groupReference().OBJECT_NAME());

        // Return the name of the included group to be appended to the model being compiled
        return new VisitorResult(new WRESL_String(includedModel));
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
        if (result.data().get(0) instanceof Svar tempSvar) {
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
            svar.timeArraySizeParseTree = generateExpressionParseTree(svar.timeArraySize);
        }

        // Return data
        return new VisitorResult(svar);
    }

    @Override
    // svarCase
    public VisitorResult visitSvarCase(wreslParser.SvarCaseContext ctx) {
        Svar svar = new Svar();

        // Loop over case statements and case information
        for (int i=0; i<=ctx.getChildCount()-1; i++) {
            VisitorResult result = visit(ctx.getChild(i));
            WRESL_CaseData caseData = (WRESL_CaseData)result.data().get(0);
            svar.addCaseData(caseData.name.toLowerCase(),
                             caseData.caseCondition,
                             caseData.caseExpressionList.get(0),
                             caseData.caseConditionTree,
                             caseData.caseExpressionTreeList.get(0));
        }

        // Return data
        return new VisitorResult(svar);

    }

    @Override
    // svarLookup
    public VisitorResult visitSvarLookup(wreslParser.SvarLookupContext ctx) {
        Svar svar = new Svar();

        // Set case condition
        svar.addCaseData(Param.defaultCaseName, Param.always, getWreslText(ctx.select()).toLowerCase(),null,ctx.select());

        // Return data
        return new VisitorResult(svar);
    }

    @Override
    // svarSum
    public VisitorResult visitSvarSum(wreslParser.SvarSumContext ctx) {
        Svar svar = new Svar();

        // Set case condition
        svar.addCaseData(Param.defaultCaseName, Param.always, getWreslText(ctx.sumExpressionBody()), null,ctx.sumExpressionBody());

        // Return data
        return new VisitorResult(svar);
    }

    @Override
    // svarValue
    public VisitorResult visitSvarValue(wreslParser.SvarValueContext ctx) {
        Svar svar = new Svar();

        // Set case condition
        svar.addCaseData(Param.defaultCaseName, Param.always, getWreslText(ctx.expression()), null, ctx.expression());

        // Return data
        return new VisitorResult(svar);
    }


    // ------------------------------------------------------------
    // --- DVAR
    // ------------------------------------------------------------
    @Override
    // Gateway for DVAR visitors
    public VisitorResult visitDvar(wreslParser.DvarContext ctx) {
        Dvar dvar = new Dvar();
        String errorMessage;

        // Integer dvar, and bounds
        WRESLComponent data = visit(ctx.defineBoundLimits()).data().get(0);
        dvar = (Dvar)data;

        // Dvar name
        dvar.name = getWreslText(ctx.OBJECT_NAME());

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
        return new VisitorResult(dvar);
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
            return new VisitorResult(dvar);
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
                dvar.lowerBoundExpressionParseTree = generateExpressionParseTree(dvar.lowerBound);

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
                dvar.upperBoundExpressionParseTree = generateExpressionParseTree(dvar.upperBound);

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
        return new VisitorResult(dvar);
    }


    // ------------------------------------------------------------
    // --- GOAL
    // ------------------------------------------------------------
    @Override
    // Gateway to GOAL-related visitor methods
    //   Returns a list of VisitorResult types that include a goal, slack/surplus dvars and
    //   associated weights.
    public VisitorResult visitGoal(wreslParser.GoalContext ctx) {
        // Visit goal body; expects a list of WRESLComponent types that include a goal,
        //    list of dvars (slack and surplus variables) and corresponding weight
        //    elements. First entry is the goal, then dvar and corresponding weight,
        //    repeating as many dvars as available
        VisitorResult result = visit(ctx.goalBody());
        List<WRESLComponent> resultList = new ArrayList<>(result.data());
        Goal goal = (Goal) resultList.get(0);

        // Goal name
        goal.name = getWreslText(ctx.OBJECT_NAME());

        // Source filename and line number
        goal.fromWresl = this.currentFile;
        goal.line = ctx.OBJECT_NAME().getSymbol().getLine();

        // Update goal in the result
        resultList.set(0, goal);

        // Return updated result
        return new VisitorResult(resultList);
    }

    @Override
    // goalShortForm
    public VisitorResult visitGoalShortForm(wreslParser.GoalShortFormContext ctx) {
        Goal goal = new Goal();

        // Set case related stuff
        goal.caseName.add(Param.defaultCaseName);
        goal.caseCondition.add(Param.always);
        goal.goalExpression.add(getWreslText(ctx));
        goal.goalExpressionParseTrees.add(generateGoalBodyParseTree(goal.goalExpression.get(0)));

        return new VisitorResult(goal);
    }

    @Override
    // goalViaCase
    public VisitorResult visitGoalViaCase(wreslParser.GoalViaCaseContext ctx) {
        Goal goal = new Goal();
        List<Dvar> dvarSlackSurplusListForGoal = new ArrayList<>();
        List<WeightElement> weightSlackSurplusListForGoal = new ArrayList<>();

        // Loop through case statements
        for (int i=0; i<ctx.goalCaseStatement().size(); i++) {
            VisitorResult result = visit(ctx.goalCaseStatement(i)); // Returns a list of VisitosResults with caseData as first entry and surplus/slack dvars and associated weights
            WRESL_CaseData caseData = (WRESL_CaseData) result.data().get(0);

            // Copy dvars and weights from result of visiting case statement
            List<Dvar> dvarSlackSurplusListForCase = new ArrayList<>();
            List<WeightElement> weightSlackSurplusListForCase = new ArrayList<>();
            for (int j=1; j<result.data().size(); j+= 2) {
                dvarSlackSurplusListForCase.add((Dvar) result.data().get(j));
                weightSlackSurplusListForCase.add((WeightElement) result.data().get(j+1));
            }

            // Update counters for surplus/slack variables
            for (int j=0; j<dvarSlackSurplusListForCase.size(); j++) {
                Dvar dvarUpdate = dvarSlackSurplusListForCase.get(j);
                WeightElement weightUpdate = weightSlackSurplusListForCase.get(j);
                String slackSurplusDvarName = dvarUpdate.name;
                String dvarName = slackSurplusDvarName.substring(0,slackSurplusDvarName.lastIndexOf("_")+1) + (i+1);
                String tempCaseExpression = caseData.caseExpressionList.get(0).replace(slackSurplusDvarName,dvarName);
                caseData.caseExpressionList.set(0, tempCaseExpression);
                caseData.caseExpressionTreeList.set(0, generateGoalBodyParseTree(tempCaseExpression));
                dvarUpdate.name = dvarName;
                dvarSlackSurplusListForGoal.add(dvarUpdate);
                weightUpdate.name = dvarName;
                weightSlackSurplusListForGoal.add(weightUpdate);
            }

            // Copy data from caseData into goal
            goal.caseName.add(caseData.name);
            goal.caseCondition.add(caseData.caseCondition);
            goal.caseConditionParseTrees.add(caseData.caseConditionTree);
            goal.goalExpression.addAll(caseData.caseExpressionList);
            goal.goalExpressionParseTrees.addAll(caseData.caseExpressionTreeList);
        }

        // Copile goal, slack/surplus dvars and weights into a list and return
        List<WRESLComponent> returnData = new ArrayList<>(List.of(goal));
        for (int i=0; i<dvarSlackSurplusListForGoal.size(); i++) {
            returnData.add(dvarSlackSurplusListForGoal.get(i));
            returnData.add(weightSlackSurplusListForGoal.get(i));
        }
        return new VisitorResult(returnData);
    }

    @Override
    // goalViaPenalty
    public VisitorResult visitGoalViaPenalty(wreslParser.GoalViaPenaltyContext ctx) {
        Goal goal = new Goal();

        // Penalty related data
        VisitorResult result = null;
        if (ctx.goalPenalties() != null) {
            result = visit(ctx.goalPenalties());
            WRESL_CaseData caseData = (WRESL_CaseData) result.data().get(0);
            goal.goalExpression.addAll(caseData.caseExpressionList);
            goal.goalExpressionParseTrees.addAll(caseData.caseExpressionTreeList);
        } else {
            // Retrieve LHS and RHS expressions
            String lhsExpression = getWreslText(ctx.expression(0));
            String rhsExpression = getWreslText(ctx.expression(1));
            String caseExpression = lhsExpression + "=" + rhsExpression;
            goal.goalExpression.add(caseExpression);
            goal.goalExpressionParseTrees.add(generateGoalBodyParseTree(caseExpression));
        }

        // Default case name and condition
        goal.caseName.add(Param.defaultCaseName);
        goal.caseCondition.add(Param.always);

        // Combine goal with slack surplus davr and associated weights from visiting the penalties
        List<WRESLComponent> returnData = new ArrayList<>(List.of(goal));
        if (result != null) {
            List<WRESLComponent> slackSurplusDvarWeightList = result.data();
            slackSurplusDvarWeightList.remove(0);
            returnData.addAll(slackSurplusDvarWeightList);
        }

        // Return data
        return new VisitorResult(returnData);
    }

    @Override
    // goalCaseStatement
    public VisitorResult visitGoalCaseStatement(wreslParser.GoalCaseStatementContext ctx) {
        List<String> caseExpressionList = new ArrayList<>();
        List<ParseTree> caseExpressionTreeList = new ArrayList<>();

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
        List<WRESLComponent> slackSurplusDvarWeights = null;
        if (ctx.goalPenalties() != null) {
            VisitorResult result = visit(ctx.goalPenalties());
            WRESL_CaseData caseData = (WRESL_CaseData) result.data().get(0);
            caseExpressionList.addAll(caseData.caseExpressionList);
            caseExpressionTreeList.addAll(caseData.caseExpressionTreeList);
            slackSurplusDvarWeights = result.data();
            slackSurplusDvarWeights.remove(0);
        } else {
            // Walk back up the parse tree and retrieve goal name and LHS expression
            ParserRuleContext goalCtx = ctx.getParent().getParent().getParent();
            wreslParser.GoalContext goal = (wreslParser.GoalContext) goalCtx;  // This is coded to fail if grammar is changed for GOAL statement so we can catch the issue quickly
            String lhsExpression = getWreslText(goal.goalBody().goalViaCase().expression());
            String rhsExpression = getWreslText(ctx.expression());
            String caseExpression = lhsExpression + "=" + rhsExpression;
            caseExpressionList.add(caseExpression);
            caseExpressionTreeList.add(generateGoalBodyParseTree(caseExpression));
        }

        // Assemble case data
        WRESL_CaseData caseData = new WRESL_CaseData(caseCondition,
                                                     caseConditionTree,
                                                     caseExpressionList,
                                                     caseExpressionTreeList);
        caseData.name = getWreslText(ctx.goalCaseName());

        // Combine case data with slack/surplus dvars and associated weights that were returned from visiting the penalties
        List<WRESLComponent> returnData = new ArrayList<>(List.of(caseData));
        if (slackSurplusDvarWeights != null) { returnData.addAll(slackSurplusDvarWeights); }

        // Return list of data
        return new VisitorResult(returnData);
    }

    @Override
    // penalties; returns a list of VistorResults with first entry being a CaseData and others dslack/surplus dvars and weights
    // Note: If surplus/slack variables are genereted, their count numbers
    //       will need to be updated in the calling method since we can't
    //       determine the count in this method
    public VisitorResult visitGoalPenalties(wreslParser.GoalPenaltiesContext ctx) {
        WRESL_CaseData caseData = new WRESL_CaseData();
        List<WeightElement> weightSlackSurplusList = new ArrayList<>();
        List<Dvar> dvarSlackSurplusList = new ArrayList<>();

        // Flags describing which constraints are defined and how
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
                        caseData.caseExpressionList.add(lhsExpression + "=" + rhsExpression);
                    } else {
                        if (numberPenaltyLT == 0.0) {
                            // LHS > RHS CONSTRAIN
                            // LHS < RHS PENALTY 0
                            caseData.caseExpressionList.add(lhsExpression + "<" + rhsExpression);
                        } else {
                            // LHS > RHS CONSTRAIN
                            // LHS < RHS PENALTY (non-zero value)
                            String slackDvar = "slack__" + goalName + "_1";
                            String weight = "-(" + penaltyLT + ")";
                            caseData.caseExpressionList.add(lhsExpression + "+"+ slackDvar + "=" + rhsExpression);
                            Dvar dvar = new Dvar();
                            dvar.name = slackDvar;
                            dvar.kind = "slack";
                            dvar.lowerBound = Param.zero;
                            dvar.upperBound = Param.upper_unbounded;
                            dvar.condition = "conditional";
                            dvar.fromWresl = this.currentFile;
                            dvar.line = ctx.penaltyLT().penaltyValue().PENALTY().getSymbol().getLine();
                            dvarSlackSurplusList.add(dvar);
                            WeightElement weightElem = new WeightElement();
                            weightElem.name = dvar.name;
                            weightElem.weight = weight;
                            weightElem.weightParseTree = generateExpressionParseTree(weight);
                            weightElem.condition = "conditional";
                            weightElem.timeArraySize = Param.zero;
                            weightElem.timeArraySizeParseTree = generateExpressionParseTree(Param.zero);
                            weightElem.fromWresl = this.currentFile;
                            weightElem.line = dvar.line;
                            weightSlackSurplusList.add(weightElem);
                        }
                    }
                } else {
                    if (numberPenaltyGT == 0.0) {
                        if (bLTConstrainExists) {
                            // LHS > RHS PENALTY 0
                            // LHS < RHS CONSTRAIN
                            caseData.caseExpressionList.add(lhsExpression + ">" + rhsExpression);
                        } else {
                            if (numberPenaltyLT == 0.0) {
                                // LHS > RHS PENALTY 0
                                // LHS < RHS PENALTY 0
                                caseData.caseExpressionList.add("1 > 0"); // Copied from WRIMS2
                            } else {
                                // LHS > RHS PENALTY 0
                                // LHS < RHS PENALTY (non-zero value)
                                String slackDvar = "slack__" + goalName + "_1";
                                String weight = "-(" + penaltyLT + ")";
                                caseData.caseExpressionList.add(lhsExpression + "+" + slackDvar + ">" + rhsExpression);
                                Dvar dvar = new Dvar();
                                dvar.name = slackDvar;
                                dvar.kind = "slack";
                                dvar.lowerBound = Param.zero;
                                dvar.upperBound = Param.upper_unbounded;
                                dvar.condition = "conditional";
                                dvar.fromWresl = this.currentFile;
                                dvar.line = ctx.penaltyLT().penaltyValue().PENALTY().getSymbol().getLine();
                                dvarSlackSurplusList.add(dvar);
                                WeightElement weightElem = new WeightElement();
                                weightElem.name = dvar.name;
                                weightElem.weight = weight;
                                weightElem.weightParseTree = generateExpressionParseTree(weight);
                                weightElem.condition = "conditional";
                                weightElem.timeArraySize = Param.zero;
                                weightElem.timeArraySizeParseTree = generateExpressionParseTree(Param.zero);
                                weightElem.fromWresl = this.currentFile;
                                weightElem.line = dvar.line;
                                weightSlackSurplusList.add(weightElem);
                            }
                        }
                    } else {
                        if (bLTConstrainExists) {
                            // LHS > RHS PENALTY (non-zero value)
                            // LHS < RHS CONSTRAIN
                            String surplusDvar = "surplus__" + goalName + "_1";
                            String weight = "-(" + penaltyGT + ")";
                            caseData.caseExpressionList.add(lhsExpression + "-" + surplusDvar + "=" + rhsExpression);
                            Dvar dvar = new Dvar();
                            dvar.name = surplusDvar;
                            dvar.kind = "surplus";
                            dvar.lowerBound = Param.zero;
                            dvar.upperBound = Param.upper_unbounded;
                            dvar.condition = "conditional";
                            dvar.fromWresl = this.currentFile;
                            dvar.line = ctx.penaltyGT().penaltyValue().PENALTY().getSymbol().getLine();
                            dvarSlackSurplusList.add(dvar);
                            WeightElement weightElem = new WeightElement();
                            weightElem.name = dvar.name;
                            weightElem.weight = weight;
                            weightElem.weightParseTree = generateExpressionParseTree(weight);
                            weightElem.condition = "conditional";
                            weightElem.timeArraySize = Param.zero;
                            weightElem.timeArraySizeParseTree = generateExpressionParseTree(Param.zero);
                            weightElem.fromWresl = this.currentFile;
                            weightElem.line = dvar.line;
                            weightSlackSurplusList.add(weightElem);
                        } else {
                            if (numberPenaltyLT == 0.0) {
                                // LHS > RHS PENALTY (non-zero value)
                                // LHS < RHS PENALTY 0
                                String surplusDvar = "surplus__" + goalName + "_1";
                                String weight = "-(" + penaltyGT + ")";
                                caseData.caseExpressionList.add(lhsExpression + "-" + surplusDvar + "<" + rhsExpression);
                                Dvar dvar = new Dvar();
                                dvar.name = surplusDvar;
                                dvar.kind = "surplus";
                                dvar.lowerBound = Param.zero;
                                dvar.upperBound = Param.upper_unbounded;
                                dvar.condition = "conditional";
                                dvar.fromWresl = this.currentFile;
                                dvar.line = ctx.penaltyGT().penaltyValue().PENALTY().getSymbol().getLine();
                                dvarSlackSurplusList.add(dvar);
                                WeightElement weightElem = new WeightElement();
                                weightElem.name = dvar.name;
                                weightElem.weight = weight;
                                weightElem.weightParseTree = generateExpressionParseTree(weight);
                                weightElem.condition = "conditional";
                                weightElem.timeArraySize = Param.zero;
                                weightElem.timeArraySizeParseTree = generateExpressionParseTree(Param.zero);
                                weightElem.fromWresl = this.currentFile;
                                weightElem.line = dvar.line;
                                weightSlackSurplusList.add(weightElem);
                            } else {
                                // LHS > RHS PENALTY (non-zero value)
                                // LHS < RHS PENALTY (non-zero value)
                                String surplusDvar = "surplus__" + goalName + "_1";
                                String surplusWeight = "-(" + penaltyGT + ")";
                                String slackDvar = "slack__" + goalName + "_1";
                                String slackWeight = "-(" + penaltyLT + ")";
                                caseData.caseExpressionList.add(lhsExpression + "-" + surplusDvar + "+" + slackDvar + "=" + rhsExpression);
                                // dvar and weight for surplus
                                Dvar dvar = new Dvar();
                                dvar.name = surplusDvar;
                                dvar.kind = "surplus";
                                dvar.lowerBound = Param.zero;
                                dvar.upperBound = Param.upper_unbounded;
                                dvar.condition = "conditional";
                                dvar.fromWresl = this.currentFile;
                                dvar.line = ctx.penaltyGT().penaltyValue().PENALTY().getSymbol().getLine();
                                dvarSlackSurplusList.add(dvar);
                                WeightElement weightElem = new WeightElement();
                                weightElem.name = dvar.name;
                                weightElem.weight = surplusWeight;
                                weightElem.weightParseTree = generateExpressionParseTree(surplusWeight);
                                weightElem.condition = "conditional";
                                weightElem.timeArraySize = Param.zero;
                                weightElem.timeArraySizeParseTree = generateExpressionParseTree(Param.zero);
                                weightElem.fromWresl = this.currentFile;
                                weightElem.line = dvar.line;
                                weightSlackSurplusList.add(weightElem);
                                // dvar and weight for slack
                                dvar = new Dvar();
                                dvar.name = slackDvar;
                                dvar.kind = "slack";
                                dvar.lowerBound = Param.zero;
                                dvar.upperBound = Param.upper_unbounded;
                                dvar.condition = "conditional";
                                dvar.fromWresl = this.currentFile;
                                dvar.line = ctx.penaltyLT().penaltyValue().PENALTY().getSymbol().getLine();
                                dvarSlackSurplusList.add(dvar);
                                weightElem = new WeightElement();
                                weightElem.name = dvar.name;
                                weightElem.weight = slackWeight;
                                weightElem.weightParseTree = generateExpressionParseTree(slackWeight);
                                weightElem.condition = "conditional";
                                weightElem.timeArraySize = Param.zero;
                                weightElem.timeArraySizeParseTree = generateExpressionParseTree(Param.zero);
                                weightElem.fromWresl = this.currentFile;
                                weightElem.line = dvar.line;
                                weightSlackSurplusList.add(weightElem);
                            }
                        }
                    }
                }
            } else {
                // Only "LHS > RHS" statement exists
                if (bGTConstrainExists) {
                    // LHS > RHS CONSTRAIN
                    caseData.caseExpressionList.add(lhsExpression + "=" + rhsExpression);
                } else  {
                    if (numberPenaltyGT == 0.0) {
                        // LHS > RHS PENALTY 0.0
                        caseData.caseExpressionList.add(lhsExpression + ">" + rhsExpression);
                    } else {
                        // LHS > RHS PENALTY (non-zero value)
                        String surplusDvar = "surplus__" + goalName + "_1";
                        String weight = "-(" + penaltyGT + ")";
                        caseData.caseExpressionList.add(lhsExpression + "-"+ surplusDvar + "=" + rhsExpression);
                        Dvar dvar = new Dvar();
                        dvar.name = surplusDvar;
                        dvar.kind = "surplus";
                        dvar.lowerBound = Param.zero;
                        dvar.upperBound = Param.upper_unbounded;
                        dvar.condition = "conditional";
                        dvar.fromWresl = this.currentFile;
                        dvar.line = ctx.penaltyGT().penaltyValue().PENALTY().getSymbol().getLine();
                        dvarSlackSurplusList.add(dvar);
                        WeightElement weightElem = new WeightElement();
                        weightElem.name = dvar.name;
                        weightElem.weight = weight;
                        weightElem.weightParseTree = generateExpressionParseTree(weight);
                        weightElem.condition = "conditional";
                        weightElem.timeArraySize = Param.zero;
                        weightElem.timeArraySizeParseTree = generateExpressionParseTree(Param.zero);
                        weightElem.fromWresl = this.currentFile;
                        weightElem.line = dvar.line;
                        weightSlackSurplusList.add(weightElem);
                    }
                }
            }
        } else {
            // Only "LHS < RHS" statement exists
            if (bLTExists) {
                if (bLTConstrainExists) {
                    // LHS < RHS CONSTRAIN
                    caseData.caseExpressionList.add(lhsExpression + "=" + rhsExpression);
                }
                if (bLTPenaltyExists) {
                    if (numberPenaltyLT == 0.0) {
                        // LHS < RHS PENALTY 0.0
                        caseData.caseExpressionList.add(lhsExpression + "<" + rhsExpression);
                    } else {
                        // LHS < RHS PENALTY (non-zero value)
                        String slackDvar = "slack__" + goalName + "_1";
                        String weight = "-(" + penaltyLT + ")";
                        caseData.caseExpressionList.add(lhsExpression + "+"+ slackDvar + "=" + rhsExpression);
                        Dvar dvar = new Dvar();
                        dvar.name = slackDvar;
                        dvar.kind = "slack";
                        dvar.lowerBound = Param.zero;
                        dvar.upperBound = Param.upper_unbounded;
                        dvar.condition = "conditional";
                        dvar.fromWresl = this.currentFile;
                        dvar.line = ctx.penaltyLT().penaltyValue().PENALTY().getSymbol().getLine();
                        dvarSlackSurplusList.add(dvar);
                        WeightElement weightElem = new WeightElement();
                        weightElem.name = dvar.name;
                        weightElem.weight = weight;
                        weightElem.weightParseTree = generateExpressionParseTree(weight);
                        weightElem.condition = "conditional";
                        weightElem.timeArraySize = Param.zero;
                        weightElem.timeArraySizeParseTree = generateExpressionParseTree(Param.zero);
                        weightElem.fromWresl = this.currentFile;
                        weightElem.line = dvar.line;
                        weightSlackSurplusList.add(weightElem);
                    }
                }
            }
        }

        // Generate parser tree for case expression
        caseData.caseExpressionTreeList.add(generateGoalBodyParseTree(caseData.caseExpressionList.get(0)));

        // Create a list of VisitorResults to return
        List<WRESLComponent> returnData = new ArrayList<>(List.of(caseData));
        for (int i=0; i<dvarSlackSurplusList.size(); i++) {
            returnData.add(dvarSlackSurplusList.get(i));
            returnData.add(weightSlackSurplusList.get(i));
        }
        return new VisitorResult(returnData);
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
        ex.name = getWreslText(ctx.OBJECT_NAME());

        // Return data
        return new VisitorResult(ex);
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
        as.name = getWreslText(ctx.OBJECT_NAME());

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
            as.timeArraySizeParseTree = generateExpressionParseTree(as.timeArraySize);
        }

        // Retrieve expression
        as.expression = getWreslText(ctx.expression());
        as.expressionParseTree = generateExpressionParseTree(as.expression);

        // Return data
        return new VisitorResult(as);
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
        ts.name = getWreslText(ctx.OBJECT_NAME());
        ts.dssBPart = ts.name;

        // Source file and line number
        ts.fromWresl = this.currentFile;
        ts.line = ctx.TIMESERIES().getSymbol().getLine();

        // Process KIND; check that only one exists
        if (ctx.kind().size() != 1) {
            errorMessages.add("There must be one and only one KIND keyword in a TIMESERIES statement!");
            throw new SyntaxErrorException(ts.fromWresl, ts.line, errorMessages);
        } else {
            ts.kind = visitorResultToString(visit(ctx.kind().get(0).specificationString()));
        }

        // Process UNITS; check that one exists
        if (ctx.units().size() != 1) {
            errorMessages.add("There must be one and only one UNITS keyword in a TIMESERIES statement!");
            throw new SyntaxErrorException(ts.fromWresl, ts.line, errorMessages);
        } else {
            ts.units = visitorResultToString(visit(ctx.units().get(0).specificationString()));
        }

        // Process CONVERT; it is optional and if exists there should be only one
        if (!ctx.convert().isEmpty()) {
            if (ctx.convert().size() != 1) {
                errorMessages.add("There must be one and only one CONVERT keyword in a TIMESERIES statement!");
                throw new SyntaxErrorException(ts.fromWresl, ts.line, errorMessages);
            } else {
                ts.convertToUnits = visitorResultToString(visit(ctx.convert().get(0).specificationString()));
            }
        }

        return new VisitorResult(ts);
    }

    @Override
    // WRESL type
    public VisitorResult visitTimeSeriesTypeDef(wreslParser.TimeSeriesTypeDefContext ctx) {
        Timeseries ts = new Timeseries();
        List<String> errorMessages = new ArrayList<>();

        // Retrieve ts name
        ts.name = getWreslText(ctx.OBJECT_NAME());

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
        } else {
            ts.dssBPart = ts.name;
        }

        // Process KIND; check that only one exists
        if (ctx.kind().size() != 1) {
            errorMessages.add("There must be one and only one KIND keyword in a TIMESERIES statement!");
            throw new SyntaxErrorException(ts.fromWresl, ts.line, errorMessages);
        } else {
            ts.kind = visitorResultToString(visit(ctx.kind().get(0).specificationString()));
        }

        // Process UNITS; check that one exists
        if (ctx.units().size() != 1) {
            errorMessages.add("There must be one and only one UNITS keyword in a TIMESERIES statement!");
            throw new SyntaxErrorException(ts.fromWresl, ts.line, errorMessages);
        } else {
            ts.units = visitorResultToString(visit(ctx.units().get(0).specificationString()));
        }

        // Process CONVERT; it is optional and if exists there should be only one
        if (!ctx.convert().isEmpty()) {
            if (ctx.convert().size() != 1) {
                errorMessages.add("There must be one and only one CONVERT keyword in a TIMESERIES statement!");
                throw new SyntaxErrorException(ts.fromWresl, ts.line, errorMessages);
            } else {
                ts.convertToUnits = visitorResultToString(visit(ctx.convert().get(0).specificationString()));
            }
        }

        return new VisitorResult(ts);
    }


    // ------------------------------------------------------------
    // --- OBJECTIVE
    // ------------------------------------------------------------
    @Override
    // Gateway to OBJECTIVE keyword visitors; returns a list of weights
    public VisitorResult visitObjective(wreslParser.ObjectiveContext ctx) {
        VisitorResult result = visit(ctx.objectiveBody());
        return result;
    }

    @Override
    // weightsByPair
    public VisitorResult visitWeightsByPair(wreslParser.WeightsByPairContext ctx) {
        List<WRESLComponent> returnData = new ArrayList<>();

        for (wreslParser.VarWeightPairContext varWeightPairCtx : ctx.varWeightPair()) {
            WeightElement weight = new WeightElement();
            weight.name = getWreslText(varWeightPairCtx.OBJECT_NAME());
            weight.weight = getWreslText(varWeightPairCtx.expression());
            weight.weightParseTree = generateExpressionParseTree(weight.weight);
            if (varWeightPairCtx.arraySizeDefinition() != null) {
                weight.timeArraySize = varWeightPairCtx.arraySizeDefinition().getText();
                weight.timeArraySizeParseTree = generateExpressionParseTree(weight.timeArraySize);
            }
            weight.fromWresl = this.currentFile;
            weight.line = varWeightPairCtx.OPEN_BRACKET().getSymbol().getLine();
            returnData.add(weight);
        }
        return new VisitorResult(returnData);
    }

    @Override
    // weightsCommon
    public VisitorResult visitWeightsCommon(wreslParser.WeightsCommonContext ctx) {
        List<WRESLComponent> returnData = new ArrayList<>();

        String weightValue = getWreslText(ctx.weight().expression());
        wreslParser.ExpressionContext weightParseTree = generateExpressionParseTree(weightValue);
        for (wreslParser.ExpressionContext variableCtx : ctx.variables().expression()) {
            WeightElement weight = new WeightElement();
            weight.name = getWreslText(variableCtx);
            weight.weight = weightValue;
            weight.weightParseTree = weightParseTree;
            weight.fromWresl = this.currentFile;
            weight.line = ctx.variables().VARIABLE().getSymbol().getLine();
            returnData.add(weight);
        }
        return new VisitorResult(returnData);
    }


    // ------------------------------------------------------------
    // --- EXPRESSIONS
    // ------------------------------------------------------------
    @Override
    // expressionComparison
    public VisitorResult visitExpressionComparison(wreslParser.ExpressionComparisonContext ctx) {
        WRESL_String expression = new WRESL_String(getWreslText(ctx));
        return new VisitorResult(expression);
    }

    @Override
    // expressionMultDiv
    public VisitorResult visitExpressionMultDiv(wreslParser.ExpressionMultDivContext ctx) {
        // Store expression to be computed later during run
        WRESL_String expression = new WRESL_String(getWreslText(ctx));
        return new VisitorResult(expression);
     }

    @Override
    // expressionAddSub
    public VisitorResult visitExpressionAddSub(wreslParser.ExpressionAddSubContext ctx) {
        // Store expression to be computed later during run
        WRESL_String expression = new WRESL_String(getWreslText(ctx));
        return new VisitorResult(expression);
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
            return null;
        }
        else {
            WRESL_String functionName = new WRESL_String(getWreslText(ctx.OBJECT_NAME()));
            return new VisitorResult(functionName);
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
            if (caseCondition.equals(Param.always)) {
                caseConditionTree = null;
            } else {
                caseConditionTree = ctx.caseCondition().caseConditionExpression();
            }
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
                                                     List.of(caseExpressionTree));
        caseData.name = caseName;
        return new VisitorResult(caseData);
    }

    @Override
    // caseViaValue
    public VisitorResult visitCaseViaValue(wreslParser.CaseViaValueContext ctx) {
        WRESL_String caseExpression = new WRESL_String(getWreslText(ctx.expression()));
        return new VisitorResult(caseExpression);
    }

    @Override
    // caseViaSelect
    public VisitorResult visitCaseViaSelect(wreslParser.CaseViaSelectContext ctx) {
        WRESL_String caseExpression = new WRESL_String(getWreslText(ctx.select()));
        return new VisitorResult(caseExpression);
    }

    @Override
    // caseViaExpression
    public VisitorResult visitCaseViaExpression(wreslParser.CaseViaExpressionContext ctx) {
        WRESL_String caseExpression = new WRESL_String(getWreslText(ctx.expression()));
        return new VisitorResult(caseExpression);
    }


    // ------------------------------------------------------------
    // --- IF STATEMENT (CAN ONLY USE SVARs DEFINED IN INITIAL STATEMENT)
    // ------------------------------------------------------------
    @Override
    // ifStatement
    public VisitorResult visitIfStatement(wreslParser.IfStatementContext ctx) {
        VisitorResult result;

        // Process first IF clause
        if (Evaluator.evaluateCondition(this.sds, ctx.ifClause().expression())) {
            return visit(ctx.ifClause().ifBlock());
        }

        // Process ELSE IF clauses
        for (int i=0; i<ctx.elseIfClause().size(); i++) {
            if (Evaluator.evaluateCondition(this.sds, ctx.elseIfClause(i).expression())) {
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
        List<WRESLComponent> ifBlockContents = new ArrayList<>();
        List<WRESLComponent> dataList;
        for (int i=1; i<ctx.getChildCount()-1; i++) {
            // Visit child
            VisitorResult result = visit(ctx.getChild(i));

            // Accumulate returned data into return variable
            ifBlockContents.addAll(result.data());
        }

        return new VisitorResult(ifBlockContents);
    }


    // ------------------------------------------------------------
    // --- MISCELLANEOUS VISIT METHODS
    // ------------------------------------------------------------
    @Override
    // arraySizeDefinition
    public VisitorResult visitArraySizeDefinition(wreslParser.ArraySizeDefinitionContext ctx) {
        WRESL_String expr = new WRESL_String(getWreslText(ctx.expression()));
        return new VisitorResult(expr);
    }

    @Override
    // specificationString
    public VisitorResult visitSpecificationString(wreslParser.SpecificationStringContext ctx) {
        String tempString = getWreslText(ctx);
        WRESL_String data = new WRESL_String(tempString.substring(0, tempString.length() - 1).substring(1));   // Remove first and last character;
        return new VisitorResult(data);
    }




    // ------------------------------------------------------------
    // ------------------------------------------------------------
    // --- HELPER METHODS
    // ------------------------------------------------------------
    // ------------------------------------------------------------

    // Convert visitor result to string
    private static String visitorResultToString(VisitorResult result) {
        String stringData;

        if (result.data().get(0) instanceof WRESL_String data) {
            stringData = data.text; }
        else {
            stringData = null;
        }

        return stringData;
    }

    // Clear memory from variables that are no longer needed
    private void clearMemory() {
        this.sequenceData = null;
        this.modelsAndGroups = null;
        this.includeFileList = null;
        this.wreslFilesMap = null;
    }

    // Convert ALIASes referenced from GOALs to DVARs and GOALs
    private ModelDataSet convertAliasToGoal(ModelDataSet mdsIn) {
        ModelDataSet mdsOut = mdsIn;

        // Create goal list and map for the new goals
        List<String> newGoalList = new ArrayList<>();
        Map<String, Goal> newGoalMap = new HashMap<>();

        // Loop over the goals of the model
        for (Goal goal : mdsIn.gMap.values()) {
            for (ParseTree goalExpressionTree : goal.goalExpressionParseTrees) {
                // Retrieve ALIASes
                Set<String> aliasList = aliasListForGoals(goalExpressionTree, mdsIn.asMap);

                // Find aliases and convert them to dvars
                for (String asName : aliasList) {
                    Alias as = mdsIn.asMap.get(asName);
                    Dvar dvar = new Dvar();
                    dvar.name = as.name;
                    dvar.fromWresl = as.fromWresl;
                    dvar.line = as.line;
                    dvar.kind = as.kind;
                    dvar.units = as.units;
                    dvar.lowerBound = Param.lower_unbounded;
                    dvar.upperBound = Param.upper_unbounded;
                    dvar.timeArraySize = as.timeArraySize;
                    dvar.timeArraySizeExpressionParseTree = as.timeArraySizeParseTree;

                    Goal goalForAlias = new Goal();
                    goalForAlias.name = as.name + "__alias";
                    goalForAlias.caseName.add(Param.defaultCaseName);
                    goalForAlias.caseCondition.add(Param.always);
                    goalForAlias.caseConditionParseTrees.add(null);
                    String caseExpression = as.name + "=" + as.expression;
                    goalForAlias.goalExpression.add(caseExpression);
                    goalForAlias.goalExpressionParseTrees.add(generateGoalBodyParseTree(caseExpression));
                    goalForAlias.fromWresl = as.fromWresl;
                    goalForAlias.line = as.line;

                    mdsOut.dvList.add(asName);
                    mdsOut.dvMap.put(asName, dvar);

                    newGoalList.add(goalForAlias.name);
                    newGoalMap.put(goalForAlias.name, goalForAlias);

                    mdsOut.asMap.remove(asName);
                    mdsOut.asList.remove(asName);
                }
            }
        }

        mdsOut.gList.addAll(newGoalList);
        mdsOut.gMap.putAll(newGoalMap);
        return mdsOut;
    }

    // Find ALIASes referenced from a GOAL to be converted into DVARs
    private Set<String> aliasListForGoals(ParseTree expressionTree, Map<String,Alias> asMap) {
        Set<String> asListToConvert = new HashSet<>();

        // Get the list of aliases
        Set<String> asList = asMap.keySet();

        // Retrieve variables
        Expression_To_Vars varFinder = new Expression_To_Vars();
        List<String> varList = varFinder.visit(expressionTree);

        // Find aliases
        if (varList != null) {
            for (String var : varList) {
                if (asList.contains(var)) {
                    asListToConvert.add(var);
                    // Check the variables referenced by the alias itself also
                    Alias as = asMap.get(var);
                    Set<String> asListToConvert1 = aliasListForGoals(as.expressionParseTree, asMap);
                    asListToConvert.addAll(asListToConvert1);
                }
            }
        }

        return asListToConvert;
    }



    // ------------------------------------------------------------
    // ------------------------------------------------------------
    // --- INNER CLASSES
    // ------------------------------------------------------------
    // ------------------------------------------------------------

    // ------------------------------------------------------------
    // --- SEQUENCE CLASS TO HELP ORDER MODELS
    // ------------------------------------------------------------
    private class Sequence {
        private String sequenceName = Param.undefined;
        private String modelName = Param.undefined;
        private int order = 0;
        private String condition = Param.always;
        private ParseTree conditionParseTree = null;
        private String timeStep = Param.undefined;
    }


    // ------------------------------------------------------------
    // --- WRESL_Case CLASS TO PROCESS CASE KEYWORD
    // ------------------------------------------------------------
    private class WRESL_CaseData extends WRESLComponent {
        private String caseCondition;
        private ParseTree caseConditionTree;
        private List<String> caseExpressionList;
        private List<ParseTree> caseExpressionTreeList;

        private WRESL_CaseData() {
            this.caseCondition = null;
            this.caseConditionTree = null;
            this.caseExpressionList = new ArrayList<>();
            this.caseExpressionTreeList = new ArrayList<>();
        }

        private WRESL_CaseData(String caseCondition,
                               ParseTree caseConditionTree,
                               List<String> caseExpressionList,
                               List<ParseTree> caseExpressionTreeList) {
            this.caseCondition = caseCondition;
            this.caseConditionTree = caseConditionTree;
            this.caseExpressionList = caseExpressionList;
            this.caseExpressionTreeList = caseExpressionTreeList;
        }
    }


    // ------------------------------------------------------------
    // --- CLASS TO REPRESENT STRINGS AS A WRESLComponent
    // ------------------------------------------------------------
    private class WRESL_String extends WRESLComponent {
        private String text;

        private WRESL_String(String text) {
            this.text = text;
        }

    }
}
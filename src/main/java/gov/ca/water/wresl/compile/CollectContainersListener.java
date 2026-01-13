package gov.ca.water.wresl.compile;

import gov.ca.water.wresl.domain.Group;
import gov.ca.water.wresl.domain.Initial;
import gov.ca.water.wresl.domain.Model;
import gov.ca.water.wresl.domain.Sequence;
import gov.ca.water.wresl.enums.Timestep;
import gov.ca.water.wresl.grammar.wreslBaseListener;
import gov.ca.water.wresl.grammar.wreslParser;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CollectContainersListener extends wreslBaseListener {
    private static final Logger logger = LoggerFactory.getLogger(CollectContainersListener.class);
    private final CollectedContainers containers;

    public CollectContainersListener() {
        this.containers = new CollectedContainers();
    }

    @Override
    public void exitInitial(wreslParser.InitialContext ctx) {
        this.containers.initial = new Initial(ctx.svar());
        logger.atDebug().setMessage("initial found").log();
    }

    @Override
    public void exitSequence(wreslParser.SequenceContext ctx) {
        wreslParser.SequenceBodyContext body = ctx.sequenceBody();

        String name = ctx.OBJECT_NAME().getText();
        int order = Integer.parseInt(body.INT().toString());
        String modelName = body.OBJECT_NAME().getText();
        wreslParser.SequenceConditionContext condition = body.sequenceCondition();
        Timestep ts = Timestep.fromContext(body.timestepSpecification());

        Sequence seq = new Sequence(name, order, modelName, condition, ts);
        this.containers.sequences.put(seq.name(), seq);
        logger.atDebug().setMessage("sequence found: {}").addArgument(seq.name()).log();
    }

    @Override
    public void exitModel(wreslParser.ModelContext ctx) {
        String name = ctx.OBJECT_NAME().getText();
        List<wreslParser.ModelBodyContext> body = ctx.modelBody();

        Model model = new Model(name, body);
        this.containers.models.put(model.name(), model);
        logger.atDebug().setMessage("model found: {}").addArgument(model.name()).log();
    }

    @Override
    public void exitGroup(wreslParser.GroupContext ctx) {
        String groupName = ctx.OBJECT_NAME().getText();
        List<wreslParser.GroupBodyContext> body = ctx.groupBody();

        Group group = new Group(groupName, body);
        this.containers.groups.put(group.name(), group);
        logger.atDebug().setMessage("group found: {}").addArgument(group.name()).log();
    }

    public CollectedContainers collectContainersFromTrees(Collection<wreslParser.StartContext> trees ) {
        ParseTreeWalker walker = new ParseTreeWalker();
        for (wreslParser.StartContext ctx : trees) {
            walker.walk(this, ctx);  // compile container classes
        }
        return this.containers;
    }
}


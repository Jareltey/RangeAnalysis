package hw5;

import common.ErrorMessage;
import common.Utils;
import soot.Local;
import soot.Unit;
import soot.ValueBox;
import soot.baf.ArrayReadInst;
import soot.baf.ArrayWriteInst;
import soot.jimple.IdentityStmt;
import soot.toolkits.graph.DominatorsFinder;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.MHGDominatorsFinder;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;
import soot.jimple.Stmt;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class IntraSignAnalysis extends ForwardFlowAnalysis<Unit, Sigma> {
    // Holds the set of local variables
    private Set<Local> locals = new HashSet<>();

    // The calling context for the analysis
    // Null if no context (e.g., when only running intraprocedurally)
    private Context ctx;

    // The input sigma for this analysis
    private Sigma sigma_i;

    /**
     * Constructor with no context. This is useful for testing the intraprocedural
     * analysis on its own.
     */
    IntraSignAnalysis(UnitGraph graph) {
        // Note the construction of a default Sigma
        this(graph, null, null);
    }

    /**
     * Allows creating an intra analysis with just the context and the input sigma,
     * since the unit graph can be grabbed from the function in the context.
     */
    IntraSignAnalysis(Context ctx, Sigma sigma_i) {
        this(new ExceptionalUnitGraph(ctx.fn.getActiveBody()), ctx, sigma_i);
    }

    IntraSignAnalysis(UnitGraph graph, Context ctx, Sigma sigma_i) {
        super(graph);
        this.ctx = ctx;
        this.sigma_i = sigma_i;

        // Collect locals
        this.locals.addAll(graph.getBody().getLocals());
    }

    // Runs the analysis
    public void run() {
        this.doAnalysis();
    }

    /**
     * Report warnings. This will use the analysis results collected by the constructor.
     */
    public void reportWarnings() {
        // TODO: Implement this (raise warnings)!
        // TODO: This implementation is incorrect, but it shows how to report a warning
        for (Unit u : this.graph) {
//            if (u instanceof ArrayReadInst || u instanceof ArrayWriteInst) {
//                Sigma sigmaBefore = this.getFlowBefore(u); // TODO: Use this info to decide if a warning is appropriate
//                for (Map.Entry<Local,Sigma.L> entry : sigmaBefore.map.entrySet()) {
//                    if (entry.getValue() == Sigma.L.N) {
//                        // Reports an error for a definite negative index
//                        Utils.reportWarning(u, ErrorMessage.NEGATIVE_INDEX_ERROR);
//                    } else if (entry.getValue() == Sigma.L.Top || entry.getValue() == Sigma.L.Bottom) {
//                        // Reports a warning for a possible negative index
//                        Utils.reportWarning(u, ErrorMessage.POSSIBLE_NEGATIVE_INDEX_WARNING);
//                    }
//                }
//             }
//            }
            Sigma sigmaBefore = this.getFlowBefore(u); // TODO: Use this info to decide if a warning is appropriate
            // Reports an error for a definite negative index
            Utils.reportWarning(u, ErrorMessage.NEGATIVE_INDEX_ERROR);
            // Reports a warning for a possible negative index
            Utils.reportWarning(u, ErrorMessage.POSSIBLE_NEGATIVE_INDEX_WARNING);
        }
    }

    /**
     * Run flow function for this unit
     *
     * @param inValue  The initial Sigma at this point
     * @param unit     The current Unit
     * @param outValue The updated Sigma after the flow function
     */
    @Override
    protected void flowThrough(Sigma inValue, Unit unit, Sigma outValue) {
        // TODO: Implement the flow function
        Stmt stmt = (Stmt) unit;
        if (stmt instanceof IdentityStmt) {
            IdentityStmt id_stmt = (IdentityStmt) stmt;
            Local var = (Local) id_stmt.getLeftOp();
            this.copy(inValue, outValue);
            outValue.map.put(var, Sigma.L.Bottom);
        } else if (stmt instanceof )


//        Context calleectx = Context.getCtx(ctx.fn ,ctx ,unit.getJavaSourceStartLineNumber());
    }

    /**
     * Initial flow information at the start of a method
     */
    @Override
    protected Sigma entryInitialFlow() {
        if (this.sigma_i != null) {
            return this.sigma_i;
        } else {
            // TODO: Implement me!
            return new Sigma(this.locals, Sigma.L.Top);
        }
    }

    /**
     * Initial flow information at each other program point
     */
    @Override
    protected Sigma newInitialFlow() {
        // TODO: Implement me!
        return new Sigma(this.locals, Sigma.L.Bottom);
    }

    /**
     * Join at a program point lifted to sets
     */
    @Override
    protected void merge(Sigma in1, Sigma in2, Sigma out) {
        // TODO: Implement me!
        for (Map.Entry<Local,Sigma.L> entry : in1.map.entrySet()) {
            Sigma.L val1 = entry.getValue();
            Sigma.L val2 = in2.map.get(entry.getKey());
            out.map.put(entry.getKey(), Sigma.join(val1, val2));
        }
    }

    /**
     * Copy for sets
     */
    @Override
    protected void copy(Sigma source, Sigma dest) {
        // TODO: Implement me!
        for (Map.Entry<Local,Sigma.L> entry : source.map.entrySet()) {
            dest.map.put(entry.getKey(), entry.getValue());
        }
    }
}

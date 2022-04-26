package hw5;

import common.ErrorMessage;
import common.Utils;
import jdk.internal.net.http.common.Pair;
import soot.Local;
import soot.Unit;
import soot.ValueBox;
import soot.baf.ArrayReadInst;
import soot.baf.ArrayWriteInst;
import soot.jimple.*;
import soot.toolkits.graph.DominatorsFinder;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.MHGDominatorsFinder;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ForwardFlowAnalysis;

import java.awt.geom.Arc2D;
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
            Sigma sigmaBefore = this.getFlowBefore(u); // TODO: Use this info to decide if a warning is appropriate
            Local ind = null;
            Stmt stmt = (Stmt) u;
            if (stmt instanceof AssignStmt) {
                AssignStmt assign_stmt = (AssignStmt) stmt;
                Local var = (Local) assign_stmt.getLeftOp();
                soot.Value expr = assign_stmt.getRightOp();
                if (expr instanceof ArrayRef) {
                    ArrayRef arr_expr = (ArrayRef) expr;
                    soot.Value index = arr_expr.getIndex();
//                    System.out.println(index.getClass().getSimpleName());
                    if (index instanceof IntConstant) {
                        Integer i = ((IntConstant) index).value;
                        if (i < 0) {
                            Utils.reportWarning(u, ErrorMessage.NEGATIVE_INDEX_ERROR);
                        }
                    } else if (index instanceof Local) {
                        ind = (Local) index;
                        Sigma.L abs = sigmaBefore.map.get(ind);
                        if (abs == Sigma.L.N) {
                            Utils.reportWarning(u, ErrorMessage.NEGATIVE_INDEX_ERROR);
                        } else if (abs == Sigma.L.Top) {
                            Utils.reportWarning(u, ErrorMessage.POSSIBLE_NEGATIVE_INDEX_WARNING);
                        }
                    }
                }
            }
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
        Pair<Double,Double> bottom = new Pair<>(Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY);
        this.copy(inValue, outValue);
        Stmt stmt = (Stmt) unit;
        if (stmt instanceof AssignStmt) {
            AssignStmt assign_stmt = (AssignStmt) stmt;
            Local var = (Local) assign_stmt.getLeftOp();
            Local var_right = null;
            soot.Value expr = assign_stmt.getRightOp();
            if (expr instanceof DoubleConstant) {
                Double d = ((DoubleConstant) expr).value;
                outValue.map.put(var, new Pair<>(d,d));

            } else if (expr instanceof BinopExpr) {
                soot.Value op1 = ((BinopExpr) expr).getOp1();
                Pair<Double,Double> var1_abstract = null;
                if (op1 instanceof DoubleConstant) {
                    DoubleConstant op1_const = (DoubleConstant) op1;
                    Double op1_val = op1_const.value;
                    var1_abstract = new Pair<>(op1_val,op1_val);
                } else if (op1 instanceof Local) {
                    Local var1 = (Local) op1;
                    var1_abstract = inValue.map.get(var1);
                }
                Pair<Double,Double> var2_abstract = null;
                soot.Value op2 = ((BinopExpr) expr).getOp2();
                if (op2 instanceof DoubleConstant) {
                    DoubleConstant op2_const = (DoubleConstant) op2;
                    Double op2_val = op2_const.value;
                    var2_abstract = new Pair<>(op2_val,op2_val);
                } else if (op2 instanceof Local) {
                    Local var2 = (Local) op2;
                    var2_abstract = inValue.map.get(var2);
                }

                if (expr instanceof AddExpr) {
                    if (var1_abstract == bottom || var2_abstract == bottom) {
                        ;
                    } else {
                        Double low = var1_abstract.first + var2_abstract.first;
                        Double high = var1_abstract.second + var2_abstract.second;
                        outValue.map.put(var, new Pair<>(low,high));
                    }
                } else if (expr instanceof SubExpr) {
                    if (var1_abstract == bottom || var2_abstract == bottom) {
                        ;
                    } else {
                        Double low = var1_abstract.first - var2_abstract.second;
                        Double high = var1_abstract.second - var2_abstract.first;
                        outValue.map.put(var, new Pair<>(low,high));
                    }
                } else if (expr instanceof MulExpr) {
                    Double candidate_1 = var1_abstract.first * var2_abstract.first;
                    Double candidate_2 = var1_abstract.first * var2_abstract.second;
                    Double candidate_3 = var1_abstract.second * var2_abstract.first;
                    Double candidate_4 = var1_abstract.second * var2_abstract.second;
                    Double low = Math.min(Math.min(Math.min(candidate_1,candidate_2),candidate_3),candidate_4);
                    Double high = Math.max(Math.max(Math.max(candidate_1,candidate_2),candidate_3),candidate_4);
                    outValue.map.put(var, new Pair<>(low,high));
                } else if (expr instanceof DivExpr) {
                    Pair<Double,Double> reciprocal_abs;
                    if (var2_abstract.first > 0 || var2_abstract.second < 0) {
                        reciprocal_abs = new Pair<>(1/var2_abstract.second, 1/var2_abstract.first);
                    } else if (var2_abstract.first == 0) {
                        reciprocal_abs = new Pair<>(1/var2_abstract.second, Double.POSITIVE_INFINITY);
                    } else if (var2_abstract.second == 0) {
                        reciprocal_abs = new Pair<>(Double.NEGATIVE_INFINITY, 1/ var2_abstract.first);
                    } else {
                        // Being conservative by assigning lattice value T, could retain more information by taking
                        // union of 2 disjoint intervals
                        reciprocal_abs = new Pair<>(Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY);;
                    }
                } else {
                    ;
                }
            } else if (expr instanceof Local) {
                var_right = (Local) expr;
                Pair<Double,Double> abs = inValue.map.get(var_right);
                outValue.map.put(var, abs);
            }
        } else if (stmt instanceof IfStmt) {
            Stmt target_stmt = ((IfStmt) stmt).getTarget();
            soot.Value cond = ((IfStmt) stmt).getCondition();
            if (cond instanceof EqExpr) {
                soot.Value left_op = ((EqExpr) cond).getOp1();
                soot.Value right_op = ((EqExpr) cond).getOp2();
                Sigma.L abs = null;
                if (left_op instanceof Local) {
                    abs = inValue.map.get((Local) left_op);
                }
                if (right_op instanceof IntConstant) {
                    ;
                }
            }
        } else if (stmt instanceof GotoStmt) {
            ;
        }
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
            Pair<Double,Double> initialVal = new Pair<>(Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY);
            return new Sigma(this.locals, initialVal);
        }
    }

    /**
     * Initial flow information at each other program point
     */
    @Override
    protected Sigma newInitialFlow() {
        // TODO: Implement me!
        Pair<Double,Double> initialVal = new Pair<>(Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY);
        return new Sigma(this.locals, initialVal);
    }

    /**
     * Join at a program point lifted to sets
     */
    @Override
    protected void merge(Sigma in1, Sigma in2, Sigma out) {
//      TODO: Implement me!
        for (Map.Entry<Local,Pair<Double,Double>> entry : in1.map.entrySet()) {
            Pair<Double,Double> val1 = entry.getValue();
            Pair<Double,Double> val2 = in2.map.get(entry.getKey());
            out.map.put(entry.getKey(), Sigma.join(val1, val2));
        }
    }

    /**
     * Copy for sets
     */
    @Override
    protected void copy(Sigma source, Sigma dest) {
        // TODO: Implement me!
        for (Map.Entry<Local,Pair<Double,Double>> entry : source.map.entrySet()) {
            dest.map.put(entry.getKey(), entry.getValue());
        }
    }
}

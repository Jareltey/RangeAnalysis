package hw5;

import common.ErrorMessage;
import common.Utils;
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
        this.copy(inValue, outValue);
        Stmt stmt = (Stmt) unit;
        if (stmt instanceof AssignStmt) {
            AssignStmt assign_stmt = (AssignStmt) stmt;
            Local var = (Local) assign_stmt.getLeftOp();
            Local var_right = null;
            soot.Value expr = assign_stmt.getRightOp();
//            System.out.println(expr.getClass().getSimpleName());
//            if (expr instanceof NumericConstant) {
            if (expr instanceof IntConstant) {
                Integer i = Integer.parseInt(expr.toString());
                if (i == 0) {
                    outValue.map.put(var, Sigma.L.Z);
                } else if (i > 0) {
                    outValue.map.put(var, Sigma.L.P);
                } else {
                    outValue.map.put(var, Sigma.L.N);
                }
            } else if (expr instanceof BinopExpr) {
                soot.Value op1 = ((BinopExpr) expr).getOp1();
                Sigma.L var1_abstract = Sigma.L.Top;
                if (op1 instanceof IntConstant) {
                    IntConstant op1_const = (IntConstant) op1;
                    Integer op1_val = op1_const.value;
                    if (op1_val == 0) {
                        var1_abstract = Sigma.L.Z;
                    } else if (op1_val > 0) {
                        var1_abstract = Sigma.L.P;
                    } else {
                        var1_abstract = Sigma.L.N;
                    }
                } else if (op1 instanceof Local) {
                    Local var1 = (Local) op1;
                    var1_abstract = inValue.map.get(var1);
                }
                Sigma.L var2_abstract = Sigma.L.Top;
                soot.Value op2 = ((BinopExpr) expr).getOp2();
                if (op2 instanceof IntConstant) {
                    IntConstant op2_const = (IntConstant) op2;
                    Integer op2_val = op2_const.value;
                    if (op2_val == 0) {
                        var2_abstract = Sigma.L.Z;
                    } else if (op2_val > 0) {
                        var2_abstract = Sigma.L.P;
                    } else {
                        var2_abstract = Sigma.L.N;
                    }
                } else if (op2 instanceof Local) {
                    Local var2 = (Local) op2;
                    var2_abstract = inValue.map.get(var2);
                }

                if (expr instanceof AddExpr) {
                    if (var1_abstract == Sigma.L.Bottom || var2_abstract == Sigma.L.Bottom) {
                        outValue.map.put(var, Sigma.L.Bottom);
                    } else if (var1_abstract == Sigma.L.Top || var2_abstract == Sigma.L.Top) {
                        outValue.map.put(var, Sigma.L.Top);
                    } else if (var1_abstract == Sigma.L.P && var2_abstract == Sigma.L.N) {
                        outValue.map.put(var, Sigma.L.Top);
                    } else if (var1_abstract == Sigma.L.N && var2_abstract == Sigma.L.P) {
                        outValue.map.put(var, Sigma.L.Top);
                    } else if (var1_abstract == Sigma.L.P) {
                        outValue.map.put(var, Sigma.L.P);
                    } else if (var1_abstract == Sigma.L.Z && var2_abstract == Sigma.L.P) {
                        outValue.map.put(var, Sigma.L.P);
                    } else if (var1_abstract == Sigma.L.N) {
                        outValue.map.put(var, Sigma.L.N);
                    } else if (var1_abstract == Sigma.L.Z && var2_abstract == Sigma.L.N) {
                        outValue.map.put(var, Sigma.L.N);
                    } else if (var1_abstract == Sigma.L.Z && var2_abstract == Sigma.L.Z) {
                        outValue.map.put(var, Sigma.L.Z);
                    }
                } else if (expr instanceof SubExpr) {
                    if (var1_abstract == Sigma.L.Bottom || var2_abstract == Sigma.L.Bottom) {
                        outValue.map.put(var, Sigma.L.Bottom);
                    } else if (var1_abstract == Sigma.L.Top || var2_abstract == Sigma.L.Top) {
                        outValue.map.put(var, Sigma.L.Top);
                    } else if (var1_abstract == Sigma.L.P && var2_abstract == Sigma.L.P) {
                        outValue.map.put(var, Sigma.L.Top);
                    } else if (var1_abstract == Sigma.L.P) {
                        outValue.map.put(var, Sigma.L.P);
                    } else if (var1_abstract == Sigma.L.N && var2_abstract == Sigma.L.N) {
                        outValue.map.put(var, Sigma.L.Top);
                    } else if (var1_abstract == Sigma.L.N) {
                        outValue.map.put(var, Sigma.L.N);
                    } else if (var1_abstract == Sigma.L.Z && var2_abstract == Sigma.L.P) {
                        outValue.map.put(var, Sigma.L.N);
                    } else if (var1_abstract == Sigma.L.Z && var2_abstract == Sigma.L.N) {
                        outValue.map.put(var, Sigma.L.P);
                    } else if (var1_abstract == Sigma.L.Z && var2_abstract == Sigma.L.Z) {
                        outValue.map.put(var, Sigma.L.Z);
                    }
                } else if (expr instanceof MulExpr) {
                    if (var1_abstract == Sigma.L.Bottom || var2_abstract == Sigma.L.Bottom) {
                        outValue.map.put(var, Sigma.L.Bottom);
                    } else if (var1_abstract == Sigma.L.Z && var2_abstract == Sigma.L.Top) {
                        outValue.map.put(var, Sigma.L.Z);
                    } else if (var1_abstract == Sigma.L.Top && var2_abstract == Sigma.L.Z) {
                        outValue.map.put(var, Sigma.L.Z);
                    } else if (var1_abstract == Sigma.L.Top || var2_abstract == Sigma.L.Top) {
                        outValue.map.put(var, Sigma.L.Top);
                    } else if (var1_abstract == Sigma.L.P && var2_abstract == Sigma.L.P) {
                        outValue.map.put(var, Sigma.L.P);
                    } else if (var1_abstract == Sigma.L.P && var2_abstract == Sigma.L.N) {
                        outValue.map.put(var, Sigma.L.N);
                    } else if (var1_abstract == Sigma.L.P && var2_abstract == Sigma.L.Z) {
                        outValue.map.put(var, Sigma.L.Z);
                    } else if (var1_abstract == Sigma.L.N && var2_abstract == Sigma.L.P) {
                        outValue.map.put(var, Sigma.L.N);
                    } else if (var1_abstract == Sigma.L.N && var2_abstract == Sigma.L.N) {
                        outValue.map.put(var, Sigma.L.P);
                    } else if (var1_abstract == Sigma.L.N && var2_abstract == Sigma.L.Z) {
                        outValue.map.put(var, Sigma.L.Z);
                    } else {
                        outValue.map.put(var, Sigma.L.Z);
                    }
                } else if (expr instanceof DivExpr) {
                    if (var1_abstract == Sigma.L.Bottom || var2_abstract == Sigma.L.Bottom) {
                        outValue.map.put(var, Sigma.L.Bottom);
                    } else if (var1_abstract == Sigma.L.Z && var2_abstract == Sigma.L.Top) {
                        outValue.map.put(var, Sigma.L.Z);
                    } else if (var1_abstract == Sigma.L.Top && var2_abstract == Sigma.L.Z) {
                        outValue.map.put(var, Sigma.L.Bottom);
                    } else if (var1_abstract == Sigma.L.Top || var2_abstract == Sigma.L.Top) {
                        outValue.map.put(var, Sigma.L.Top);
                    } else if (var1_abstract == Sigma.L.P && var2_abstract == Sigma.L.Z) {
                        outValue.map.put(var, Sigma.L.Bottom);
                    } else if (var1_abstract == Sigma.L.P && var2_abstract == Sigma.L.P) {
                        outValue.map.put(var, Sigma.L.P);
                    } else if (var1_abstract == Sigma.L.P && var2_abstract == Sigma.L.N) {
                        outValue.map.put(var, Sigma.L.N);
                    } else if (var1_abstract == Sigma.L.N && var2_abstract == Sigma.L.Z) {
                        outValue.map.put(var, Sigma.L.Bottom);
                    } else if (var1_abstract == Sigma.L.N && var2_abstract == Sigma.L.P) {
                        outValue.map.put(var, Sigma.L.N);
                    } else if (var1_abstract == Sigma.L.N && var2_abstract == Sigma.L.N) {
                        outValue.map.put(var, Sigma.L.P);
                    } else if (var1_abstract == Sigma.L.Z && var2_abstract == Sigma.L.Z) {
                        outValue.map.put(var, Sigma.L.Bottom);
                    } else {
                        outValue.map.put(var, Sigma.L.Z);
                    }
                } else {
                    ;
                }
            } else if (expr instanceof Local) {
                var_right = (Local) expr;
                Sigma.L abs = inValue.map.get(var_right);
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
            System.out.println(target_stmt.toString());
            System.out.println(cond.toString());
        } else if (stmt instanceof GotoStmt) {
            ;
        }
    }

//        Context calleectx = Context.getCtx(ctx.fn ,ctx ,unit.getJavaSourceStartLineNumber());


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
//      TODO: Implement me!
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

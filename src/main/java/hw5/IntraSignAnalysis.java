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
import soot.toolkits.scalar.Pair;

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

    // Mapping from program points to most recent Sigma_in
    public Map<Integer,Sigma> program_map = new HashMap<>();

    // Mapping from local (array) to its length
    public Map<Local,Integer> array_len_map = new HashMap<>();

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
        for (Unit u : this.graph) {
            Pair<Double, Double> bottom = new Pair<>(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
            Pair<Double, Double> top = new Pair<>(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            Sigma sigmaBefore = this.getFlowBefore(u);
            Stmt stmt = (Stmt) u;
            if (stmt instanceof AssignStmt) {
                AssignStmt assign_stmt = (AssignStmt) stmt;
                Local var = (Local) assign_stmt.getLeftOp();
                soot.Value expr = assign_stmt.getRightOp();
                if (expr instanceof ArrayRef) {
                    ArrayRef arr_expr = (ArrayRef) expr;
                    soot.Value index = arr_expr.getIndex();
                    soot.Value base = arr_expr.getBase();
                    int arr_len = this.array_len_map.get(base);
                    if (index instanceof IntConstant) {
                        Integer i = ((IntConstant) index).value;
                        if (i < 0) {
                            Utils.reportWarning(u, ErrorMessage.NEGATIVE_INDEX_ERROR);
                        } else if (i >= arr_len) {
                            Utils.reportWarning(u, ErrorMessage.EXCEED_ARRAY_LENGTH_ERROR);
                        }
                    } else if (index instanceof Local) {
                        Local ind = (Local) index;
                        Pair<Double, Double> range = sigmaBefore.map.get(ind);
                        Double low = range.getO1();
                        Double high = range.getO2();
                        if (high < 0) {
                            Utils.reportWarning(u, ErrorMessage.NEGATIVE_INDEX_ERROR);
                        } else if (low >= arr_len) {
                            Utils.reportWarning(u, ErrorMessage.EXCEED_ARRAY_LENGTH_ERROR);
                        } else if (low < 0 && high < arr_len) {
                            Utils.reportWarning(u, ErrorMessage.POSSIBLE_NEGATIVE_INDEX_WARNING);
                        } else if (low < 0 && high >= arr_len) {
                            Utils.reportWarning(u, ErrorMessage.EITHER_NEGATIVE_INDEX_OR_EXCEED_ARRAY_LENGTH_WARNING);
                        } else if (low >= 0 && high >= arr_len) {
                            Utils.reportWarning(u, ErrorMessage.POSSIBLE_EXCEED_ARRAY_LENGTH_WARNING);
                        } else {
                            ;
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
        Pair<Double, Double> bottom = new Pair<>(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
        this.copy(inValue, outValue);
        Stmt stmt = (Stmt) unit;
        if (stmt instanceof AssignStmt) {
            AssignStmt assign_stmt = (AssignStmt) stmt;
            Local var = (Local) assign_stmt.getLeftOp();
            Local var_right = null;
            soot.Value expr = assign_stmt.getRightOp();
            if (expr instanceof DoubleConstant) {
                Double d = ((DoubleConstant) expr).value;
                outValue.map.put(var, new Pair<>(d, d));
            } else if (expr instanceof CastExpr) {
                CastExpr cast_expr = (CastExpr) expr;
                soot.Value casted_op = cast_expr.getOp();
                Local casted_local = (Local) casted_op;
                Pair<Double,Double> casted_local_range = inValue.map.get(casted_local);
                outValue.map.put(var,casted_local_range);
            } else if (expr instanceof BinopExpr) {
                soot.Value op1 = ((BinopExpr) expr).getOp1();
                Pair<Double, Double> var1_abstract = null;
                if (op1 instanceof DoubleConstant) {
                    DoubleConstant op1_const = (DoubleConstant) op1;
                    Double op1_val = op1_const.value;
                    var1_abstract = new Pair<>(op1_val, op1_val);
                } else if (op1 instanceof Local) {
                    Local var1 = (Local) op1;
                    var1_abstract = inValue.map.get(var1);
                }
                Pair<Double, Double> var2_abstract = null;
                soot.Value op2 = ((BinopExpr) expr).getOp2();
                if (op2 instanceof DoubleConstant) {
                    DoubleConstant op2_const = (DoubleConstant) op2;
                    Double op2_val = op2_const.value;
                    var2_abstract = new Pair<>(op2_val, op2_val);
                } else if (op2 instanceof Local) {
                    Local var2 = (Local) op2;
                    var2_abstract = inValue.map.get(var2);
                }

                if (expr instanceof AddExpr) {
                    if (var1_abstract == bottom || var2_abstract == bottom) {
                        ;
                    } else {
                        Double low = var1_abstract.getO1() + var2_abstract.getO1();
                        Double high = var1_abstract.getO2() + var2_abstract.getO2();
                        outValue.map.put(var, new Pair<>(low, high));
                    }
                } else if (expr instanceof SubExpr) {
                    if (var1_abstract == bottom || var2_abstract == bottom) {
                        ;
                    } else {
                        Double low = var1_abstract.getO1() - var2_abstract.getO2();
                        Double high = var1_abstract.getO2() - var2_abstract.getO1();
                        outValue.map.put(var, new Pair<>(low, high));
                    }
                } else if (expr instanceof MulExpr) {
                    Double candidate_1 = var1_abstract.getO1() * var2_abstract.getO1();
                    Double candidate_2 = var1_abstract.getO1() * var2_abstract.getO2();
                    Double candidate_3 = var1_abstract.getO2() * var2_abstract.getO1();
                    Double candidate_4 = var1_abstract.getO2() * var2_abstract.getO2();
                    Double low = Math.min(Math.min(Math.min(candidate_1, candidate_2), candidate_3), candidate_4);
                    Double high = Math.max(Math.max(Math.max(candidate_1, candidate_2), candidate_3), candidate_4);
                    outValue.map.put(var, new Pair<>(low, high));
                } else if (expr instanceof DivExpr) {
                    Pair<Double, Double> reciprocal_abstract;
                    if (var2_abstract.getO1() > 0 || var2_abstract.getO2() < 0) { // 0 not in range of second operand
                        reciprocal_abstract = new Pair<>(1 / var2_abstract.getO2(), 1 / var2_abstract.getO1());
                    } else if (var2_abstract.getO1() == 0) { // 0 is lower bound of second operand
                        reciprocal_abstract = new Pair<>(1 / var2_abstract.getO2(), Double.POSITIVE_INFINITY);
                    } else if (var2_abstract.getO2() == 0) { // 0 is upper bound of second operand
                        reciprocal_abstract = new Pair<>(Double.NEGATIVE_INFINITY, 1 / var2_abstract.getO1());
                    } else { // 0 in middle of range of second operand
                        // being conservative by assigning lattice value T, could alternatively retain more information
                        // by taking union of 2 disjoint intervals
                        reciprocal_abstract = new Pair<>(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
                    }
                    Double candidate_1 = var1_abstract.getO1() * reciprocal_abstract.getO1();
                    Double candidate_2 = var1_abstract.getO1() * reciprocal_abstract.getO2();
                    Double candidate_3 = var1_abstract.getO2() * reciprocal_abstract.getO1();
                    Double candidate_4 = var1_abstract.getO2() * reciprocal_abstract.getO2();
                    Double low = Math.min(Math.min(Math.min(candidate_1, candidate_2), candidate_3), candidate_4);
                    Double high = Math.max(Math.max(Math.max(candidate_1, candidate_2), candidate_3), candidate_4);
                    outValue.map.put(var, new Pair<>(low, high));
                }
            } else if (expr instanceof Local) {
                var_right = (Local) expr;
                Pair<Double, Double> abs = inValue.map.get(var_right);
                outValue.map.put(var, abs);
            } else if (expr instanceof NewArrayExpr) {
                NewArrayExpr new_arr_expr = (NewArrayExpr) expr;
                IntConstant arr_len_const = (IntConstant)(new_arr_expr.getSize());
                this.array_len_map.put(var,arr_len_const.value);
//                System.out.println(array_len_map);
            }
        } else if (stmt instanceof IfStmt) { // apply widening operator at head of loop
            Sigma prev = this.program_map.get(stmt.getJavaSourceStartLineNumber());
            if (prev == null) { // reaching head of loop for first time, initialize all variables as bottom
                prev = new Sigma(this.locals, new Pair<>(Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY));
            }
            Sigma curr = inValue;
            Sigma res = wideningOperator(prev, curr);
            this.copy(res, outValue);
            inValue = res;
        } else if (stmt instanceof GotoStmt) { // don't update sigma_out for GotoStmt
            ;
        } else {
            ;
        }
        if (stmt instanceof IfStmt) { // store sigma after applying widening operator as most recent sigma_in for that line
            this.program_map.put(stmt.getJavaSourceStartLineNumber(), inValue);
        }
        // System.out.printf("Set %d to %s with %s\n", stmt.getJavaSourceStartLineNumber(), inValue.toString(), stmt.getClass().getSimpleName());
    }

    /**
     * Widening operator to make the analysis terminate by making the height of the lattice finite
     *
     * @param prev  The previous Sigma_in at a particular line in the program
     * @param curr  The current Sigma_in at the same line in the program
     */
    public Sigma wideningOperator(Sigma prev, Sigma curr) {
        Pair<Double, Double> bottom = new Pair<>(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
        Sigma res = new Sigma();
        for (Map.Entry<Local,Pair<Double,Double>> entry: prev.map.entrySet()) {
            Local var = entry.getKey();
            Pair<Double,Double> prev_range = entry.getValue();
            Pair<Double,Double> curr_range = curr.map.get(var);
            if (prev_range.equals(bottom)) {
                res.map.put(var,curr_range);
            } else {
                double prev_low = prev_range.getO1();
                double prev_high = prev_range.getO2();
                double curr_low = curr_range.getO1();
                double curr_high = curr_range.getO2();
                double new_low, new_high;
                if (prev_low <= curr_low) {
                    new_low = prev_low;
                } else {
                    new_low = Double.NEGATIVE_INFINITY;
                }
                if (prev_high >= curr_high) {
                    new_high = prev_high;
                } else {
                    new_high = Double.POSITIVE_INFINITY;
                }
                res.map.put(var,new Pair<>(new_low,new_high));
            }
        }
        return res;
    }

    /**
     * Initial flow information at the start of a method
     */
    @Override
    protected Sigma entryInitialFlow() {
        if (this.sigma_i != null) {
            return this.sigma_i;
        } else {
            Pair<Double,Double> initialVal = new Pair<>(Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY);
            return new Sigma(this.locals, initialVal);
        }
    }

    /**
     * Initial flow information at each other program point
     */
    @Override
    protected Sigma newInitialFlow() {
        Pair<Double,Double> initialVal = new Pair<>(Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY);
        return new Sigma(this.locals, initialVal);
    }

    /**
     * Join at a program point lifted to sets
     */
    @Override
    protected void merge(Sigma in1, Sigma in2, Sigma out) {
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
        for (Map.Entry<Local,Pair<Double,Double>> entry : source.map.entrySet()) {
            dest.map.put(entry.getKey(), entry.getValue());
        }
    }
}

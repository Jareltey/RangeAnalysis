package hw5;

import soot.Local;
import soot.SootMethod;
import soot.toolkits.scalar.Pair;

import java.lang.*;
import java.util.HashMap;
import java.util.*;
/**
 * A class to represent abstract values at a program point.
 */
public class Sigma {
    /**
     * Abstract values
     * Elements of lattice are Pair<Double,Double>
     */

    // Maps locals to abstract values
    public Map<Local, Pair<Double,Double>> map;

    /**
     * An empty sigma
     */
    public Sigma() {
        this.map = new HashMap<>();
    }

    /**
     * An initialized sigma
     * @param locals the locals at this point
     * @param initialVal initial value to use
     */
    public Sigma(Iterable<Local> locals, Pair<Double,Double> initialVal) {
        this.map = new HashMap<>();
        for (Local l : locals) {
            this.map.put(l, initialVal);
        }
    }

    /**
     * Join for two abstract values
     */
    public static Pair<Double,Double> join(Pair<Double,Double> v1, Pair<Double,Double> v2) {
        Double min = Math.min(v1.getO1(), v2.getO1());
        Double max = Math.max(v1.getO2(), v2.getO2());
        Pair<Double,Double> res = new Pair<>(min,max);
        return res;
    }

    public String toString() {
        Set<Local> keys = map.keySet();
        StringBuilder str = new StringBuilder("[ ");
        for (Local key : keys) {
            str.append(key.getName()).append(": ").append(map.get(key)).append("; ");
        }
        return str + " ]";
    }

    public void copy(Sigma dest) {
        dest.map = new HashMap<>(map);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {return false;}
        if (this == obj) {return true;}
        return (obj instanceof Sigma) && (this.map.equals(((Sigma) obj).map));
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }
}


package hw5;

import jdk.internal.net.http.common.Pair;
import soot.Local;
import soot.SootMethod;
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

//    enum L {
//        Top, Bottom, N, P, Z
//    }

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
        // TODO: Implement union
        Double min = Math.min(v1.first, v2.first);
        Double max = Math.max(v1.second, v2.second);
        Pair<Double,Double> res = new Pair<Double,Double>(min,max);
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
        // TODO: Implement me!
        if (obj == null) {return false;}
        if (this == obj) {return true;}
        return (obj instanceof Sigma) && (this.map.equals(((Sigma) obj).map));
    }

    @Override
    public int hashCode() {
        // TODO: Implement me!
        return this.toString().hashCode();
    }
}


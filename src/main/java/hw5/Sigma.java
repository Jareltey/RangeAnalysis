package hw5;

import soot.Local;
import soot.SootMethod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A class to represent abstract values at a program point.
 */
public class Sigma {
    /**
     * Abstract values
     */
    enum L {
        Top, Bottom, N, P, Z
    }

    // Maps locals to abstract values
    public Map<Local, L> map;

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
    public Sigma(Iterable<Local> locals, L initialVal) {
        this.map = new HashMap<>();
        for (Local l : locals) {
            this.map.put(l, initialVal);
        }
    }

    /**
     * Join for two abstract values
     */
    public static L join(L v1, L v2) {
        // TODO: Implement union
        if (v1 == L.Top || v2 == L.Top) {return L.Top;}
        else if (v1 == L.P && (v2 == L.Bottom || v2 == L.P)) {return L.P;}
        else if (v1 == L.P && (v2 == L.N || v2 == L.Z)) {return L.Top;}
        else if (v2 == L.P && (v1 == L.Bottom || v1 == L.P)) {return L.P;}
        else if (v2 == L.P && (v1 == L.N || v1 == L.Z)) {return L.Top;}
        else if (v1 == L.N && (v2 == L.Bottom || v2 == L.N)) {return L.N;}
        else if (v1 == L.N && v2 == L.Z) {return L.Top;}
        else if (v2 == L.N && (v1 == L.Bottom || v1 == L.N)) {return L.N;}
        else if (v2 == L.N && v1 == L.Z) {return L.Top;}
        else if (v1 == L.Bottom && v2 == L.Bottom) {return L.Bottom;}
        else {return L.Z;}
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
        return true;
    }

    @Override
    public int hashCode() {
        // TODO: Implement me!
        return 0;
    }
}
package inputs;

public class IntraTest {

    public static void test0() {
        double a,b,c;
        int ignore;
        int[] array = new int[5];
        a = 2.0; // a -> [2.0,2.0]
        b = 2.0; // b -> [2.0,2.0]
        c = a - b; // c -> [0.0,0.0]
        ignore = array[(int)c]; // OK
        while (c != b) {
            c += 1.0; // 1st iteration: c -> [1.0,1.0]
                      // 2nd iteration: c -> [1.0,+∞]
            ignore = array[(int)c]; // 1st iteration: OK
                                    // 2nd iteration: WARNING
        } // c -> [0.0,+∞]
        ignore = array[(int)c]; // WARNING
    }

    public static void test1() {
        double a,b,c,d,e;
        int ignore;
        int[] array = new int[5];
        a = 5.0; // a -> [5.0,5.0]
        b = 2.0; // b -> [2.0,2.0]
        c = a - b; // c -> [3.0,3.0]
        ignore = array[(int)c]; // OK
        d = b + c; // d -> [5.0,5.0]
        ignore = array[(int)d]; // ERROR
        e = b - a; // e -> [-3.0,-3.0]
        ignore = array[(int)e]; // ERROR
    }

    public static void test2() {
        double a,b,c,d,e;
        int ignore;
        int[] array = new int[7];
        a = 1.0; // a -> [1.0,1.0]
        b = 2.0; // b -> [2.0,2.0]
        if (a != b) {
            a += 1.0; // a -> [2.0,2.0]
        } else {
            a -= 1.0; // a -> [0.0,0.0]
        } // a -> [0.0,2.0]
        ignore = array[(int)a]; // OK
        if (b != a) {
            b *= 2; // b -> [4.0,4.0]
        } else {
            b /= 2; // b -> [1.0,1.0]
        } // b -> [1.0,4.0]
        ignore = array[(int)b]; // OK
        c = a + b; // c -> [1.0,6.0]
        ignore = array[(int)c]; // OK
        d = b - a; // d -> [-1.0,4.0]
        ignore = array[(int)d]; // WARNING
        e = a * b; // e -> [0.0,8.0]
        ignore = array[(int)e]; // WARNING
    }

    public static void test3() {
        double a,b,c;
        int ignore;
        int[] array = new int[5];
        a = getInt(); // a -> [-∞,+∞]
        b = 1.0; // b -> [1.0,1.0]
        c = a / b; // c -> [-∞,+∞]
        ignore = array[(int)c]; // WARNING
    }

    public static void test4() {
        double a,b,c,d,e,f;
        int ignore;
        int[] array = new int[5];
        a = 2.0; // a -> [2.0,2.0]
        b = 6.0; // b -> [6.0,6.0]
        if (condition()) {
            c = b - a; // c -> [4.0,4.0]
        } else {
            c = b + a; // c -> [8.0,8.0]
        } // c -> [4.0,8.0]
        ignore = array[(int)c]; // WARNING
        if (a > 0) {
            d = a; // d -> [2.0,2.0]
        } else {
            d = b - a; // d -> [4.0,4.0]
        } // d -> [2.0,4.0]
        ignore = array[(int)d]; // OK
        e = c / d; // e -> [1.0,4.0]
        ignore = array[(int)e]; // OK
        d -= 4.0; // d -> [-2.0,0.0]
        f = c / d; // f -> [-∞,-2.0]
        ignore = array[(int)f]; // ERROR
    }

    public static int getInt() {
        return 0;
    }

    public static boolean condition() {
        return true;
    }
}

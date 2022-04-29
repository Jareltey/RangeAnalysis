package inputs;

public class IntraTest {

    // Encountered an issue - statements where a variable is assigned a value only appear in the unit graph
    // when I subsequently index into an array using that variable. As a result, dataflow analysis (keeping
    // track of the range of variables) is only possible when statements where I index into an array using the
    // variables are present.

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
        }
                                // c -> [0.0,+∞]
        ignore = array[(int)c]; // WARNING
    }

//    public static void test1() {
//        double a,b,c,d;
//        int ignore;
//        int[] array = new int[5];
//        a = 5.0; // a -> [5.0,5.0]
//        b = 2.0; // b -> [2.0,2.0]
//        c = a - b; // c -> [3.0,3.0]
//        d = b + c; // d -> [5.0,5.0]
//        ignore = array[(int)c]; // OK
//        ignore = array[(int)d]; // ERROR
//    }

//    public static void test2() {
//        double a,b,c,d,e;
//        int ignore;
//        int[] array = new int[7];
//        a = 1.0; // a -> [1.0,1.0]
//        b = 2.0; // b -> [2.0,2.0]
//        if (a != b) {
//            a += 1.0; // a -> [2.0,2.0]
//        } else {
//            a -= 1.0; // a -> [0.0,0.0]
//        } // a -> [0.0,2.0]
//        ignore = array[(int)a]; // OK
//        if (b != a) {
//            b *= 2; // b -> [4.0,4.0]
//        } else {
//            b /= 2; // b -> [1.0,1.0]
//        } // b -> [1.0,4.0]
//        ignore = array[(int)b]; // OK
//        c = a + b; // c -> [1.0,6.0]
//        ignore = array[(int)c]; // OK
//        d = b - a; // d -> [-1.0,4.0]
//        ignore = array[(int)d]; // WARNING
//        e = a * b; // e -> [0.0,8.0]
//        ignore = array[(int)e]; // WARNING
//    }

//    public static void test3() {
//        double a,b,c,d;
//        int ignore;
//        int[] array = new int[5];
//        a = getInt(); // a -> [-∞,+∞]
//        b = 1.0; // b -> [1.0,1.0]
//        c = a / b; // c -> [-∞,+∞]
//        ignore = array[(int)c]; // WARNING
//    }


//    public static void test1() {
//        int x, y, z, w;
//        int[] array = new int[5];
//        x = 0;
//        y = 5;
//        z = -3;
//        y = y * y;
//        if (condition())
//            w = y * x;
//        else
//            w = x * z;
//        while (condition())
//            z = y * z;
//        int ignore = array[w];
//        ignore = array[x];
//        ignore = array[y];
//        ignore = array[z]; // ERROR
//    }



//    public static void test4() {
//        int x, y, z, w;
//        int[] array = new int[5];
//        y = 5;
//        z = -3;
//        if (condition())
//            w = y;
//        else
//            w = z;
//        int ignore = array[w]; // WARNING
//    }

//    public static void test5() {
//        int x, y, z, w;
//        int[] array = new int[5];
//        x = getInt();
//        y = 5;
//        z = x * y;
//        int ignore = array[z]; // WARNING
//    }

    public static int getInt() {
        return 0;
    }

    public static boolean condition() {
        return true;
    }
}

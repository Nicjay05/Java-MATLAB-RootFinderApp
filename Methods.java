package com.mycompany.findingtheroots;

public class Methods {

    
    // BISECTION METHOD
    
    public static void bisection(double xl, double xu, int maxIter) {
        double xr = 0;
        System.out.println("Bisection Method");
        System.out.println("Iter\tXl\t\tXu\t\tXr\t\tf(Xr)");
        System.out.println("-".repeat(70));

        for (int i = 1; i <= maxIter; i++) {
            xr = (xl + xu) / 2;
            double fxl = Function.f(xl);
            double fxr = Function.f(xr);

            System.out.printf("%d\t%.6f\t%.6f\t%.6f\t%.8f%n", i, xl, xu, xr, fxr);

            if (fxl * fxr < 0)
                xu = xr;
            else
                xl = xr;
        }

        System.out.println("-".repeat(70));
        System.out.println("Approx root: " + xr);
    }

   
    // FALSE POSITION METHOD
    
    public static void falsePosition(double xl, double xu, int maxIter) {
        double xr = 0;
        System.out.println("False Position Method");
        System.out.println("Iter\tXl\t\tXu\t\tXr\t\tf(Xr)");
        System.out.println("-".repeat(70));

        for (int i = 1; i <= maxIter; i++) {
            double fxl = Function.f(xl);
            double fxu = Function.f(xu);

            xr = xu - (fxu * (xl - xu)) / (fxl - fxu);
            double fxr = Function.f(xr);

            System.out.printf("%d\t%.6f\t%.6f\t%.6f\t%.8f%n", i, xl, xu, xr, fxr);

            if (fxl * fxr < 0)
                xu = xr;
            else
                xl = xr;
        }

        System.out.println("-".repeat(70));
        System.out.println("Approx root: " + xr);
    }

    
    // NEWTON-RAPHSON METHOD
    
    public static void newtonRaphson(double x0, int maxIter) {
        double x = x0;
        System.out.println("Newton-Raphson Method");
        System.out.println("Iter\tX\t\tf(X)\t\tf'(X)");
        System.out.println("-".repeat(60));

        for (int i = 1; i <= maxIter; i++) {
            double fx  = Function.f(x);
            double dfx = Function.df(x);

            x = x - fx / dfx;

            System.out.printf("%d\t%.8f\t%.8f\t%.8f%n", i, x, fx, dfx);
        }

        System.out.println("-".repeat(60));
        System.out.println("Approx root: " + x);
    }

    
    // SECANT METHOD
    
    public static void secant(double x0, double x1, int maxIter) {
        double x2 = 0;
        System.out.println("Secant Method");
        System.out.println("Iter\tx0\t\tx1\t\tx2\t\tf(x2)");
        System.out.println("-".repeat(70));

        for (int i = 1; i <= maxIter; i++) {
            x2 = x1 - (Function.f(x1) * (x0 - x1)) /
                       (Function.f(x0) - Function.f(x1));

            System.out.printf("%d\t%.6f\t%.6f\t%.6f\t%.8f%n", i, x0, x1, x2, Function.f(x2));

            x0 = x1;
            x1 = x2;
        }

        System.out.println("-".repeat(70));
        System.out.println("Approx root: " + x1);
    }

    
    // INCREMENTAL SEARCH
    // Now shows a full iteration table with columns:
    //   Iter | x1 | x2 | f(x1) | f(x2) | Sign change?
    
    public static void incremental(double start, double step, int maxIter) {
        System.out.println("Incremental Search  (step = " + step + ")");
        System.out.printf("%-6s %-12s %-12s %-14s %-14s %s%n",
            "Iter", "x1", "x2", "f(x1)", "f(x2)", "Sign Change?");
        System.out.println("-".repeat(75));

        double x1 = start;
        boolean found = false;

        for (int i = 1; i <= maxIter; i++) {
            double x2  = x1 + step;
            double fx1 = Function.f(x1);
            double fx2 = Function.f(x2);
            boolean signChange = fx1 * fx2 < 0;

            System.out.printf("%-6d %-12.6f %-12.6f %-14.8f %-14.8f %s%n",
                i, x1, x2, fx1, fx2, signChange ? "<-- root here" : "");

            if (signChange) {
                System.out.println("-".repeat(75));
                System.out.printf("Root found between %.6f and %.6f%n", x1, x2);
                found = true;
                break;
            }

            x1 = x2;
        }

        if (!found) {
            System.out.println("-".repeat(75));
            System.out.println("No root found in the search range.");
        }
    }
}
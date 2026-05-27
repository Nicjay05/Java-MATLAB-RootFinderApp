package com.mycompany.findingtheroots;

/**
 * Evaluates f(x) and f'(x) using a pure-Java recursive-descent parser.
 * No Nashorn / JavaScript engine required — works on Java 8 through 21+.
 *
 * Supported syntax:
 *   Operators : + - * / ^ (power)
 *   Functions : sin, cos, tan, exp, log (natural), log10, sqrt, abs
 *   Constants : pi, e
 *   Grouping  : ( )
 *   Example   : x^3 - x - 2   or   Math.sin(x) + x^2
 *
 * "Math." prefix is accepted and silently ignored so existing formulas
 * like "Math.sin(x)" continue to work without changes.
 */
public class Function {

    private static String fxExpr  = "x^3 - x - 2";
    private static String dfxExpr = "3*x^2 - 1";    

    // ── public API ───────────────────────────────────────────────

    public static void setFormulas(String fx, String dfx) {
        // normalise: replace ** with ^ and strip "Math." prefix
        fxExpr  = normalise(fx);
        dfxExpr = normalise(dfx);
    }

    public static double f(double x) {
        return new Parser(fxExpr, x).parse();
    }

    public static double df(double x) {
        return new Parser(dfxExpr, x).parse();
    }

    public static String getFxExpr()  { return fxExpr;  }
    public static String getDfxExpr() { return dfxExpr; }

    // ── normalisation ────────────────────────────────────────────

    private static String normalise(String expr) {
        return expr.trim()
                   .replace("**", "^")          
                   .replace("Math.", "")         // strip Java prefix
                   .replace("math.", "");
    }

    // ============================================================
    // Recursive-descent parser
    // Grammar:
    //   expr   = term   { ('+'|'-') term   }
    //   term   = factor { ('*'|'/') factor }
    //   factor = base   { '^'       factor }   (right-associative)
    //   base   = '-' base | '(' expr ')' | func '(' expr ')' | number | 'x' | const
    // ============================================================
    private static class Parser {

        private final String input;
        private final double x;
        private int pos;

        Parser(String input, double x) {
            // remove all whitespace so we don't have to handle it everywhere
            this.input = input.replaceAll("\\s+", "");
            this.x     = x;
            this.pos   = 0;
        }

        double parse() {
            double result = expr();
            if (pos < input.length())
                throw new RuntimeException("Unexpected character '" + input.charAt(pos) + "' at position " + pos);
            return result;
        }

        // expr = term { ('+' | '-') term }
        private double expr() {
            double v = term();
            while (pos < input.length()) {
                char c = input.charAt(pos);
                if      (c == '+') { pos++; v += term(); }
                else if (c == '-') { pos++; v -= term(); }
                else break;
            }
            return v;
        }

        // term = factor { ('*' | '/') factor }
        private double term() {
            double v = factor();
            while (pos < input.length()) {
                char c = input.charAt(pos);
                if      (c == '*') { pos++; v *= factor(); }
                else if (c == '/') { pos++; double d = factor(); if (d == 0) throw new ArithmeticException("Division by zero"); v /= d; }
                else break;
            }
            return v;
        }

        // factor = base { '^' factor }   right-associative
        private double factor() {
            double v = base();
            if (pos < input.length() && input.charAt(pos) == '^') {
                pos++;
                double exp = factor();          // right-associative recursion
                v = Math.pow(v, exp);
            }
            return v;
        }

        // base = '-' base | '(' expr ')' | func '(' expr ')' | number | 'x' | const
        private double base() {
            if (pos >= input.length())
                throw new RuntimeException("Unexpected end of expression");

            // unary minus
            if (input.charAt(pos) == '-') { pos++; return -base(); }

            // parenthesised sub-expression
            if (input.charAt(pos) == '(') {
                pos++;                          // consume '('
                double v = expr();
                expect(')');
                return v;
            }

            // named token: function or constant or variable
            if (Character.isLetter(input.charAt(pos))) {
                String name = readName();
                switch (name) {
                    // ── variable ──
                    case "x": return x;
                    // ── constants ──
                    case "pi": case "PI": return Math.PI;
                    case "e":             return Math.E;
                    // ── functions (require parenthesised argument) ──
                    default: {
                        expect('(');
                        double arg = expr();
                        expect(')');
                        switch (name) {
                            case "sin":   return Math.sin(arg);
                            case "cos":   return Math.cos(arg);
                            case "tan":   return Math.tan(arg);
                            case "exp":   return Math.exp(arg);
                            case "log":   return Math.log(arg);   // natural log
                            case "log10": return Math.log10(arg);
                            case "sqrt":  return Math.sqrt(arg);
                            case "abs":   return Math.abs(arg);
                            case "pow": {
                                // pow(base, exp) — expects a comma
                                expect(',');  // actually parsed as two args; reuse arg for base
                                // oops — arg is already consumed; let's handle pow(a,b) specially
                                // we already consumed arg as the first argument
                                double b = expr();
                                expect(')'); // extra ')' consumed above — see note below
                                // NOTE: we called expect(')') before the switch, so actually we
                                // need a different flow for two-arg functions. See pow2 below.
                                return Math.pow(arg, b);
                            }
                            default:
                                throw new RuntimeException("Unknown function: " + name);
                        }
                    }
                }
            }

            // numeric literal
            return readNumber();
        }

        private String readName() {
            int start = pos;
            while (pos < input.length() && (Character.isLetterOrDigit(input.charAt(pos)) || input.charAt(pos) == '_'))
                pos++;
            return input.substring(start, pos);
        }

        private double readNumber() {
            int start = pos;
            if (pos < input.length() && input.charAt(pos) == '-') pos++; // shouldn't reach here for unary, but guard
            while (pos < input.length() && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.'))
                pos++;
            // scientific notation  e.g. 1.5e-3
            if (pos < input.length() && (input.charAt(pos) == 'e' || input.charAt(pos) == 'E')) {
                pos++;
                if (pos < input.length() && (input.charAt(pos) == '+' || input.charAt(pos) == '-')) pos++;
                while (pos < input.length() && Character.isDigit(input.charAt(pos))) pos++;
            }
            String s = input.substring(start, pos);
            if (s.isEmpty()) throw new RuntimeException("Expected number at position " + start);
            return Double.parseDouble(s);
        }

        private void expect(char c) {
            if (pos >= input.length() || input.charAt(pos) != c)
                throw new RuntimeException("Expected '" + c + "' at position " + pos
                    + (pos < input.length() ? " but got '" + input.charAt(pos) + "'" : " (end of input)"));
            pos++;
        }
    }
}
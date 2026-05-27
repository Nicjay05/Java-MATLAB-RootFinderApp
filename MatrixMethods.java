package com.mycompany.findingtheroots;

public class MatrixMethods {

    // ============================================================
    // 1. MATRIX ADDITION
    // ============================================================
    public static double[][] add(double[][] A, double[][] B) {
        int n = A.length, m = A[0].length;
        if (B.length != n || B[0].length != m)
            throw new IllegalArgumentException("Matrices must be the same size for addition.");

        double[][] result = new double[n][m];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < m; j++)
                result[i][j] = A[i][j] + B[i][j];
        return result;
    }

    // ============================================================
    // 2. MATRIX MULTIPLICATION
    // ============================================================
    public static double[][] multiply(double[][] A, double[][] B) {
        int rowsA = A.length, colsA = A[0].length, colsB = B[0].length;
        if (colsA != B.length)
            throw new IllegalArgumentException(
                "Columns of A (" + colsA + ") must equal rows of B (" + B.length + ").");

        double[][] result = new double[rowsA][colsB];
        for (int i = 0; i < rowsA; i++)
            for (int j = 0; j < colsB; j++)
                for (int k = 0; k < colsA; k++)
                    result[i][j] += A[i][k] * B[k][j];
        return result;
    }

    // ============================================================
    // 3. TRANSPOSE
    // ============================================================
    public static double[][] transpose(double[][] A) {
        int rows = A.length, cols = A[0].length;
        double[][] result = new double[cols][rows];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                result[j][i] = A[i][j];
        return result;
    }

    // ============================================================
    // 4. DETERMINANT (recursive cofactor expansion)
    // ============================================================
    public static double determinant(double[][] A) {
        int n = A.length;
        if (n != A[0].length)
            throw new IllegalArgumentException("Matrix must be square.");
        if (n == 1) return A[0][0];
        if (n == 2) return A[0][0] * A[1][1] - A[0][1] * A[1][0];

        double det = 0;
        for (int col = 0; col < n; col++)
            det += Math.pow(-1, col) * A[0][col] * determinant(getSubMatrix(A, 0, col));
        return det;
    }

    // ============================================================
    // 5. ADJOINT (transpose of cofactor matrix)
    // ============================================================
    public static double[][] adjoint(double[][] A) {
        int n = A.length;
        if (n != A[0].length)
            throw new IllegalArgumentException("Matrix must be square.");

        double[][] cofactorMatrix = new double[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                cofactorMatrix[i][j] = Math.pow(-1, i + j) * determinant(getSubMatrix(A, i, j));
        return transpose(cofactorMatrix);
    }

    // ============================================================
    // 6. INVERSE  (adjoint / determinant)
    // ============================================================
    public static double[][] inverse(double[][] A) {
        int n = A.length;
        if (n != A[0].length)
            throw new IllegalArgumentException("Matrix must be square.");

        double det = determinant(A);
        if (Math.abs(det) < 1e-10)
            throw new ArithmeticException("Matrix is singular — inverse does not exist.");

        double[][] adj = adjoint(A);
        double[][] inv = new double[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                inv[i][j] = adj[i][j] / det;
        return inv;
    }

    // ============================================================
    // 7. MATRIX POWER  (A^exp via repeated multiplication)
    // ============================================================
    public static double[][] power(double[][] A, int exp) {
        int n = A.length;
        if (n != A[0].length)
            throw new IllegalArgumentException("Matrix must be square.");
        if (exp < 0)
            throw new IllegalArgumentException("Exponent must be non-negative.");

        // Identity for power 0
        if (exp == 0) {
            double[][] identity = new double[n][n];
            for (int i = 0; i < n; i++) identity[i][i] = 1;
            return identity;
        }

        double[][] result = deepCopy(A);
        for (int p = 1; p < exp; p++)
            result = multiply(result, A);
        return result;
    }

    // ============================================================
    // 8. SOLVE  Ax = b  via Gaussian elimination
    //    Pass the augmented matrix [A | b]
    // ============================================================
    public static double[] solveEquations(double[][] augmented) {
        int n = augmented.length;
        for (double[] row : augmented)
            if (row.length != n + 1)
                throw new IllegalArgumentException("Augmented matrix must be n × (n+1).");

        double[][] mat = new double[n][n + 1];
        for (int i = 0; i < n; i++) mat[i] = augmented[i].clone();

        // Forward elimination with partial pivoting
        for (int col = 0; col < n; col++) {
            int maxRow = col;
            for (int row = col + 1; row < n; row++)
                if (Math.abs(mat[row][col]) > Math.abs(mat[maxRow][col])) maxRow = row;

            double[] temp = mat[col]; mat[col] = mat[maxRow]; mat[maxRow] = temp;

            if (Math.abs(mat[col][col]) < 1e-10)
                throw new ArithmeticException("System has no unique solution.");

            for (int row = col + 1; row < n; row++) {
                double factor = mat[row][col] / mat[col][col];
                for (int k = col; k <= n; k++)
                    mat[row][k] -= factor * mat[col][k];
            }
        }

        // Back substitution
        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            x[i] = mat[i][n];
            for (int j = i + 1; j < n; j++) x[i] -= mat[i][j] * x[j];
            x[i] /= mat[i][i];
        }
        return x;
    }

    // ============================================================
    // HELPERS
    // ============================================================
    private static double[][] getSubMatrix(double[][] A, int removeRow, int removeCol) {
        int n = A.length;
        double[][] sub = new double[n - 1][n - 1];
        int ri = 0;
        for (int i = 0; i < n; i++) {
            if (i == removeRow) continue;
            int ci = 0;
            for (int j = 0; j < n; j++) {
                if (j == removeCol) continue;
                sub[ri][ci++] = A[i][j];
            }
            ri++;
        }
        return sub;
    }

    private static double[][] deepCopy(double[][] A) {
        double[][] copy = new double[A.length][A[0].length];
        for (int i = 0; i < A.length; i++) copy[i] = A[i].clone();
        return copy;
    }

    public static String matrixToString(double[][] A) {
        StringBuilder sb = new StringBuilder();
        for (double[] row : A) {
            sb.append("[ ");
            for (double val : row) sb.append(String.format("%10.4f ", val));
            sb.append("]\n");
        }
        return sb.toString();
    }

    public static double[][] parseMatrix(String text) {
        String[] lines = text.trim().split("\\n");
        int rows = lines.length;
        String[] firstTokens = lines[0].trim().split("\\s+");
        int cols = firstTokens.length;

        double[][] matrix = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            String[] tokens = lines[i].trim().split("\\s+");
            if (tokens.length != cols)
                throw new IllegalArgumentException(
                    "Row " + (i + 1) + " has " + tokens.length + " values, expected " + cols + ".");
            for (int j = 0; j < cols; j++)
                matrix[i][j] = Double.parseDouble(tokens[j]);
        }
        return matrix;
    }
}
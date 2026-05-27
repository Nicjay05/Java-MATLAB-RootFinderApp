package com.mycompany.findingtheroots;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class RootSolverGUI extends JFrame {

    // ── Colors (Navy Blue & Beige theme) ────────────────────────
    private static final Color BG_DARK    = new Color(10,  20,  50);   // deep navy
    private static final Color BG_PANEL   = new Color(16,  30,  70);   // navy panel
    private static final Color BG_INPUT   = new Color(22,  40,  90);   // input fields
    private static final Color ACCENT     = new Color(196, 178, 140);  // khaki/beige
    private static final Color ACCENT2    = new Color(220, 205, 165);  // light beige
    private static final Color TXT_PRIMARY= new Color(237, 230, 210);  // warm off-white
    private static final Color TXT_MUTED  = new Color(140, 150, 160);  // muted gray-blue
    private static final Color CLR_DANGER = new Color(180,  55,  55);  // muted red
    private static final Color CLR_SOLVE  = new Color(196, 178, 140);  // beige for solve btn

    // ── Equation presets (label → fx expression, dfx expression) ─
    private static final String[][] PRESETS = {
        { "-- Select Preset --",         "",                    ""             },
        { "Cubic",                        "x^3 - x - 2",        "3*x^2 - 1"   },
        { "Quadratic",                    "x^2 - 4",            "2*x"          },
        { "Simple Polynomial",            "x^3 - 2*x^2 + x - 1","3*x^2 - 4*x + 1"},
        { "Exponential",                  "exp(x) - 3*x",       "exp(x) - 3"  },
        { "Mixed Polynomial",             "x^4 - 3*x^3 + x^2 - 2*x + 1",
                                          "4*x^3 - 9*x^2 + 2*x - 2"           },
        { "High Degree Polynomial",       "x^6 - 7*x^5 + x^4 - 3*x^2 + 2",
                                          "6*x^5 - 35*x^4 + 4*x^3 - 6*x"     },
        { "Newton-Raphson (classic)",     "x^3 - 2*x - 5",      "3*x^2 - 2"   },
        { "Trigonometric",                "sin(x) - x/2",       "cos(x) - 0.5"},
        { "Logarithmic",                  "log(x) - x + 2",     "1/x - 1"     },
    };

    // ── Root Finder components ───────────────────────────────────
    private JTextField xlField, xuField, x0Field, iterField;
    private JTextArea  outputArea;
    private JComboBox<String> methodBox;

    // ── Equation Editor components ───────────────────────────────
    private JComboBox<String> presetBox;
    private JTextField fxField, dfxField;

    // ── Graph ─────────────────────────────────────────────────────
    private GraphPlotter graphPlotter;

    // ── Matrix components ────────────────────────────────────────
    private JComboBox<String> sizeBox;
    private JTextArea matrixInputArea, matrixOutputArea;

    public RootSolverGUI() {
        setTitle("Root Solver & Matrix Calculator");
        setSize(1200, 820);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        styleTabPane(tabs);
        tabs.addTab("  Root Finder + Graph  ", createRootPanel());
        tabs.addTab("  Matrix Operations  ",   createMatrixPanel());
        add(tabs, BorderLayout.CENTER);

        // Bottom bar
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 6));
        bottom.setBackground(BG_PANEL);
        bottom.setBorder(new MatteBorder(1, 0, 0, 0, ACCENT.darker()));
        bottom.add(makeButton("Clear All", CLR_DANGER, TXT_PRIMARY, e -> clearAll()));
        bottom.add(makeButton("Exit",      CLR_DANGER, TXT_PRIMARY, e -> System.exit(0)));
        add(bottom, BorderLayout.SOUTH);
    }

    // ============================================================
    // ROOT FINDER PANEL
    // ============================================================
    private JPanel createRootPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel left = new JPanel(new BorderLayout(0, 8));
        left.setBackground(BG_DARK);
        left.setPreferredSize(new Dimension(380, 0));
        left.add(createEquationEditorPanel(), BorderLayout.NORTH);
        left.add(createParametersPanel(),     BorderLayout.CENTER);

        graphPlotter = new GraphPlotter();
        graphPlotter.setBorder(navyTitledBorder("  Function Plot  "));

        panel.add(left,         BorderLayout.WEST);
        panel.add(graphPlotter, BorderLayout.CENTER);
        return panel;
    }

    // ============================================================
    // EQUATION EDITOR — mirrors the MATLAB preset + free-type layout
    // ============================================================
    private JPanel createEquationEditorPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BG_PANEL);
        panel.setBorder(navyTitledBorder("  Equation Editor  "));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(5, 6, 5, 6);
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Row 0 — preset label
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 3;
        panel.add(makeLabel("Preset:", TXT_MUTED, 10), gbc);

        // Row 1 — preset dropdown  (spans full width)
        gbc.gridy = 1; gbc.gridwidth = 3;
        String[] presetNames = new String[PRESETS.length];
        for (int i = 0; i < PRESETS.length; i++) presetNames[i] = PRESETS[i][0];
        presetBox = new JComboBox<>(presetNames);
        styleCombo(presetBox);
        presetBox.addActionListener(e -> onPresetSelected());
        panel.add(presetBox, gbc);

        // Row 2 — hint
        gbc.gridy = 2; gbc.gridwidth = 3;
        panel.add(makeLabel("  or type your own equation below", TXT_MUTED, 9), gbc);

        // Row 3 — f(x) label + field
        gbc.gridy = 3; gbc.gridwidth = 1; gbc.weightx = 0;
        panel.add(makeLabel("f(x) =", ACCENT2, 12), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        fxField = makeTextField("x^3 - x - 2");
        fxField.setToolTipText("Type any function of x, e.g. x^3 - x - 2, exp(x) - 3*x");
        panel.add(fxField, gbc);

        // Row 4 — f'(x) label + field + Apply
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1; gbc.weightx = 0;
        panel.add(makeLabel("f'(x) =", ACCENT2, 12), gbc);
        gbc.gridx = 1; gbc.gridwidth = 1; gbc.weightx = 1.0;
        dfxField = makeTextField("3*x^2 - 1");
        dfxField.setToolTipText("Derivative of f(x) — required for Newton-Raphson");
        panel.add(dfxField, gbc);
        gbc.gridx = 2; gbc.gridwidth = 1; gbc.weightx = 0;
        panel.add(makeButton("Apply", ACCENT, BG_DARK, e -> applyFormula()), gbc);

        return panel;
    }

    private void onPresetSelected() {
        int idx = presetBox.getSelectedIndex();
        if (idx == 0) return;                          // "-- Select Preset --"
        String fx  = PRESETS[idx][1];
        String dfx = PRESETS[idx][2];
        fxField .setText(fx);
        dfxField.setText(dfx);
        // Auto-apply so the graph updates immediately
        Function.setFormulas(fx, dfx);
        graphPlotter.refresh();
    }

    private void applyFormula() {
        String fx  = fxField .getText().trim();
        String dfx = dfxField.getText().trim();
        try {
            Function.setFormulas(fx, dfx);
            Function.f(1.0);
            Function.df(1.0);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Formula has a syntax error:\n" + ex.getMessage(),
                "Formula Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        graphPlotter.refresh();
    }

    // ============================================================
    // PARAMETERS PANEL
    // ============================================================
    private JPanel createParametersPanel() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.setBackground(BG_DARK);

        JPanel inputPanel = new JPanel(new GridLayout(6, 2, 6, 6));
        inputPanel.setBackground(BG_PANEL);
        inputPanel.setBorder(navyTitledBorder("  Parameters  "));

        inputPanel.add(makeLabel("Xl:", ACCENT, 11));
        xlField = makeTextField("1");
        inputPanel.add(xlField);

        inputPanel.add(makeLabel("Xu:", ACCENT, 11));
        xuField = makeTextField("2");
        inputPanel.add(xuField);

        inputPanel.add(makeLabel("Initial guess x₀:", ACCENT, 11));
        x0Field = makeTextField("1.5");
        inputPanel.add(x0Field);

        inputPanel.add(makeLabel("Iterations:", ACCENT, 11));
        iterField = makeTextField("10");
        inputPanel.add(iterField);

        inputPanel.add(makeLabel("Method:", ACCENT, 11));
        methodBox = new JComboBox<>(new String[]{
            "Bisection", "False Position", "Newton-Raphson", "Secant", "Incremental"
        });
        styleCombo(methodBox);
        inputPanel.add(methodBox);

        inputPanel.add(makeButton("Solve",         CLR_SOLVE, BG_DARK, e -> solve()));
        inputPanel.add(makeButton("Refresh Graph", BG_INPUT,  ACCENT,  e -> graphPlotter.refresh()));

        outputArea = new JTextArea();
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        outputArea.setEditable(false);
        outputArea.setBackground(BG_INPUT);
        outputArea.setForeground(ACCENT2);
        outputArea.setCaretColor(ACCENT2);
        JScrollPane scroll = new JScrollPane(outputArea);
        scroll.setBorder(navyTitledBorder("  Output  "));
        scroll.getViewport().setBackground(BG_INPUT);

        wrapper.add(inputPanel, BorderLayout.NORTH);
        wrapper.add(scroll,     BorderLayout.CENTER);
        return wrapper;
    }

    // ============================================================
    // MATRIX PANEL
    // ============================================================
    private JPanel createMatrixPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBackground(BG_PANEL);
        topPanel.setBorder(new MatteBorder(0, 0, 1, 0, ACCENT.darker()));
        topPanel.add(makeLabel("Matrix size:", ACCENT, 11));
        sizeBox = new JComboBox<>(new String[]{"2x2", "3x3", "4x4", "5x5"});
        styleCombo(sizeBox);
        topPanel.add(sizeBox);

        matrixInputArea = new JTextArea(10, 26);
        styleTextArea(matrixInputArea);
        JScrollPane inputScroll = new JScrollPane(matrixInputArea);
        inputScroll.setBorder(navyTitledBorder("  Matrix Input  (space-separated; blank line between two matrices)  "));
        inputScroll.getViewport().setBackground(BG_INPUT);

        matrixOutputArea = new JTextArea(10, 26);
        styleTextArea(matrixOutputArea);
        matrixOutputArea.setEditable(false);
        JScrollPane outputScroll = new JScrollPane(matrixOutputArea);
        outputScroll.setBorder(navyTitledBorder("  Result  "));
        outputScroll.getViewport().setBackground(BG_INPUT);

        JPanel opPanel = new JPanel(new GridLayout(4, 2, 6, 6));
        opPanel.setBackground(BG_PANEL);
        opPanel.setBorder(navyTitledBorder("  Operations  "));
        for (String op : new String[]{"Add","Multiply","Transpose","Determinant","Adjoint","Inverse","Power","Solve Equations"}) {
            opPanel.add(makeButton(op, BG_INPUT, ACCENT2, e -> performMatrixOperation(((JButton)e.getSource()).getText())));
        }

        JPanel center = new JPanel(new GridLayout(1, 2, 10, 0));
        center.setBackground(BG_DARK);
        center.add(inputScroll);
        center.add(outputScroll);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(center,   BorderLayout.CENTER);
        panel.add(opPanel,  BorderLayout.EAST);
        return panel;
    }

    // ============================================================
    // SOLVE
    // ============================================================
    private void solve() {
        try {
            double xl   = Double.parseDouble(xlField.getText());
            double xu   = Double.parseDouble(xuField.getText());
            double x0   = Double.parseDouble(x0Field.getText());
            int    iter = Integer.parseInt(iterField.getText());
            String method = (String) methodBox.getSelectedItem();

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.PrintStream ps  = new java.io.PrintStream(baos);
            java.io.PrintStream old = System.out;
            System.setOut(ps);

            switch (method) {
                case "Bisection"      -> Methods.bisection(xl, xu, iter);
                case "False Position" -> Methods.falsePosition(xl, xu, iter);
                case "Newton-Raphson" -> Methods.newtonRaphson(x0, iter);
                case "Secant"         -> Methods.secant(xl, x0, iter);
                case "Incremental"    -> Methods.incremental(xl, (xu - xl) / iter, iter);
            }

            System.out.flush();
            System.setOut(old);
            outputArea.setText(baos.toString());

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage());
        }
    }

    // ============================================================
    // MATRIX OPERATIONS
    // ============================================================
    private void performMatrixOperation(String operation) {
        try {
            String input = matrixInputArea.getText().trim();
            switch (operation) {
                case "Add", "Multiply" -> {
                    String[] parts = input.split("\\n\\n");
                    if (parts.length < 2) { matrixOutputArea.setText("Enter two matrices separated by a blank line."); return; }
                    double[][] A = MatrixMethods.parseMatrix(parts[0]);
                    double[][] B = MatrixMethods.parseMatrix(parts[1]);
                    matrixOutputArea.setText(MatrixMethods.matrixToString(
                        operation.equals("Add") ? MatrixMethods.add(A, B) : MatrixMethods.multiply(A, B)));
                }
                case "Power" -> {
                    String exp = JOptionPane.showInputDialog(this, "Enter exponent:");
                    if (exp == null) return;
                    matrixOutputArea.setText(MatrixMethods.matrixToString(
                        MatrixMethods.power(MatrixMethods.parseMatrix(input), Integer.parseInt(exp))));
                }
                case "Solve Equations" -> {
                    double[] sol = MatrixMethods.solveEquations(MatrixMethods.parseMatrix(input));
                    StringBuilder sb = new StringBuilder("Solution:\n");
                    for (int i = 0; i < sol.length; i++)
                        sb.append(String.format("x%d = %.6f%n", i + 1, sol[i]));
                    matrixOutputArea.setText(sb.toString());
                }
                case "Determinant" -> matrixOutputArea.setText(String.format("Determinant = %.6f",
                    MatrixMethods.determinant(MatrixMethods.parseMatrix(input))));
                default -> matrixOutputArea.setText(MatrixMethods.matrixToString(switch (operation) {
                    case "Transpose" -> MatrixMethods.transpose(MatrixMethods.parseMatrix(input));
                    case "Adjoint"   -> MatrixMethods.adjoint(MatrixMethods.parseMatrix(input));
                    case "Inverse"   -> MatrixMethods.inverse(MatrixMethods.parseMatrix(input));
                    default -> throw new IllegalArgumentException("Unknown: " + operation);
                }));
            }
        } catch (Exception ex) {
            matrixOutputArea.setText("Error: " + ex.getMessage());
        }
    }

    private void clearAll() {
        xlField.setText(""); xuField.setText(""); x0Field.setText(""); iterField.setText("");
        fxField.setText(""); dfxField.setText("");
        presetBox.setSelectedIndex(0);
        outputArea.setText(""); matrixInputArea.setText(""); matrixOutputArea.setText("");
    }

    // ============================================================
    // STYLE HELPERS
    // ============================================================
    private void styleTabPane(JTabbedPane tabs) {
        tabs.setBackground(BG_PANEL);
        tabs.setForeground(ACCENT);
        tabs.setFont(new Font("SansSerif", Font.BOLD, 12));
    }

    private JLabel makeLabel(String text, Color fg, int size) {
        JLabel l = new JLabel(text);
        l.setForeground(fg);
        l.setFont(new Font("SansSerif", Font.BOLD, size));
        l.setOpaque(false);
        return l;
    }

    private JTextField makeTextField(String value) {
        JTextField tf = new JTextField(value);
        tf.setBackground(BG_INPUT);
        tf.setForeground(TXT_PRIMARY);
        tf.setCaretColor(ACCENT2);
        tf.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tf.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(ACCENT.darker(), 1),
            new EmptyBorder(3, 6, 3, 6)));
        return tf;
    }

    private void styleCombo(JComboBox<?> box) {
        box.setBackground(BG_INPUT);
        box.setForeground(TXT_PRIMARY);
        box.setFont(new Font("SansSerif", Font.PLAIN, 11));
        box.setBorder(new LineBorder(ACCENT.darker(), 1));
    }

    private void styleTextArea(JTextArea area) {
        area.setBackground(BG_INPUT);
        area.setForeground(ACCENT2);
        area.setCaretColor(ACCENT2);
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        area.setBorder(new EmptyBorder(4, 6, 4, 6));
    }

    private JButton makeButton(String text, Color bg, Color fg, ActionListener al) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFont(new Font("SansSerif", Font.BOLD, 11));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(al);
        // Hover effect
        Color hoverBg = bg.brighter();
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(hoverBg); }
            public void mouseExited (MouseEvent e) { b.setBackground(bg);      }
        });
        return b;
    }

    private TitledBorder navyTitledBorder(String title) {
        TitledBorder tb = BorderFactory.createTitledBorder(
            new LineBorder(ACCENT.darker(), 1), title,
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 10), ACCENT);
        return tb;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RootSolverGUI().setVisible(true));
    }
}
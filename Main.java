package com.mycompany.findingtheroots;
import javax.swing.*;
import java.awt.*;


public class Main extends JFrame {

    // Root Finding Components
    private JTextField xlField, xuField, x0Field, iterField;
    private JTextArea outputArea;
    private JComboBox<String> methodBox;

    // Matrix Components
    private JComboBox<String> sizeBox;
    private JTextArea matrixInputArea, matrixOutputArea;

    public Main() {
        setTitle("Advanced Root Solver & Matrix Calculator");
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // ========== TAB PANEL ==========
        JTabbedPane tabs = new JTabbedPane();

        // --- TAB 1: Root Finding ---
        tabs.addTab("Root Finder", createRootPanel());

        // --- TAB 2: Matrix Operations ---
        tabs.addTab("Matrix Operations", createMatrixPanel());

        add(tabs, BorderLayout.CENTER);

        // ========== BOTTOM BUTTONS ==========
        JPanel bottomPanel = new JPanel();
        JButton clearBtn = new JButton("Clear");
        JButton exitBtn = new JButton("Exit");

        clearBtn.addActionListener(e -> clearAll());
        exitBtn.addActionListener(e -> System.exit(0));

        bottomPanel.add(clearBtn);
        bottomPanel.add(exitBtn);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    // ==================== ROOT FINDER PANEL ====================
    private JPanel createRootPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Root Finding Inputs"));

        inputPanel.add(new JLabel("Xl:"));
        xlField = new JTextField("1");
        inputPanel.add(xlField);

        inputPanel.add(new JLabel("Xu:"));
        xuField = new JTextField("2");
        inputPanel.add(xuField);

        inputPanel.add(new JLabel("Initial Guess (x0):"));
        x0Field = new JTextField("1.5");
        inputPanel.add(x0Field);

        inputPanel.add(new JLabel("Iterations:"));
        iterField = new JTextField("10");
        inputPanel.add(iterField);

        inputPanel.add(new JLabel("Method:"));
        methodBox = new JComboBox<>(new String[]{
                "Bisection", "False Position", "Newton-Raphson", "Secant", "Incremental"
        });
        inputPanel.add(methodBox);

        JButton solveBtn = new JButton("Solve");
        solveBtn.addActionListener(e -> solveRoot());

        JButton graphBtn = new JButton("Plot Graph");
        graphBtn.addActionListener(e -> GraphPlotter.plot());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(solveBtn);
        buttonPanel.add(graphBtn);

        outputArea = new JTextArea();
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JScrollPane scroll = new JScrollPane(outputArea);

        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.add(inputPanel, BorderLayout.NORTH);
        rootPanel.add(buttonPanel, BorderLayout.CENTER);
        rootPanel.add(scroll, BorderLayout.SOUTH);

        panel.add(rootPanel);
        return panel;
    }

    private void solveRoot() {
        // Same logic as before (kept for brevity)
        outputArea.setText("Root finding result will appear here...\n");
    }

    // ==================== MATRIX PANEL ====================
    private JPanel createMatrixPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Matrix Operations"));

        // Size Selector
        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Matrix Size:"));
        sizeBox = new JComboBox<>(new String[]{"2x2", "3x3", "4x4", "5x5"});
        sizeBox.addActionListener(e -> updateMatrixInputSize());
        topPanel.add(sizeBox);

        // Input Area
        matrixInputArea = new JTextArea(8, 30);
        matrixInputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane inputScroll = new JScrollPane(matrixInputArea);
        inputScroll.setBorder(BorderFactory.createTitledBorder("Enter Matrix (space separated)"));

        // Output Area
        matrixOutputArea = new JTextArea(10, 30);
        matrixOutputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane outputScroll = new JScrollPane(matrixOutputArea);
        outputScroll.setBorder(BorderFactory.createTitledBorder("Result"));

        // Operation Buttons
        JPanel opPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        opPanel.setBorder(BorderFactory.createTitledBorder("Operations"));

        JButton[] buttons = {
            new JButton("Add"), new JButton("Multiply"),
            new JButton("Transpose"), new JButton("Determinant"),
            new JButton("Adjoint"), new JButton("Inverse"),
            new JButton("Power"), new JButton("Solve Equations")
        };

        for (JButton btn : buttons) {
            btn.addActionListener(e -> performMatrixOperation(btn.getText()));
            opPanel.add(btn);
        }

        JPanel center = new JPanel(new GridLayout(1, 2, 10, 10));
        center.add(inputScroll);
        center.add(outputScroll);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);
        panel.add(opPanel, BorderLayout.EAST);

        return panel;
    }

    private void updateMatrixInputSize() {
        matrixInputArea.setText(""); // Clear for new size
    }

    private void performMatrixOperation(String operation) {
        matrixOutputArea.setText("Operation: " + operation + "\nResult will be shown here...\n");
  
    }

    private void clearAll() {
        if (xlField != null) xlField.setText("");
        if (xuField != null) xuField.setText("");
        if (x0Field != null) x0Field.setText("");
        if (iterField != null) iterField.setText("");
        if (outputArea != null) outputArea.setText("");
        if (matrixInputArea != null) matrixInputArea.setText("");
        if (matrixOutputArea != null) matrixOutputArea.setText("");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new RootSolverGUI().setVisible(true);
        });
    }
}

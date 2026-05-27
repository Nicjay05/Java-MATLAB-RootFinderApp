package com.mycompany.findingtheroots;

import org.jfree.chart.*;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;

public class GraphPlotter extends JPanel {

    // ── Navy Blue & Beige palette (matches RootSolverGUI) ───────
    private static final Color BG_DARK   = new Color(10,  20,  50);
    private static final Color BG_PLOT   = new Color(16,  30,  70);
    private static final Color BG_GRID   = new Color(30,  50, 100);
    private static final Color ACCENT    = new Color(196, 178, 140);
    private static final Color ACCENT2   = new Color(220, 205, 165);
    private static final Color ZERO_LINE = new Color(120, 140, 170);
    private static final Color TXT_LIGHT = new Color(220, 210, 185);
    private static final Color ROOT_DOT  = new Color(255, 220,  80);  // bright gold dot

    // ── Plot range ───────────────────────────────────────────────
    private static final double X_MIN    = -10;
    private static final double X_MAX    =  10;
    private static final double STEP     =  0.05;

    public GraphPlotter() {
        setLayout(new BorderLayout());
        setBackground(BG_DARK);
        buildAndShow();
    }

    private void buildAndShow() {
        // ── f(x) curve series ────────────────────────────────────
        XYSeries curve = new XYSeries("f(x)");
        for (double x = X_MIN; x <= X_MAX; x += STEP) {
            try {
                double y = Function.f(x);
                if (Double.isFinite(y) && Math.abs(y) < 1e6) curve.add(x, y);
            } catch (Exception ignored) {}
        }

        // ── y = 0 reference line ─────────────────────────────────
        XYSeries zero = new XYSeries("y = 0");
        zero.add(X_MIN, 0);
        zero.add(X_MAX, 0);

        // ── Root points: sign-change detection ───────────────────
        // Walk the curve; wherever f changes sign, interpolate the root x
        XYSeries roots = new XYSeries("Root(s)");
        double prevX = X_MIN;
        double prevY;
        try { prevY = Function.f(prevX); } catch (Exception e) { prevY = Double.NaN; }

        for (double x = X_MIN + STEP; x <= X_MAX; x += STEP) {
            double y;
            try { y = Function.f(x); } catch (Exception e) { prevX = x; prevY = Double.NaN; continue; }

            if (Double.isFinite(prevY) && Double.isFinite(y) && prevY * y < 0) {
                // Linear interpolation to find the crossing
                double rootX = prevX - prevY * (x - prevX) / (y - prevY);
                roots.add(rootX, 0.0);  // place dot ON the x-axis
            }
            prevX = x;
            prevY = y;
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(curve);   // series 0
        dataset.addSeries(zero);    // series 1
        dataset.addSeries(roots);   // series 2

        // ── Chart ─────────────────────────────────────────────────
        JFreeChart chart = ChartFactory.createXYLineChart(
            "f(x) = " + Function.getFxExpr(),
            "x", "f(x)", dataset
        );

        chart.setBackgroundPaint(BG_DARK);
        chart.getTitle().setPaint(TXT_LIGHT);
        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 13));

        chart.getLegend().setBackgroundPaint(BG_PLOT);
        chart.getLegend().setItemPaint(ACCENT2);
        chart.getLegend().setItemFont(new Font("SansSerif", Font.PLAIN, 11));

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(BG_PLOT);
        plot.setOutlinePaint(ACCENT.darker());
        plot.setDomainGridlinePaint(BG_GRID);
        plot.setRangeGridlinePaint(BG_GRID);
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinesVisible(true);

        plot.getDomainAxis().setLabelPaint(ACCENT);
        plot.getDomainAxis().setTickLabelPaint(ACCENT2);
        plot.getDomainAxis().setAxisLinePaint(ACCENT.darker());
        plot.getDomainAxis().setTickMarkPaint(ACCENT.darker());
        plot.getDomainAxis().setLabelFont(new Font("SansSerif", Font.BOLD, 11));

        plot.getRangeAxis().setLabelPaint(ACCENT);
        plot.getRangeAxis().setTickLabelPaint(ACCENT2);
        plot.getRangeAxis().setAxisLinePaint(ACCENT.darker());
        plot.getRangeAxis().setTickMarkPaint(ACCENT.darker());
        plot.getRangeAxis().setLabelFont(new Font("SansSerif", Font.BOLD, 11));

        // ── Renderer ──────────────────────────────────────────────
        XYLineAndShapeRenderer r = new XYLineAndShapeRenderer(true, false);

        // Series 0 — f(x) curve: beige line, no dots
        r.setSeriesPaint(0, ACCENT);
        r.setSeriesStroke(0, new BasicStroke(2.2f));
        r.setSeriesShapesVisible(0, false);

        // Series 1 — y=0: dashed muted blue, no dots
        r.setSeriesPaint(1, ZERO_LINE);
        r.setSeriesStroke(1, new BasicStroke(1.2f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER, 10f, new float[]{6f}, 0f));
        r.setSeriesShapesVisible(1, false);

        // Series 2 — root points: no line, large bright gold filled dot
        r.setSeriesLinesVisible(2, false);
        r.setSeriesShapesVisible(2, true);
        r.setSeriesPaint(2, ROOT_DOT);
        r.setSeriesShape(2, new Ellipse2D.Double(-7, -7, 14, 14));
        r.setSeriesFillPaint(2, ROOT_DOT);
        r.setUseFillPaint(true);
        r.setSeriesOutlinePaint(2, BG_DARK);
        r.setSeriesOutlineStroke(2, new BasicStroke(1.5f));
        r.setDrawOutlines(true);

        plot.setRenderer(r);

        // ── Chart panel ───────────────────────────────────────────
        ChartPanel cp = new ChartPanel(chart);
        cp.setMouseWheelEnabled(true);
        cp.setBackground(BG_DARK);

        removeAll();
        add(cp, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void refresh() {
        buildAndShow();
    }

    public static void plot() {
        JFrame frame = new JFrame("Graph — " + Function.getFxExpr());
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().setBackground(new Color(10, 20, 50));
        frame.setSize(720, 520);
        frame.add(new GraphPlotter());
        frame.setVisible(true);
    }
}
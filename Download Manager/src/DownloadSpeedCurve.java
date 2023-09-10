
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

public class DownloadSpeedCurve extends ApplicationFrame {

    public DownloadSpeedCurve(String title, String chartTitle, double[] xData, double[] yData) {
        super(title);

        DefaultXYDataset dataset = createDataset(xData, yData);
        JFreeChart chart = createChart(dataset, chartTitle);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(800, 600));
        setContentPane(chartPanel);
    }

    private DefaultXYDataset createDataset(double[] xData, double[] yData) {
        DefaultXYDataset dataset = new DefaultXYDataset();
        double[][] data = { xData, yData };
        dataset.addSeries("Download Speed", data);
        return dataset;
    }

    private JFreeChart createChart(DefaultXYDataset dataset, String title) {
        JFreeChart chart = ChartFactory.createXYLineChart(
                title,
                "Time (seconds)",
                "Download Speed (KB/s)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        XYPlot plot = chart.getXYPlot();
        XYSplineRenderer renderer = new XYSplineRenderer();
        plot.setRenderer(renderer);

        return chart;
    }

    public static void showDownloadSpeedCurve(String title, String chartTitle, double[] xData, double[] yData) {
        DownloadSpeedCurve chart = new DownloadSpeedCurve(title, chartTitle, xData, yData);
        chart.pack();
        RefineryUtilities.centerFrameOnScreen(chart);
        chart.setVisible(true);
    }
}

package output;

import error.OTMErrorLog;
import error.OTMException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeriesCollection;
import common.Scenario;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.io.*;
import java.util.stream.IntStream;

public abstract class AbstractOutput implements InterfaceOutput {

    public enum Type {
        link_flw,
        link_veh,
        vht,
        lanegroup_flw,
        lanegroup_veh,
        lanegroup_avgveh,
        cell_flw,
        cell_veh,
        path_travel_time,
        vehicle_events,
        vehicle_class,
        vehicle_travel_time,
        controller,
        actuator,
        sensor
    }

    public Scenario scenario;
    public Type type;

    // file output
    public final String output_folder;
    public Writer writer;
    public final String prefix;
    public final boolean write_to_file;

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public AbstractOutput(Scenario scenario,String prefix, String output_folder) {
        this.scenario = scenario;
        this.output_folder = output_folder;
        this.prefix = prefix;

        if(this.prefix!=null)
            this.prefix.replaceAll("\\\\", File.separator);

        this.write_to_file = output_folder!=null && prefix!=null;

    }

    //////////////////////////////////////////////////////
    // InterfaceOutput
    //////////////////////////////////////////////////////

    @Override
    public void open() throws OTMException {
        if(!write_to_file)
            return;
        try {
            String filename = get_output_file();
            if( filename!=null && !filename.isEmpty() ) {
                writer = new OutputStreamWriter(new FileOutputStream(filename));
            }
        } catch (FileNotFoundException exc) {
            throw new OTMException(exc);
        }
    }

    @Override
    public void close() throws OTMException {
        if(writer==null)
            return;
        try {
            writer.close();
        } catch (IOException e) {
            throw new OTMException(e);
        }
    }

    //////////////////////////////////////////////////////
    // incomplete implementation
    //////////////////////////////////////////////////////

    public void validate(OTMErrorLog errorLog){
        if(write_to_file){
            File path = new File(output_folder);
            if(!path.exists())
                errorLog.addError("Could not pth: " + path);
        }
    }

    public void initialize(Scenario scenario) throws OTMException {
        if(write_to_file) {
            this.close();
            this.open();
        }
    }

    public String get_output_file() {
        return write_to_file ? output_folder + File.separator + prefix : null;
    }

    //////////////////////////////////////////////////////
    // final
    //////////////////////////////////////////////////////

    public final Type getType() {
        return type;
    }

    //////////////////////////////////////////////////////
    // static
    //////////////////////////////////////////////////////

    public static void make_time_chart(XYSeriesCollection dataset,String yaxis_label,String filename) throws OTMException {

        JFreeChart chart = ChartFactory.createXYLineChart("",
                "time", yaxis_label, dataset,
                PlotOrientation.VERTICAL,
                true,false,false);

        //customization
        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        plot.setRenderer(renderer);

        // line
        for(int i=0;i<dataset.getSeriesCount();i++)
            renderer.setSeriesStroke(i, new BasicStroke(2f));

        // markers
        long series_size = IntStream.range(0, dataset.getSeriesCount()).mapToLong(i->dataset.getItemCount(i)).max().getAsLong();
        if(series_size>30)
            for(int i=0;i<dataset.getSeriesCount();i++)
                renderer.setSeriesShapesVisible(i,false);
        else
            for(int i=0;i<dataset.getSeriesCount();i++)
                renderer.setSeriesShape(i, new Ellipse2D.Double(-3, -3, 6, 6));

        // remove outline
        plot.setOutlineVisible(false);

        // white background
        plot.setBackgroundPaint(Color.WHITE);

        // grid
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.BLACK);
        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(Color.BLACK);

        // package
        ChartPanel chartPanel = new ChartPanel( chart );
        chartPanel.setSize(new java.awt.Dimension( 560 , 367 ) );

        // save to file
        try {
            OutputStream out = new FileOutputStream(filename);
            ChartUtilities.writeChartAsPNG(out,
                    chart,
                    chartPanel.getWidth(),
                    chartPanel.getHeight());
        } catch (IOException e) {
            throw new OTMException(e);
        }
    }

}

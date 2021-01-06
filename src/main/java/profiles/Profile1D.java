package profiles;

import error.OTMErrorLog;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import utils.OTMUtils;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Profile1D {

    public float start_time;
    public Float dt;
    public List<Double> values;

    ////////////////////////////////
    // construction
    ////////////////////////////////

    public Profile1D(Float start_time,Float dt,List<Double> values){
        this.start_time = start_time==null ? 0f : start_time;
        this.dt = dt;
        this.values = values;
    }

    public Profile1D(Float start_time, Float dt){
        this.start_time = start_time==null ? 0 : start_time;
        this.dt = dt;
        this.values = new ArrayList<>();
    }

    public void validate_pre_init(OTMErrorLog errorLog){

        // start_time >= 0
        if(start_time<0)
            errorLog.addError("start_time<0");

        // dt >= 0
        if(dt!=null && dt<0)
            errorLog.addError("dt<0");

        if((dt==null || dt==0) && values.size()>1)
            errorLog.addError("(dt==0 || dt==null) && values.size()>1");

        // values >= 0
        if(!values.isEmpty() && Collections.min(values)<0)
            errorLog.addError("Collections.min(values)<0");
    }

    public void add_entry(double x){
        values.add(x);
    }

    ////////////////////////////////
    // in place operations
    ////////////////////////////////

    public void sum(Profile1D x){
        if(start_time!=x.start_time || dt!=x.dt || values.size()!=x.values.size())
            return;
        for(int i=0;i<values.size();i++)
            this.values.set(i,values.get(i)+x.values.get(i));
    }

    public void multiply(double x){
        if(OTMUtils.approximately_equals(x,1.0))
            return;
        for(int i=0;i<values.size();i++)
            values.set(i,values.get(i)*x);
    }

    public void round(){
        for(int i=0;i<values.size();i++)
            values.set(i,(double) Math.round(values.get(i)));
    }

    public void pad_to_length(int newsize){
        if(newsize<=values.size())
            return;
        double lastvalue = values.get(values.size()-1);
        for(int i=values.size();i<newsize;i++)
            values.add(lastvalue);
    }

    ///////////////////////////////////////
    // not in place operations
    ///////////////////////////////////////

    public List<Double> diff(){
        if(values.size()<2)
            return null;
        List<Double> x = new ArrayList<>();
        for(int i=1;i<values.size();i++)
            x.add(values.get(i)-values.get(i-1));
        return x;
    }

    /** used to convert cumulative counts to flows **/
    public List<Double> difftimes(double alpha){
        if(values.size()<2)
            return null;
        List<Double> x = new ArrayList<>();
        for(int i=1;i<values.size();i++)
            x.add(alpha*(values.get(i)-values.get(i-1)));
        return x;
    }

    ////////////////////////////////
    // get
    ////////////////////////////////

    public Float get_dt(){
        return dt;
    }

    public List<Float> get_times(){
        List<Float> times = new ArrayList<>();
        float currtime = start_time;
        times.add(currtime);
        for(int i=0;i<values.size()-1;i++){
            currtime += dt;
            times.add(currtime);
        }
        return times;
    }

    public List<Double> get_values(){
        return values;
    }

    public int get_length(){
        return values.size();
    }

    public double get_value_for_time(float time){
        if(time<start_time)
            return 0d;
        if(dt==null || dt==0)
            return get_ith_value(0);
        return get_ith_value((int)((time-start_time)/dt));
    }

    // returns values corresponding to the next change after "now"
    // If there are no more changes, return null.
    public TimeValue get_change_following(float now){
        if(now<start_time)
            return new TimeValue(start_time,get_ith_value(0));
        if(dt==null || dt==0)
            return null;

        int index = (int)((now+dt-start_time)/dt);
        if(index>get_length()-1)
            return null;
        return new TimeValue(start_time + index*dt,get_ith_value(index));
    }

    public double get_ith_value(int i){
        int step = Math.max(0,Math.min(i,values.size()-1));
        return values.get(step);
    }

    public float get_next_update_time(float now){
        return  start_time + dt + dt*((float)Math.floor((now-start_time)/dt));
    }

    ///////////////////////////////////////
    // plot
    ///////////////////////////////////////

    public void plot_and_save(String filename){

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(get_series(""));

        JFreeChart chart = ChartFactory.createXYLineChart("BLA",
                "time", "vehicles", dataset,
                PlotOrientation.VERTICAL,
                true,false,false);

        //customization
        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        plot.setRenderer(renderer);

        // line
        renderer.setSeriesPaint(0, Color.RED);
        renderer.setSeriesStroke(0, new BasicStroke(1.5f));

        // remove outline
        plot.setOutlineVisible(false);

        // white background
        plot.setBackgroundPaint(Color.WHITE);

        // grid
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.BLACK);
        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(Color.BLACK);

        // markers
        if(values.size()>30)
            renderer.setSeriesShapesVisible(0,false);
        else
            renderer.setSeriesShape(0, new Ellipse2D.Double(-3, -3, 6, 6));

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
            e.printStackTrace();
        }

    }

    public XYSeries get_series(String key) {
        XYSeries series = new XYSeries(key);
        float time = start_time;
        for(double value : values){
            series.add(time,value);
            time += dt;
        }
        return series;
    }

    ///////////////////////////////////////
    // other
    ///////////////////////////////////////

    public Profile1D clone() {
        Profile1D x = new Profile1D(this.start_time,this.dt);
        this.values.forEach(v->x.values.add(v));
        return x;
    }

    @Override
    public String toString() {
        return "Profile1D{" +
                "start_time=" + start_time +
                ", dt=" + dt +
                ", values=" + values +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Profile1D profile1D = (Profile1D) o;
        return Float.compare(profile1D.start_time, start_time) == 0 &&
                Objects.equals(dt, profile1D.dt) &&
                Objects.equals(values, profile1D.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start_time, dt, values);
    }

}

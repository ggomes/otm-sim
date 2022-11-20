package output;

import control.AbstractController;
import control.sigint.ControllerSignalPretimed;
import dispatch.Dispatcher;
import error.OTMException;
import org.jfree.data.xy.XYSeriesCollection;
import cmd.RunParameters;
import core.Scenario;

public class OutputController extends AbstractOutputEvent {

    public long controller_id;

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public OutputController(Scenario scenario, String prefix, String output_folder, Long controller_id) throws OTMException {
        super(scenario, prefix, output_folder);
        this.type = Type.controller;
        this.controller_id = controller_id;
    }

    //////////////////////////////////////////////////////
    // InterfaceOutput
    //////////////////////////////////////////////////////

    @Override
    public void register(RunParameters props, Dispatcher dispatcher) throws OTMException {
        AbstractController controller = scenario.controllers.get(controller_id);
        if (controller != null)
            controller.set_event_output(this);
        else   // register with all controllers
            for (AbstractController c : scenario.controllers.values())
                c.set_event_output(this);
    }

    @Override
    public String get_output_file() {
        return write_to_file ? super.get_output_file() + "_controller_" + controller_id + "_.txt" : null;
    }

    //////////////////////////////////////////////////////
    // InterfacePlottable
    //////////////////////////////////////////////////////

    @Override
    public void plot(String filename) throws OTMException {

        AbstractController controller = scenario.controllers.get(controller_id);

        XYSeriesCollection dataset = null;

//        if(controller instanceof ControllerSignalPretimed)
//            dataset = plot_pretimed_controller();

        if(dataset==null)
            throw new OTMException("Plotting not implemented for this type of controller.");

        make_time_chart(dataset,String.format("Controller %d",controller_id),"",filename);

    }

    @Override
    public String get_yaxis_label() {
        return "command";
    }

    //////////////////////////////////////////////////////
    // private
    //////////////////////////////////////////////////////

//    private XYSeriesCollection plot_pretimed_controller(){
//
//        // get the actuator
//        ControllerSignalPretimed controller = (ControllerSignalPretimed)  scenario.controllers.get(controller_id);
//
//        // initialize the dataset. add series per phase
//        XYSeriesCollection dataset = new XYSeriesCollection();
//        XYSeries series = new XYSeries(controller_id);
//        dataset.addSeries(series);
//
//        // go through events
//        for(AbstractEventWrapper absevent : this.events){
//            EventControllerScheduleTransitionInfo e = (EventControllerScheduleTransitionInfo) absevent;
//            series.add(absevent.timestamp,e.current_item);
//        }
//
//        return dataset;
//    }

}
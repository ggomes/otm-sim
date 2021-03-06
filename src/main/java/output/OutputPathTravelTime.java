package output;

import commodity.Path;
import error.OTMException;
import profiles.Profile1D;
import core.Scenario;

import java.io.IOException;
import java.util.List;

public class OutputPathTravelTime extends AbstractOutputTimedPath {

    public boolean instantaneous = true;

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public OutputPathTravelTime(Scenario scenario, String prefix, String output_folder, Long subnetwork_id, Float outDt) throws OTMException {
        super(scenario, prefix, output_folder, null, subnetwork_id, outDt);
        this.type = Type.path_travel_time;
    }

    //////////////////////////////////////////////////////
    // InterfaceOutput
    //////////////////////////////////////////////////////

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);
        if(!write_to_file)
            profile = new Profile1D(0f,outDt);
    }

    @Override
    public String get_output_file() {
        return write_to_file ? super.get_output_file() + "_path_tt.txt" : null;
    }

    //////////////////////////////////////////////////////
    // AbstractOutputTimed
    //////////////////////////////////////////////////////

    @Override
    public void write(float timestamp) throws OTMException {
        super.write(timestamp);

        double travel_time = instantaneous ?
                compute_instantaneous_travel_time() :
                compute_predictive_travel_time(0f);

        if(write_to_file){
            try {
                writer.write(String.format("%f\n",travel_time));
            } catch (IOException e) {
                throw new OTMException(e);
            }
        } else {
            profile.add_entry(travel_time);
        }
    }

    //////////////////////////////////////////////////////
    // InterfacePlottable
    //////////////////////////////////////////////////////

    @Override
    public String get_yaxis_label() {
        return "travel time";
    }

    //////////////////////////////////////////////////////
    // final
    //////////////////////////////////////////////////////

    public final double compute_predictive_travel_time(float start_time){
//        float curr_time = start_time;
//        for(Link link:path.ordered_links)
//            curr_time += link.link_tt.get_value_for_time(curr_time);
//        return (double) (curr_time-start_time);
        return 0d;
    }

    public final List<Double> get_travel_times_sec(){
        return profile.get_values();
    }

//    private double get_value_for_link(Link link){
//
//
//        // TODO FIX THIS
//        return Double.NaN;
//
//        switch(info.model_type){
//
//            case pq:
//            case micro:
//                return info.get_mean_and_clear();
////                return info.has_samples() ? info.get_mean_and_clear() : link.model.get_ff_travel_time();
//
//            case ctm:
//            case mn:
//                return link.get_current_average_travel_time();
//
//            case none:
//                return Double.NaN;
//
//            default:
//                System.err.println("NOT IMPLEMENTED.");
//                return Double.NaN;
//
//        }
//
//    }

//    public double compute_instantaneous_travel_time(float start_time){
//        return Double.NaN;
//
////        return path.ordered_links.stream()
////                .mapToDouble(link->link_tt.get(link.getId()).travel_times.get_value_for_time(start_time))
////                .sum();
//    }

    //////////////////////////////////////////////////////
    // private
    //////////////////////////////////////////////////////

    private double compute_instantaneous_travel_time(){
        return path.get_ordered_links().stream().
                mapToDouble(link->link.link_tt.instantaneous_travel_time)
                .sum();
    }

}

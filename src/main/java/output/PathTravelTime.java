package output;

import commodity.Path;
import common.AbstractVehicle;
import common.Link;
import error.OTMException;
import profiles.Profile1D;
import runner.Scenario;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PathTravelTime extends AbstractOutputTimedSubnetwork {

    public Path path;

    // vehicle id to entry_time
    private Map<Long,Float> entry_time = new HashMap<>();

    private Map<Long,LinkTravelTime> link_tt;

    public PathTravelTime(Scenario scenario, String prefix, String output_folder, Long subnetwork_id, Float outDt) throws OTMException {
        super(scenario, prefix, output_folder, null, subnetwork_id, outDt);

        if(subnetwork==null)
            return;

        this.path = (Path) this.subnetwork;

        // register with links
        link_tt = new HashMap<>();
        for(Link link : path.ordered_links) {
            link.add_travel_timer(this);
            link_tt.put(link.getId(),new LinkTravelTime(link));
        }

    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        if(!write_to_file)
            for(LinkTravelTime x : link_tt.values())
                 x.initialize(outDt);
    }

    @Override
    public String get_output_file() {
        return super.get_output_file() + "_path_tt.txt";
    }

    //////////////////////////////////////////////////////
    // write
    //////////////////////////////////////////////////////

    public void vehicle_enter(float timestamp, AbstractVehicle vehicle){
        entry_time.put(vehicle.getId(),timestamp);
    }

    // Used by pq links
    public void vehicle_exit(float timestamp, AbstractVehicle vehicle,Long link_id,Link next_link){

        // I dont know about this vehicle
        if(!entry_time.containsKey(vehicle.getId()))
            return;

        // consider only vehicles that continue on the path
        if( path.has(next_link) )
            link_tt.get(link_id).add_sample(timestamp-entry_time.get(vehicle.getId()));

        entry_time.remove(vehicle);
    }

    @Override
    public void write(float timestamp,Object obj) throws OTMException {
        super.write(timestamp,null);
        if(write_to_file){
            try {
                boolean isfirst=true;
                for(Link link : path.ordered_links){
                    if(!isfirst)
                        writer.write(AbstractOutputTimed.delim);
                    isfirst = false;
                    writer.write(String.format("%f",get_value_for_link(link)));
                }
                writer.write("\n");
            } catch (IOException e) {
                throw new OTMException(e);
            }
        } else {

            for(Link link : path.ordered_links) {
                LinkTravelTime info = link_tt.get(link.getId());
                info.travel_time.add(get_value_for_link(link));
            }

        }
    }

    private double get_value_for_link(Link link){

        LinkTravelTime info = link_tt.get(link.getId());

        switch(info.model_type){

            case pq:
            case micro:
                return info.has_samples() ? info.get_mean_and_clear() : link.model.get_ff_travel_time();

            case ctm:
            case mn:
                return link.get_current_average_travel_time();

            case none:
                return Double.NaN;

            default:
                System.err.println("NOT IMPLEMENTED.");
                return Double.NaN;

        }

    }

    //////////////////////////////////////////////////////
    // read
    //////////////////////////////////////////////////////

    public Long get_path_id(){
        return this.path.getId();
    }

    public List<Long> get_link_ids(){
        return this.path.links.stream().map(link-> link.getId()).collect(Collectors.toList());
    }

    public double compute_instantaneous_travel_time(float start_time){
        return path.ordered_links.stream()
                .mapToDouble(link->link_tt.get(link.getId()).travel_time.get_value_for_time(start_time))
                .sum();
    }

    public List<Double> compute_instantaneous_travel_times(float start_time, float dt, int n){
        List<Double> tt = new ArrayList<>();
        float time=start_time;
        for(int i=0;i<n;i++){
            tt.add(compute_instantaneous_travel_time(time));
            time+=dt;
        }
        return tt;
    }

    public double compute_predictive_travel_time(float start_time){
        float curr_time = start_time;
        for(Link link:path.ordered_links)
            curr_time += link_tt.get(link.getId()).travel_time.get_value_for_time(curr_time);
        return (double) (curr_time-start_time);
    }

    public List<Double> compute_predictive_travel_times(float start_time, float dt, int n){
        List<Double> tt = new ArrayList<>();
        float time=start_time;
        for(int i=0;i<n;i++){
            tt.add(compute_predictive_travel_time(time));
            time+=dt;
        }
        return tt;
    }

    public Profile1D get_travel_times_for_link(Long link_id){
        return link_tt.get(link_id).travel_time;
    }

}

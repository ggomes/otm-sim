package api;

import error.OTMException;
import output.*;
import runner.Scenario;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class Output {

    private Scenario scenario;
    protected Output(Scenario scenario){
        this.scenario = scenario;
    }

    // ----------------------------------------------
    // get
    // ----------------------------------------------

    /**
     * Undocumented
     * @return Undocumented
     */
    public Set<AbstractOutput> get_data(){
        Set<AbstractOutput> x = new HashSet<>();
        for(AbstractOutput output : scenario.outputs)
            if(!output.write_to_file)
                x.add(output);
        return x;
    }

    /**
     * Undocumented
     * @return Undocumented
     */
    public Set<String> get_file_names(){
        return scenario.outputs.stream().map(x->x.get_output_file()).collect(toSet());
    }

    /**
     * Undocumented
     */
    public void clear(){
        scenario.outputs.clear();
    }

    // ----------------------------------------------
    // Newtwork
    // ----------------------------------------------

    /**
     * Undocumented
     * @param prefix Undocumented
     * @param output_folder Undocumented
     */
    public void request_lanegroups(String prefix,String output_folder){
        try {
            this.scenario.outputs.add(new LaneGroups(scenario,prefix,output_folder));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    // ----------------------------------------------
    // Links
    // ----------------------------------------------

    /**
     * Undocumented
     * @param prefix Undocumented
     * @param output_folder Undocumented
     * @param commodity_id Undocumented
     * @param link_ids Undocumented
     * @param outDt Undocumented
     */
    public void request_links_flow(String prefix, String output_folder, Long commodity_id, List<Long> link_ids, Float outDt){
        try {
            this.scenario.outputs.add(new LinkFlow(scenario,prefix,output_folder,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Undocumented
     * @param commodity_id Undocumented
     * @param link_ids Undocumented
     * @param outDt Undocumented
     */
    public void request_links_flow(Long commodity_id,List<Long> link_ids,Float outDt){
        try {
            this.scenario.outputs.add(new LinkFlow(scenario,null,null,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Undocumented
     * @param link_ids Undocumented
     * @param outDt Undocumented
     */
    public void request_links_flow(List<Long> link_ids,Float outDt){
        try {
            this.scenario.outputs.add(new LinkFlow(scenario,null,null,null,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Undocumented
     * @param prefix Undocumented
     * @param output_folder Undocumented
     * @param commodity_id Undocumented
     * @param link_ids Undocumented
     * @param outDt Undocumented
     */
    public void request_links_veh(String prefix,String output_folder,Long commodity_id,List<Long> link_ids,Float outDt){
        try {
            this.scenario.outputs.add(new LinkVehicles(scenario,prefix,output_folder,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Undocumented
     * @param commodity_id Undocumented
     * @param link_ids Undocumented
     * @param outDt Undocumented
     */
    public void request_links_veh(Long commodity_id,List<Long> link_ids,Float outDt){
        try {
            this.scenario.outputs.add(new LinkVehicles(scenario,null,null,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Undocumented
     * @param link_ids Undocumented
     * @param outDt Undocumented
     */
    public void request_links_veh(List<Long> link_ids,Float outDt){
        try {
            this.scenario.outputs.add(new LinkVehicles(scenario,null,null,null,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    // lanegroups ==============================================

    /**
     * Undocumented
     * @param prefix Undocumented
     * @param output_folder Undocumented
     * @param commodity_id Undocumented
     * @param link_ids Undocumented
     * @param outDt Undocumented
     */
    public void request_lanegroup_flw(String prefix,String output_folder,Long commodity_id,List<Long> link_ids,Float outDt){
        try {
            this.scenario.outputs.add(new LaneGroupFlow(scenario,prefix,output_folder,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Undocumented
     * @param commodity_id Undocumented
     * @param link_ids Undocumented
     * @param outDt Undocumented
     */
    public void request_lanegroup_flw(Long commodity_id,List<Long> link_ids,Float outDt){
        try {
            this.scenario.outputs.add(new LaneGroupFlow(scenario,null,null,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Undocumented
     * @param prefix Undocumented
     * @param output_folder Undocumented
     * @param commodity_id Undocumented
     * @param link_ids Undocumented
     * @param outDt Undocumented
     */
    public void request_lanegroup_veh(String prefix,String output_folder,Long commodity_id,List<Long> link_ids,Float outDt){
        try {
            this.scenario.outputs.add(new LaneGroupVehicles(scenario,prefix,output_folder,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Undocumented
     * @param commodity_id Undocumented
     * @param link_ids Undocumented
     * @param outDt Undocumented
     */
    public void request_lanegroup_veh(Long commodity_id,List<Long> link_ids,Float outDt){
        try {
            this.scenario.outputs.add(new LaneGroupVehicles(scenario,null,null,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    // ----------------------------------------------
    // Subnetworks
    // ----------------------------------------------

    /**
     * Request the travel times on a given path be recorded
     * @param prefix Undocumented
     * @param output_folder Undocumented
     * @param subnetwork_id Undocumented
     * @param outDt Undocumented
     */
    public void request_path_travel_time(String prefix,String output_folder,Long subnetwork_id,Float outDt){
        try {
            PathTravelTimeWriter path_tt = new PathTravelTimeWriter(scenario,prefix,output_folder,subnetwork_id,outDt);
            this.scenario.outputs.add(path_tt);
            this.scenario.add_path_travel_time(path_tt);
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Request the travel times on a given path be recorded. Results are held in memory, not written to a file.
     * @param subnetwork_id Undocumented
     * @param outDt Undocumented
     */
    public void request_path_travel_time(Long subnetwork_id,Float outDt){
        request_path_travel_time(null,null,subnetwork_id,outDt);
    }

    /**
     * Undocumented
     * @param prefix Undocumented
     * @param output_folder Undocumented
     * @param commodity_id Undocumented
     * @param subnetwork_id Undocumented
     * @param outDt Undocumented
     */
    public void request_subnetwork_vht(String prefix,String output_folder,Long commodity_id,Long subnetwork_id,Float outDt){
        try {
            this.scenario.outputs.add(new LinkVHT(scenario,prefix,output_folder,commodity_id,subnetwork_id,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    // ----------------------------------------------
    // Vehicles
    // ----------------------------------------------

    /**
     * Undocumented
     * @param commodity_id Undocumented
     */
    public void request_vehicle_events(float commodity_id){
        try {
            this.scenario.outputs.add(new EventsVehicle(scenario,null,null,(long) commodity_id));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Undocumented
     * @param prefix Undocumented
     * @param output_folder Undocumented
     * @param commodity_id Undocumented
     */
    public void request_vehicle_events(String prefix,String output_folder,Long commodity_id){
        try {
            this.scenario.outputs.add(new EventsVehicle(scenario,prefix,output_folder,commodity_id));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Undocumented
     * @param prefix Undocumented
     * @param output_folder Undocumented
     */
    public void request_vehicle_class(String prefix,String output_folder){
        this.scenario.outputs.add(new VehicleClass(scenario,prefix,output_folder));
    }

    /**
     * Undocumented
     * @param prefix Undocumented
     * @param output_folder Undocumented
     */
    public void request_vehicle_travel_time(String prefix,String output_folder){
        this.scenario.outputs.add(new VehicleTravelTime(scenario,prefix,output_folder));
    }

    // ----------------------------------------------
    // Sensors and actuators
    // ----------------------------------------------

    /**
     * Undocumented
     * @param prefix Undocumented
     * @param output_folder Undocumented
     * @param actuator_id Undocumented
     */
    public void request_actuator(String prefix,String output_folder,Long actuator_id){
        try {
            this.scenario.outputs.add(new EventsActuator(scenario,prefix,output_folder,actuator_id));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Undocumented
     * @param actuator_id Undocumented
     */
    public void request_actuator(Long actuator_id){
        request_actuator(null,null,actuator_id);
    }

    // ----------------------------------------------
    // Controllers
    // ----------------------------------------------

    /**
     * Undocumented
     * @param prefix Undocumented
     * @param output_folder Undocumented
     * @param controller_id Undocumented
     */
    public void request_controller(String prefix,String output_folder,Long controller_id){
        try {
            this.scenario.outputs.add(new EventsController(scenario,prefix,output_folder,controller_id));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Undocumented
     * @param controller_id Undocumented
     */
    public void request_controller(Long controller_id){
        request_controller(null,null, controller_id);
    }


}

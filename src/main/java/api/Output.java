package api;

import error.OTMException;
import output.*;
import runner.Scenario;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * Methods for requesting outputs and calculating different metrics.
 */
public class Output {

    private Scenario scenario;
    protected Output(Scenario scenario){
        this.scenario = scenario;
    }

    // ----------------------------------------------
    // get
    // ----------------------------------------------

    /**
     * Get the set of all output objects.
     * @return Set of output objects
     */
    public Set<AbstractOutput> get_data(){
        Set<AbstractOutput> x = new HashSet<>();
        for(AbstractOutput output : scenario.outputs)
            if(!output.write_to_file)
                x.add(output);
        return x;
    }

    /**
     * Get the set of all output file names.
     * @return Set of all output file names
     */
    public Set<String> get_file_names(){
        return scenario.outputs.stream()
                .map(x->x.get_output_file())
                .filter(x->x!=null)
                .collect(toSet());
    }

    /**
     * Clear the outputs.
     */
    public void clear(){
        scenario.outputs.clear();
    }

    // ----------------------------------------------
    // Network
    // ----------------------------------------------

    /**
     * Request lane group outputs
     * @param prefix Prefix for the output files.
     * @param output_folder Output folder.
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
     * Request link flows.
     * @param prefix Prefix for the output files.
     * @param output_folder Output folder.
     * @param commodity_id Id for the requested vehicle type.
     * @param link_ids Collection of requested link ids
     * @param outDt Output sampling time in seconds.
     */
    public void request_links_flow(String prefix, String output_folder, Long commodity_id, Collection<Long> link_ids, Float outDt){
        try {
            this.scenario.outputs.add(new LinkFlow(scenario,prefix,output_folder,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Request link flows.
     * @param commodity_id Id for the requested vehicle type.
     * @param link_ids Collection of requested link ids
     * @param outDt Output sampling time in seconds.
     */
    public void request_links_flow(Long commodity_id,Collection<Long> link_ids,Float outDt){
        try {
            this.scenario.outputs.add(new LinkFlow(scenario,null,null,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Request link flows.
     * @param link_ids Collection of requested link ids
     * @param outDt Output sampling time in seconds.
     */
    public void request_links_flow(Collection<Long> link_ids,Float outDt){
        try {
            this.scenario.outputs.add(new LinkFlow(scenario,null,null,null,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Request link vehicles.
     * @param prefix Prefix for the output files.
     * @param output_folder Output folder.
     * @param commodity_id Id for the requested vehicle type.
     * @param link_ids Collection of requested link ids
     * @param outDt Output sampling time in seconds.
     */
    public void request_links_veh(String prefix,String output_folder,Long commodity_id,Collection<Long> link_ids,Float outDt){
        try {
            this.scenario.outputs.add(new LinkVehicles(scenario,prefix,output_folder,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Request link vehicles.
     * @param commodity_id Id for the requested vehicle type.
     * @param link_ids Collection of requested link ids
     * @param outDt Output sampling time in seconds.
     */
    public void request_links_veh(Long commodity_id,Collection<Long> link_ids,Float outDt){
        try {
            this.scenario.outputs.add(new LinkVehicles(scenario,null,null,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Request link vehicles.
     * @param link_ids Collection of requested link ids
     * @param outDt Output sampling time in seconds.
     */
    public void request_links_veh(Collection<Long> link_ids,Float outDt){
        try {
            this.scenario.outputs.add(new LinkVehicles(scenario,null,null,null,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    // lanegroups ==============================================

    /**
     * Request lane group flows.
     * @param prefix Prefix for the output files.
     * @param output_folder Output folder.
     * @param commodity_id Id for the requested vehicle type.
     * @param link_ids Collection of requested link ids
     * @param outDt Output sampling time in seconds.
     */
    public void request_lanegroup_flw(String prefix,String output_folder,Long commodity_id,Collection<Long> link_ids,Float outDt){
        try {
            this.scenario.outputs.add(new LaneGroupFlow(scenario,prefix,output_folder,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Request lane group flows.
     * @param commodity_id Id for the requested vehicle type.
     * @param link_ids Collection of requested link ids
     * @param outDt Output sampling time in seconds.
     */
    public void request_lanegroup_flw(Long commodity_id,Collection<Long> link_ids,Float outDt){
        try {
            this.scenario.outputs.add(new LaneGroupFlow(scenario,null,null,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Request lane group vehicles.
     * @param prefix Prefix for the output files.
     * @param output_folder Output folder.
     * @param commodity_id Id for the requested vehicle type.
     * @param link_ids Collection of requested link ids
     * @param outDt Output sampling time in seconds.
     */
    public void request_lanegroup_veh(String prefix,String output_folder,Long commodity_id,Collection<Long> link_ids,Float outDt){
        try {
            this.scenario.outputs.add(new LaneGroupVehicles(scenario,prefix,output_folder,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Request lane group vehicles.
     * @param commodity_id Id for the requested vehicle type.
     * @param link_ids Collection of requested link ids
     * @param outDt Output sampling time in seconds.
     */
    public void request_lanegroup_veh(Long commodity_id,Collection<Long> link_ids,Float outDt){
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
     * Request the travel times on a given path.
     * @param prefix Prefix for the output files.
     * @param output_folder Output folder.
     * @param subnetwork_id Id of the requested subnetwork.
     * @param outDt Output sampling time in seconds.
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
     * Request the travel times on a given path.
     * @param subnetwork_id Id of the requested subnetwork.
     * @param outDt Output sampling time.
     */
    public void request_path_travel_time(Long subnetwork_id,Float outDt){
        request_path_travel_time(null,null,subnetwork_id,outDt);
    }

    /**
     * Request VHT for a subnetwork
     * @param prefix Prefix for the output files.
     * @param output_folder Output folder.
     * @param commodity_id Id for the requested vehicle type.
     * @param subnetwork_id Id of the requested subnetwork.
     * @param outDt Output sampling time.
     */
    public void request_subnetwork_vht(String prefix,String output_folder,Long commodity_id,Long subnetwork_id,Float outDt){
        try {
            this.scenario.outputs.add(new LinkVHT(scenario,prefix,output_folder,commodity_id,subnetwork_id,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Request VHT for a subnetwork
     * @param commodity_id Id for the requested vehicle type.
     * @param subnetwork_id Id of the requested subnetwork.
     * @param outDt Output sampling time.
     */
    public void request_subnetwork_vht(Long commodity_id,Long subnetwork_id,Float outDt){
        request_subnetwork_vht(null,null,commodity_id,subnetwork_id,outDt);
    }

    // ----------------------------------------------
    // Vehicles
    // ----------------------------------------------

    /**
     * Request vehicle events.
     * @param commodity_id Id for the requested vehicle type.
     */
    public void request_vehicle_events(Long commodity_id){
        request_vehicle_events(null,null,commodity_id);
    }

    /**
     * Request vehicle events.
     * @param prefix Prefix for the output files.
     * @param output_folder Output folder.
     * @param commodity_id Id for the requested vehicle type.
     */
    public void request_vehicle_events(String prefix,String output_folder,Long commodity_id){
        try {
            this.scenario.outputs.add(new EventsVehicle(scenario,prefix,output_folder,commodity_id));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Request vehicle class.
     * @param prefix Prefix for the output files.
     * @param output_folder Output folder.
     */
    public void request_vehicle_class(String prefix,String output_folder){
        this.scenario.outputs.add(new VehicleClass(scenario,prefix,output_folder));
    }

    /**
     * Request vehicle class.
     */
    public void request_vehicle_class(){
        this.scenario.outputs.add(new VehicleClass(scenario,null,null));
    }

    /**
     * Request vehicle travel times.
     * @param prefix Prefix for the output files.
     * @param output_folder Output folder.
     */
    public void request_vehicle_travel_time(String prefix,String output_folder){
        this.scenario.outputs.add(new VehicleTravelTime(scenario,prefix,output_folder));
    }

    /**
     * Request vehicle travel times.
     */
    public void request_vehicle_travel_time(){
        this.scenario.outputs.add(new VehicleTravelTime(scenario,null,null));
    }

    // ----------------------------------------------
    // Sensors and actuators
    // ----------------------------------------------

    /**
     * Request actuator events.
     * @param prefix Prefix for the output files.
     * @param output_folder Output folder.
     * @param actuator_id Actuator id.
     */
    public void request_actuator(String prefix,String output_folder,Long actuator_id){
        try {
            this.scenario.outputs.add(new EventsActuator(scenario,prefix,output_folder,actuator_id));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Request actuator events.
     * @param actuator_id Actuator id.
     */
    public void request_actuator(Long actuator_id){
        request_actuator(null,null,actuator_id);
    }

    // ----------------------------------------------
    // Controllers
    // ----------------------------------------------

    /**
     * Request controller events
     * @param prefix Prefix for the output files.
     * @param output_folder Output folder.
     * @param controller_id Controller id
     */
    public void request_controller(String prefix,String output_folder,Long controller_id){
        try {
            this.scenario.outputs.add(new EventsController(scenario,prefix,output_folder,controller_id));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Request controller events
     * @param controller_id Controller id
     */
    public void request_controller(Long controller_id){
        request_controller(null,null, controller_id);
    }


}

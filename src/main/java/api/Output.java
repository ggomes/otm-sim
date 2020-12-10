package api;

import error.OTMException;
import models.vehicle.spatialq.OutputQueues;
import output.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * Methods for requesting outputs and calculating different metrics.
 */
public class Output {

    private api.OTM myapi;
    protected Output(api.OTM myapi){
        this.myapi = myapi;
    }

    ////////////////////////////////////////////////////////
    // outputs
    ////////////////////////////////////////////////////////

    /**
     * Get the set of all output objects.
     * @return Set of output objects
     */
    public Set<AbstractOutput> get_data(){
        Set<AbstractOutput> x = new HashSet<>();
        for(AbstractOutput output : myapi.scenario.outputs)
            if(!output.write_to_file)
                x.add(output);
        return x;
    }

    /**
     * Get the set of all output file names.
     * @return Set of all output file names
     */
    public Set<String> get_file_names(){
        return myapi.scenario.outputs.stream()
                .map(x->x.get_output_file())
                .filter(x->x!=null)
                .collect(toSet());
    }

    /**
     * Clear the outputs.
     */
    public void clear(){
        myapi.scenario.outputs.clear();
    }

    ////////////////////////////////////////////////////////
    // output requests
    ////////////////////////////////////////////////////////

    // ----------------------------------------------
    // lane groups
    // ----------------------------------------------

    /**
     * Request lane group outputs
     * @param prefix Prefix for the output files.
     * @param output_folder Output folder.
     */
    public void request_lanegroups(String prefix,String output_folder){
        try {
            this.myapi.scenario.outputs.add(new OutputLaneGroups(myapi.scenario,prefix,output_folder));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    // ----------------------------------------------
    // Links state
    // ----------------------------------------------

    /**
     * Request link flows
     * @param prefix Prefix for the output files. null means do not write to file.
     * @param output_folder Output folder. null means do not write to file.
     * @param commodity_id Id for the requested vehicle type. null means aggregate over commodities.
     * @param link_ids Collection of requested link ids
     * @param outDt Output sampling time in seconds.
     */
    public void request_links_flow(String prefix, String output_folder, Long commodity_id, Collection<Long> link_ids, Float outDt){
        try {
            this.myapi.scenario.outputs.add(new OutputLinkFlow(myapi.scenario,prefix,output_folder,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Request link vehicles.
     * @param prefix Prefix for the output files. null means do not write to file.
     * @param output_folder Output folder. null means do not write to file.
     * @param commodity_id Id for the requested vehicle type. null means aggregate over commodities.
     * @param link_ids Collection of requested link ids
     * @param outDt Output sampling time in seconds.
     */
    public void request_links_veh(String prefix,String output_folder,Long commodity_id,Collection<Long> link_ids,Float outDt){
        try {
            this.myapi.scenario.outputs.add(new OutputLinkVehicles(myapi.scenario,prefix,output_folder,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Request link wsum of vehicles.
     * @param prefix Prefix for the output files. null means do not write to file.
     * @param output_folder Output folder. null means do not write to file.
     * @param commodity_id Id for the requested vehicle type. null means aggregate over commodities.
     * @param link_ids Collection of requested link ids
     * @param outDt Output sampling time in seconds.
     */
    public void request_links_sum_veh(String prefix,String output_folder,Long commodity_id,Collection<Long> link_ids,Float outDt){
        try {
            this.myapi.scenario.outputs.add(new OutputLinkSumVehicles(myapi.scenario,prefix,output_folder,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Request vehicles in a mesoscopic queue.
     * @param prefix Prefix for the output files. null means do not write to file.
     * @param output_folder Output folder. null means do not write to file.
     * @param commodity_id Id for the requested vehicle type. null means aggregate over commodities.
     * @param link_ids Collection of requested link ids
     * @param outDt Output sampling time in seconds.
     */
    public void request_link_queues(String prefix,String output_folder,Long commodity_id, Collection<Long> link_ids, Float outDt){
        try {
            this.myapi.scenario.outputs.add(new OutputQueues(myapi.scenario,prefix,output_folder,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    // ----------------------------------------------
    // Lane group state
    // ----------------------------------------------

    /**
     * Request lane group flows.
     * @param prefix Prefix for the output files. null means do not write to file.
     * @param output_folder Output folder. null means do not write to file.
     * @param commodity_id Id for the requested vehicle type. null means aggregate over commodities.
     * @param link_ids Collection of requested link ids
     * @param outDt Output sampling time in seconds.
     */
    public void request_lanegroup_flw(String prefix,String output_folder,Long commodity_id,Collection<Long> link_ids,Float outDt){
        try {
            this.myapi.scenario.outputs.add(new OutputLaneGroupFlow(myapi.scenario,prefix,output_folder,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Request lane group vehicles.
     * @param prefix Prefix for the output files. null means do not write to file.
     * @param output_folder Output folder. null means do not write to file.
     * @param commodity_id Id for the requested vehicle type. null means aggregate over commodities.
     * @param link_ids Collection of requested link ids
     * @param outDt Output sampling time in seconds.
     */
    public void request_lanegroup_veh(String prefix,String output_folder,Long commodity_id,Collection<Long> link_ids,Float outDt){
        try {
            this.myapi.scenario.outputs.add(new OutputLaneGroupVehicles(myapi.scenario,prefix,output_folder,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Request lane group sum vehicles over simulation time steps. Works only for timestep-based models.
     * @param prefix Prefix for the output files. null means do not write to file.
     * @param output_folder Output folder. null means do not write to file.
     * @param commodity_id Id for the requested vehicle type. null means aggregate over commodities.
     * @param link_ids Collection of requested link ids
     * @param outDt Output sampling time in seconds.
     */
    public void request_lanegroup_sum_veh(String prefix, String output_folder, Long commodity_id, Collection<Long> link_ids, Float outDt){
        try {
            this.myapi.scenario.outputs.add(new OutputLaneGroupSumVehicles(myapi.scenario,prefix,output_folder,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    // ----------------------------------------------
    // Cell state (fluid only)
    // ----------------------------------------------

    /**
     * Request cell flows.
     * @param prefix Prefix for the output files. null means do not write to file.
     * @param output_folder Output folder. null means do not write to file.
     * @param commodity_id Id for the requested vehicle type. null means aggregate over commodities.
     * @param link_ids Collection of requested link ids
     * @param outDt Output sampling time in seconds.
     */
    public void request_cell_flw(String prefix,String output_folder,Long commodity_id,Collection<Long> link_ids,Float outDt){
        try {
            this.myapi.scenario.outputs.add(new OutputCellFlow(myapi.scenario,prefix,output_folder,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Request cell vehicles.
     * @param prefix Prefix for the output files. null means do not write to file.
     * @param output_folder Output folder. null means do not write to file.
     * @param commodity_id Id for the requested vehicle type. null means aggregate over commodities.
     * @param link_ids Collection of requested link ids
     * @param outDt Output sampling time in seconds.
     */
    public void request_cell_veh(String prefix,String output_folder,Long commodity_id,Collection<Long> link_ids,Float outDt){
        try {
            this.myapi.scenario.outputs.add(new OutputCellVehicles(myapi.scenario,prefix,output_folder,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Request cell sum of vehicles. Works only for timestep-based models.
     * @param prefix Prefix for the output files. null means do not write to file.
     * @param output_folder Output folder. null means do not write to file.
     * @param commodity_id Id for the requested vehicle type. null means aggregate over commodities.
     * @param link_ids Collection of requested link ids
     * @param outDt Output sampling time in seconds.
     */
    public void request_cell_sum_veh(String prefix, String output_folder, Long commodity_id, Collection<Long> link_ids, Float outDt){
        try {
            this.myapi.scenario.outputs.add(new OutputCellSumVehicles(myapi.scenario,prefix,output_folder,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Request cell vehicles moving downstream. Works only for timestep-based models.
     * @param prefix Prefix for the output files. null means do not write to file.
     * @param output_folder Output folder. null means do not write to file.
     * @param commodity_id Id for the requested vehicle type. null means aggregate over commodities.
     * @param link_ids Collection of requested link ids
     * @param outDt Output sampling time in seconds.
     */
    public void request_cell_sum_veh_dwn(String prefix, String output_folder, Long commodity_id, Collection<Long> link_ids, Float outDt){
        try {
            this.myapi.scenario.outputs.add(new OutputCellSumVehiclesDwn(myapi.scenario,prefix,output_folder,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Request cell vehicles moving to outside lane group. Works only for timestep-based models.
     * @param prefix Prefix for the output files. null means do not write to file.
     * @param output_folder Output folder. null means do not write to file.
     * @param commodity_id Id for the requested vehicle type. null means aggregate over commodities.
     * @param link_ids Collection of requested link ids
     * @param outDt Output sampling time in seconds.
     */
    public void request_cell_lanechange_out(String prefix, String output_folder, Long commodity_id, Collection<Long> link_ids, Float outDt){
        try {
            this.myapi.scenario.outputs.add(new OutputCellLanechangeOut(myapi.scenario,prefix,output_folder,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Request cell vehicles moving to incside lane group. Works only for timestep-based models.
     * @param prefix Prefix for the output files. null means do not write to file.
     * @param output_folder Output folder. null means do not write to file.
     * @param commodity_id Id for the requested vehicle type. null means aggregate over commodities.
     * @param link_ids Collection of requested link ids
     * @param outDt Output sampling time in seconds.
     */
    public void request_cell_lanechange_in(String prefix, String output_folder, Long commodity_id, Collection<Long> link_ids, Float outDt){
        try {
            this.myapi.scenario.outputs.add(new OutputCellLanechangeIn(myapi.scenario,prefix,output_folder,commodity_id,link_ids,outDt));
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    // ----------------------------------------------
    // Subnetwork state
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
            OutputPathTravelTime path_tt = new OutputPathTravelTime(myapi.scenario,prefix,output_folder,subnetwork_id,outDt);
            this.myapi.scenario.outputs.add(path_tt);
            this.myapi.scenario.add_path_travel_time(path_tt);
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
            this.myapi.scenario.outputs.add(new OutputLinkVHT(myapi.scenario,prefix,output_folder,commodity_id,subnetwork_id,outDt));
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
    // Vehicle events
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
            this.myapi.scenario.outputs.add(new OutputVehicle(myapi.scenario,prefix,output_folder,commodity_id));
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
        this.myapi.scenario.outputs.add(new OutputVehicleClass(myapi.scenario,prefix,output_folder));
    }

    /**
     * Request vehicle class.
     */
    public void request_vehicle_class(){
        this.myapi.scenario.outputs.add(new OutputVehicleClass(myapi.scenario,null,null));
    }

    /**
     * Request vehicle travel times.
     * @param prefix Prefix for the output files.
     * @param output_folder Output folder.
     */
    public void request_vehicle_travel_time(String prefix,String output_folder){
        this.myapi.scenario.outputs.add(new OutputTravelTime(myapi.scenario,prefix,output_folder));
    }

    /**
     * Request vehicle travel times.
     */
    public void request_vehicle_travel_time(){
        this.myapi.scenario.outputs.add(new OutputTravelTime(myapi.scenario,null,null));
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
            this.myapi.scenario.outputs.add(new OutputController(myapi.scenario,prefix,output_folder,controller_id));
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

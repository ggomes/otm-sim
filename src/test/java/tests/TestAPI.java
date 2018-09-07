package tests;

import api.API;
import api.APIopen;
import common.Link;
import error.OTMException;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import output.animation.AnimationInfo;
import runner.OTM;
import runner.Scenario;

import java.util.Map;

public class TestAPI extends AbstractTest {

    static API api;
    static APIopen apiopen;
    static float sim_dt = 2f;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        api = OTM.load_test("line",sim_dt,true,"ctm");
        apiopen = new APIopen(api);
    }

    //////////////////////////////////////////////////////////////////

    @Test
    @Ignore
    public void test_get_version(){
    }

    @Test
    @Ignore
    public void test_set_random_seed(){
    }

    @Test
    @Ignore
    public void test_get_scenario_info(){
    }

    @Test
    @Ignore
    public void test_set_stochastic_process(){
    }

    ////////////////////////////////////////////////////////
    // commodities
    ////////////////////////////////////////////////////////

    @Test
    @Ignore
    public void test_get_num_commodities(){
    }

    @Test
    @Ignore
    public void test_get_commodities(){
    }

    @Test
    @Ignore
    public void test_get_commodity_with_id(){
    }

    @Test
    @Ignore
    public void test_get_commodity_ids(){
    }

    ////////////////////////////////////////////////////////
    // subnetworks and paths
    ////////////////////////////////////////////////////////

    @Test
    @Ignore
    public void test_get_num_subnetworks(){
    }

    @Test
    @Ignore
    public void test_get_subnetwork_ids(){
    }

    @Test
    @Ignore
    public void test_get_path_ids(){
    }

    @Test
    @Ignore
    public void test_get_subnetworks(){
    }

    @Test
    @Ignore
    public void test_get_subnetwork_with_id(){
    }

    ////////////////////////////////////////////////////////
    // network
    ////////////////////////////////////////////////////////

    @Test
    @Ignore
    public void test_get_num_links(){
    }

    @Test
    @Ignore
    public void test_get_num_nodes(){
    }

    @Test
    @Ignore
    public void test_get_links(){
    }

    @Test
    @Ignore
    public void test_get_link_with_id(){
    }

    @Test
    @Ignore
    public void test_get_link_ids(){
    }

    @Test
    public void test_get_node_ids(){
    }

    @Test
    @Ignore
    public void test_get_source_link_ids(){
    }

    @Test
    @Ignore
    public void test_get_in_lanegroups_for_road_connection(){
    }

    @Test
    @Ignore
    public void test_get_out_lanegroups_for_road_connection(){

    }

    ////////////////////////////////////////////////////////
    // demands / splits
    ////////////////////////////////////////////////////////

    @Test
    @Ignore
    public void test_get_demands(){

    }

    @Test
    @Ignore
    public void test_get_demand_with_ids(){

    }

    @Test
    @Ignore
    public void test_clear_all_demands(){

    }

    @Test
    @Ignore
    public void test_set_demand_on_path_in_vph() {
//        set_demand_on_path_in_vph(long path_id,long commodity_id,float start_time,float dt,List<Double> values)
    }

    @Test
    @Ignore
    public void test_get_total_trips() {

    }

    ////////////////////////////////////////////////////////
    // sensors
    ////////////////////////////////////////////////////////

    @Test
    @Ignore
    public void test_get_num_sensors(){

    }

    @Test
    @Ignore
    public void test_get_sensors(){

    }

    @Test
    @Ignore
    public void test_get_sensor_with_id(){
//        get_sensor_with_id(long id)

    }

    ////////////////////////////////////////////////////////
    // controllers
    ////////////////////////////////////////////////////////

    @Test
    @Ignore
    public void test_get_num_controllers(){

    }

    @Test
    @Ignore
    public void test_get_controllers(){

    }

    @Test
    @Ignore
    public void test_get_controller_with_id(){
//        get_controller_with_id(long id)
    }

    ////////////////////////////////////////////////////////
    // actuators
    ////////////////////////////////////////////////////////

    @Test
    @Ignore
    public void test_get_num_actuators(){

    }

    @Test
    @Ignore
    public void test_get_actuators(){

    }

    @Test
    @Ignore
    public void test_get_actuator_with_id(){
//        get_actuator_with_id(long id)
    }

    ////////////////////////////////////////////////////////
    // performance
    ////////////////////////////////////////////////////////

    @Test
    @Ignore
    public void test_get_performance(){

    }

    @Test
    @Ignore
    public void test_get_performance_for_commodity() {
//        get_performance_for_commodity(Long commodity_id)

    }

    ////////////////////////////////////////////////////////
    // outputs
    ////////////////////////////////////////////////////////

    @Test
    @Ignore
    public void test_get_output_data(){

    }

    @Test
    @Ignore
    public void test_clear_output_requests(){
    }

    @Test
    @Ignore
    public void test_get_outputs(){
    }

    // network ................

    @Test
    @Ignore
    public void test_request_lanegroups(){
//        request_lanegroups(String prefix,String output_folder)
    }

    // links ....................

    @Test
    @Ignore
    public void test_request_links_veh(){
//        request_links_veh(String prefix,String output_folder,Long commodity_id,List<Long> link_ids,Float outDt
//        request_links_veh(Long commodity_id,List<Long> link_ids,Float outDt)
    }

    @Test
    @Ignore
    public void test_request_links_flow(){
//        request_links_flow(String prefix,String output_folder,Long commodity_id,List<Long> link_ids,Float outDt
//        request_links_flow(Long commodity_id,List<Long> link_ids,Float outDt)
    }

    // lanegroups ...............

    @Test
    @Ignore
    public void test_request_lanegroup_flw(){
//        request_lanegroup_flw(String prefix,String output_folder,Long commodity_id,List<Long> link_ids,Float outDt)
//        request_lanegroup_flw(Long commodity_id,List<Long> link_ids,Float outDt)
    }

    @Test
    @Ignore
    public void test_request_lanegroup_veh(){
//        request_lanegroup_veh(String prefix,String output_folder,Long commodity_id,List<Long> link_ids,Float outDt)
//        request_lanegroup_veh(Long commodity_id,List<Long> link_ids,Float outDt)
    }

    // subnetwroks ..............

    @Test
    @Ignore
    public void test_request_path_travel_time(){
//        request_path_travel_time(String prefix,String output_folder,Long subnetwork_id,Float outDt)
//        request_path_travel_time(Long subnetwork_id,Float outDt)
    }


    @Test
    @Ignore
    public void test_request_subnetwork_vht(){
//        request_subnetwork_vht(String prefix,String output_folder,Long commodity_id,Long subnetwork_id,Float outDt
    }

    // vehicles .................

    @Test
    @Ignore
    public void test_request_vehicle_events(){
//        request_vehicle_events(float commodity_id)
//        request_vehicle_events(String prefix,String output_folder,Long commodity_id)

    }

    @Test
    @Ignore
    public void test_request_vehicle_class(){
//        request_vehicle_class(String prefix,String output_folder)
    }

    @Test
    @Ignore
    public void test_request_vehicle_travel_time(){
//        request_vehicle_travel_time(String prefix,String output_folder)
    }

    // sensors ..................

    // actuators ...............

    @Test
    @Ignore
    public void test_request_actuator(){
//        request_actuator(String prefix,String output_folder,Long actuator_id)
//        request_actuator(Long actuator_id)
    }

    // controllers ..............

    @Test
    @Ignore
    public void test_request_controller(){
//        request_controller(Long controller_id)
    }

    ////////////////////////////////////////////////////////
    // animation
    ////////////////////////////////////////////////////////

    @Test
    @Ignore
    public void test_initialize() {
//        initialize(float start_time)
    }

    @Test
    @Ignore
    public void test_advance() {
//        advance(float duration)
    }

    @Test
    @Ignore
    public void test_get_current_time(){
    }

    @Test
    @Ignore
    public void test_get_animation_info() {

        try {

            float start_time = 0f;
            float duration = 300f;

            api.initialize(start_time);

            Scenario scenario = apiopen.scenario();
            Link link = scenario.network.links.get(0L);

            final int steps = (int) (duration / sim_dt);
            for (int i=1; i<=steps; i++) {

                api.advance(sim_dt);
                AnimationInfo info = api.get_animation_info();
                Map<Long,Double> x = info.get_total_vehicles_per_link();
//                System.out.println(((output.animation.macro.LinkInfo)info.link_info.get(0L)).lanegroup_info.get(0L).cell_info.get(0).comm_vehicles.get(1L));
            }
        } catch (OTMException e) {
            System.err.print(e);
        }

    }

}

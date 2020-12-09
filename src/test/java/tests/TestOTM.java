package tests;

import api.OTM;
import error.OTMException;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestOTM extends AbstractTest {

    public static api.OTM otm;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        otm = new OTM();
        otm.load_test("line_ctm");
    }

    //////////////////////////////////////////////////////////////////

    @Test
    public void test_get_version(){
        assertTrue(!api.OTM.get_version().isEmpty());
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
    public void test_get_num_commodities(){
        assertEquals(otm.scenario.get_num_commodities(),1);
    }

    @Test
    public void test_get_commodity_ids(){
        assertEquals((long) otm.scenario.get_commodity_ids().iterator().next(),1l);
    }

    ////////////////////////////////////////////////////////
    // subnetworks and paths
    ////////////////////////////////////////////////////////

    @Test
    public void test_get_num_subnetworks(){
        assertEquals(otm.scenario.get_num_subnetworks(),1);
    }

    @Test
    public void test_get_subnetwork_ids(){
        assertEquals((long)otm.scenario.get_subnetwork_ids().iterator().next(),0l);
    }

    @Test
    public void test_get_path_ids(){
        assertEquals((long)otm.scenario.get_path_ids().iterator().next(),0l);
    }

    ////////////////////////////////////////////////////////
    // network
    ////////////////////////////////////////////////////////

    @Test
    public void test_get_num_links(){
        assertEquals(otm.scenario.get_num_links(),6);
    }

    @Test
    public void test_get_num_nodes(){
        assertEquals(otm.scenario.get_num_nodes(),7);
    }

    @Test
    public void test_get_link_ids(){
        Set<Long> link_ids = otm.scenario.get_link_ids();
        for(long id=0;id<6;id++)
            assertTrue(link_ids.contains(id));
        assertEquals(link_ids.size(),6);
    }

    @Test
    public void test_get_node_ids(){
        Set<Long> node_ids = otm.scenario.get_node_ids();
        for(long id=0;id<7;id++)
            assertTrue(node_ids.contains(id));
        assertEquals(node_ids.size(),7);
    }

    @Test
    public void test_get_source_link_ids(){
        Set<Long> source_ids = otm.scenario.get_source_link_ids();
        assertEquals((long)source_ids.iterator().next(),0l);
    }

    ////////////////////////////////////////////////////////
    // demands / splits
    ////////////////////////////////////////////////////////

//    @Test
//    public void test_get_demands(){
//        Set<DemandInfo> demands = otm.scenario.get_demands().get(0l);
//        DemandInfo demand = demands.iterator().next();
//        assertEquals((long)demand.getCommodity_id(),1l);
//        assertEquals(demand.getLink_id(),0l);
//    }

    @Ignore
    @Test
    public void test_get_total_trips() {
        assertEquals(otm.scenario.get_total_trips(),583.3333333333334,0.0001);
    }

    ////////////////////////////////////////////////////////
    // sensors
    ////////////////////////////////////////////////////////

    @Test
    public void test_get_num_sensors(){
        assertEquals(otm.scenario.get_num_sensors(),0);
    }

    ////////////////////////////////////////////////////////
    // controllers
    ////////////////////////////////////////////////////////

    @Test
    public void test_get_num_controllers(){
        assertEquals(otm.scenario.get_num_controllers(),0);
    }

    ////////////////////////////////////////////////////////
    // actuators
    ////////////////////////////////////////////////////////

    @Test
    public void test_get_num_actuators(){
        assertEquals(otm.scenario.get_num_actuators(),0);
    }

    ////////////////////////////////////////////////////////
    // outputs
    ////////////////////////////////////////////////////////

    @Test
    @Ignore
    public void test_get_output_data(){
        System.out.println(otm.output.get_data());
    }

    @Test
    @Ignore
    public void test_clear_output_requests(){
        otm.output.clear();
    }

    @Test
    @Ignore
    public void test_get_outputs(){
        System.out.println(otm.output.get_data());
    }

    // network ................

    @Test
    @Ignore
    public void test_request_lanegroups(){
        otm.output.request_lanegroups("lgs",output_folder);
    }

    // links ....................

    @Test
    @Ignore
    public void test_request_links_veh(){
        otm.output.request_links_veh("lv",output_folder,null,null,10f);
    }

    @Test
    @Ignore
    public void test_request_links_flow(){
        otm.output.request_links_flow("lf",output_folder,null,null,10f);
    }

    // lanegroups ...............

    @Test
    @Ignore
    public void test_request_lanegroup_flw(){
        otm.output.request_lanegroup_flw("lgf",output_folder,null,null,10f);
    }

    @Test
    @Ignore
    public void test_request_lanegroup_veh(){
        otm.output.request_lanegroup_veh("lgv",output_folder,null,null,10f);
    }

    // subnetwroks ..............

    @Test
    @Ignore
    public void test_request_path_travel_time(){
        otm.output.request_path_travel_time("ptt",output_folder,0l,10f);
    }


    @Test
    @Ignore
    public void test_request_subnetwork_vht(){
        otm.output.request_subnetwork_vht("vht",output_folder,null,0l,10f);
    }

    // vehicles .................

    @Test
    @Ignore
    public void test_request_vehicle_events(){
        otm.output.request_vehicle_events("vehev",output_folder,null);
    }

    @Test
    @Ignore
    public void test_request_vehicle_class(){
        otm.output.request_vehicle_class("vc",output_folder);
    }

    @Test
    @Ignore
    public void test_request_vehicle_travel_time(){
        otm.output.request_vehicle_travel_time("vtt",output_folder);
    }

    // controllers ..............

    @Test
    @Ignore
    public void test_request_controller(){
        otm.output.request_controller(0l);
    }

    ////////////////////////////////////////////////////////
    // animation
    ////////////////////////////////////////////////////////

    @Test
    @Ignore
    public void test_advance() {
        try {
            otm.advance(100f);
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    @Test
    @Ignore
    public void test_get_current_time(){
        System.out.println(otm.get_current_time());
    }

    @Test
    public void test_get_animation_info() {

//        try {
//
//            float start_time = 0f;
//            float duration = 300f;
//
//            otm.initialize(start_time);
//
//            Scenario scenario = apidev.scenario();
//            Link link = scenario.network.links.get(0L);
//
//            final int steps = (int) (duration / sim_dt);
//            for (int i=1; i<=steps; i++) {
//
//                otm.advance(sim_dt);
//                AnimationInfo info = otm.get_animation_info();
//                Map<Long,Double> x = info.get_total_vehicles_per_link();
////                System.out.println(((output.animation.macro.LinkInfo)info.link_info.get(0L)).lanegroup_info.get(0L).cell_info.get(0).comm_vehicles.get(1L));
//            }
//        } catch (OTMException e) {
//            System.err.print(e);
//        }

    }

}

package tests;

import api.OTM;
import api.OTMdev;
import api.info.CommodityInfo;
import api.info.DemandInfo;
import api.info.LinkInfo;
import api.info.SubnetworkInfo;
import error.OTMException;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import xml.JaxbLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestOTM extends AbstractTest {

    public static api.OTM otm;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        otm = new OTM();
        otm.load_test("line");
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
    public void test_get_commodities(){
        Set<CommodityInfo> commodities = otm.scenario.get_commodities();
        CommodityInfo comm = commodities.iterator().next();
        assertEquals(comm.getId(),1);
    }

    @Test
    public void test_get_commodity_with_id(){
        CommodityInfo comm = otm.scenario.get_commodity_with_id(1l);
        assertEquals(comm.getId(),1);
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

    @Test
    public void test_get_subnetworks(){
        Set<SubnetworkInfo> subnet = otm.scenario.get_subnetworks();
        assertEquals(subnet.iterator().next().getId(),0l);
    }

    @Test
    public void test_get_subnetwork_with_id(){
        SubnetworkInfo subnet = otm.scenario.get_subnetwork_with_id(0l);
        assertEquals(subnet.getId(),0l);
    }

    ////////////////////////////////////////////////////////
    // network
    ////////////////////////////////////////////////////////

    @Test
    public void test_get_num_links(){
        assertEquals(otm.scenario.get_num_links(),3);
    }

    @Test
    public void test_get_num_nodes(){
        assertEquals(otm.scenario.get_num_nodes(),4);
    }

    @Test
    public void test_get_links(){
        Map<Long,LinkInfo> links = otm.scenario.get_links();
        assertEquals(links.get(1l).getId(),1l);
    }

    @Test
    public void test_get_link_with_id(){
        LinkInfo info = otm.scenario.get_link_with_id(2l);
        assertEquals(info.getId(),2l);
    }

    @Test
    public void test_get_link_ids(){
        Set<Long> link_ids = otm.scenario.get_link_ids();
        assertTrue(link_ids.contains(1l));
        assertTrue(link_ids.contains(2l));
        assertTrue(link_ids.contains(3l));
        assertEquals(link_ids.size(),3);
    }

    @Test
    public void test_get_node_ids(){
        Set<Long> node_ids = otm.scenario.get_node_ids();
        assertTrue(node_ids.contains(1l));
        assertTrue(node_ids.contains(2l));
        assertTrue(node_ids.contains(3l));
        assertTrue(node_ids.contains(4l));
        assertEquals(node_ids.size(),4);
    }

    @Test
    public void test_get_source_link_ids(){
        Set<Long> source_ids = otm.scenario.get_source_link_ids();
        assertEquals((long)source_ids.iterator().next(),1l);
    }

    ////////////////////////////////////////////////////////
    // demands / splits
    ////////////////////////////////////////////////////////

//    @Test
//    public void test_get_demands(){
//        DemandInfo demands = otm.scenario.get_demands().iterator().next();
//        assertEquals((long)demands.getCommodity_id(),1l);
//        assertEquals(demands.getLink_id(),1l);
//    }

    @Test
    @Ignore
    public void test_set_demand_on_path_in_vph() {
        try {
            long path_id = 0l;
            long commodity_id = 1l;
            float start_time = 0;
            float dt = 10f;
            List<Double> values = new ArrayList<>();
            otm.scenario.set_demand_on_path_in_vph(path_id,commodity_id,start_time,dt,values);
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_get_total_trips() {
        assertEquals(otm.scenario.get_total_trips(),416.666666666666,0.0001);
    }

    ////////////////////////////////////////////////////////
    // sensors
    ////////////////////////////////////////////////////////

    @Test
    public void test_get_num_sensors(){
        assertEquals(otm.scenario.get_num_sensors(),0);
    }

    @Test
    @Ignore
    public void test_get_sensor_with_id(){
        System.out.println(otm.scenario.get_sensor_with_id(1l));
    }

    ////////////////////////////////////////////////////////
    // controllers
    ////////////////////////////////////////////////////////

    @Test
    public void test_get_num_controllers(){
        assertEquals(otm.scenario.get_num_controllers(),0);
    }

    @Test
    @Ignore
    public void test_get_controllers(){
        System.out.println(otm.scenario.get_controllers());
    }

    @Test
    @Ignore
    public void test_get_controller_with_id(){
        System.out.println(otm.scenario.get_controller_with_id(1l));
    }

    ////////////////////////////////////////////////////////
    // actuators
    ////////////////////////////////////////////////////////

    @Test
    public void test_get_num_actuators(){
        assertEquals(otm.scenario.get_num_actuators(),0);
    }

    @Test
    @Ignore
    public void test_get_actuators(){
        System.out.println(otm.scenario.get_actuators());
    }

    @Test
    @Ignore
    public void test_get_actuator_with_id(){
        System.out.println(otm.scenario.get_actuator_with_id(1l));
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

    // sensors ..................

    // actuators ...............

    @Test
    @Ignore
    public void test_request_actuator(){
        otm.output.request_actuator("act", output_folder,1l);
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

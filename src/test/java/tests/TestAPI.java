/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package tests;

import api.API;
import api.APIopen;
import api.info.CommodityInfo;
import api.info.DemandInfo;
import api.info.LinkInfo;
import api.info.SubnetworkInfo;
import commodity.Commodity;
import commodity.Subnetwork;
import common.Link;
import error.OTMException;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import output.animation.AnimationInfo;
import runner.OTM;
import runner.Scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
    public void test_get_version(){
        assertTrue(!OTM.getGitHash().isEmpty());
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
        assertEquals(api.get_num_commodities(),1);
    }

    @Test
    public void test_get_commodities(){
        Set<CommodityInfo> commodities = api.get_commodities();
        CommodityInfo comm = commodities.iterator().next();
        assertEquals(comm.getId(),1);
    }

    @Test
    public void test_get_commodity_with_id(){
        CommodityInfo comm = api.get_commodity_with_id(1l);
        assertEquals(comm.getId(),1);
    }

    @Test
    public void test_get_commodity_ids(){
        assertEquals((long) api.get_commodity_ids().iterator().next(),1l);
    }

    ////////////////////////////////////////////////////////
    // subnetworks and paths
    ////////////////////////////////////////////////////////

    @Test
    public void test_get_num_subnetworks(){
        assertEquals(api.get_num_subnetworks(),1);
    }

    @Test
    public void test_get_subnetwork_ids(){
        assertEquals((long)api.get_subnetwork_ids().iterator().next(),0l);
    }

    @Test
    public void test_get_path_ids(){
        assertEquals((long)api.get_path_ids().iterator().next(),0l);
    }

    @Test
    public void test_get_subnetworks(){
        Set<SubnetworkInfo> subnet = api.get_subnetworks();
        assertEquals(subnet.iterator().next().getId(),0l);
    }

    @Test
    public void test_get_subnetwork_with_id(){
        SubnetworkInfo subnet = api.get_subnetwork_with_id(0l);
        assertEquals(subnet.getId(),0l);
    }

    ////////////////////////////////////////////////////////
    // network
    ////////////////////////////////////////////////////////

    @Test
    public void test_get_num_links(){
        assertEquals(api.get_num_links(),3);
    }

    @Test
    public void test_get_num_nodes(){
        assertEquals(api.get_num_nodes(),4);
    }

    @Test
    public void test_get_links(){
        Map<Long,LinkInfo> links = api.get_links();
        assertEquals(links.get(1l).getId(),1l);
    }

    @Test
    public void test_get_link_with_id(){
        LinkInfo info = api.get_link_with_id(2l);
        assertEquals(info.getId(),2l);
    }

    @Test
    public void test_get_link_ids(){
        List<Long> link_ids = api.get_link_ids();
        assertTrue(link_ids.contains(1l));
        assertTrue(link_ids.contains(2l));
        assertTrue(link_ids.contains(3l));
        assertEquals(link_ids.size(),3);
    }

    @Test
    public void test_get_node_ids(){
        List<Long> node_ids = api.get_node_ids();
        assertTrue(node_ids.contains(1l));
        assertTrue(node_ids.contains(2l));
        assertTrue(node_ids.contains(3l));
        assertTrue(node_ids.contains(4l));
        assertEquals(node_ids.size(),4);
    }

    @Test
    public void test_get_source_link_ids(){
        List<Long> source_ids = api.get_source_link_ids();
        assertEquals((long)source_ids.iterator().next(),1l);
    }

    ////////////////////////////////////////////////////////
    // demands / splits
    ////////////////////////////////////////////////////////

    @Test
    public void test_get_demands(){
        DemandInfo demands = api.get_demands().iterator().next();
        assertEquals((long)demands.getCommodity_id(),1l);
        assertEquals(demands.getLink_id(),1l);
    }

    @Test
    @Ignore
    public void test_set_demand_on_path_in_vph() {
        try {
            long path_id = 0l;
            long commodity_id = 1l;
            float start_time = 0;
            float dt = 10f;
            List<Double> values = new ArrayList<>();
            api.set_demand_on_path_in_vph(path_id,commodity_id,start_time,dt,values);
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_get_total_trips() {
        assertEquals(api.get_total_trips(),416.666666666666,0.0001);
    }

    ////////////////////////////////////////////////////////
    // sensors
    ////////////////////////////////////////////////////////

    @Test
    public void test_get_num_sensors(){
        assertEquals(api.get_num_sensors(),0);
    }

    @Test
    @Ignore
    public void test_get_sensors(){
        System.out.println(api.get_sensors());

    }

    @Test
    @Ignore
    public void test_get_sensor_with_id(){
        System.out.println(api.get_sensor_with_id(1l));
    }

    ////////////////////////////////////////////////////////
    // controllers
    ////////////////////////////////////////////////////////

    @Test
    public void test_get_num_controllers(){
        assertEquals(api.get_num_controllers(),0);
    }

    @Test
    @Ignore
    public void test_get_controllers(){
        System.out.println(api.get_controllers());
    }

    @Test
    @Ignore
    public void test_get_controller_with_id(){
        System.out.println(api.get_controller_with_id(1l));
    }

    ////////////////////////////////////////////////////////
    // actuators
    ////////////////////////////////////////////////////////

    @Test
    public void test_get_num_actuators(){
        assertEquals(api.get_num_actuators(),0);
    }

    @Test
    @Ignore
    public void test_get_actuators(){
        System.out.println(api.get_actuators());
    }

    @Test
    @Ignore
    public void test_get_actuator_with_id(){
        System.out.println(api.get_actuator_with_id(1l));
    }

    ////////////////////////////////////////////////////////
    // outputs
    ////////////////////////////////////////////////////////

    @Test
    @Ignore
    public void test_get_output_data(){
        System.out.println(api.get_output_data());
    }

    @Test
    @Ignore
    public void test_clear_output_requests(){
        api.clear_output_requests();
    }

    @Test
    @Ignore
    public void test_get_outputs(){
        System.out.println(api.get_outputs());
    }

    // network ................

    @Test
    @Ignore
    public void test_request_lanegroups(){
        api.request_lanegroups("lgs",output_folder);
    }

    // links ....................

    @Test
    @Ignore
    public void test_request_links_veh(){
        api.request_links_veh("lv",output_folder,null,null,10f);
    }

    @Test
    @Ignore
    public void test_request_links_flow(){
        api.request_links_flow("lf",output_folder,null,null,10f);
    }

    // lanegroups ...............

    @Test
    @Ignore
    public void test_request_lanegroup_flw(){
        api.request_lanegroup_flw("lgf",output_folder,null,null,10f);
    }

    @Test
    @Ignore
    public void test_request_lanegroup_veh(){
        api.request_lanegroup_veh("lgv",output_folder,null,null,10f);
    }

    // subnetwroks ..............

    @Test
    @Ignore
    public void test_request_path_travel_time(){
        api.request_path_travel_time("ptt",output_folder,0l,10f);
    }


    @Test
    @Ignore
    public void test_request_subnetwork_vht(){
        api.request_subnetwork_vht("vht",output_folder,null,0l,10f);
    }

    // vehicles .................

    @Test
    @Ignore
    public void test_request_vehicle_events(){
        api.request_vehicle_events("vehev",output_folder,null);
    }

    @Test
    @Ignore
    public void test_request_vehicle_class(){
        api.request_vehicle_class("vc",output_folder);
    }

    @Test
    @Ignore
    public void test_request_vehicle_travel_time(){
        api.request_vehicle_travel_time("vtt",output_folder);
    }

    // sensors ..................

    // actuators ...............

    @Test
    @Ignore
    public void test_request_actuator(){
        api.request_actuator("act", output_folder,1l);
    }

    // controllers ..............

    @Test
    @Ignore
    public void test_request_controller(){
        api.request_controller(0l);
    }

    ////////////////////////////////////////////////////////
    // animation
    ////////////////////////////////////////////////////////

    @Test
    @Ignore
    public void test_advance() {
        try {
            api.advance(100f);
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    @Test
    @Ignore
    public void test_get_current_time(){
        System.out.println(api.get_current_time());
    }

    @Test
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

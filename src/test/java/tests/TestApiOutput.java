package tests;

import api.OTM;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class TestApiOutput extends AbstractTest {

    public static api.OTM otm;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        otm = new OTM();
        otm.load_test("line_ctm");
    }

    ////////////////////////////////////////////////////////
    // outputs
    ////////////////////////////////////////////////////////

    // network ................

    @Test
    @Ignore
    public void test_request_lanegroups(){
        otm.output.request_lanegroups("lgs",output_folder);
    }

    // links ....................

    @Test
    public void test_request_links_flow(){
        otm.output.request_links_flow("lf",output_folder,null,null,10f);
    }

    @Test
    public void test_request_links_veh(){
        otm.output.request_links_veh("lv",output_folder,null,null,10f);
    }

    // lanegroups ...............

    @Test
    public void test_request_lanegroup_flw(){
        otm.output.request_lanegroup_flw("lgf",output_folder,null,null,10f);
    }

    @Test
    public void test_request_lanegroup_veh(){
        otm.output.request_lanegroup_veh("lgv",output_folder,null,null,10f);
    }

    // subnetwroks ..............

    @Test
    public void test_request_path_travel_time(){
        otm.output.request_path_travel_time("ptt",output_folder,0l,10f);
    }

    @Test
    public void test_request_subnetwork_vht(){
        otm.output.request_subnetwork_vht("vht",output_folder,null,0l,10f);
    }

    // vehicles .................

    @Test
    public void test_request_vehicle_events(){
        otm.output.request_vehicle_events("vehev",output_folder,null);
    }

    @Test
    public void test_request_vehicle_class(){
        otm.output.request_vehicle_class("vc",output_folder);
    }

    @Test
    public void test_request_vehicle_travel_time(){
        otm.output.request_vehicle_travel_time("vtt",output_folder);
    }

    // controllers ..............

    @Test
    public void test_request_controller(){
        otm.output.request_controller(0l);
    }

}

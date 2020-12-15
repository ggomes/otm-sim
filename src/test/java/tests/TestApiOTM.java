package tests;

import api.OTM;
import error.OTMException;
import models.AbstractModel;
import models.fluid.AbstractFluidModel;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import output.animation.AbstractLinkInfo;
import output.animation.AnimationInfo;
import output.animation.macro.LaneGroupInfo;

import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.*;

public class TestApiOTM extends AbstractTest {

    public static api.OTM otm;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        otm = new OTM();
        otm.load_test("line_ctm");
    }

    ////////////////////////////////////////////////////////
    // load / save
    ////////////////////////////////////////////////////////

    @Test
    public void test_load(){

    }

    @Test
    public void test_load_from_jaxb(){

    }

    @Test
    public void test_load_test() {

    }

    @Test
    public void test_save()  {

    }

    ////////////////////////////////////////////////////////
    // initialize
    ////////////////////////////////////////////////////////

    @Test
    public void test_initialize()  {

    }

    ////////////////////////////////////////////////////////
    // run
    ////////////////////////////////////////////////////////

    @Test
    public void test_run() {

    }

    @Test
    public void test_advance() {

    }

    @Test
    public void test_terminate() {
    }

    ////////////////////////////////////////////////////////
    // getters
    ////////////////////////////////////////////////////////

    @Test
    public void test_get_path_ids(){

    }

    @Test
    public void test_add_subnetwork() {

    }

    @Test
    public void test_remove_subnetwork() {

    }

    @Test
    public void test_subnetwork_remove_links() {

    }

    @Test
    public void test_subnetwork_add_links() {

    }

    ////////////////////////////////////////////////////////
    // network
    ////////////////////////////////////////////////////////

    @Test
    public void test_get_link_connectivity(){

    }

    @Test
    public void test_get_source_link_ids(){
        Set<Long> source_ids = otm.get_source_link_ids();
        assertEquals((long)source_ids.iterator().next(),0l);
    }

    @Test
    public void test_get_in_lanegroups_for_road_connection(){

    }

    @Test
    public void test_get_out_lanegroups_for_road_connection(){

    }

    @Test
    public void test_get_link2lgs(){

    }

    ////////////////////////////////////////////////////////
    // STATE getters and setters -- may be model specific
    ////////////////////////////////////////////////////////

    @Test
    public void test_get_link_queues()  {

    }

    @Test
    public void test_set_link_vehicles() {
    }

    @Test
    public void test_clear_all_demands(){

    }

    @Test
    public void test_get_total_trips() {
        assertEquals(otm.get_total_trips(),583.3333333333334,0.0001);
    }

    ////////////////////////////////////////////////////////
    // animation info
    ////////////////////////////////////////////////////////

    @Ignore
    @Test
    public void test_get_animation_info() {

        try {

            float start_time = 0f;
            float duration = 100f;

            api.OTM otm = new api.OTM();
            otm.load_test("line_ctm");

            Set<Float> sim_dts = otm.scenario.network.models.values().stream()
                    .filter(m->m.type== AbstractModel.Type.Fluid)
                    .map(m->((AbstractFluidModel)m).dt_sec)
                    .collect(toSet());

            if(sim_dts.size()!=1)
                fail();

            float sim_dt = sim_dts.iterator().next();

            otm.initialize(start_time);
            float time = start_time;
            float end_time = start_time+duration;
            while(time<end_time){
                otm.advance(sim_dt);
                AnimationInfo info = otm.get_animation_info();
                AbstractLinkInfo link_info = info.link_info.get(3L);
                LaneGroupInfo lg_info2 = (output.animation.macro.LaneGroupInfo) link_info.lanegroup_info.get(2L);
                assertNotNull(lg_info2);
                time += sim_dt;
            }
            otm.terminate();
        } catch (OTMException e) {
            System.out.print(e);
            fail();
        }
    }

    ////////////////////////////////////////////////////////
    // getters
    ////////////////////////////////////////////////////////

    @Test
    public void test_get_current_time(){

    }

    ////////////////////////////////////////////////////////
    // static
    ////////////////////////////////////////////////////////

    @Test
    public void test_get_version(){
        assertTrue(!api.OTM.get_version().isEmpty());
    }

    @Test
    public void test_set_random_seed(){

    }

}

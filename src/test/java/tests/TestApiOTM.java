package tests;

import api.OTM;
import error.OTMException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
        try {
            otm.advance(100f);
        } catch (OTMException e) {
            e.printStackTrace();
        }
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

    ////////////////////////////////////////////////////////
    // getters
    ////////////////////////////////////////////////////////

    @Test
    public void test_get_current_time(){
        System.out.println(otm.get_current_time());
    }

    ////////////////////////////////////////////////////////
    // static
    ////////////////////////////////////////////////////////

    @Test
    public void test_get_version(){
        assertTrue(!api.OTM.get_version().isEmpty());
    }

    @Test
    public void test_set_random_seed(long seed){

    }

}

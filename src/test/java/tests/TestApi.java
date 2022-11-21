package tests;

import control.AbstractController;
import control.sigint.ControllerSignalFollower;
import core.OTM;
import error.OTMException;
import core.AbstractModel;
import core.AbstractFluidModel;
import org.junit.Ignore;
import org.junit.Test;
import output.animation.AbstractLinkInfo;
import output.animation.AnimationInfo;
import output.animation.macro.LaneGroupInfo;

import java.util.*;

import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.*;

public class TestApi extends AbstractTest {

    @Test
    public void test_load(){

        try {
            String configfile = "/home/gomes/code/otm/otm-sim/src/test/resources/test_configs/intersection.xml";
            String png_folder = "/home/gomes/code/otm/otm-sim/temp";
            float outdt = 2f;

            OTM otm = new OTM(configfile,false);

            otm.output.request_links_flow(null,null,null,null,outdt);
            otm.output.request_links_veh(null,null,null,null,outdt);

            otm.run(0f,300f);
            otm.plot_outputs(png_folder);

        } catch (OTMException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void test_intersection_api(){

        try {
            String configfile = "/home/gomes/Desktop/sadlk/intersection.xml";
            String png_folder = "/home/gomes/Desktop/sadlk/";
            float sim_dt = 2f;

            OTM otm = new OTM(configfile,false);

            List<Long> output_links = new ArrayList<>();
//            output_links.add(66l);
            output_links.add(6l);

            otm.output.request_lanegroup_flw(null,null,null,output_links,sim_dt);
            otm.output.request_lanegroup_veh(null,null,null,output_links,sim_dt);
//            otm.output.request_links_flow(null,null,null,output_links,sim_dt);
//            otm.output.request_links_veh(null,null,null,output_links,sim_dt);
            otm.output.request_controller("a",png_folder,0l);

            ControllerSignalFollower cntr = (ControllerSignalFollower) otm.scenario.controllers.get(0l);

            Map<Integer,ArrayList<Integer> > stages = new HashMap<>();
            stages.put(0,new ArrayList(Arrays.asList(1,5)));
            stages.put(1,new ArrayList(Arrays.asList(2,6)));

            otm.initialize(0f);

            float time = 0f;
            while(time<300){
                otm.advance(sim_dt);

                if(time==50f)
                    cntr.set_active_phases(stages.get(0));
                if(time==150f)
                    cntr.set_active_phases(stages.get(1));

                time += sim_dt;
            }

            otm.terminate();

            otm.plot_outputs(png_folder);

        } catch (OTMException e) {
            e.printStackTrace();
        }

    }

    @Ignore
    @Test
    public void test_set_model(){
        try {

            jaxb.Model model = new jaxb.Model();
            model.setIsDefault(true);
            model.setType("ctm");
            model.setName("new ctm");

            jaxb.ModelParams mp = new jaxb.ModelParams();
            mp.setSimDt(2f);
            mp.setMaxCellLength(100f);
            model.setModelParams(mp);

            OTM otm = OTM.load_test("line_ctm");
            otm.scenario.set_model(model);

        } catch (OTMException e) {
            e.printStackTrace();
        }

    }

    @Ignore
    @Test
    public void test_get_animation_info() {

        try {

            float start_time = 0f;
            float duration = 100f;

            OTM otm = OTM.load_test("line_ctm");

            Set<Float> sim_dts = otm.scenario.models.values().stream()
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
                AnimationInfo info = otm.scenario.get_animation_info();
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

    @Test
    public void test_get_version(){
        assertTrue(!OTM.get_version().isEmpty());
    }

}

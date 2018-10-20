package tests;

import api.API;
import api.info.DemandInfo;
import error.OTMException;
import jaxb.Demand;
import org.junit.Test;
import output.animation.AbstractLaneGroupInfo;
import output.animation.AbstractLinkInfo;
import output.animation.AnimationInfo;
import output.animation.macro.LaneGroupInfo;
import runner.OTM;
import xml.JaxbLoader;

import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class TestAnimationInfo {

    @Test
    public void run_step_by_step() {
        try {

            float start_time = 0f;
            float duration = 100f;
            float sim_dt = 2f;

            API api = OTM.load_test("line", sim_dt,true,"ctm");

            api.initialize(start_time);
            float time = start_time;
            float end_time = start_time+duration;
            while(time<end_time){
                api.advance(sim_dt);
                AnimationInfo info = api.get_animation_info();
                AbstractLinkInfo link_info = info.link_info.get(3L);
                LaneGroupInfo lg_info2 = (output.animation.macro.LaneGroupInfo) link_info.lanegroup_info.get(2L);
                assertNotNull(lg_info2);
                time += sim_dt;
            }
        } catch (OTMException e) {
            System.out.print(e);
            fail();
        }
    }

}

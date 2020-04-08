package tests;

import error.OTMException;
import models.AbstractModel;
import models.fluid.AbstractFluidModel;
import org.junit.Test;
import output.animation.AbstractLinkInfo;
import output.animation.AnimationInfo;
import output.animation.macro.LaneGroupInfo;

import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class TestAnimationInfo {

    @Test
    public void run_step_by_step() {
        try {

            float start_time = 0f;
            float duration = 100f;

            api.OTM otm = new api.OTM();
            otm.load_test("line");

            Set<Float> sim_dts = (new api.OTMdev(otm)).scenario.network.models.values().stream()
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
        } catch (OTMException e) {
            System.out.print(e);
            fail();
        }
    }

}

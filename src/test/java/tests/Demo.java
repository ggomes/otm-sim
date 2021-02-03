package tests;

import error.OTMException;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.fail;

public class Demo {

    @Test
    public void DemoLoad()  {
        try {
            core.OTM otm = new core.OTM("src/test/resources/test_configs/line_ctm.xml", true);
            System.out.println(otm==null ? "Failure" : "Success");
        } catch (Exception e) {
            System.err.print(e.getMessage());
            fail();
        }
    }

    @Test
    public void DemoRun()  {
        try {
            // load the scenario
            core.OTM otm = new core.OTM("src/test/resources/test_configs/line_ctm.xml", true);

            // request 10-second sampling of all link flows and densities
            float outdt = 10f;  // sampling time in seconds
            Set<Long> link_ids = otm.scenario.network.link_ids();  // request all link ids
            otm.output.request_links_flow(null, null,null,link_ids, outdt);
            otm.output.request_links_veh(null,null,null, link_ids, outdt);

            // run the simulation for 200 seconds
            otm.run(0,200f);

            // plot the output by iterating through the requested output data and
            // calling the 'plot_for_links' method.
            otm.plot_outputs("temp");

        } catch (OTMException e) {
            System.err.print(e.getMessage());
            fail();
        }
    }

    @Test
    public void DemoRunStep() {
        try {
            float start_time = 0f;
            float duration = 3600f;
            float advance_time = 300f;

            // load the scenario
            core.OTM otm = new core.OTM("src/test/resources/test_configs/line_ctm.xml", true);

            // initialize (prepare/rewind the simulation)
            otm.initialize(start_time);

            // run step-by-step using the 'advance' method
            float time = start_time;
            float end_time = start_time + duration;

            while (time < end_time) {
                System.out.println(time);
                otm.advance(advance_time);

                // Insert your code here -----

                time += advance_time;
            }

        } catch (OTMException e) {
            System.err.print(e.getMessage());
            fail();
        }
    }
}

package tests;

import error.OTMException;
import org.junit.Test;
import output.AbstractOutput;
import output.OutputLinkFlow;
import output.OutputLinkVehicles;
import output.OutputPathTravelTime;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.fail;

public class TestOutput extends AbstractTest {

    @Test
    public void run_one() {
        try {

            String configfile = "/home/gomes/Dropbox/gabriel/work/otm/efforts/paradise/from_Millard/Paradise_meso.xml";
            String output_folder = "/home/gomes/Desktop/test/";
            float duration = 25000f;
            float outdt = 30f;

            // Load ..............................
            api.OTM otm = new api.OTM(configfile,true,false);

            // Request .............................
            Set<Long> link_ids = otm.scenario.get_link_ids();
            for(Long path_id : otm.scenario.get_path_ids())
                otm.output().request_path_travel_time(path_id,outdt);
//            otm.output().request_path_travel_time(1l,outdt);

            otm.output().request_links_flow(null,link_ids,outdt);
            otm.output().request_links_veh(null,link_ids,outdt);
//            otm.output().request_vehicle_travel_time();


            // Run .................................
            otm.run(0,duration);

            // Print output .........................
            for(AbstractOutput output :  otm.output.get_data()){

                switch(output.type){
                    case link_veh:
                        ((OutputLinkVehicles) output).plot_for_links(null, String.format("%sveh.png", output_folder));
                        break;
                    case link_flw:
                        ((OutputLinkFlow) output).plot_for_links(null, String.format("%sflow.png", output_folder));
                        break;
                    case vehicle_travel_time:
                        break;
                    case path_travel_time:
                        OutputPathTravelTime ptt = (OutputPathTravelTime) output;
                        ptt.plot(String.format("%stt%d.png", output_folder,ptt.get_path_id()));

                        List<Double> travel_times = ptt.get_travel_times_sec();
                        System.out.println(travel_times);
                        break;
                }

            }

        } catch (OTMException e) {
            System.out.print(e);
            fail();
        }
    }


}

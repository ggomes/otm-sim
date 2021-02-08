package tests;

import core.OTM;
import error.OTMException;
import org.junit.Ignore;
import org.junit.Test;
import output.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class DebugRuns extends AbstractTest {

    @Ignore
    @Test
    public void run_one() {
        try {

            // ..........................................

            boolean makeplots = true;

            boolean do_links        = true;
            boolean do_lanegroups   = true;
            boolean do_cells        = true;
            boolean do_subnetworks  = false;
            boolean do_vehicles     = false;
            boolean do_controllers  = false;

            boolean sysout2file = false;
            String configfile = "/home/gomes/Desktop/x/test_events.xml";
            float start_time = 0f;
            float duration = 600f;
            float outdt = 2f;
            String prefix = makeplots?null:"x";
            String outfolder = makeplots?null:"/home/gomes/Desktop/x/output";
            String png_folder = "/home/gomes/Desktop/x/output";
            Set<Long> link_ids = Set.of(2l,3l,4l);

            Long subnetid = null;
            Long cntrl_id = null;

            Long comm_id= null;

            // ..........................................

            // redirect System.out to a file
            if(sysout2file) {
                try {
                    File file = new File(String.format("%s/log.txt", outfolder));
                    PrintStream stream = null;
                    stream = new PrintStream(file);
                    System.out.println("System.out directed to " + file.getAbsolutePath());
                    System.setOut(stream);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }

            // Timer ..............................
            long startTime = System.nanoTime();

            // Load ..............................
            OTM otm = new OTM(configfile,false);

            long endTime = System.nanoTime();
            double seconds = (endTime - startTime)/1e9;
            System.out.println("Load time: " + seconds);
            startTime = endTime;

            if(link_ids==null)
                link_ids = otm.scenario.network.links.keySet();

            // links
            if(do_links){
                otm.output.request_links_flow(prefix,outfolder,comm_id,link_ids,outdt);
                otm.output.request_links_veh(prefix,outfolder,comm_id,link_ids,outdt);
//                otm.output.request_links_sum_veh(prefix,outfolder,comm_id,link_ids,outdt);
//                otm.output.request_link_queues(prefix,outfolder,comm_id,link_ids,outdt);
            }

            // lane groups
            if(do_lanegroups){
                otm.output.request_lanegroups(prefix,outfolder);
                otm.output.request_lanegroup_flw(prefix,outfolder,comm_id,link_ids,outdt);
                otm.output.request_lanegroup_veh(prefix,outfolder,comm_id,link_ids,outdt);
//                otm.output.request_lanegroup_sum_veh(prefix,outfolder,comm_id,link_ids,outdt);
            }

            // cells
            if(do_cells){
                otm.output.request_cell_flw(prefix,outfolder,comm_id,link_ids,outdt);
                otm.output.request_cell_veh(prefix,outfolder,comm_id,link_ids,outdt);
//                otm.output.request_cell_sum_veh(prefix,outfolder,comm_id,link_ids,outdt);
//                otm.output.request_cell_sum_veh_dwn(prefix,outfolder,comm_id,link_ids,outdt);
//                otm.output.request_cell_lanechange_out(prefix,outfolder,comm_id,link_ids,outdt);
//                otm.output.request_cell_lanechange_in(prefix,outfolder,comm_id,link_ids,outdt);
            }

            // subnetworks
            if(do_subnetworks){
                otm.output.request_path_travel_time(prefix,outfolder,subnetid,outdt);
                otm.output.request_subnetwork_vht(prefix,outfolder,comm_id,subnetid,outdt);
            }

            // vehicle events
            if(do_vehicles){
                otm.output.request_vehicle_events(prefix,outfolder,comm_id);
                otm.output.request_vehicle_class(prefix,outfolder);
                otm.output.request_travel_time(prefix,outfolder);
            }

            // controllers
            if(do_controllers){
                otm.output.request_controller(prefix,outfolder,cntrl_id);
            }

            // Timer
            endTime = System.nanoTime();
            seconds = (endTime - startTime)/1e9;
            System.out.println("Requests time: " + seconds);
            startTime = endTime;

            // Run .................................
            otm.run(start_time,duration);

            // Timer
            endTime = System.nanoTime();
            seconds = (endTime - startTime)/1e9;
            System.out.println("Run time: " + seconds);
            startTime = endTime;

            otm.plot_outputs(png_folder);

        } catch (OTMException e) {
            System.out.print(e);
            fail();
        }

    }

    @Ignore
    @Test
    public void run_step_by_step(){

        try {

            String configfile = "/home/gomes/Downloads/aaa_0.xml";

            float start_time = 21600f;
            float duration = 1000f;
            float outdt = 300f;
            float simdt = 5f;
            String prefix = "x";
            String outfolder = "/home/gomes/Downloads";

            // Load ..............................
            OTM otm = new OTM(configfile,true);

            // Output requests .....................
            Set<Long> link_ids = new HashSet<>();   //otm.scenario.get_link_ids();
            link_ids.add(3l);

//            otm.output.request_lanegroup_flw(0l,link_ids,outdt);
            otm.output.request_lanegroup_veh(null,null,0l,link_ids,outdt);

            // Run .................................
            otm.initialize(start_time);
            otm.advance(start_time);

            int simsteps = (int) Math.ceil(duration/simdt);
            int steps_taken = 0;
            while(steps_taken<simsteps){
                otm.advance(simdt);
                steps_taken += 1;
            }

            otm.terminate();

            // Print output .........................
            for(AbstractOutput output :  otm.output.get_data()){

                if (output instanceof OutputLaneGroupFlow) {
                    OutputLaneGroupFlow x = (OutputLaneGroupFlow) output;
                    String commid = x.commodity==null ? "all" : String.format("%d",x.commodity.getId());
                    x.plot_for_links(null,  String.format("%s/%s_lg_flow.png", outfolder, commid));
                }

                if (output instanceof OutputLaneGroupVehicles) {
                    ((OutputLaneGroupVehicles) output).plot_for_links(null,String.format("%s/lg_veh.png", outfolder));
                }

            }

        } catch (OTMException e) {
            System.out.print(e);
            fail();
        }

    }

    @Ignore
    @Test
    public void load_save(){
        try {
            String configfile = "/home/gomes/Desktop/aaa/aaa_1.xml";
            OTM otm = new OTM(configfile,true);
            otm.save("/home/gomes/Desktop/aaa/bbb.xml");
            assertNotNull(otm);
        } catch (OTMException e) {
            fail(e.getMessage());
        }
    }

    @Ignore
    @Test
    public void load_with_plugin() {
        try {
            String configfile = "/home/gomes/code/otm-models/cfg/line.xml";
            OTM otm = new OTM(configfile,true);
            assertNotNull(otm);
        } catch (OTMException e) {
            fail(e.getMessage());
        }
    }

    @Ignore
    @Test
    public void test_set_model(){
        try {

            OTM otm = new OTM("/home/gomes/Desktop/x/setmodel/test_set_model.xml",true);

            String prefix = "aaa";
            String outfolder = "/home/gomes/Desktop/x/setmodel/out";
            Long comm_id = null;
            Collection<Long> link_ids = null;
            float outdt = 5f;
            long cntrl_id = 0l;
            long subnetid = 0l;

            otm.output.request_links_flow(prefix,outfolder,comm_id,link_ids,outdt);

            otm.output.request_links_flow(prefix,outfolder,comm_id,link_ids,outdt);
            otm.output.request_links_veh(prefix,outfolder,comm_id,link_ids,outdt);
            otm.output.request_links_sum_veh(prefix,outfolder,comm_id,link_ids,outdt);
            otm.output.request_link_queues(prefix,outfolder,comm_id,link_ids,outdt);

            otm.output.request_lanegroups(prefix,outfolder);
            otm.output.request_lanegroup_flw(prefix,outfolder,comm_id,link_ids,outdt);
            otm.output.request_lanegroup_veh(prefix,outfolder,comm_id,link_ids,outdt);
            otm.output.request_lanegroup_sum_veh(prefix,outfolder,comm_id,link_ids,outdt);

            otm.output.request_cell_flw(prefix,outfolder,comm_id,link_ids,outdt);
            otm.output.request_cell_veh(prefix,outfolder,comm_id,link_ids,outdt);
            otm.output.request_cell_sum_veh(prefix,outfolder,comm_id,link_ids,outdt);
            otm.output.request_cell_sum_veh_dwn(prefix,outfolder,comm_id,link_ids,outdt);
            otm.output.request_cell_lanechange_out(prefix,outfolder,comm_id,link_ids,outdt);
            otm.output.request_cell_lanechange_in(prefix,outfolder,comm_id,link_ids,outdt);

//            otm.output.request_path_travel_time(prefix,outfolder,subnetid,outdt);
//            otm.output.request_subnetwork_vht(prefix,outfolder,comm_id,subnetid,outdt);

            otm.output.request_vehicle_events(prefix,outfolder,comm_id);
            otm.output.request_vehicle_class(prefix,outfolder);
            otm.output.request_travel_time(prefix,outfolder);

            otm.output.request_controller(prefix,outfolder,cntrl_id);




            jaxb.Model model = new jaxb.Model();
            model.setIsDefault(true);
            model.setType("ctm");
            model.setName("new ctm");

            jaxb.ModelParams mp = new jaxb.ModelParams();
            mp.setSimDt(2f);
            mp.setMaxCellLength(100f);
            model.setModelParams(mp);

            otm.scenario.set_model(model);

            otm.run(0f,3600f);

        } catch (OTMException e) {
            e.printStackTrace();
        }

    }

}

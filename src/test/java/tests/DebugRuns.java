package tests;

import core.OTM;
import error.OTMException;
import models.vehicle.spatialq.OutputLinkQueues;
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

            boolean do_links        = false;
            boolean do_lanegroups   = true;
            boolean do_cells        = true;
            boolean do_subnetworks  = false;
            boolean do_vehicles     = false;
            boolean do_controllers  = false;

            boolean sysout2file = false;
            String configfile = "/home/gomes/Desktop/x/opttest_models.xml";
            float start_time = 0f;
            float duration = 100f;
            float outdt = 5f;
            String prefix = makeplots?null:"x";
            String outfolder = makeplots?null:"/home/gomes/Desktop/x/models";
            String png_folder = "/home/gomes/Desktop/x/models";
            Set<Long> link_ids =  Set.of(6l,8l);

            Long subnetid = null;
            Long cntrl_id = null;

            Long comm_id=null;

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

            // Load ..............................
            OTM otm = new OTM(configfile,true);

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
                otm.output.request_lanegroup_sum_veh(prefix,outfolder,comm_id,link_ids,outdt);
            }

            // cells
            if(do_cells){
                otm.output.request_cell_flw(prefix,outfolder,comm_id,link_ids,outdt);
                otm.output.request_cell_veh(prefix,outfolder,comm_id,link_ids,outdt);
                otm.output.request_cell_sum_veh(prefix,outfolder,comm_id,link_ids,outdt);
                otm.output.request_cell_sum_veh_dwn(prefix,outfolder,comm_id,link_ids,outdt);
                otm.output.request_cell_lanechange_out(prefix,outfolder,comm_id,link_ids,outdt);
                otm.output.request_cell_lanechange_in(prefix,outfolder,comm_id,link_ids,outdt);
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

            // Run .................................
            otm.run(start_time,duration);

            // Print output .........................
            for(AbstractOutput output :  otm.output.get_data()){

                // links
                if (output instanceof OutputLinkFlow)
                    ((OutputLinkFlow) output).plot_for_links(null, String.format("%s/link_flow.png", png_folder));

                if (output instanceof OutputLinkVehicles)
                    ((OutputLinkVehicles) output).plot_for_links(null, String.format("%s/link_veh.png", png_folder));

                if (output instanceof OutputLinkSumVehicles)
                    ((OutputLinkSumVehicles) output).plot_for_links(null, String.format("%s/link_sumveh.png", png_folder));

                if (output instanceof OutputLinkQueues)
                    ((OutputLinkQueues) output).plot(String.format("%s/link_sumqueues.png", png_folder));

                // lane groups

                if (output instanceof OutputLaneGroupFlow) {
                    OutputLaneGroupFlow x = (OutputLaneGroupFlow) output;
                    String commid = x.commodity==null ? "all" : String.format("%d",x.commodity.getId());
                    String title = "Commodity " + (x.commodity==null ? "all" : x.commodity.name);
                    x.plot_for_links(null,title,  String.format("%s/lg_flow_%s.png", png_folder, commid));
                }

                if (output instanceof OutputLaneGroupVehicles) {
                    String title = "";
                    ((OutputLaneGroupVehicles) output).plot_for_links(null, title,String.format("%s/lg_veh.png", png_folder));
                }

                if (output instanceof OutputLaneGroupSumVehicles) {
                    String title = "";
                    ((OutputLaneGroupSumVehicles) output).plot_for_links(null, title,String.format("%s/lg_sumveh.png", png_folder));
                }

                // cells

                if (output instanceof OutputCellFlow)
                    ((OutputCellFlow) output).plot_for_links(null, String.format("%s/cell_flow.png", png_folder));

                if (output instanceof OutputCellVehicles)
                    ((OutputCellVehicles) output).plot_for_links(null, String.format("%s/cell_veh.png", png_folder));

                if (output instanceof OutputCellSumVehicles)
                    ((OutputCellSumVehicles) output).plot_for_links(null, String.format("%s/cell_sumveh.png", png_folder));

                if (output instanceof OutputCellSumVehiclesDwn)
                    ((OutputCellSumVehiclesDwn) output).plot_for_links(null, String.format("%s/cell_sumvehdwn.png", png_folder));

                if (output instanceof OutputCellLanechangeOut)
                    ((OutputCellLanechangeOut) output).plot_for_links(null, String.format("%s/cell_lc_out.png", png_folder));

                if (output instanceof OutputCellLanechangeIn)
                    ((OutputCellLanechangeIn) output).plot_for_links(null, String.format("%s/cell_lc_in.png", png_folder));

                // subnetworks
                if (output instanceof OutputPathTravelTime)
                    ((OutputPathTravelTime) output).plot(String.format("%s/path_tt.png", png_folder));

                if (output instanceof OutputSubnetworkVHT)
                    ((OutputSubnetworkVHT) output).plot_for_links(null, String.format("%s/vht.png", png_folder));

                // vehicle events

                if (output instanceof OutputVehicleEvents)
                    ((OutputVehicleEvents) output).plot(String.format("%s/veh_events.png", png_folder));

//                if (output instanceof OutputVehicleClass)
//                    ((OutputVehicleClass) output).plot(String.format("%s/veh_class.png", png_folder));

                if (output instanceof OutputTravelTime)
                    ((OutputTravelTime) output).plot(String.format("%s/veh_traveltime.png", png_folder));

                // controllers

                if (output instanceof OutputController)
                    ((OutputController) output).plot(String.format("%s/controller.png", png_folder));

            }

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
                    String title = "Commodity " + (x.commodity==null ? "all" : x.commodity.name);
                    x.plot_for_links(null,title,  String.format("%s/lg_flow_%s.png", outfolder, commid));
                }

                if (output instanceof OutputLaneGroupVehicles) {
                    String title = "";
                    ((OutputLaneGroupVehicles) output).plot_for_links(null, title,String.format("%s/lg_veh.png", outfolder));
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

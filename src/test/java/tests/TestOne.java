package tests;

import error.OTMException;
import org.junit.Ignore;
import org.junit.Test;
import output.*;
import runner.OTM;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class TestOne extends AbstractTest {

    @Ignore
    @Test
    public void run_step_by_step() {
        try {

            float start_time = 0f;
            float duration = 3600f;
            float advance_time = 1f;

            String configfile = "/home/gomes/Downloads/z_0.xml";
            String prefix = "test";
            String output_folder = "/home/gomes/Desktop/miami/2/";

            System.out.println("Loading.");
            api.OTM otm = new api.OTM();
            otm.load(configfile,true,false);

            System.out.println("Initializing.");
            otm.initialize(start_time);

            float time = start_time;
            float end_time = start_time+duration;
            while(time<end_time){
                otm.advance(advance_time);
                System.out.println(otm.get_current_time());
                time += advance_time;
            }

        } catch (OTMException e) {
            System.out.print(e);
            fail();
        }
    }

    @Test
    public void load_one() {
        try {
            String configfile = "/home/gomes/Desktop/nrel_presentation/lakewood_savesd.xml";
            api.OTM otm = new api.OTM(configfile,false,false);
            assertNotNull(otm);
        } catch (OTMException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void load_save(){
        try {
            String configfile = "/home/gomes/Desktop/nrel_presentation/lakewood.xml";
            api.OTM otm = new api.OTM(configfile,false,false);
            otm.save("/home/gomes/Desktop/nrel_presentation/lakewood_savesd.xml");
            assertNotNull(otm);
        } catch (OTMException e) {
            fail(e.getMessage());
        }

    }

//    @Test
//    public void load_with_plugin() {
//        try {
//            String configfile = "/home/gomes/code/otm-models/cfg/line.xml";
//            api.OTM otm = new api.OTM(configfile,true,false);
//            assertNotNull(otm);
//        } catch (OTMException e) {
//            fail(e.getMessage());
//        }
//    }

    @Ignore
    @Test
    public void load_for_traffic_assignment() {
        try {

            float sample_dt = 2;
            int num_samp = 10;
            long path_id = 1;

            float start_time = 0.0f;
            float time_horizon = 1000f;

            String configfile = "C:\\Users\\gomes\\Desktop\\seven_links.xml";
            String outfolder  = "C:\\Users\\gomes\\Desktop\\";
            api.OTM otm = new api.OTM(configfile);

//            List<ODInfo> od_infos = otm.scenario.get_od_info();
//            ODInfo od_info = od_infos.get(0);
//            List<SubnetworkInfo> xxx = od_info.get_subnetworks();


            otm.output.request_path_travel_time(path_id, sample_dt);
//            otm.request_links_flow(null, api.get_link_ids(), sample_dt);
//            otm.request_links_veh(null, api.get_link_ids(), sample_dt);


            otm.set_random_seed(1);

            otm.run(start_time,time_horizon);

            boolean instantaneous = true;
            for(AbstractOutput output : otm.output.get_data()){

//                if (output instanceof LinkFlow)
//                    ((LinkFlow) output).plot_for_links(null, String.format("%sflow.png", outfolder));
//
//                if (output instanceof LinkVehicles)
//                    ((LinkVehicles) output).plot_for_links(null, String.format("%sveh.png", outfolder));

//                if(output instanceof PathTravelTimeWriter){
//                    PathTravelTimeWriter ptt = (PathTravelTimeWriter) output;
//                    List<Double> cost_list;
//                    if(instantaneous)
//                        cost_list = ptt.compute_instantaneous_travel_times(start_time, sample_dt, num_samp);
//                    else
//                        cost_list = ptt.compute_predictive_travel_times(start_time, sample_dt, num_samp);
//
//                    System.out.println(cost_list);
//
//                }
            }


        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

//    @Ignore
    @Test
    public void run_one() {
        try {

            // redirect System.out to a file
//            File file = new File("/home/gomes/Desktop/test/122/log.txt");
//            PrintStream stream = new PrintStream(file);
//            System.out.println("From now on "+file.getAbsolutePath()+" will be your console");
//            System.setOut(stream);

//            System.out.println("t\tlg\tc\tflwin\tflwdwn\tflwout");

            String configfile = "/home/gomes/Downloads/231_0.xml";

            float duration = 10000f;
            float outdt = 5f;
            String prefix = "x";
            String output_folder = "/home/gomes/Downloads";

            // Load ..............................
            api.OTM otm = new api.OTM(configfile,true,false);

            // Output requests .....................
            Set<Long> link_ids =  new HashSet<>(); //otm.scenario.get_link_ids();
//            link_ids.add(1l);
            link_ids.add(2l);
//            link_ids.add(7l);
//            link_ids.add(3l);

            Long comm_id=1l;

            // links

//            otm.output.request_links_flow(null, link_ids, outdt);
//            otm.output.request_links_veh(null, link_ids, outdt);
//            otm.output.request_links_sum_veh(null,link_ids,outdt);

//            otm.output.request_links_flow(prefix,output_folder,null, link_ids, outdt);
//            otm.output.request_links_veh(prefix,output_folder,null, link_ids, outdt);

            // lanegroups

//            otm.output.request_lanegroup_flw(comm_id,link_ids,outdt);
//            otm.output.request_lanegroup_veh(comm_id,link_ids,outdt);
//            otm.output.request_lanegroup_sum_veh(comm_id,link_ids,outdt);

//            otm.output.request_lanegroups(prefix,output_folder);
//            otm.output.request_lanegroup_flw(prefix,output_folder,null,link_ids,outdt);
//            otm.output.request_lanegroup_veh(prefix,output_folder,null,link_ids,outdt);
//            otm.output.request_lanegroup_sum_veh(prefix,output_folder,null,link_ids,outdt);

            // cells

            otm.output.request_cell_flw(comm_id, link_ids, outdt);
            otm.output.request_cell_veh(comm_id,link_ids, outdt);
            otm.output.request_cell_lanechange_out(comm_id, link_ids, outdt);
            otm.output.request_cell_lanechange_in(comm_id, link_ids, outdt);
//            otm.output.request_cell_sum_veh(null, link_ids, outdt);

            otm.output.request_cell_flw(prefix,output_folder,null, link_ids, outdt);
            otm.output.request_cell_veh(prefix,output_folder,null,link_ids, outdt);
            otm.output.request_cell_lanechange_out(prefix,output_folder,null, link_ids, outdt);
            otm.output.request_cell_lanechange_in(prefix,output_folder,null, link_ids, outdt);
//            otm.output.request_cell_sum_veh(prefix,output_folder,null, link_ids, outdt);

            /////////////////////////
//            List<ODInfo> od_infos = api.get_od_info();
//            ODInfo od_info = od_infos.get(0);
//            List<SubnetworkInfo> paths = od_info.get_subnetworks();
//
//            long path_id = paths.get(0).getId();
//
//            api.request_path_travel_time(path_id, outdt);

//            api.request_links_flow(null, api.get_link_ids(), outdt);
//            api.request_links_veh(null, api.get_link_ids(), outdt);

//            otm.output.request_controller(prefix,output_folder,0l);
//            otm.output.request_controller(0L);

            // Run .................................
            otm.run(0,duration);

            // Print output .........................
            for(AbstractOutput output :  otm.output.get_data()){

                if (output instanceof OutputLinkFlow)
                    ((OutputLinkFlow) output).plot_for_links(null, String.format("%s/link_flow.png", output_folder));

                if (output instanceof OutputLinkVehicles)
                    ((OutputLinkVehicles) output).plot_for_links(null, String.format("%s/link_veh.png", output_folder));

                if (output instanceof OutputLinkSumVehicles)
                    ((OutputLinkSumVehicles) output).plot_for_links(null, String.format("%s/link_sumveh.png", output_folder));

                if (output instanceof OutputLaneGroupFlow) {
                    OutputLaneGroupFlow x = (OutputLaneGroupFlow) output;
                    String commid = x.commodity==null ? "all" : String.format("%d",x.commodity.getId());
                    String title = "Commodity " + (x.commodity==null ? "all" : x.commodity.name);
                    x.plot_for_links(null,title,  String.format("%s/lg_flow_%s.png", output_folder, commid));
                }

                if (output instanceof OutputLaneGroupVehicles) {
                    String title = "";
                    ((OutputLaneGroupVehicles) output).plot_for_links(null, title,String.format("%s/lg_veh.png", output_folder));
                }

                if (output instanceof OutputLaneGroupSumVehicles) {
                    String title = "";
                    ((OutputLaneGroupSumVehicles) output).plot_for_links(null, title,String.format("%s/lg_sumveh.png", output_folder));
                }

                if (output instanceof OutputCellFlow)
                    ((OutputCellFlow) output).plot_for_links(null, String.format("%s/cell_flow.png", output_folder));

                if (output instanceof OutputCellVehicles)
                    ((OutputCellVehicles) output).plot_for_links(null, String.format("%s/cell_veh.png", output_folder));

                if (output instanceof OutputCellSumVehicles)
                    ((OutputCellSumVehicles) output).plot_for_links(null, String.format("%s/cell_sumveh.png", output_folder));

                if (output instanceof OutputCellLanechangeOut)
                    ((OutputCellLanechangeOut) output).plot_for_links(null, String.format("%s/cell_lc_out.png", output_folder));

                if (output instanceof OutputCellLanechangeIn)
                    ((OutputCellLanechangeIn) output).plot_for_links(null, String.format("%s/cell_lc_in.png", output_folder));





//                if (output instanceof OutputController)
//                    ((OutputController) output).plot(String.format("%s/controller%d.png",output_folder,((OutputController) output).controller_id));

//                if(output instanceof PathTravelTimeWriter){
//                    PathTravelTimeWriter ptt = (PathTravelTimeWriter) output;
//                    List<Double> travel_times = ptt.get_travel_times_sec();
//                    System.out.println(travel_times);
//                }

            }

        } catch (OTMException e) {
            System.out.print(e);
            fail();
        }
//        catch (FileNotFoundException e) {
//            System.out.print(e);
//            fail();
//        }
    }

    @Ignore
    @Test
    public void run_RL() {


//        String configfile = "C:\\Users\\gomes\\Desktop\\traffic_master\\Capstone_0314.xml";
        String configfile = "C:\\Users\\gomes\\Desktop\\traffic_master\\n_Capstone_0426_9000.xml";

        float duration = 3600f;
        float outdt = 300f;

        // Load ..............................
        api.OTM otm = null;

        try {
            otm = new api.OTM(configfile);

//            // Output requests .....................
//            List<Long> list_orig_link_ids = otm.scenario.get_link_ids();
//            otm.output.request_links_flow(null, list_orig_link_ids, outdt);
//            otm.output.request_links_veh(null, list_orig_link_ids, outdt);
//
//            List<Long> ramp_ids = new ArrayList<>();
//            for(ActuatorInfo act_info : otm.scenario.get_actuators())
//                ramp_ids.add(act_info.target.getId());

//            ControllerCapacity controller = (ControllerCapacity) otm.scenario.get_actual_controller_with_id(1);

            // Qtable loop

            double[] sum_vehicles = new double[6];
            double[] sum_flow = new double[6];

//            for( int i=0 ; i<6 ; i++){
//
//                // update control
//                for(Long ramp_id : ramp_ids) {
//                    float rate_ramp = i*300f;
//                    controller.set_rate_vph_for_actuator(ramp_id, rate_ramp);
//                }
//
//                System.out.println("i=" + i);
//
//                otm.run(0,duration);
//
//                // extract output .........................
//                sum_vehicles[i] = 0d;
//                sum_flow[i] = 0d;
//
//                for(AbstractOutput output :  otm.output.get_data()){
//
//                    if (output instanceof LinkFlow){
//
//                        for(Long link_id : list_orig_link_ids) {
//                            Profile1D profile = ((LinkFlow) output).get_flow_profile_for_link_in_vph(link_id);
//                            List<Double> values = profile.get_values();
//    //                        System.out.println(String.format("LinkFlow: id=%d num_values=%d",link_id,values.size()));
//
//                            sum_flow[i] += values.stream().mapToDouble(x->x).sum();
//                        }
//                    }
//
//
//                    if (output instanceof LinkVehicles) {
//                        for(Long link_id : list_orig_link_ids) {
//                            Profile1D profile = ((LinkVehicles) output).get_profile_for_linkid(link_id);
//                            List<Double> values = profile.get_values();
//    //                        System.out.println(String.format("LinkVehicles: id=%d num_values=%d",link_id,values.size()));
//
//                            sum_vehicles[i] += values.stream().mapToDouble(x->x).sum();
//                        }
//                    }
//
//                }
//
//                System.out.println(i + "\t" + sum_flow[i] + "\t" + sum_vehicles[i]);
//
//                // do some Qtable calculation



//            }
        } catch (OTMException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void run_main(){

        //    0 command
        //    1 configfile
        //    2 prefix
        //    3 output_request
        //    4 output folder
        //    5 start_time
        //    6 duration

        String resource_folder = (new File("src/test/resources/")).getAbsolutePath()+ File.separator ;

        String[] args = new String[7];
        args[0] = "-run";
        args[1] = resource_folder + "test_configs/line_ctm.xml";
        args[2] = "mytest";
        args[3] = resource_folder+"sample_output_request.xml";
        args[4] = "temp";
        args[5] = "0";
        args[6] = "100";

        OTM.main(args);

    }

}

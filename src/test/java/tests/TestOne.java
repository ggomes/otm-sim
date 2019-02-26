/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package tests;

import api.API;
import api.info.LinkInfo;
import api.info.ODInfo;
import api.info.Profile1DInfo;
import error.OTMException;
import org.junit.Ignore;
import org.junit.Test;
import output.*;
import runner.OTM;

import java.util.List;

import static org.junit.Assert.fail;

public class TestOne extends AbstractTest {

    @Ignore
    @Test
    public void test_load_for_static_traffic_assignment() {
        try {

            // TODO Add large network to test configurations
            String configfile = "C:\\Users\\gomes\\Dropbox\\gabriel\\work\\beats\\beats_share\\MetroManila_unfiltered.xml";

            API api = OTM.load_for_static_traffic_assignment(configfile);
            
            System.out.println(api.get_node_ids().size());
            System.out.println(api.get_link_connectivity().size());

            LinkInfo link = api.get_link_with_id(107948L);

            System.out.println(link.getFull_length());

            List<ODInfo> odinfo = api.get_od_info();

            Profile1DInfo profile = odinfo.get(0).get_total_demand_vps();

            System.out.println(profile);

        } catch (OTMException e) {
            System.out.print(e);
            fail();
        }
    }

    @Ignore
    @Test
    public void run_step_by_step() {
        try {

            float start_time = 0f;
            float duration = 3600f;

            API api = OTM.load_test("onramp_offramp_1",true);

            api.initialize(start_time);

            float time = start_time;
            float end_time = start_time+duration;
//            while(time<end_time){
//                api.advance(sim_dt);
//                time += sim_dt;
//            }

        } catch (OTMException e) {
            System.out.print(e);
            fail();
        }
    }

    @Ignore
    @Test
    public void load_one() {
        try {
            String configfile = "C:\\Users\\gomes\\Desktop\\xxx\\Capstone_0211.xml";
            API api = OTM.load(configfile,true);
//            API api = OTM.load("/home/gomes/code/otm-mpi-bb/config/100.xml",true);
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    @Ignore
    @Test
    public void run_one() {
        try {

//            String configfile = "C:\\Users\\gomes\\Downloads\\aimsun_extracted.xml";
            String configfile = "C:\\Users\\gomes\\Desktop\\xxx\\Capstone_0211.xml";

            float duration = 1000f;
            float outdt = 10f;
            String prefix = "test";
            String output_folder = "temp/";

            // Load ..............................
            API api = null;
            try {
                api = OTM.load(configfile,true);
            } catch (OTMException e) {
                e.printStackTrace();
            }

            api.set_stochastic_process("deterministic");

            // Output requests .....................
//            api.request_links_flow(prefix,output_folder,null, api.get_link_ids(), outdt);
//            api.request_links_veh(prefix,output_folder,null, api.get_link_ids(), outdt);

//            api.request_links_flow(null, api.get_link_ids(), outdt);
//            api.request_links_veh(null, api.get_link_ids(), outdt);

//            api.request_controller(1L);
//            api.request_actuator(1L);

            // Run .................................
            api.run(0,duration);

            // Print output .........................
            String outfolder = "temp/";
            for(AbstractOutput output :  api.get_output_data()){

                if (output instanceof EventsActuator)
                    ((EventsActuator) output).plot(String.format("%sactuator%d.png",outfolder,((EventsActuator) output).actuator_id));

                if (output instanceof EventsController)
                    ((EventsController) output).plot(String.format("%scontroller%d.png",outfolder,((EventsController) output).controller_id));

                if (output instanceof LinkFlow)
                    ((LinkFlow) output).plot_for_links(null,String.format("%sflow.png",outfolder));

                if (output instanceof LinkVehicles)
                    ((LinkVehicles) output).plot_for_links(null,String.format("%sveh.png",outfolder));

            }

        } catch (OTMException e) {
            System.out.print(e);
            fail();
        }
    }

    @Ignore
    @Test
    public void run_one_test() {
        try {
            float duration = 1000f;
            float outdt = 10f;
            String prefix = "test";
            String output_folder = "temp/";

            // Load ..............................
            API api = null;
            try {
                api = OTM.load_test("signal_nopocket",true);
//                api = OTM.load("C:\\Users\\gomes\\vbox_shared\\all_cfgs\\100.xml",true,"ctm");
            } catch (OTMException e) {
                e.printStackTrace();
            }

            api.set_stochastic_process("deterministic");

            // Output requests .....................
            api.request_links_flow(prefix,output_folder,null, api.get_link_ids(), outdt);
            api.request_links_veh(prefix,output_folder,null, api.get_link_ids(), outdt);

//            api.request_links_flow(null, api.get_link_ids(), outdt);
//            api.request_links_veh(null, api.get_link_ids(), outdt);

//            api.request_controller(1L);
//            api.request_actuator(1L);

            // Run .................................
            api.run(0,duration);

            // Print output .........................
            String outfolder = "temp/";
            for(AbstractOutput output :  api.get_output_data()){

                if (output instanceof EventsActuator)
                    ((EventsActuator) output).plot(String.format("%sactuator%d.png",outfolder,((EventsActuator) output).actuator_id));

                if (output instanceof EventsController)
                    ((EventsController) output).plot(String.format("%scontroller%d.png",outfolder,((EventsController) output).controller_id));

                if (output instanceof LinkFlow)
                    ((LinkFlow) output).plot_for_links(null,String.format("%sflow.png",outfolder));

                if (output instanceof LinkVehicles)
                    ((LinkVehicles) output).plot_for_links(null,String.format("%sveh.png",outfolder));

            }

        } catch (OTMException e) {
            System.out.print(e);
            fail();
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

        String[] args = new String[7];
        args[0] = "-run";
        args[1] = "C:\\Users\\gomes\\Dropbox\\gabriel\\work\\pub\\current\\otm\\exp\\exp_micro.xml";
        args[2] = "micro";
        args[3] = "C:\\Users\\gomes\\Dropbox\\gabriel\\work\\pub\\current\\otm\\exp\\output_micro.xml";
        args[4] = "C:\\Users\\gomes\\Dropbox\\gabriel\\work\\pub\\current\\otm\\exp\\output";
        args[5] = "0";
        args[6] = "100";

        OTM.main(args);

    }

}

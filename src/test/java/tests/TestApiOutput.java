package tests;

import api.OTM;
import error.OTMException;
import models.vehicle.spatialq.OutputQueues;
import org.junit.Test;
import output.*;

import java.util.Set;

public class TestApiOutput extends AbstractTest {

    @Test
    public void test_outputs(){

        try {

            float start_time = 0f;
            float duration = 3600f;
            float outDt = 10f;

            api.OTM otm = new OTM();
            otm.load_test("output_test");

            Set<Long> link_ids = otm.scenario.network.links.keySet();
            long commid = 1l;
            long subnetid = 0l;

            // links
            otm.output.request_links_flow("lf",output_folder,commid,link_ids,outDt);
            otm.output.request_links_veh("lv",output_folder,commid,link_ids,outDt);
            otm.output.request_links_sum_veh("lsv",output_folder,commid,link_ids,outDt);
            otm.output.request_link_queues("lq",output_folder,commid,link_ids,outDt);

            // lanegroups ...............
            otm.output.request_lanegroups("lgs",output_folder);
            otm.output.request_lanegroup_flw("lgf",output_folder,commid,link_ids,outDt);
            otm.output.request_lanegroup_veh("lgv",output_folder,commid,link_ids,outDt);
            otm.output.request_lanegroup_sum_veh("lgsv",output_folder,commid,link_ids,outDt);

            // cells
            otm.output.request_cell_flw("cf",output_folder,commid,link_ids,outDt);
            otm.output.request_cell_veh("cv",output_folder,commid,link_ids,outDt);
            otm.output.request_cell_sum_veh("csv",output_folder,commid,link_ids,outDt);
            otm.output.request_cell_sum_veh_dwn("csvd",output_folder,commid,link_ids,outDt);
            otm.output.request_cell_lanechange_out("clco",output_folder,commid,link_ids,outDt);
            otm.output.request_cell_lanechange_in("clci",output_folder,commid,link_ids,outDt);

            // subnetwroks ..............
            otm.output.request_path_travel_time("ptt",output_folder,subnetid,outDt);
            otm.output.request_subnetwork_vht("vht",output_folder,commid,subnetid,outDt);

            // vehicles .................
            otm.output.request_vehicle_events("vehev",output_folder,commid);
            otm.output.request_vehicle_class("vc",output_folder);
            otm.output.request_vehicle_travel_time("vtt",output_folder);

            // controllers ..............
            otm.output.request_controller("cntrl", output_folder, 0l);

            // Run .................................
            otm.run(start_time,duration);

            // Print output .........................
            for(AbstractOutput output :  otm.output.get_data()){

                // links
                if (output instanceof OutputLinkFlow)
                    System.out.println("OutputLinkFlow");

                if (output instanceof OutputLinkVehicles)
                    System.out.println("OutputLinkVehicles");

                if (output instanceof OutputLinkSumVehicles)
                    System.out.println("OutputLinkSumVehicles");

                if (output instanceof OutputQueues)
                    System.out.println("OutputQueues");

                // lane groups
                if (output instanceof OutputLaneGroups)
                    System.out.println("OutputLaneGroups");

                if (output instanceof OutputLaneGroupFlow)
                    System.out.println("OutputLaneGroupFlow");

                if (output instanceof OutputLaneGroupVehicles)
                    System.out.println("OutputLaneGroupVehicles");

                if (output instanceof OutputLaneGroupSumVehicles)
                    System.out.println("OutputLaneGroupSumVehicles");

                // cells
                if (output instanceof OutputCellFlow)
                    System.out.println("OutputCellFlow");

                if (output instanceof OutputCellVehicles)
                    System.out.println("OutputCellVehicles");

                if (output instanceof OutputCellSumVehicles)
                    System.out.println("OutputCellSumVehicles");

                if (output instanceof OutputCellSumVehiclesDwn)
                    System.out.println("OutputCellSumVehiclesDwn");

                if (output instanceof OutputCellLanechangeOut)
                    System.out.println("OutputCellLanechangeOut");

                if (output instanceof OutputCellLanechangeIn)
                    System.out.println("OutputCellLanechangeIn");

                // subnetworks
                if (output instanceof OutputPathTravelTime)
                    System.out.println("OutputPathTravelTime");

                if (output instanceof OutputLinkVHT)
                    System.out.println("OutputLinkVHT");

                // vehicle events
                if (output instanceof OutputVehicle)
                    System.out.println("OutputVehicle");

                if (output instanceof OutputVehicleClass)
                    System.out.println("OutputVehicleClass");

                if (output instanceof OutputTravelTime)
                    System.out.println("OutputTravelTime");

                // controllers
                if (output instanceof OutputController)
                    System.out.println("OutputController");

            }

        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

}

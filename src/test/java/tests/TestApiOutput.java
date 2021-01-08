package tests;

import core.OTM;
import error.OTMException;
import org.junit.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestApiOutput extends AbstractTest {

    @Test
    public void test_outputs() {

        try {

            float start_time = 0f;
            float duration = 3600f;
            float outDt = 10f;

            OTM otm = OTM.load_test("output_test");

            Set<Long> link_ids = otm.scenario.network.links.keySet();
            Long commid = null;
            Long subnetid = null;

            // lanegroups ...............
            otm.output.request_lanegroups(null, null);

            // vehicles .................
            otm.output.request_vehicle_class(null, null);
            otm.output.request_travel_time(null, null);

            // controllers ..............
            otm.output.request_controller(null, null, 0l);

            // links
            otm.output.request_links_flow(null, null, commid, link_ids, outDt);
            otm.output.request_links_veh(null, null, commid, link_ids, outDt);
            otm.output.request_links_sum_veh(null, null, commid, link_ids, outDt);
//            otm.output.request_link_queues(null, null, commid, link_ids, outDt);

            // lanegroups ...............
            otm.output.request_lanegroup_flw(null, null, commid, link_ids, outDt);
            otm.output.request_lanegroup_veh(null, null, commid, link_ids, outDt);
            otm.output.request_lanegroup_sum_veh(null, null, commid, link_ids, outDt);

            // cells
            otm.output.request_cell_flw(null, null, commid, link_ids, outDt);
            otm.output.request_cell_veh(null, null, commid, link_ids, outDt);
            otm.output.request_cell_sum_veh(null, null, commid, link_ids, outDt);
            otm.output.request_cell_sum_veh_dwn(null, null, commid, link_ids, outDt);
            otm.output.request_cell_lanechange_out(null, null, commid, link_ids, outDt);
            otm.output.request_cell_lanechange_in(null, null, commid, link_ids, outDt);

            // vehicles .................
            otm.output.request_vehicle_events(null, null, commid);

            // subnetworks ..............
//            otm.output.request_path_travel_time(null, null, 0l, outDt);
//            otm.output.request_subnetwork_vht(null, null, commid, subnetid, outDt);

            // Run .................................
            otm.run(start_time,duration);

            Set<String> classes = Set.of(
                    "OutputLinkFlow",
                    "OutputLinkVehicles",
                    "OutputLinkSumVehicles",
//                    "OutputLinkQueues",
                    "OutputLaneGroups",
                    "OutputLaneGroupFlow",
                    "OutputLaneGroupVehicles",
                    "OutputLaneGroupSumVehicles",
                    "OutputCellFlow",
                    "OutputCellVehicles",
                    "OutputCellSumVehicles",
                    "OutputCellSumVehiclesDwn",
                    "OutputCellLanechangeOut",
                    "OutputCellLanechangeIn",
                    "OutputVehicleClass",
                    "OutputTravelTime",
                    "OutputVehicleEvents",
//                    "OutputPathTravelTime",
//                    "OutputSubnetworkVHT",
                    "OutputController"
            );

          Set<String> classes_req = otm.output.get_data().stream().map(x->x.getClass().getSimpleName()).collect(Collectors.toSet());

          assertEquals(classes_req,classes);

        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

}

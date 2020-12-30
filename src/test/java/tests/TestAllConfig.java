package tests;

import api.OTM;
import error.OTMException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import output.AbstractOutput;
import output.OutputLinkFlow;
import output.OutputLinkVehicles;
import utils.OTMUtils;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Set;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class TestAllConfig extends AbstractTest {

    String testname;
    final float start_time = 0f;
    final float duration = 2000f;
    final boolean makeplots = true;
    final Float outDt = 2f;

    public TestAllConfig(String testname){
        this.testname = testname;
    }

    @Test
    public void test_load() {
        try {
            System.out.println(testname + " load");
            OTM otm = new OTM();
            otm.load_test(testname);
        } catch (OTMException e) {
            System.err.print(e);
            fail();
        }
    }

    @Test
    public void test_run() {
        try {

            System.out.println(testname + " run");

            OTM otm = new OTM();
            otm.load_test(testname);

            // request outputs
            String prefix = testname;
            Set<Long> link_ids = otm.scenario().network.links.keySet();
            for(Long comm : otm.scenario().commodities.keySet()) {
                otm.output().request_links_flow(makeplots?null:prefix,makeplots?null:output_folder, comm, link_ids, outDt);
                otm.output().request_links_veh(makeplots?null:prefix, makeplots?null:output_folder, comm, link_ids, outDt);
            }

            // run the simulation
            otm.run(start_time,duration);

            // plot
            for(AbstractOutput output :  otm.output().get_data()) {
                if (output instanceof OutputLinkFlow)
                    ((OutputLinkFlow) output).plot_for_links(null, String.format("%s/%s_link_flow.png", output_folder,prefix));
                if (output instanceof OutputLinkVehicles)
                    ((OutputLinkVehicles) output).plot_for_links(null, String.format("%s/%s_link_veh.png", output_folder,prefix));
            }

            // check the output against expects
            for(String output_path : otm.output().get_file_names())
                compare_files(output_path);

        }

        catch (OTMException e) {
            System.err.print(e);
            fail();
        }

    }

    private void compare_files(String output_path){

        File outfile = new File(output_path);
        String outname = outfile.getName();

        System.out.println("-------- " + outname + " -----------");

        ClassLoader classLoader = getClass().getClassLoader();

        URL url = classLoader.getResource("test_output/" + outname);

        if(url==null)
            fail("File not found: " + outname);

        File known_outfile = new File(url.getFile());

        ArrayList<ArrayList<Double>> f1 = OTMUtils.read_matrix_csv_file(outfile);
        ArrayList<ArrayList<Double>> f2 = OTMUtils.read_matrix_csv_file(known_outfile);
        assertEquals(f1.size(),f2.size());
        for(int i=0;i<f1.size();i++){
            ArrayList<Double> x1 = f1.get(i);
            ArrayList<Double> x2 = f2.get(i);
            assertEquals(x1.size(),x2.size());
            for(int j=0;j<x1.size();j++) {
                boolean is_same = Math.abs(x1.get(j) - x2.get(j)) < 0.1;

                if(!is_same)
                    System.out.println(String.format("%d\t%d\t%f\t%f",i,j,x1.get(j),x2.get(j)));

                assertTrue(is_same);
            }
        }
    }

}

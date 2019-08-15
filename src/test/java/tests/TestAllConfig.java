package tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class TestAllConfig extends AbstractTest {

    String testname;
    float start_time = 0f;
    float duration = 100f;

    public TestAllConfig(String testname){
        this.testname = testname;
    }

    @Test
    public void test_load() {
//        try {
//            System.out.println(testname);
//
//            API api = OTM.load_test(testname,true);
//            assertNotNull(api);
//        } catch (OTMException e) {
//            System.err.print(e);
//            fail();
//        }
    }

    @Test
    public void test_run_ctm() {
        System.out.println(testname);
        run();
    }

    ///////////////////////////////////////////////
    // private
    ///////////////////////////////////////////////

    private void run() {
//        try {
//
//            API api = OTM.load_test(testname,true);
//            List<Long> link_ids = api.scenario.get_link_ids();
//            Float outDt = 2f;
//
//            // request outputs
//            for(CommodityInfo comm : api.scenario.get_commodities()) {
//                String prefix = "ctm" + "_" + testname;
//                api.output.request_links_flow(prefix,output_folder, comm.getId(), link_ids, outDt);
//                api.output.request_links_veh(prefix, output_folder, comm.getId(), link_ids, outDt);
//            }
//
//            // run the simulation
//            api.run(start_time,duration);
//
//            // check the output against expects
//            for(String output_path : api.output.get_file_names())
//                compare_files(output_path);
//
//        }
//
//        catch (OTMException e) {
//            System.err.print(e);
//            fail();
//        }

    }

    private void compare_files(String output_path){

        File outfile = new File(output_path);
        String outname = outfile.getName();

        ClassLoader classLoader = getClass().getClassLoader();
        File known_outfile = new File(classLoader.getResource("test_output/" + outname).getFile());

        ArrayList<ArrayList<Double>> f1 = read_matrix_csv_file(outfile);
        ArrayList<ArrayList<Double>> f2 = read_matrix_csv_file(known_outfile);
        assertEquals(f1.size(),f2.size());
        for(int i=0;i<f1.size();i++){
            ArrayList<Double> x1 = f1.get(i);
            ArrayList<Double> x2 = f2.get(i);
            assertEquals(x1.size(),x2.size());
            for(int j=0;j<x1.size();j++) {
                boolean is_same = Math.abs(x1.get(j) - x2.get(j)) < 0.1;

                if(!is_same)
                    System.out.println(x1.get(j) + "\t" + x2.get(j));

                assertTrue(is_same);
            }
        }
    }

    private static ArrayList<ArrayList<Double>> read_matrix_csv_file(File file) {
        if(file==null)
            return null;
        ArrayList<ArrayList<Double>> x = new ArrayList<>();
        try {
            Scanner inputStream= new Scanner(file);
            while(inputStream.hasNext()){
                String data= inputStream.next();
                String[] values = data.split(",");
                ArrayList<Double> z = new ArrayList<>();
                for(String str  : values)
                    z.add(Double.parseDouble(str));
                x.add(z);
            }
            inputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return x;
    }

}

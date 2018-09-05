package tests;

import api.API;
import api.info.CommodityInfo;
import error.OTMException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import runner.OTM;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class TestAllConfig extends AbstractTest {

    String testname;
    float start_time = 0f;
    float duration = 300f;
    float sim_dt = 2f;

    public TestAllConfig(String testname){
        this.testname = testname;
    }

    @Test
    public void test_load() {
        try {
            API api = OTM.load_test(testname,sim_dt,true,"ctm");
            assertNotNull(api);
        } catch (OTMException e) {
            System.out.print(e);
            fail();
        }
    }

    @Test
    public void test_run_ctm() {
        run("ctm");
    }

    @Test
    public void test_run_mn() {
        run("mn");
    }

    ///////////////////////////////////////////////
    // private
    ///////////////////////////////////////////////

    private void run(String model) {
        try {

            API api = OTM.load_test(testname,sim_dt,true,"ctm");
            List<Long> link_ids = api.get_link_ids();
            Float outDt = sim_dt;

            // prefix
            File f = new File(testname);
            String filename = f.getName();
            filename = filename.substring(0, filename.lastIndexOf('.'));
            String prefix = model + "_" + filename;

            // request outputs
            for(CommodityInfo comm : api.get_commodities()) {
                api.request_links_flow(prefix,output_folder, comm.getId(), link_ids, outDt);
                api.request_links_veh(prefix, output_folder, comm.getId(), link_ids, outDt);
            }

            // run the simulation
            api.run(start_time,duration);

            // TODO reinsert this
//            // check the output against expects
//            for(String output_path : api.get_outputs()){
//                File f1 = new File(output_path);
//                File f2 = new File(expected_output_folder,f1.getName());
//                compare_files(f1,f2);
//            }

        }

        catch (OTMException e) {
            System.out.print(e);
            fail();
        }

    }

    private void compare_files(File file1,File file2){
        ArrayList<ArrayList<Double>> f1 = read_matrix_csv_file(file1);
        ArrayList<ArrayList<Double>> f2 = read_matrix_csv_file(file2);
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

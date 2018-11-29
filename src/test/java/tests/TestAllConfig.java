/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
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
    float duration = 100f;
    float sim_dt = 2f;

    public TestAllConfig(String testname){
        this.testname = testname;
    }

    @Test
    public void test_load() {
        try {
            API api = OTM.load_test(testname,sim_dt,true);
            assertNotNull(api);
        } catch (OTMException e) {
            System.out.print(e);
            fail();
        }
    }

    @Test
    public void test_run_ctm() {
        run();
    }

    ///////////////////////////////////////////////
    // private
    ///////////////////////////////////////////////

    private void run() {
        try {

            API api = OTM.load_test(testname,sim_dt,true);
            List<Long> link_ids = api.get_link_ids();
            Float outDt = sim_dt;

            // request outputs
            for(CommodityInfo comm : api.get_commodities()) {
                String prefix = "ctm" + "_" + testname;
                api.request_links_flow(prefix,output_folder, comm.getId(), link_ids, outDt);
                api.request_links_veh(prefix, output_folder, comm.getId(), link_ids, outDt);
            }

            // run the simulation
            api.run(start_time,duration);

            // check the output against expects
            for(String output_path : api.get_outputs())
                compare_files(output_path);

        }

        catch (OTMException e) {
            System.out.print(e);
            fail();
        }

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

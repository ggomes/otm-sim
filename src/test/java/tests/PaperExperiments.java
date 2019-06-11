package tests;

import org.junit.Test;
import runner.OTM;

public class PaperExperiments extends AbstractTest {

    static String base_folder = "C:\\Users\\gomes\\Dropbox\\gabriel\\work\\pub\\current\\otm-sim\\exp\\config\\";
    static String[] args = new String[7];

    static {
        args[0] = "-run";
        args[5] = "0";
        args[6] = "4000";
    }

    static void run(String s,String x){
        System.out.println("----------- "  + s + "  " + x + " -------------");
        String myfolder = base_folder + s + "\\";
        args[1] = myfolder + x + "\\exp_" + x + ".xml";
        args[2] = x;
        args[3] = myfolder + x + "\\output_" + x + ".xml";
        args[4] = myfolder + x + "\\output";
        OTM.main(args);
    }

    /** DETERMINISTIC **/

    // fluid 1

    @Test
    public void run_d_fluid1_fluid1(){
        run("d","fluid1_fluid1");
    }

    @Test
    public void run_d_fluid1_fluid2(){
        run("d","fluid1_fluid2");
    }

    @Test
    public void run_d_fluid1_meso(){
        run("d","fluid1_meso");
    }

    @Test
    public void run_d_fluid1_micro(){
        run("d","fluid1_micro");
    }

    // fluid 2

    @Test
    public void run_d_fluid2_fluid1(){
        run("d","fluid2_fluid1");
    }

    @Test
    public void run_d_fluid2_fluid2(){
        run("d","fluid2_fluid2");
    }

    @Test
    public void run_d_fluid2_meso(){
        run("d","fluid2_meso");
    }

    @Test
    public void run_d_fluid2_micro(){
        run("d","fluid2_micro");
    }

    // meso

    @Test
    public void run_d_meso_fluid1(){
        run("d","meso_fluid1");
    }

    @Test
    public void run_d_meso_fluid2(){
        run("d","meso_fluid2");
    }

    @Test
    public void run_d_meso_meso(){
        run("d","meso_meso");
    }

    @Test
    public void run_d_meso_micro(){
        run("d","meso_micro");
    }

    // micro

    @Test
    public void run_d_micro_fluid1(){
        run("d","micro_fluid1");
    }

    @Test
    public void run_d_micro_fluid2(){
        run("d","micro_fluid2");
    }

    @Test
    public void run_d_micro_meso(){
        run("d","micro_meso");
    }

    @Test
    public void run_d_micro_micro(){
        run("d","micro_micro");
    }

    /** STOCHASTIC **/

    // fluid 1

    @Test
    public void run_p_fluid1_fluid1(){
        run("p","fluid1_fluid1");
    }

    @Test
    public void run_p_fluid1_fluid2(){
        run("p","fluid1_fluid2");
    }

    @Test
    public void run_p_fluid1_meso(){
        run("p","fluid1_meso");
    }

    @Test
    public void run_p_fluid1_micro(){
        run("p","fluid1_micro");
    }

    // fluid 2

    @Test
    public void run_p_fluid2_fluid1(){
        run("p","fluid2_fluid1");
    }

    @Test
    public void run_p_fluid2_fluid2(){
        run("p","fluid2_fluid2");
    }

    @Test
    public void run_p_fluid2_meso(){
        run("p","fluid2_meso");
    }

    @Test
    public void run_p_fluid2_micro(){
        run("p","fluid2_micro");
    }

    // meso

    @Test
    public void run_p_meso_fluid1(){
        run("p","meso_fluid1");
    }

    @Test
    public void run_p_meso_fluid2(){
        run("p","meso_fluid2");
    }

    @Test
    public void run_p_meso_meso(){
        run("p","meso_meso");
    }

    @Test
    public void run_p_meso_micro(){
        run("p","meso_micro");
    }

    // micro

    @Test
    public void run_p_micro_fluid1(){
        run("p","micro_fluid1");
    }

    @Test
    public void run_p_micro_fluid2(){
        run("p","micro_fluid2");
    }

    @Test
    public void run_p_micro_meso(){
        run("p","micro_meso");
    }

    @Test
    public void run_p_micro_micro(){
        run("p","micro_micro");
    }


}

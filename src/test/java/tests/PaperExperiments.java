package tests;

import org.junit.Test;
import runner.OTM;

public class PaperExperiments extends AbstractTest {

    static String base_folder = "C:\\Users\\gomes\\Dropbox\\gabriel\\work\\pub\\current\\otm\\exp\\";
    static String[] args = new String[7];

    static {
        args[0] = "-run";
        args[5] = "0";
        args[6] = "2000";
    }

    static void run(String x){
        System.out.println("----------- " + x + " -------------");
        args[1] = base_folder + x + "\\exp_" + x + ".xml";
        args[2] = x;
        args[3] = base_folder + x + "\\output_" + x + ".xml";
        args[4] = base_folder + x + "\\output";
        OTM.main(args);
    }

    @Test
    public void run_fluid_fluid(){
        run("fluid_fluid");
    }

    @Test
    public void run_fluid_meso(){
        run("fluid_meso");
    }

    @Test
    public void run_fluid_micro(){
        run("fluid_micro");
    }

    @Test
    public void run_meso_fluid(){
        run("meso_fluid");
    }

    @Test
    public void run_meso_meso(){
        run("meso_meso");
    }

    @Test
    public void run_meso_micro(){
        run("meso_micro");
    }

    @Test
    public void run_micro_fluid(){
        run("micro_fluid");
    }

    @Test
    public void run_micro_meso(){
        run("micro_meso");
    }

    @Test
    public void run_micro_micro(){
        run("micro_micro");
    }

}

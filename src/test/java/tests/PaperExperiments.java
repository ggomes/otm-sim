package tests;

import org.junit.Test;
import runner.OTM;

public class PaperExperiments extends AbstractTest {

    static String base_folder = "C:\\Users\\gomes\\Dropbox\\gabriel\\work\\pub\\current\\otm\\exp\\";
    static String[] args = new String[7];

    static {
        args[0] = "-run";
        args[5] = "0";
        args[6] = "1000";
    }

    static void run(String x){
        args[1] = base_folder + x + "\\exp_" + x + ".xml";
        args[2] = x;
        args[3] = base_folder + x + "\\output_" + x + ".xml";
        args[4] = base_folder + x + "\\output";
        OTM.main(args);
    }

    @Test
    public void run_fluid(){
        run("fluid");
    }

    @Test
    public void run_meso(){
        run("meso");
    }

    @Test
    public void run_fluid_meso(){
        run("fluid_meso");
    }

    @Test
    public void run_micro(){
        run("micro");
    }

}

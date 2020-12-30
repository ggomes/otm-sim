package tests;

import cmd.OTM;
import org.junit.Test;

import java.io.File;

public class TestCmd {

    @Test
    public void run_main(){

        //    0 command
        //    1 configfile
        //    2 prefix
        //    3 output_request
        //    4 output folder
        //    5 start_time
        //    6 duration

        String resource_folder = (new File("src/test/resources/")).getAbsolutePath()+ File.separator ;

        String[] args = new String[7];
        args[0] = "-run";
//        args[1] = resource_folder + "test_configs/line_ctm.xml";
//        args[2] = "mytest";
//        args[3] = resource_folder+"sample_output_request.xml";
//        args[4] = "temp";
//        args[5] = "0";
//        args[6] = "100";

        args[1] = "/home/gomes/Desktop/test/intersection.xml";
        args[2] = "cmd";
        args[3] = "/home/gomes/Desktop/test/request.xml";
        args[4] = "/home/gomes/Desktop/test/output";
        args[5] = "0";
        args[6] = "300";

        OTM.main(args);

    }

}

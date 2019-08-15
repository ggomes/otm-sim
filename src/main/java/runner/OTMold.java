package runner;

import api.OTM;
import api.OTMdev;
import dispatch.Dispatcher;
import dispatch.EventStopSimulation;
import error.OTMException;
import jaxb.OutputRequests;

import models.AbstractModel;
import output.AbstractOutput;
import output.EventsActuator;
import output.EventsController;
import output.EventsVehicle;
import output.LaneGroupFlow;
import output.LaneGroupVehicles;
import output.LaneGroups;
import output.LinkFlow;
import output.LinkVHT;
import output.LinkVehicles;
import output.PathTravelTimeWriter;
import output.VehicleClass;
import output.VehicleTravelTime;
import utils.OTMUtils;
import xml.JaxbLoader;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class OTMold {

    private static Dispatcher dispatcher;

    // 0:  -help      Display usage message.
    //     -version   Display version information.
    //     -load      Load and validate a config file. arguments: <configfile>
    //     -run       Run a config file with default paramters. arguments: <configfile> <prefix> <output request file> <output folder> <start_time> <duration>
    //         1: configfile: absolute location and name of the configuration file.
    //         2: prefix: string to be pre-pended to all output files.
    //         3: output request file: absolute location and name of the output request file.
    //         4: output folder: folder where the output files should go.
    //         5: start_time: [integer] start time for the simrultion in seconds after midnight.
    //         6: duration: [integer] simulation duration in seconds.
    public static void main(String[] args) {

        if (0 == args.length) {
            System.err.print(get_usage());
            return;
        }

        String cmd = args[0];
        String[] arguments = new String[args.length - 1];
        System.arraycopy(args, 1, arguments, 0, args.length - 1);

        // load and validate
        if (cmd.equals("-load")){
            try {
                OTM otm = new OTM(arguments[0],true,false); // config
            } catch (OTMException e) {
                e.printStackTrace();
                return;
            }

            System.out.println("Load successful!");
        } else

        // run
        //    0 configfile
        //    1 prefix
        //    2 output_request
        //    3 output folder
        //    4 start_time
        //    5 duration
        if (cmd.equals("-run")){
            try {

                if(arguments.length<6) {
                    System.err.println("Not enough input arguments.");
                    return;
                }

                String configfile = arguments[0];
                String prefix = arguments[1];
                String output_requests_file = arguments[2];
                String output_folder = arguments[3];
                int start_time = Integer.parseInt(arguments[4]);
                int duration = Integer.parseInt(arguments[5]);

                OTM otm = new OTM(configfile,true,false);

                otm.run(start_time,duration,output_requests_file,prefix,output_folder);

            } catch (OTMException e) {
                e.printStackTrace();
            }
        }

         // version
        else if (cmd.equals("-version")){
            System.out.println("otm-base: " + OTMUtils.getBaseGitHash());
            System.out.println("otm-sim: " + getGitHash());
        }

        // help
        else if (cmd.equals("-help"))
            System.out.print(get_usage());

        else
            System.err.print(get_usage());
    }

//    public static OTMdev loaddev(String configfile) throws OTMException {
//        return new OTMdev(load(configfile));
//    }
//
//    public static OTM load(String configfile) throws OTMException {
//        return new OTM(configfile,true,false);
//    }

//    public static API load_test(String testname,boolean validate) throws OTMException {
//        if(!JaxbLoader.get_test_config_names().contains(testname))
//            return null;
//        API api = new API();
//        api.load_test(JaxbLoader.get_test_filename(testname),validate);
//        return api;
//    }

//    public static OTM load_xml(String configfile) throws OTMException {
//        return new OTM(configfile,true,true);
//    }
//
//    public static OTM load(String configfile, boolean validate) throws OTMException {
//        return new OTM(configfile,validate,false);
//    }
//
//    public static void run(Scenario scenario,String runfile) throws OTMException {
//        run(scenario,new RunParameters(runfile));
//    }
//
//    public static void run(Scenario scenario,String prefix,String output_requests_file,String output_folder,float start_time,float duration) throws OTMException {
//        OTMold.run(scenario,new RunParameters(prefix,output_requests_file,output_folder,start_time,duration));
//    }
//


    ///////////////////////////////////////////////////
    // static
    ///////////////////////////////////////////////////

    private static String get_usage(){
        String str =
                "Usage: [-help|-version|-load file]\n" +
                        "\t-help\t\tDisplay usage message.\n" +
                        "\t-version\tDisplay version information.\n" +
                        "\t-load\t\tLoad and validate a config file. arguments: <configfile>\n" +
                        "\t-run\t\tRun a config file with default paramters. arguments: <configfile> <prefix> <output request file> <output folder> <start_time> <duration>\n" +
                        "\t\tconfigfile: absolute location and name of the configuration file.\n" +
                        "\t\tprefix: string to be pre-pended to all output files.\n" +
                        "\t\toutput request file: absolute location and name of the output request file.\n" +
                        "\t\toutput folder: folder where the output files should go.\n" +
                        "\t\tstart_time: [integer] start time for the simrultion in seconds after midnight.\n" +
                        "\t\tduration: [integer] simulation duration in seconds.\n";
        return str;
    }

    public static String getGitHash(){
        InputStream inputStream = OTMold.class.getResourceAsStream("/otm-sim.properties");
        Properties properties = new Properties();
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read properties file", e);
        }
        finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
        return properties.getProperty("sim.git");
    }


}

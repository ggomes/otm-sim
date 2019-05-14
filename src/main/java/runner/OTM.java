/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package runner;

import api.API;
import api.APIopen;
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
import output.PathTravelTime;
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
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class OTM {

    private static Dispatcher dispatcher;


/**
 0:  -help      Display usage message.
     -version   Display version information.
     -load      Load and validate a config file. arguments: <configfile>
     -run       Run a config file with default paramters. arguments: <configfile> <prefix> <output request file> <output folder> <start_time> <duration>
         1: configfile: absolute location and name of the configuration file.
         2: prefix: string to be pre-pended to all output files.
         3: output request file: absolute location and name of the output request file.
         4: output folder: folder where the output files should go.
         5: start_time: [integer] start time for the simrultion in seconds after midnight.
         6: duration: [integer] simulation duration in seconds.
  */

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
                OTM.load(arguments[0]); // config
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

                API api = OTM.load(configfile,true);

                api.run(prefix,output_requests_file,output_folder,start_time,duration);

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


    public static APIopen loadOpen(String configfile) throws OTMException {
        API api = new API();
        api.load(configfile);
        return new APIopen(api);
    }

    public static API load(String configfile) throws OTMException {
        API api = new API();
        api.load(configfile);
        return api;
    }

    public static API load_test(String testname,boolean validate) throws OTMException {
        if(!JaxbLoader.get_test_config_names().contains(testname))
            return null;
        API api = new API();
        api.load_test(JaxbLoader.get_test_filename(testname),validate);
        return api;
    }

    public static API load_for_static_traffic_assignment(String configfile) throws OTMException {
        API api = new API();
        api.load_for_static_traffic_assignment(configfile);
        return api;
    }

    public static API load(String configfile,boolean validate) throws OTMException {
        System.out.println("Load");
        API api = new API();
        List<Long> times = api.load(configfile,validate);
        System.out.println("\tTime to load XML: " + String.format("%.1f",(times.get(1)-times.get(0))/1000d) + " seconds.");
        System.out.println("\tTime to configure scenario: " + String.format("%.1f",(times.get(2)-times.get(1))/1000d) + " seconds.");
        return api;
    }

    public static void run(Scenario scenario,String runfile) throws OTMException {
        run(scenario,new RunParameters(runfile));
    }

    public static void run(Scenario scenario,String prefix,String output_requests_file,String output_folder,float start_time,float duration) throws OTMException {
        OTM.run(scenario,new RunParameters(prefix,output_requests_file,output_folder,start_time,duration));
    }

    public static void run(Scenario scenario,RunParameters runParams) throws OTMException {
        initialize(scenario,runParams);
        advance(scenario,runParams.duration);
        scenario.is_initialized = false;
    }

    public static void initialize(Scenario scenario,float start_time) throws OTMException {
        RunParameters runParams = new RunParameters(null,null,null,start_time,Float.NaN);
        initialize(scenario,runParams);
    }

    public static void initialize(Scenario scenario,RunParameters runParams) throws OTMException {

        // build and attach dispatcher
        dispatcher = new Dispatcher(runParams.start_time);

        // append outputs from output request file ..................
        if(runParams.output_requests_file!=null && !runParams.output_requests_file.isEmpty()) {
            jaxb.OutputRequests jaxb_or = load_output_request(runParams.output_requests_file, true);
            scenario.outputs.addAll(create_outputs_from_jaxb(scenario, runParams.prefix, runParams.output_folder, jaxb_or));
        }

        // initialize
        scenario.initialize(dispatcher,runParams);
    }

    public static void advance(Scenario scenario,float duration) throws OTMException {

        dispatcher.set_continue_simulation(true);

        float now = dispatcher.current_time;

        // register stop the simulation
        dispatcher.set_stop_time(now+duration);
        dispatcher.register_event(new EventStopSimulation(scenario,dispatcher,now+duration));

        // register initial events for each model
        scenario.network.models.values().forEach(m->m.register_with_dispatcher(scenario, dispatcher,now));

        // process all events
        dispatcher.dispatch_events_to_stop();

    }

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
        InputStream inputStream = OTM.class.getResourceAsStream("/otm-sim.properties");
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

    private static jaxb.OutputRequests load_output_request(String filename, boolean validate) throws OTMException {
        try {

            JAXBContext jaxbContext = JAXBContext.newInstance(OutputRequests.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            if(validate) {
                SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                InputStream resourceAsStream = JaxbLoader.class.getResourceAsStream("/outputs.xsd");
                Schema schema = sf.newSchema(new StreamSource(resourceAsStream));
                unmarshaller.setSchema(schema);
            }

            OutputRequests jaxb_outputrequests = (OutputRequests) unmarshaller.unmarshal(new File(filename));
            return jaxb_outputrequests;
        } catch(org.xml.sax.SAXException e){
            throw new OTMException(e);
        }  catch (JAXBException e) {
            throw new OTMException(e);
        }
    }

    private static Set<AbstractOutput> create_outputs_from_jaxb(Scenario scenario, String prefix, String output_folder, jaxb.OutputRequests jaxb_ors) throws OTMException{
        Set<AbstractOutput> outputs = new HashSet<>();
        if(jaxb_ors==null)
            return outputs;
        AbstractOutput output;
        for(jaxb.OutputRequest jaxb_or : jaxb_ors.getOutputRequest()){

            Long commodity_id = jaxb_or.getCommodity();
            Float outDt = jaxb_or.getDt();

            if(jaxb_or.getModel()!=null){
                if(!scenario.network.models.containsKey(jaxb_or.getModel()))
                    throw new OTMException("Bad model name in output : " + jaxb_or.getModel());
                AbstractModel model = scenario.network.models.get(jaxb_or.getModel());
                output = model.create_output_object(scenario,prefix,output_folder,jaxb_or);
            }

            else {

                switch (jaxb_or.getQuantity()) {
                    case "lanegroups":
                        output = new LaneGroups(scenario, prefix, output_folder);
                        break;
                    case "link_flw":
                        output = new LinkFlow(scenario, prefix, output_folder, commodity_id, null, outDt);
                        break;
                    case "link_veh":
                        output = new LinkVehicles(scenario, prefix, output_folder, commodity_id, null, outDt);
                        break;
                    case "lanegroup_flw":
                        output = new LaneGroupFlow(scenario, prefix, output_folder, commodity_id, null, outDt);
                        break;
                    case "lanegroup_veh":
                        output = new LaneGroupVehicles(scenario, prefix, output_folder, commodity_id, null, outDt);
                        break;
                    case "link_vht":
                        output = new LinkVHT(scenario, prefix, output_folder, commodity_id, null, outDt);
                        break;
                    case "vehicle_events":
                        output = new EventsVehicle(scenario, prefix, output_folder, commodity_id);
                        break;
                    case "vehicle_class":
                        output = new VehicleClass(scenario, prefix, output_folder);
                        break;
                    case "vehicle_travel_time":
                        output = new VehicleTravelTime(scenario, prefix, output_folder);
                        break;
                    case "controller":
                        output = new EventsController(scenario, prefix, output_folder, jaxb_or.getController());
                        break;
                    case "actuator":
                        output = new EventsActuator(scenario, prefix, output_folder, jaxb_or.getActuator());
                        break;
                    case "path_travel_time":
                        output = new PathTravelTime(scenario, prefix, output_folder, null, outDt);
                        break;
                    default:
                        throw new OTMException("Bad output identifier : " + jaxb_or.getQuantity());
                }

            }

            if(output!=null)
                outputs.add(output);
        }
        return outputs;
    }

}

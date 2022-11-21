package core;

import dispatch.Dispatcher;
import dispatch.EventStopSimulation;
import error.OTMException;
import jaxb.OutputRequests;
import models.vehicle.spatialq.OutputLinkQueues;
import output.*;
import cmd.RunParameters;
import utils.OTMUtils;
import xml.JaxbLoader;
import xml.JaxbWriter;

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
import java.util.*;

/**
 * Public API. The methods in the API are of three types. Basic scenario loading and running
 * methods belong to the OTM class. Methods for querying and modifying a scenario belong to
 * api.Scenario. Methods for requesting outputs and calculating different metrics are in core.Output.
 */
public class OTM {

    /**
     * Container for the scenario
     */
    public core.Scenario scenario;

    /**
     * Container for the simulation output
     */
    public core.Output output;

    ////////////////////////////////////////////////////////
    // construction
    ////////////////////////////////////////////////////////

    public OTM(core.Scenario scenario, core.Output output){
        this.scenario = scenario;
        this.output = output;
        this.output.set_api(this);
    }

    /**
     * Constructor.
     * @throws OTMException Undocumented
     * @param configfile Configuration file.
     * @param validate_pre_init Validate
     */
    public OTM(String configfile, boolean validate_pre_init) throws OTMException {
        this( ScenarioFactory.create_scenario(JaxbLoader.load_scenario(configfile),validate_pre_init) , new Output() );
    }

    public OTM(jaxb.Scenario jscenario, boolean validate) throws OTMException {
        this(ScenarioFactory.create_scenario(jscenario,validate), new Output());
    }

    /** Load a scenario from the standard tests.
     * @throws OTMException Undocumented
     * @param testname Name of the test.
     * @return An OTM instance
     * **/
    public static OTM load_test(String testname) throws OTMException  {
        return new OTM(JaxbLoader.load_test_scenario(testname),true);
    }

    /** Get the output object
     * @return Output object
     * **/
    public Output output(){
        return output;
    }

    /** Get the scenario object
     * @return scenario object
     * **/
    public Scenario scenario(){
        return scenario;
    }

    ////////////////////////////////////////////////////////
    // save
    ////////////////////////////////////////////////////////


    /** Save the scenario to a file.
     * @param file Output file name
     * **/
    public void save(String file)  {
        try {
            JaxbWriter.save_scenario(scenario.to_jaxb(),file);
        } catch (OTMException e) {
            System.err.println("ERROR");
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    ////////////////////////////////////////////////////////
    // run
    ////////////////////////////////////////////////////////

    /**
     * Run the simulation.
     * @param prefix Prefix for the output files (used if output_folder is not null).
     * @param output_requests_file XML file with output requests.
     * @param output_folder Folder for the output. If null, then keep output in memory (needed to run plotting functions).
     * @param start_time Initial time in seconds.
     * @param duration Duration of the simulation in seconds.
     * @param validate_post_init If true, runs validations that can only happen once the model is specified.
     * @throws OTMException Undocumented
     */
    public void run(String prefix,String output_requests_file,String output_folder,float start_time,float duration,boolean validate_post_init) throws OTMException {
        if(!scenario.network.links.values().stream().allMatch(link->link.model!=null))
            throw new OTMException("Not all links have assigned models.");
        initialize(start_time,output_requests_file,prefix,output_folder,validate_post_init);
        advance(duration);
        terminate();
    }

    /**
     * Run the simulation.
     * @param start_time Initial time in seconds.
     * @param duration Duration of the simulation in seconds.
     * @throws OTMException Undocumented
     */
    public void run(float start_time,float duration) throws OTMException {
        run(null,null,null,start_time,duration,true);
    }

    ////////////////////////////////////////////////////////
    // initialize
    ////////////////////////////////////////////////////////

    /**
     *  Validate and initialize all components of the scenario.
     * @throws OTMException Undocumented
     * @param start_time Initial time in seconds.
     * @param output_requests_file Absolute location and name of file with output requests.
     * @param prefix Prefix for the output.
     * @param output_folder Folder for the output.
     * @param validate_post_init Whether or not to run post-initialize validation
     */
    public void initialize(float start_time,String output_requests_file,String prefix,String output_folder,boolean validate_post_init) throws OTMException {

        // build and attach dispatcher
        Dispatcher dispatcher = new Dispatcher();

        // append outputs from output request file ..................
        if(output_requests_file!=null && !output_requests_file.isEmpty()) {
            jaxb.OutputRequests jaxb_or = load_output_request(output_requests_file, true);
            scenario.outputs.addAll(create_outputs_from_jaxb(scenario,prefix,output_folder, jaxb_or));
        }

        // initialize
        RunParameters runParams = new RunParameters(prefix,output_requests_file,output_folder,start_time);
        scenario.initialize(dispatcher,runParams,validate_post_init);
    }

    /**
     *  Validate and initialize all components of the scenario. This function must be called prior to calling "advance".
     * @throws OTMException Undocumented
     * @param start_time Initial time in seconds.
     */
    public void initialize(float start_time) throws OTMException {
        initialize(start_time,null,null,null,true);
    }

    ////////////////////////////////////////////////////////
    // advance, terminate
    ////////////////////////////////////////////////////////

    /**
     *  Advance the simulation in time.
     * @param duration Seconds to advance.
     * @throws OTMException Undocumented
     */
    public void advance(float duration) throws OTMException {

        Dispatcher dispatcher = scenario.dispatcher;

        dispatcher.set_continue_simulation(true);

        // register stop the simulation
        float now = dispatcher.current_time;
        dispatcher.set_stop_time(now+duration);
        dispatcher.register_event(new EventStopSimulation(scenario,dispatcher,now+duration));

        // register scenario events
        scenario.events.values().stream()
                .filter(e->e.timestamp>=now && e.timestamp<now+duration)
                .forEach(e->dispatcher.register_event(e));

        // process all events
        dispatcher.dispatch_events_to_stop();
    }

    /**
     *  Complete the simulation by closing output files. This should be called explicitly if the simulation is
     *  executed with calls to "advance". The "run" method already calls it internally.
     */
    public void terminate() {
        scenario.terminate();
    }

    ////////////////////////////////////////////////////////
    // plot
    ////////////////////////////////////////////////////////

    /**
     * Create standard plots for all outputs and write them (as png files) to the output folder.
     * @param out_folder Output folder
     * @throws OTMException Undocumented.
     */
    public void plot_outputs(String out_folder) throws OTMException {
        plot_outputs(this.output.get_data(),out_folder);
    }

    /**
     * Create standard plots for given outputs and write them (as png files) to the output folder.
     * @param outputs Set of AbstractOutput
     * @param out_folder Output folder
     * @throws OTMException Undocumented.
     */
    public static void plot_outputs(Set<AbstractOutput> outputs,String out_folder) throws OTMException {
        for(AbstractOutput output :  outputs){

            String commid;

            if(output instanceof AbstractOutputTimed){
                AbstractOutputTimed x = (AbstractOutputTimed) output;
                commid = x.commodity==null ? "all" : String.format("%d",x.commodity.getId());
            } else
                commid = "";

            // links
            if (output instanceof OutputLinkFlow)
                ((OutputLinkFlow) output).plot_for_links(null, String.format("%s/%s_link_flow.png", out_folder, commid));

            if (output instanceof OutputLinkVehicles)
                ((OutputLinkVehicles) output).plot_for_links(null, String.format("%s/%s_link_veh.png", out_folder, commid));

            if (output instanceof OutputLinkSumVehicles)
                ((OutputLinkSumVehicles) output).plot_for_links(null, String.format("%s/%s_link_sumveh.png", out_folder, commid));

            if (output instanceof OutputLinkQueues)
                ((OutputLinkQueues) output).plot(String.format("%s/%s_link_sumqueues.png", out_folder,commid));

            // lane groups
            if (output instanceof OutputLaneGroupFlow)
                ((OutputLaneGroupFlow) output).plot_for_links(null,  String.format("%s/%s_lg_flow.png", out_folder, commid));

            if (output instanceof OutputLaneGroupVehicles)
                ((OutputLaneGroupVehicles) output).plot_for_links(null,String.format("%s/%s_lg_veh.png", out_folder, commid));

            if (output instanceof OutputLaneGroupSumVehicles)
                ((OutputLaneGroupSumVehicles) output).plot_for_links(null,String.format("%s/%s_lg_sumveh.png", out_folder, commid));

            // cells
            if (output instanceof OutputCellFlow)
                ((OutputCellFlow) output).plot_for_links(null, String.format("%s/%s_cell_flow.png", out_folder, commid));

            if (output instanceof OutputCellVehicles)
                ((OutputCellVehicles) output).plot_for_links(null, String.format("%s/%s_cell_veh.png", out_folder, commid));

            if (output instanceof OutputCellSumVehicles)
                ((OutputCellSumVehicles) output).plot_for_links(null, String.format("%s/%s_cell_sumveh.png", out_folder, commid));

            if (output instanceof OutputCellSumVehiclesDwn)
                ((OutputCellSumVehiclesDwn) output).plot_for_links(null, String.format("%s/%s_cell_sumvehdwn.png", out_folder, commid));

            if (output instanceof OutputCellLanechangeOut)
                ((OutputCellLanechangeOut) output).plot_for_links(null, String.format("%s/%s_cell_lc_out.png", out_folder, commid));

            if (output instanceof OutputCellLanechangeIn)
                ((OutputCellLanechangeIn) output).plot_for_links(null, String.format("%s/%s_cell_lc_in.png", out_folder, commid));

            // subnetworks
            if (output instanceof OutputPathTravelTime)
                ((OutputPathTravelTime) output).plot(String.format("%s/%s_path_tt.png", out_folder, commid));

            if (output instanceof OutputSubnetworkVHT)
                ((OutputSubnetworkVHT) output).plot_for_links(null, String.format("%s/%s_vht.png", out_folder, commid));

            // vehicle events
            if (output instanceof OutputVehicleEvents)
                ((OutputVehicleEvents) output).plot(String.format("%s/veh_events.png", out_folder));

//                if (output instanceof OutputVehicleClass)
//                    ((OutputVehicleClass) output).plot(String.format("%s/veh_class.png", png_folder));

            if (output instanceof OutputTravelTime)
                ((OutputTravelTime) output).plot(String.format("%s/veh_traveltime.png", out_folder));

            // controllers

            if (output instanceof OutputController)
                ((OutputController) output).plot(String.format("%s/controller.png", out_folder));

        }
    }

    ////////////////////////////////////////////////////////
    // getters
    ////////////////////////////////////////////////////////

    /**
     * Current simulation time in seconds.
     * @return Current simulation time in seconds.
     */
    public float get_current_time(){
        return scenario.dispatcher.current_time;
    }

    /**
     * Git hash for the current build.
     * @return Git hash for the current build.
     */
    public static String get_version(){
        InputStream inputStream = cmd.OTM.class.getResourceAsStream("/otm-sim.properties");
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

    /**
     * Set the seed for the simulator's random number generator.
     * @param seed Any number
     */
    public static void set_random_seed(long seed){
        OTMUtils.set_random_seed(seed);
    }

    ////////////////////////////////////////////////////////
    // private
    ////////////////////////////////////////////////////////

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

    private static Set<AbstractOutput> create_outputs_from_jaxb(core.Scenario scenario, String prefix, String output_folder, jaxb.OutputRequests jaxb_ors) throws OTMException{
        Set<AbstractOutput> outputs = new HashSet<>();
        if(jaxb_ors==null)
            return outputs;
        AbstractOutput output;
        for(jaxb.OutputRequest jaxb_or : jaxb_ors.getOutputRequest()){

            Long commodity_id = jaxb_or.getCommodity();
            Float outDt = jaxb_or.getDt();

            if(jaxb_or.getModel()!=null){
                if(!scenario.models.containsKey(jaxb_or.getModel()))
                    throw new OTMException("Bad model name in output : " + jaxb_or.getModel());
                AbstractModel model = scenario.models.get(jaxb_or.getModel());
                output = model.create_output(scenario,prefix,output_folder,jaxb_or);
            }

            else {

                switch (jaxb_or.getQuantity()) {
                    case "lanegroups":
                        output = new OutputLaneGroups(scenario, prefix, output_folder);
                        break;
                    case "link_flw":
                        output = new OutputLinkFlow(scenario, prefix, output_folder, commodity_id, null, outDt);
                        break;
                    case "link_veh":
                        output = new OutputLinkVehicles(scenario, prefix, output_folder, commodity_id, null, outDt);
                        break;
                    case "lanegroup_flw":
                        output = new OutputLaneGroupFlow(scenario, prefix, output_folder, commodity_id, null, outDt);
                        break;
                    case "lanegroup_veh":
                        output = new OutputLaneGroupVehicles(scenario, prefix, output_folder, commodity_id, null, outDt);
                        break;
                    case "subnetwork_vht":
                        output = new OutputSubnetworkVHT(scenario, prefix, output_folder, commodity_id, null, outDt);
                        break;
                    case "vehicle_events":
                        output = new OutputVehicleEvents(scenario, prefix, output_folder, commodity_id);
                        break;
                    case "vehicle_class":
                        output = new OutputVehicleClass(scenario, prefix, output_folder);
                        break;
                    case "vehicle_travel_time":
                        output = new OutputTravelTime(scenario, prefix, output_folder);
                        break;
                    case "controller":
                        output = new OutputController(scenario, prefix, output_folder, jaxb_or.getController());
                        break;
//                    case "actuator":
//                        output = new OutputActuator(scenario, prefix, output_folder, jaxb_or.getActuator());
//                        break;
                    case "path_travel_time":
                        output = new OutputPathTravelTime(scenario, prefix, output_folder, null, outDt);
                        scenario.path_tt_manager.add_path_travel_time_writer((OutputPathTravelTime) output);
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
package api;

import dispatch.Dispatcher;
import dispatch.EventStopSimulation;
import error.OTMException;
import jaxb.OutputRequests;
import models.AbstractModel;
import output.*;
import runner.OTMold;
import runner.RunParameters;
import runner.ScenarioFactory;
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
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class OTM {

    protected Dispatcher dispatcher;
    protected runner.Scenario scn;
    public Scenario scenario;
    public Output output;
    public Performance performance;

    public OTM(String configfile, boolean validate, boolean jaxb_only) throws OTMException {
        jaxb.Scenario jaxb_scenario = JaxbLoader.load_scenario(configfile,validate);
        if(jaxb_only)
            this.scn =  ScenarioFactory.create_scenario_for_static_traffic_assignment(jaxb_scenario);
        else
           this.scn =  ScenarioFactory.create_scenario(jaxb_scenario,validate);
        scenario = new Scenario(scn);
        output = new Output(scn);
        performance = new Performance(scn);
    }

    ////////////////////////////////////////////////////////
    // static
    ////////////////////////////////////////////////////////

    /**
     * Undocumented
     * @return git hash for the current build.
     */
    public static String get_version(){
        return OTMold.getGitHash();
    }

    /**
     * Undocumented
     * @param seed Undocumented
     */
    public static void set_random_seed(long seed){
        OTMUtils.set_random_seed(seed);
    }

    ////////////////////////////////////////////////////////
    // run
    ////////////////////////////////////////////////////////

    /**
     * Undocumented
     * @param start_time Undocumented
     * @param duration Undocumented
     * @throws OTMException Undocumented
     */
    public void run(float start_time,float duration) throws OTMException {
        initialize(start_time);
        advance(duration);
        scn.is_initialized = false;
    }

    /**
     *
     * @param start_time Undocumented
     * @param duration Undocumented
     * @param output_requests_file Undocumented
     * @param prefix Undocumented
     * @param output_folder Undocumented
     * @throws OTMException Undocumented
     */
    public void run(float start_time,float duration,String output_requests_file,String prefix,String output_folder) throws OTMException {
        initialize(start_time,output_requests_file,prefix,output_folder);
        advance(duration);
        scn.is_initialized = false;
    }

    /**
     *  Undocumented
     * @param start_time Undocumented
     * @throws OTMException Undocumented
     */
    public void initialize(float start_time) throws OTMException {
        initialize(start_time,null,null,null);
    }

    /**
     *  Undocumented
     * @param start_time Undocumented
     * @param output_requests_file Undocumented
     * @param prefix Undocumented
     * @param output_folder Undocumented
     * @throws OTMException Undocumented
     */
    public void initialize(float start_time,String output_requests_file,String prefix,String output_folder) throws OTMException {

        // build and attach dispatcher
        dispatcher = new Dispatcher(start_time);

        // append outputs from output request file ..................
        if(output_requests_file!=null && !output_requests_file.isEmpty()) {
            jaxb.OutputRequests jaxb_or = load_output_request(output_requests_file, true);
            scn.outputs.addAll(create_outputs_from_jaxb(scn,prefix,output_folder, jaxb_or));
        }

        // initialize
        RunParameters runParams = new RunParameters(prefix,output_requests_file,output_folder,start_time,0f);
        scn.initialize(dispatcher,runParams);
    }

    /**
     *  Undocumented
     * @param duration Undocumented
     * @throws OTMException Undocumented
     */
    public void advance(float duration) throws OTMException {

        dispatcher.set_continue_simulation(true);

        float now = dispatcher.current_time;

        // register stop the simulation
        dispatcher.set_stop_time(now+duration);
        dispatcher.register_event(new EventStopSimulation(scn,dispatcher,now+duration));

        // register initial events for each model
        scn.network.models.values().forEach(m->m.register_with_dispatcher(scn, dispatcher,now));

        // process all events
        dispatcher.dispatch_events_to_stop();

    }

    /**
     * Undocumented
     * @return Undocumented
     */
    public float get_current_time(){
        return scn.get_current_time();
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

    private static Set<AbstractOutput> create_outputs_from_jaxb(runner.Scenario scenario, String prefix, String output_folder, jaxb.OutputRequests jaxb_ors) throws OTMException{
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
                        output = new PathTravelTimeWriter(scenario, prefix, output_folder, null, outDt);
                        scenario.path_tt_manager.add_path_travel_time_writer((PathTravelTimeWriter) output);
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
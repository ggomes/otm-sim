package control;

import actuator.AbstractActuator;
import dispatch.Dispatcher;
import dispatch.EventPoke;
import dispatch.InterfacePokable;
import error.OTMErrorLog;
import error.OTMException;
import output.OutputEventsController;
import runner.InterfaceScenarioElement;
import runner.Scenario;
import runner.ScenarioElementType;
import sensor.AbstractSensor;
import utils.OTMUtils;

import java.util.*;

public abstract class AbstractController implements InterfacePokable, InterfaceScenarioElement {

    public enum Algorithm {
        sig_pretimed,
        plugin
    }

    public static final Map<Algorithm, AbstractActuator.Type> map_algorithm_actuator = new HashMap<>();
    static {
        map_algorithm_actuator.put( Algorithm.sig_pretimed  , AbstractActuator.Type.signal );
    }

    public long id;
    public Algorithm type;
    public float dt;

    public Set<AbstractActuator> actuators;
    public Map<Long,String> actuator_usage;

    public Set<AbstractSensor> sensors;
    public Map<Long,String> sensor_usage;

    public OutputEventsController event_listener;

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public AbstractController(Scenario scenario, jaxb.Controller jaxb_controller) throws OTMException {
        this.id = jaxb_controller.getId();

        String controller_type = jaxb_controller.getType();
        this.type = is_inbuilt(controller_type) ? Algorithm.valueOf(controller_type) : Algorithm.plugin;
        this.dt = jaxb_controller.getDt();

        // below this does not apply for scenario-less controllers  ..............................
        if(scenario==null)
            return;

        // read actuators ..............................................................
        actuators = new HashSet<>();
        actuator_usage = new HashMap<>();
        if(jaxb_controller.getTargetActuators()!=null){

            // read usage-less
            if(jaxb_controller.getTargetActuators().getIds()!=null){
                List<Long> ids = OTMUtils.csv2longlist(jaxb_controller.getTargetActuators().getIds());
                Iterator it = ids.iterator();
                while(it.hasNext()){
                    AbstractActuator act = (AbstractActuator) scenario.get_element(ScenarioElementType.actuator,(Long) it.next());
                    if(act==null)
                        throw new OTMException("Bad actuator id in controller");
                    if(act.myController!=null)
                        throw new OTMException("Multiple controllers assigned to single actuator");
                    actuators.add(act);
                    actuator_usage.put(act.id,"");
                    act.myController=this;
                }
            }

            // read actuators with usage
            for(jaxb.TargetActuator jaxb_act : jaxb_controller.getTargetActuators().getTargetActuator()){
                AbstractActuator act = (AbstractActuator) scenario.get_element(ScenarioElementType.actuator,jaxb_act.getId());
                if(act==null)
                    throw new OTMException("Bad actuator id in controller");
                if(act.myController!=null)
                    throw new OTMException("Multiple controllers assigned to single actuator");
                actuators.add(act);
                actuator_usage.put(act.id,jaxb_act.getUsage());
                act.myController=this;
            }
        }

        // read sensors ..............................................................
        sensors = new HashSet<>();
        sensor_usage = new HashMap<>();
        if(jaxb_controller.getFeedbackSensors()!=null){

            // read usage-less
            if(jaxb_controller.getFeedbackSensors().getIds()!=null){
                List<Long> ids = OTMUtils.csv2longlist(jaxb_controller.getFeedbackSensors().getIds());
                Iterator it = ids.iterator();
                while(it.hasNext()){
                    AbstractSensor sensor = (AbstractSensor) scenario.get_element(ScenarioElementType.sensor,(Long) it.next());
                    if(sensor==null)
                        throw new OTMException("Bad sensor id in controller");
                    sensors.add(sensor);
                    sensor_usage.put(sensor.id,"");
                }
            }

            // read actuators with usage
            for(jaxb.FeedbackSensor jaxb_sensor : jaxb_controller.getFeedbackSensors().getFeedbackSensor()){
                AbstractSensor sensor = (AbstractSensor) scenario.get_element(ScenarioElementType.sensor,jaxb_sensor.getId());
                if(sensor==null)
                    throw new OTMException("Bad sensor id in controller");
                sensors.add(sensor);
                sensor_usage.put(sensor.id,jaxb_sensor.getUsage());
            }
        }


        // read sensors
//        sensors = new ArrayList<Sensor>();
//        sensor_usage = new ArrayList<String>();
//        if(jaxbC.getFeedbackSensors()!=null && jaxbC.getFeedbackSensors().getFeedbackSensor()!=null){
//            for(FeedbackSensor fs : jaxbC.getFeedbackSensors().getFeedbackSensor()){
//                sensors.add(getMyScenario().get.sensorWithId(fs.getId()));
//                sensor_usage.add(fs.getUsage()==null ? "" : fs.getUsage());
//            }
//        }
    }

    public void validate(OTMErrorLog errorLog){
        if(type==null)
            errorLog.addError("myType==null");
        if(actuators.isEmpty())
            errorLog.addError("actuators.isEmpty()");
    }

    abstract public void initialize(Scenario scenario,float now) throws OTMException;

    abstract public void register_initial_events(Dispatcher dipatcher);

    ///////////////////////////////////////////////////
    // listeners
    ///////////////////////////////////////////////////

    public void set_event_listener(OutputEventsController e) throws OTMException {
        if(event_listener !=null)
            throw new OTMException("multiple listeners for commodity");
        event_listener = e;
    }

    ///////////////////////////////////////////////////
    // getters
    ///////////////////////////////////////////////////

    public boolean is_inbuilt(String name){
        for (Algorithm me : Algorithm.values()) {
            if (me.name().equalsIgnoreCase(name))
                return true;
        }
        return false;
    }

    abstract public Object get_current_command();

    ///////////////////////////////////////////////////
    // update
    ///////////////////////////////////////////////////

    abstract public void update_controller(Dispatcher dispatcher, float timestamp) throws OTMException ;

    @Override
    public void poke(Dispatcher dispatcher, float timestamp) throws OTMException  {
        update_controller(dispatcher,timestamp);

        // wake up in dt, if dt is defined
        if(dt >0)
            dispatcher.register_event(new EventPoke(dispatcher,1,timestamp+ dt,this));
    }

    ////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////

    @Override
    public long getId() {
        return id;
    }

    @Override
    public ScenarioElementType getScenarioElementType() {
        return ScenarioElementType.controller;
    }

}

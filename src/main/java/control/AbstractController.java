package control;

import actuator.AbstractActuator;
import core.InterfaceEventWriter;
import control.command.InterfaceCommand;
import dispatch.AbstractEvent;
import dispatch.Dispatcher;
import dispatch.EventPoke;
import dispatch.Pokable;
import error.OTMErrorLog;
import error.OTMException;
import output.AbstractOutputEvent;
import output.OutputController;
import core.InterfaceScenarioElement;
import core.Scenario;
import core.ScenarioElementType;
import output.events.EventWrapperController;
import sensor.AbstractSensor;
import utils.OTMUtils;

import java.util.*;

public abstract class AbstractController implements Pokable, InterfaceScenarioElement, InterfaceEventWriter {

    public enum Algorithm {
        schedule,
        sig_follower,
        sig_pretimed,
        rm_alinea,
        rm_fixed_rate,
        rm_open,
        rm_closed,
        lg_restrict,
        lg_toll,
        lg_lanechange,
        frflow,
        linkflow,
        plugin
    }

    public final Scenario scenario;
    public final long id;
    public final Algorithm type;
    public Float dt;
    public float start_time;
    public float end_time;
    protected boolean is_on;

    public Map<Long,AbstractActuator> actuators;
    public Map<String,AbstractActuator> actuator_by_usage;

    public Set<AbstractSensor> sensors;
    public Map<String,AbstractSensor> sensor_by_usage;

    public OutputController event_output;

    public Map<Long, InterfaceCommand> command;    // actuator id -> command

    protected abstract void update_command(Dispatcher dispatcher) throws OTMException;
    public abstract Class get_actuator_class();
    protected abstract void configure() throws OTMException;

    ///////////////////////////////////////////
    // construction and initialization
    ///////////////////////////////////////////

    public AbstractController(Scenario scenario, jaxb.Controller jaxb_controller) throws OTMException {
        this.scenario = scenario;
        this.id = jaxb_controller.getId();

        String controller_type = jaxb_controller.getType();
        this.type = is_inbuilt(controller_type) ? Algorithm.valueOf(controller_type) : Algorithm.plugin;
        this.dt = jaxb_controller.getDt();
        this.start_time = jaxb_controller.getStartTime();
        this.end_time = jaxb_controller.getEndTime()==null ? Float.POSITIVE_INFINITY : jaxb_controller.getEndTime();
        this.is_on = false;

        // below this does not apply for scenario-less controllers  ..............................
        if(scenario==null)
            return;

        // read actuators ..............................................................
        actuators = new HashMap<>();
        actuator_by_usage = new HashMap<>();
        if(jaxb_controller.getTargetActuators()!=null){

            // read usage-less
            if(!jaxb_controller.getTargetActuators().getIds().isEmpty()){
                List<Long> ids = OTMUtils.csv2longlist(jaxb_controller.getTargetActuators().getIds());
                Iterator it = ids.iterator();
                while(it.hasNext()){
                    AbstractActuator act = (AbstractActuator) scenario.get_element(ScenarioElementType.actuator,(Long) it.next());
                    if(act==null)
                        throw new OTMException("Bad actuator id in controller");
                    if(act.myController!=null)
                        throw new OTMException("Multiple controllers assigned to single actuator");
                    actuators.put(act.id,act);
                }
            }

            // read actuators with usage
            for(jaxb.TargetActuator jaxb_act : jaxb_controller.getTargetActuators().getTargetActuator()){
                AbstractActuator act = (AbstractActuator) scenario.get_element(ScenarioElementType.actuator,jaxb_act.getId());
                if(act==null)
                    throw new OTMException("Bad actuator id in controller");
                if(act.myController!=null)
                    throw new OTMException("Multiple controllers assigned to single actuator");
                actuators.put(act.id,act);
                if (jaxb_act.getUsage() != null) {
                    if(actuator_by_usage.containsKey(jaxb_act.getUsage()))
                        throw new OTMException("Repeated value in actuator usage for controller " +this.id);
                    actuator_by_usage.put(jaxb_act.getUsage(),act);
                }
            }
        }

        this.command = new HashMap<>();
        for(AbstractActuator act : actuators.values())
            command.put(act.id,null);

        // read sensors ..............................................................
        sensors = new HashSet<>();
        sensor_by_usage = new HashMap<>();
        if(jaxb_controller.getFeedbackSensors()!=null){

            // read usage-less
            if(!jaxb_controller.getFeedbackSensors().getIds().isEmpty()){
                List<Long> ids = OTMUtils.csv2longlist(jaxb_controller.getFeedbackSensors().getIds());
                Iterator it = ids.iterator();
                while(it.hasNext()){
                    AbstractSensor sensor = (AbstractSensor) scenario.get_element(ScenarioElementType.sensor,(Long) it.next());
                    if(sensor==null)
                        throw new OTMException("Bad sensor id in controller");
                    sensors.add(sensor);
                }
            }

            // read actuators with usage
            for(jaxb.FeedbackSensor jaxb_sensor : jaxb_controller.getFeedbackSensors().getFeedbackSensor()){
                AbstractSensor sensor = (AbstractSensor) scenario.get_element(ScenarioElementType.sensor,jaxb_sensor.getId());
                if(sensor==null)
                    throw new OTMException("Bad sensor id in controller");
                sensors.add(sensor);
                sensor_by_usage.put(jaxb_sensor.getUsage(),sensor);
            }
        }

    }

    public final void initialize(Scenario scenario,boolean override_targets) throws OTMException {

        float now = scenario.dispatcher.current_time;

        for(AbstractActuator x : actuators.values()) {
            x.myController = this;
            x.initialize(scenario, now ,override_targets);
        }

        // validate
        OTMErrorLog errorLog = validate_post_init();
        errorLog.check();

        // configure
        configure();

        this.is_on = true;

        // poke
        poke(scenario.dispatcher,scenario.dispatcher.current_time);
    }

    public void turn_off(){
        if(!is_on)
            return;

        this.is_on = false;

        // remove all future events
        this.scenario.dispatcher.remove_events_for_recipient(AbstractEvent.class,this);
    }

    public void turn_on(){
        if(is_on)
            return;

        is_on = true;

    }

    ///////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////

    @Override
    public final Long getId() {
        return id;
    }

    @Override
    public final ScenarioElementType getSEType() {
        return ScenarioElementType.controller;
    }

    public void validate_pre_init(OTMErrorLog errorLog) {
        if(type==null)
            errorLog.addError("myType==null");
        if(actuators.values().contains(null))
            errorLog.addError("Controller has a null actuator");
        if(sensors.contains(null))
            errorLog.addError("Controller has a null sensor");
        for(AbstractActuator act : actuators.values())
            if( ! this.get_actuator_class().isAssignableFrom( act.getClass()) )
                errorLog.addError("Bad actuator type in controller.");
    }

    public OTMErrorLog validate_post_init() {
        OTMErrorLog errorLog = new OTMErrorLog();
        if (actuators != null)
            actuators.values().stream().forEach(x -> x.validate_post_init(errorLog));
        return errorLog;
    }

    @Override
    public OTMErrorLog to_jaxb() {
        return null;
    }

    ///////////////////////////////////////////
    // Pokable
    ///////////////////////////////////////////

    @Override
    public final void poke(Dispatcher dispatcher, float timestamp) throws OTMException  {

        if(!is_on)
            return;

        update_command(dispatcher);

        // send immediately to actuators that lack a dt
        for(AbstractActuator act : actuators.values())
            if(act.dt==null && act.myController==this)
                act.process_command(command.get(act.id),timestamp);

        // write to output
        if(event_output!=null)
            event_output.write(new EventWrapperController(timestamp,command));

        // wake up in dt, if dt is defined
        if(dt!=null && dt>0)
            dispatcher.register_event(new EventPoke(dispatcher,20,timestamp+dt,this));
    }

    ///////////////////////////////////////////////////
    // InterfaceEventWriter
    ///////////////////////////////////////////////////

    @Override
    public void set_event_output(AbstractOutputEvent e) throws OTMException {
        if(event_output !=null)
            throw new OTMException("multiple listeners for controller");
        if(!(e instanceof OutputController))
            throw new OTMException("Wrong type of listener");
        event_output = (OutputController) e;
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

    public final InterfaceCommand get_command_for_actuator_id(Long act_id){
        return command.get(act_id);
    }

    public final InterfaceCommand get_command_for_actuator_usage(String act_usage){
        if(!actuator_by_usage.containsKey(act_usage))
            return null;
        return get_command_for_actuator_id(actuator_by_usage.get(act_usage).getId());
    }

}

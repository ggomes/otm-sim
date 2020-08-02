package actuator;

import common.*;
import control.AbstractController;
import control.command.InterfaceCommand;
import dispatch.Dispatcher;
import dispatch.EventPoke;
import dispatch.Pokable;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.Actuator;
import utils.OTMUtils;

import java.util.HashSet;
import java.util.Set;

public abstract class AbstractActuator implements Pokable, InterfaceScenarioElement {

    public enum Type {
        lanegroupclosure,
        lanegroupspeed,
        signal,
        meter,
        stop
    }

    public long id;
    public abstract Type getType();
    public float dt;

    public AbstractController myController;
    public InterfaceActuatorTarget target;

    abstract public void process_controller_command(InterfaceCommand command, float timestamp) throws OTMException;

    ///////////////////////////////////////////
    // construction
    ///////////////////////////////////////////

    public AbstractActuator(Scenario scenario, jaxb.Actuator jaxb_actuator) throws OTMException {
        this.id = jaxb_actuator.getId();
//        this.type = Type.valueOf(jaxb_actuator.getType());
        this.dt = jaxb_actuator.getDt();
        if(jaxb_actuator.getActuatorTarget()!=null){
            jaxb.ActuatorTarget e = jaxb_actuator.getActuatorTarget();
            Long id = e.getId()==null ? null : Long.parseLong(e.getId());

            ScenarioElementType type = null;
            try {
                // this will throw an exception if the type is not a ScenarioElementType
                type = ScenarioElementType.valueOf(e.getType());

                // otherwise we can find the element and register
                InterfaceActuatorTarget x;

                // if it is a lanegroup, then the id is for the link, and lanes must be used
                if(type==ScenarioElementType.lanegroups){
                    String str = e.getContent();
                    LaneGroupSet xx = new LaneGroupSet();

                    // READ LANEGROUP STRING
                    String [] a0 = str.split(",");
                    if(a0.length<1)
                        throw new OTMException("Poorly formatted string. (CN_23v4-str0)");
                    for(String lg_str : a0){
                        String [] a1 = lg_str.split("[(]");

                        if(a1.length!=2)
                            throw new OTMException("Poorly formatted string. (90hm*@$80)");

                        Long linkid = Long.parseLong(a1[0]);
                        Link link = scenario.network.links.get(linkid);

                        if(link==null)
                            throw new OTMException("Poorly formatted string. (24n2349))");

                        String [] a2 = a1[1].split("[)]");

                        if(a2.length!=1)
                            throw new OTMException("Poorly formatted string. (3g50jmdrthk)");

                        int [] lanes = OTMUtils.read_lanes(a2[0],link.full_lanes);

                        Set<AbstractLaneGroup> lgs = link.get_unique_lanegroups_for_dn_lanes(lanes[0],lanes[1]);
                        if(lgs.size()!=1)
                            throw new OTMException("Actuator target does not define a unique lane group");

                        xx.lgs.add(lgs.iterator().next());

                    }

                    x = xx;

                } else {
                    x = (InterfaceActuatorTarget) scenario.get_element(type,id);
                }

                this.target = x;
                if(target!=null)
                    target.register_actuator(this);


            } catch (IllegalArgumentException illegalArgumentException) {
                // if exception is thrown, set target to null.
                // and resolve in higher level constructor
                this.target = null;
            }
        }
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
        return ScenarioElementType.actuator;
    }

    @Override
    public void validate(OTMErrorLog errorLog) {
        if(target==null)
            errorLog.addWarning("Actuator has no target");
    }

    @Override
    public void register_with_dispatcher(Dispatcher dispatcher) {
        dispatcher.register_event(new EventPoke(dispatcher,30,dispatcher.current_time,this));
    }

    @Override
    public OTMErrorLog to_jaxb() {
        return null;
    }

    /////////////////////////////////////////////////////////////////////
    // Pokable
    /////////////////////////////////////////////////////////////////////

    @Override
    public void poke(Dispatcher dispatcher, float timestamp) throws OTMException {

        // process the command
        if(myController!=null)
            process_controller_command(myController.get_command_for_actuator_id(id),timestamp);

        // wake up in dt, if dt is defined
        if(dt>0)
            dispatcher.register_event(new EventPoke(dispatcher,3,timestamp+dt,this));
    }

    /////////////////////////////////////////////////////////////////////
    // InterfaceEventWriter
    /////////////////////////////////////////////////////////////////////

//    @Override
//    public void set_event_output(AbstractOutputEvent e) throws OTMException {
//        if(event_output !=null)
//            throw new OTMException("multiple listeners for actuator.");
//        if(!(e instanceof OutputActuator))
//            throw new OTMException("Wrong type of listener");
//        event_output = (OutputActuator)e;
//    }

    /////////////////////////////////////////////////////////////////////
    // AbstractActuatorLanegroup
    /////////////////////////////////////////////////////////////////////

    protected Set<AbstractLaneGroup> read_lanegroups(Scenario scenario, Actuator jact) throws OTMException {

        Set<AbstractLaneGroup> lanegroups = new HashSet<>();

        if(jact.getActuatorTarget()!=null && jact.getActuatorTarget().getType().equalsIgnoreCase("lanegroup")) {
            jaxb.ActuatorTarget e = jact.getActuatorTarget();

            Long link_id = null; //e.getId();
            if(!scenario.network.links.containsKey(link_id))
                throw new OTMException("Unknown link id in actuator " + id );
            Link link = scenario.network.links.get(link_id);

//            int [] x = OTMUtils.read_lanes(e.getLanes(),link.full_lanes);
//            int start_lane = x[0];
//            int end_lane = x[1];
//
//            lanegroups = link.get_unique_lanegroups_for_dn_lanes(start_lane, end_lane);

        }

        return lanegroups;
    }

    /////////////////////////////////////////////////////////////////////
    // get
    /////////////////////////////////////////////////////////////////////

//    public Type getType() {
//        return type;
//    }

}

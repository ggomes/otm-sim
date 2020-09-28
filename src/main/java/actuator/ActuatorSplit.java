package actuator;

import commodity.Commodity;
import common.Link;
import common.Node;
import common.Scenario;
import control.command.InterfaceCommand;
import dispatch.EventSplitChange;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.Actuator;
import jaxb.Parameter;
import keys.KeyCommodityLink;
import profiles.SplitMatrixProfile;

public class ActuatorSplit extends AbstractActuator {

    private Link linkin;
    private Link linkout;
    private Commodity comm;

    public ActuatorSplit(Scenario scenario, Actuator jact) throws OTMException {
        super(scenario, jact);

        if(jact.getActuatorTarget()!=null && jact.getActuatorTarget().getParameters()!=null){
            for(Parameter p : jact.getActuatorTarget().getParameters().getParameter()){
                switch(p.getName()){
                    case "linkin":
                        this.linkin = scenario.network.links.get(Long.parseLong(p.getValue()));
                        break;
                    case "linkout":
                        this.linkout = scenario.network.links.get(Long.parseLong(p.getValue()));
                        break;
                    case "comm":
                        this.comm = scenario.commodities.get(Long.parseLong(p.getValue()));
                        break;
                }
            }
        }
    }

    @Override
    public Type getType() {
        return Type.split;
    }

    @Override
    public void validate(OTMErrorLog errorLog) {
        super.validate(errorLog);
        if(linkin==null)
            errorLog.addError("ActuatorSplit: linkin==null");
        if(linkout==null)
            errorLog.addError("ActuatorSplit: linkout==null");
        if(comm==null)
            errorLog.addError("ActuatorSplit: comm==null");

        // confirm they are connected
        if(linkin.end_node!=linkout.start_node)
            errorLog.addError("ActuatorSplit: linkin.end_node!=linkout.start_node");
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        Node node = linkin.end_node;

        long commid = comm.getId();

        // delete the existng splits.
        if(linkin.split_profile.containsKey(commid)){
            SplitMatrixProfile smp = linkin.split_profile.get(commid);
            scenario.dispatcher.remove_events_for_recipient(EventSplitChange.class,smp);
            linkin.split_profile.remove(commid);
        }

        System.out.println(String.format("%.1f\tActuatorSplit\tinitialize",scenario.dispatcher.current_time));

    }

    @Override
    public void process_controller_command(InterfaceCommand command, float timestamp) throws OTMException {

//



        System.out.println(String.format("%.1f\tActuatorSplit\tprocess_controller_command",timestamp));
    }

}

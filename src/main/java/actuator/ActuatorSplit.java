package actuator;

import commodity.Commodity;
import common.Link;
import common.Node;
import common.Scenario;
import control.command.CommandNumber;
import control.command.InterfaceCommand;
import dispatch.EventSplitChange;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.Actuator;
import jaxb.Parameter;
import profiles.SplitMatrixProfile;

import java.util.HashMap;
import java.util.Map;

public class ActuatorSplit extends AbstractActuator {

    public Link linkin;
    public Link linkFR;
    public Link linkML;
    public Commodity comm;

    private SplitMatrixProfile smp;

    public ActuatorSplit(Scenario scenario, Actuator jact) throws OTMException {
        super(scenario, jact);

        if(jact.getParameters()!=null){
            for(Parameter p : jact.getParameters().getParameter()){
                switch(p.getName()){
                    case "linkin":
                        this.linkin = scenario.network.links.get(Long.parseLong(p.getValue()));
                        break;
                    case "linkout":
                        this.linkFR = scenario.network.links.get(Long.parseLong(p.getValue()));
                        break;
                    case "comm":
                        this.comm = scenario.commodities.get(Long.parseLong(p.getValue()));
                        break;
                }
            }
        }

        if(linkFR==null)
            return;

        Node node = linkin.end_node;
        linkML = node.out_links.stream()
                .filter(link->link!=linkFR)
                .findFirst().get();
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
        if(linkFR==null)
            errorLog.addError("ActuatorSplit: linkFR==null");
        if(linkML==null)
            errorLog.addError("ActuatorSplit: linkML==null");
        if(comm==null)
            errorLog.addError("ActuatorSplit: comm==null");

        // confirm they are connected
        if(linkin.end_node!=linkFR.start_node)
            errorLog.addError("ActuatorSplit: linkin.end_node!=linkout.start_node");

        // only offramp like arrangement allowed
        if(linkin.end_node.out_links.size()!=2)
            errorLog.addError("ActuatorSplit: linkin.end_node.out_links.size()!=2");
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {

        if(initialized)
            return;

        super.initialize(scenario);


        long commid = comm.getId();

        // delete the existng splits.
        if(linkin.split_profile.containsKey(commid)){
            scenario.dispatcher.remove_events_for_recipient(EventSplitChange.class,linkin.split_profile.get(commid));
            this.smp = linkin.split_profile.get(commid);
            linkin.split_profile.remove(commid);
        }
        else{
            this.smp = new SplitMatrixProfile(commid,linkin);
        }

        // create the new split ratio matrix
        linkin.split_profile.put(commid, smp);
    }

    @Override
    public void process_controller_command(InterfaceCommand command, float timestamp) throws OTMException {
        if(command==null)
            return;
        double outsplit = (double) ((CommandNumber)command).value;
        Map<Long,Double> outlink2split = new HashMap<>();
        outlink2split.put(linkFR.getId(),outsplit);
        outlink2split.put(linkML.getId(),1d-outsplit);
        smp.set_current_splits(outlink2split);
    }

}

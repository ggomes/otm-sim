package actuator;

import commodity.Commodity;
import common.Link;
import common.Node;
import common.Scenario;
import control.command.CommandDoubleMap;
import control.command.CommandNumber;
import control.command.InterfaceCommand;
import dispatch.EventSplitChange;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.Actuator;
import jaxb.Parameter;
import profiles.SplitMatrixProfile;
import utils.OTMUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ActuatorSplit extends AbstractActuator {

    public Link linkMLup;
    public Link linkMLdwn;
    public Set<Link> linkFRs = new HashSet<>();
    public Commodity comm;

    private SplitMatrixProfile smp;

    public ActuatorSplit(Scenario scenario, Actuator jact) throws OTMException {
        super(scenario, jact);

        if(jact.getParameters()!=null){
            for(Parameter p : jact.getParameters().getParameter()){
                switch(p.getName()){
                    case "linkin":
                        this.linkMLup = scenario.network.links.get(Long.parseLong(p.getValue()));
                        break;
                    case "linksout":
                        this.linkFRs.addAll(OTMUtils.csv2longlist(p.getValue()).stream().map(l->scenario.network.links.get(l)).collect(Collectors.toSet()));
                        break;
                    case "comm":
                        this.comm = scenario.commodities.get(Long.parseLong(p.getValue()));
                        break;
                }
            }
        }

        if(linkMLup==null || linkFRs.isEmpty())
            return;

        Node node = linkMLup.end_node;
        linkMLdwn = node.out_links.stream()
                .filter(link->!linkFRs.contains(link))
                .findFirst().get();
    }

    @Override
    public Type getType() {
        return Type.split;
    }

    @Override
    public void validate(OTMErrorLog errorLog) {
        super.validate(errorLog);
        if(linkMLup ==null)
            errorLog.addError("ActuatorSplit: linkin==null");
        if(linkFRs.isEmpty())
            errorLog.addError("ActuatorSplit: linkFRs.isEmpty()");
        if(linkFRs.contains(null))
            errorLog.addError("ActuatorSplit: linkFRs.contains(null)");
        if(linkMLdwn ==null)
            errorLog.addError("ActuatorSplit: linkML==null");
        if(comm==null)
            errorLog.addError("ActuatorSplit: comm==null");

        Set<Link> allout = new HashSet<>();
        allout.add(linkMLdwn);
        allout.addAll(linkFRs);
        if(allout.size()!=linkMLup.end_node.out_links.size())
            errorLog.addError("Actuator split: not all outlinks are represented.");

        // confirm they are connected
        if(linkFRs.stream().anyMatch(linkFR->linkMLup.end_node!=linkFR.start_node))
            errorLog.addError("ActuatorSplit: linkin.end_node!=linkout.start_node");

    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {

        if(initialized)
            return;

        super.initialize(scenario);

        long commid = comm.getId();

        // delete the existng splits.
        if(linkMLup.split_profile.containsKey(commid)){
            scenario.dispatcher.remove_events_for_recipient(EventSplitChange.class, linkMLup.split_profile.get(commid));
            this.smp = linkMLup.split_profile.get(commid);
            linkMLup.split_profile.remove(commid);
        }
        else{
            this.smp = new SplitMatrixProfile(commid, linkMLup);
        }

        // create the new split ratio matrix
        linkMLup.split_profile.put(commid, smp);
    }

    @Override
    public void process_controller_command(InterfaceCommand command, float timestamp) throws OTMException {
        if(command==null)
            return;
        smp.set_and_rectify_splits(((CommandDoubleMap)command).values,linkMLdwn.getId());
    }

}

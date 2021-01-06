package actuator;

import commodity.Commodity;
import control.command.CommandDoubleArray;
import core.Link;
import core.Node;
import core.Scenario;
import control.command.InterfaceCommand;
import core.ScenarioElementType;
import dispatch.EventSplitChange;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.Actuator;
import jaxb.Parameter;
import profiles.SplitMatrixProfile;
import utils.OTMUtils;

import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

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
                        this.linkFRs.addAll(OTMUtils.csv2longlist(p.getValue()).stream().map(l->scenario.network.links.get(l)).collect(toSet()));
                        break;
                    case "comm":
                        this.comm = scenario.commodities.get(Long.parseLong(p.getValue()));
                        break;
                }
            }
        }

        if(linkMLup==null || linkFRs.isEmpty())
            return;

        Node node = linkMLup.get_end_node();
        linkMLdwn = node.get_out_links().stream()
                .filter(link->link.get_road_type()== Link.RoadType.freeway)
                .findFirst().get();
    }

    @Override
    public Type getType() {
        return Type.split;
    }

    @Override
    public void validate_pre_init(OTMErrorLog errorLog) {
        super.validate_pre_init(errorLog);
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

//        Set<Link> allout = new HashSet<>();
//        allout.add(linkMLdwn);
//        allout.addAll(linkFRs);
//        if(allout.size()!=linkMLup.end_node.out_links.size()) {
//            Set<Link> missing = new HashSet<Link>();
//            missing.addAll(linkMLup.end_node.out_links);
//            missing.removeAll(allout);
//            errorLog.addError(String.format("In actuator id=%d, output link(s) %s is(are) not represented.",id,
//                    OTMUtils.comma_format(missing.stream().map(x->x.getId()).collect(toSet()))));
//        }

        // confirm they are connected
        if(linkFRs.stream().anyMatch(linkFR->linkMLup.get_end_node()!=linkFR.get_start_node()))
            errorLog.addError("ActuatorSplit: linkin.end_node!=linkout.start_node");

    }

    @Override
    public void initialize(Scenario scenario, float start_time,boolean override_targets) throws OTMException {

        if(initialized)
            return;

        super.initialize(scenario,start_time,override_targets);

        long commid = comm.getId();

        // delete the existng splits.
        if(linkMLup.have_split_for_commodity(commid)){
            scenario.dispatcher.remove_events_for_recipient(EventSplitChange.class, linkMLup.get_split_profile(commid));
            this.smp = linkMLup.get_split_profile(commid);
            linkMLup.remove_split_profile(commid);
        }
        else{
            this.smp = new SplitMatrixProfile(commid, linkMLup);
        }

        // create the new split ratio matrix
        linkMLup.set_split_profile(commid, smp);

        // register the actuator
        target.register_actuator(commids,this,override_targets);
    }

    @Override
    public void process_controller_command(InterfaceCommand command, float timestamp) throws OTMException {
        if(command==null)
            return;
        if(!(command instanceof CommandDoubleArray))
            throw new OTMException("Bad command type.");

//        smp.set_and_rectify_splits(((CommandDoubleArray)command).values,linkMLdwn.getId());
    }

    @Override
    protected ScenarioElementType get_target_class() {
        return null;
    }
}

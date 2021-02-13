package actuator;

import control.AbstractController;
import control.command.CommandDoubleArray;
import control.command.CommandLongToDouble;
import core.Link;
import core.Scenario;
import control.command.InterfaceCommand;
import core.ScenarioElementType;
import dispatch.Dispatcher;
import dispatch.EventSplitChange;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.Actuator;
import jaxb.Parameter;
import profiles.SplitMatrixProfile;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ActuatorSplit extends AbstractActuator {

    public Link link_from;
    public Link link_to;
    public Link links_dn;

    public ActuatorSplit(Scenario scenario, Actuator jact) throws OTMException {
        super(scenario, jact);

        link_from = (Link) target;

        if(jact.getParameters()!=null){
            for(Parameter p : jact.getParameters().getParameter()){
                switch(p.getName()){
                    case "linkto":
                        this.link_to = scenario.network.links.get(Long.parseLong(p.getValue()));
                        break;
                }
            }
        }

        if(link_from==null || link_to==null)
            return;

        Set<Link> links_dns = new HashSet<>();
        links_dns.addAll(link_from.get_end_node().get_out_links());
        links_dns.remove(link_to);

        if(links_dns.size()!=1)
            throw new OTMException("Actuator split has more than one alternative link");

        links_dn = links_dns.iterator().next();
    }

    @Override
    public Type getType() {
        return Type.split;
    }

    @Override
    public void validate_pre_init(OTMErrorLog errorLog) {
        super.validate_pre_init(errorLog);
        if(link_from ==null)
            errorLog.addError("ActuatorSplit: link_from==null");
        if(link_to==null)
            errorLog.addError("ActuatorSplit: link_to.isEmpty()");
        if(links_dn==null)
            errorLog.addError("ActuatorSplit: links_dn==null");
        if(commids==null || commids.isEmpty())
            errorLog.addError("ActuatorSplit: commids==null || commids.isEmpty()");

        // confirm they are connected
        if(!link_from.get_end_node().get_out_links().contains(link_to))
            errorLog.addError("ActuatorSplit: link_from does not connect to link_to");
    }

    @Override
    public void initialize(Scenario scenario, float timestamp,boolean override_targets) throws OTMException {

        if(initialized)
            return;

        super.initialize(scenario,timestamp,override_targets);

//        // delete the existng splits.
//        for(long commid : commids){
//
////            SplitMatrixProfile smp;
////            if(link_from.have_split_for_commodity(commid))
////                smp = link_from.get_split_profile(commid);
////            else {
////                smp = new SplitMatrixProfile(commid, link_from);
////                link_from.set_split_profile(commid, smp);
////            }
////            this.smps.put(commid, smp);
//        }


        // register the actuator
//        target.register_actuator(commids,this,override_targets);
    }

    @Override
    public void process_command(InterfaceCommand command, float timestamp) throws OTMException {

//        Dispatcher dispatcher = link_from.get_scenario().dispatcher;
//
//        // return to nominal splits
//        if(command==null) {
//            for (long commid : commids) {
//
//                // if I have splits, then reset these
//                if (link_from.have_split_for_commodity(commid)) {
//                    SplitMatrixProfile smp = link_from.get_split_profile(commid);
//                    smp.initialize(dispatcher);
//                }
//
//                // otherwise it might be a flow actuator
//                else if(link_from.acts_flowToLinks!=null) {
//                    for(Map<Long,ActuatorFlowToLinks> acts_flowToLink : link_from.acts_flowToLinks.values()){
//                        if(acts_flowToLink.containsKey(commid)){
//                            ActuatorFlowToLinks act = acts_flowToLink.get(commid);
//                            AbstractController cntrl = act.myController;
//                            cntrl.turn_on();
//                            CommandLongToDouble cmd = new CommandLongToDouble();
//                            cmd.X.put(link_to.getId(),0d);
//                            act.process_command(cmd,timestamp);
//                        }
//
//                    }
//                }
//
//            }
//        }
//
//        // set splits in the command
//        else {
//
//            for(long commid : commids) {
//
//                // if I have a split for this commodity, then modify it
//                if(link_from.have_split_for_commodity(commid)) {
//                    SplitMatrixProfile smp = link_from.get_split_profile(commid);
//
//                    // remove future split change events
//                    dispatcher.remove_events_for_recipient(EventSplitChange.class, smp);
//
//                    // set split
//                    smp.set_and_rectify_splits(((CommandDoubleArray) command).as_map(), links_dn.getId());
//                }
//
//                // otherwise there may be a flow actuator
//                else if(link_from.acts_flowToLinks!=null) {
//                    for(Map<Long,ActuatorFlowToLinks> acts_flowToLink : link_from.acts_flowToLinks.values()){
//                        if(acts_flowToLink.containsKey(commid)){
//                            ActuatorFlowToLinks act = acts_flowToLink.get(commid);
//                            AbstractController cntrl = act.myController;
//                            cntrl.turn_off();
//                            CommandLongToDouble cmd = new CommandLongToDouble();
//                            cmd.X.put(link_to.getId(),0d);
//                            act.process_command(cmd,timestamp);
//                        }
//
//                    }
//                }
//
//            }
//
//        }

    }

    @Override
    protected ScenarioElementType get_target_class() {
        return ScenarioElementType.link;
    }

    @Override
    protected InterfaceCommand command_off() {
        return null;
    }

}

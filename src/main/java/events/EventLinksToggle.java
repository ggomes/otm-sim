package events;

import actuator.ActuatorFlowToLinks;
import control.command.CommandLongToDouble;
import control.commodity.ControllerFlowToLinks;
import core.Link;
import core.Scenario;
import dispatch.AbstractEvent;
import dispatch.EventSplitChange;
import error.OTMErrorLog;
import error.OTMException;
import profiles.SplitMatrixProfile;
import utils.OTMUtils;

import java.util.*;
import java.util.stream.Collectors;

public class EventLinksToggle extends AbstractScenarioEvent {

//    ActuatorSplit actuator;
    boolean isopen;

//    public List<Link> link_from;
    public List<Link> links_to;
//    public List<Link> links_dn;

    public EventLinksToggle(long id, EventType type, float timestamp,String name) {
        super(id, type, timestamp,name);
    }

    public EventLinksToggle(Scenario scenario, jaxb.Event jev){
        super(jev);

        links_to = new ArrayList<>();
        if(jev.getEventTarget()!=null && jev.getEventTarget().getType().equals("links") && jev.getEventTarget().getIds()!=null)
            for(Long linkid : OTMUtils.csv2longlist(jev.getEventTarget().getIds()))
                if(scenario.network.links.containsKey(linkid))
                    links_to.add(scenario.network.links.get(linkid));

        this.isopen = true;
        if(jev.getParameters()!=null)
            for(jaxb.Parameter p : jev.getParameters().getParameter())
                if(p.getName().equals("isopen"))
                    this.isopen = Boolean.parseBoolean(p.getValue());
    }

    @Override
    public void validate_pre_init(OTMErrorLog errorLog) {
        super.validate_pre_init(errorLog);
        if(links_to.contains(null))
            errorLog.addError("Bad link id in event");
    }

    @Override
    public void validate_post_init(OTMErrorLog errorLog) {

    }

    @Override
    public void action() throws OTMException {

        Scenario scenario = dispatcher.scenario;

        Set<Link> links_from = links_to.stream()
                .flatMap(link->link.get_roadconnections_entering().stream())
                .map(rc -> rc.get_start_link())
                .collect(Collectors.toSet());

        for (long commid : scenario.commodities.keySet()) {

            for (Link link_from : links_from) {

                Collection<Link> my_links_to = link_from.get_next_links();
                my_links_to.retainAll(links_to);

                boolean has_splits = link_from.have_split_for_commodity(commid);
                if (has_splits) {

                    SplitMatrixProfile smp = link_from.get_split_profile(commid);
                    if (isopen)
                        smp.initialize(dispatcher);
                    else {
                        // remove future split change events
//                        dispatcher.remove_events_for_recipient(EventSplitChange.class, smp);

                        // set split
                        Map<Long,Double> newsplit = new HashMap<>();
                        for(Link link_to : my_links_to)
                            if(smp.outlink2split.containsKey(link_to.getId()))
                                newsplit.put(link_to.getId(), 0d);
                        if(!newsplit.isEmpty())
                            smp.set_some_current_splits(newsplit);
                    }
                }

                // flow actuator .......................
                if (link_from.acts_flowToLinks != null) {

                    for (Map<Long, ActuatorFlowToLinks> acts_flowToLink : link_from.acts_flowToLinks.values()) {
                        if (acts_flowToLink.containsKey(commid)) {

                            ActuatorFlowToLinks act = acts_flowToLink.get(commid);
                            ControllerFlowToLinks cntrl = (ControllerFlowToLinks) act.myController;

                            if (isopen) {
                                cntrl.poke(dispatcher,timestamp);
                            } else {
                                // remove future events
                                dispatcher.remove_events_for_recipient(AbstractEvent.class,cntrl);
                                dispatcher.remove_events_for_recipient(AbstractEvent.class,act);

                                // send a zero to the actuator
                                CommandLongToDouble cmd = new CommandLongToDouble();
                                for(Link link_to : my_links_to)
                                    if (cntrl.outlink2profile.containsKey(link_to.getId()))
                                        cmd.X.put(link_to.getId(), 0d);
                                act.process_command(cmd, timestamp);
                            }
                        }
                    }
                }
            }
        }
    }
}

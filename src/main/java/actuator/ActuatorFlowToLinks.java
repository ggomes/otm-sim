package actuator;

import commodity.Commodity;
import common.Link;
import common.Scenario;
import control.command.CommandDoubleArray;
import control.command.InterfaceCommand;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.Actuator;
import jaxb.Parameter;
import models.fluid.AbstractFluidModel;
import packet.PacketLink;
import utils.OTMUtils;

import java.util.*;

public class ActuatorFlowToLinks extends AbstractActuator {

    public final Link linkMLup;
    public final List<Long> outlink_ids;
    public final Commodity comm;

    public double [] outlink2flows;
    public double total_outlink2flows;
    public Map<Long,Double> outlink2portion = null;
    public double gamma = 1d;

    public ActuatorFlowToLinks(Scenario scenario, Actuator jact) throws OTMException {
        super(scenario, jact);

        Link temp_linkMLup = null;
        Commodity temp_comm = null;
        List<Long> temp_outlinkids = null;

        if(jact.getParameters()!=null){
            for(Parameter p : jact.getParameters().getParameter()){
                switch(p.getName()){
                    case "linkin":
                        temp_linkMLup = scenario.network.links.get(Long.parseLong(p.getValue()));
                        break;
                    case "linksout":
                        temp_outlinkids = OTMUtils.csv2longlist(p.getValue());
                        break;
                    case "comm":
                        temp_comm = scenario.commodities.get(Long.parseLong(p.getValue()));
                        break;
                }
            }
        }

        this.linkMLup = temp_linkMLup;
        this.comm = temp_comm;
        this.outlink_ids = temp_outlinkids;
    }

    @Override
    public Type getType() {
        return Type.flowtolink;
    }

    @Override
    public void validate(OTMErrorLog errorLog) {
        super.validate(errorLog);
        if (linkMLup == null)
            errorLog.addError("ActuatorSplit: linkin==null");
        if (outlink_ids.isEmpty())
            errorLog.addError("ActuatorSplit: linkFRs.isEmpty()");
        if (outlink_ids.contains(null))
            errorLog.addError("ActuatorSplit: linkFRs.contains(null)");
        if (comm == null)
            errorLog.addError("ActuatorSplit: comm==null");

//        // confirm they are connected
//        if(linkFRs.stream().anyMatch(linkFR->linkMLup.end_node!=linkFR.start_node))
//            errorLog.addError("ActuatorSplit: linkin.end_node!=linkout.start_node");

    }

    @Override
    public void initialize(Scenario scenario, float start_time) throws OTMException {

        if (initialized)
            return;

        super.initialize(scenario, start_time);

        linkMLup.set_actuator_flowToLinks(this);

        outlink2flows = new double[outlink_ids.size()];
        outlink2portion= new HashMap<>();
        for(Long linkid : outlink_ids)
            outlink2portion.put(linkid, Double.NaN);
    }

    @Override
    public void process_controller_command(InterfaceCommand command, float timestamp) throws OTMException {

        if(command==null)
            return;

        CommandDoubleArray c = (CommandDoubleArray)command;
        double alpha = ((AbstractFluidModel)linkMLup.model).dt_sec / 3600f;
        this.total_outlink2flows = 0d;
        for(int i=0;i<c.ids.length;i++) {
            double x = c.values[i] * alpha;
            outlink2flows[i] = x;
            total_outlink2flows += x;
        }
    }

    public void update_for_packet(PacketLink vp){
        double alphaoverv = 1d / Math.max( vp.total_macro_vehicles() , total_outlink2flows );
        for(int i=0;i<outlink_ids.size();i++)
            outlink2portion.put(outlink_ids.get(i), outlink2flows[i] * alphaoverv);
        gamma = 1d - total_outlink2flows * alphaoverv;
    }

    public double calculate_sumbetac(Map<Long, Double> current_splits){
        double sumbetac = 0d;
        for(Map.Entry<Long,Double> e : current_splits.entrySet()){
            if(outlink_ids.contains(e.getKey()))
                continue;
            sumbetac += e.getValue();
        }
        return sumbetac;
    }

}

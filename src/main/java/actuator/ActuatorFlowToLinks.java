package actuator;

import commodity.Commodity;
import control.command.CommandLongToDouble;
import control.commodity.ControllerFlowToLinks;
import core.*;
import control.command.InterfaceCommand;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.Actuator;

import java.util.*;

public class ActuatorFlowToLinks extends AbstractActuator {

//    private Link linkMLup;
//    public List<Long> outlink_ids;
    public long rcid;

    public Map<Long,Double> outlink2flows;
    public double total_outlink2flows;

    public ActuatorFlowToLinks(Scenario scenario,Actuator jact) throws OTMException {
        super(scenario, jact);

        Long temp_rcid = null;
        List<Long> temp_outlinkids = null;
        if(jact.getParameters()!=null){
            for(jaxb.Parameter p : jact.getParameters().getParameter()){
                switch(p.getName()){
                    case "rcid":
                        temp_rcid = Long.parseLong(p.getValue());
                        break;
//                    case "linksout":
//                        temp_outlinkids = OTMUtils.csv2longlist(p.getValue());
//                        break;
                }
            }
        }

        rcid = temp_rcid==null ? Long.MIN_VALUE : temp_rcid;

        dt = null;

    }

    @Override
    protected ScenarioElementType get_target_class() {
        return ScenarioElementType.link;
    }

    @Override
    public Type getType() {
        return Type.flowtolink;
    }

    @Override
    public void validate_pre_init(OTMErrorLog errorLog) {
        super.validate_pre_init(errorLog);
        if (target == null)
            errorLog.addError("ActuatorFlowToLinks: target==null");
//        if (outlink_ids.isEmpty())
//            errorLog.addError("ActuatorFlowToLinks: outlink_ids.isEmpty()");
//        if (outlink_ids.contains(null))
//            errorLog.addError("ActuatorFlowToLinks: outlink_ids.contains(null)");
        if (commids.size()!=1)
            errorLog.addError("ActuatorFlowToLinks: commids.size()!=1");
        if( !((Link)target).get_roadconnections_entering().stream().anyMatch(x->x.getId()==rcid) )
            errorLog.addError("Road connection does not enter the target link");
    }

    @Override
    public void initialize(Scenario scenario, float timestamp,boolean override_targets) throws OTMException {

        if (initialized)
            return;

        super.initialize(scenario, timestamp,override_targets);

        if(!commids.isEmpty()){
            long commid = commids.iterator().next();
            if(!scenario.commodities.containsKey(commid))
                throw new OTMException("Bad commodity id in ActuatorFlowToLinks");
            Commodity comm = scenario.commodities.get(commid);
            if(comm.pathfull)
                throw new OTMException("Pathfull commodity in ActuatorFlowToLinks.");
        }

        outlink2flows= new HashMap<>();
        for(Long linkid : ((ControllerFlowToLinks)myController).outlink2profile.keySet())
            outlink2flows.put(linkid, Double.NaN);

        // register the actuator
        target.register_actuator(commids,this,override_targets);
    }

//    @Override
//    public void validate_post_init(OTMErrorLog errorLog) {
//        super.validate_post_init(errorLog);
//        if (rc == null)
//            errorLog.addError("ActuatorFlowToLinks: rc==null");
//    }

    @Override
    public void process_command(InterfaceCommand command, float timestamp) throws OTMException {
        if(command==null)
            return;
        if(!(command instanceof CommandLongToDouble))
            throw new OTMException("Bad command type.");
        CommandLongToDouble cmnd = (CommandLongToDouble)command;
        Link link = (Link) target;
        double modeldt = ((AbstractFluidModel) link.get_model()).dt_sec / 3600d;
        for(Map.Entry<Long,Double> e : cmnd.X.entrySet())
            outlink2flows.put(e.getKey(), e.getValue() * modeldt);
        this.total_outlink2flows = outlink2flows.values().stream().mapToDouble(x->x).sum();
    }

//    public void update_for_packet(PacketLink vp){
//        long commid = commids.iterator().next();
//        double alphaoverv = 1d / Math.max( vp.total_macro_vehicles_of_commodity(commid) , total_outlink2flows );
//        for(int i=0;i<outlink_ids.size();i++)
//            outlink2portion.put(outlink_ids.get(i), outlink2flows[i] * alphaoverv);
//        gamma = 1d - total_outlink2flows * alphaoverv;
//    }


    @Override
    protected InterfaceCommand command_off() {
        return null;
    }

}

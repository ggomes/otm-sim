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

    public long rcid;

    public Map<Long,Double> outlink2flows;
    public double total_outlink2flows;
    public double total_unactuated_split; // total split to links not controlled by this actuator.

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
                }
            }
        }

        rcid = temp_rcid==null ? Long.MIN_VALUE : temp_rcid;

        dt = null;
        total_unactuated_split = 0d;

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

    public void update_total_unactuated_splits(Map<Long,Double> outlink2split){
        if(outlink2split==null)
            return;
        for(Map.Entry<Long,Double> e : outlink2split.entrySet())
            if(!outlink2flows.containsKey(e.getKey()))
                total_unactuated_split += e.getValue();
    }

    @Override
    protected InterfaceCommand command_off() {
        return null;
    }

}

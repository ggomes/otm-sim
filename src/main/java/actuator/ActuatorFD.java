package actuator;

import common.AbstractLaneGroup;
import dispatch.Dispatcher;
import error.OTMException;
import jaxb.Actuator;
import runner.Scenario;

public class ActuatorFD extends AbstractActuator {

    public ActuatorFD(Scenario scenario, Actuator jaxb_actuator) throws OTMException {
        super(scenario, jaxb_actuator);
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {

    }

    @Override
    public void process_controller_command(Object command, Dispatcher dispatcher, float timestamp) throws OTMException {
        if(command==null)
            return;
        AbstractLaneGroup lanegroup = (AbstractLaneGroup) target;
        FDCommand newfd = (FDCommand) command;

        if(newfd.max_speed_mps!=null)
            lanegroup.set_max_speed_mps(newfd.max_speed_mps);

        if(newfd.max_flow_vpspl!=null)
            lanegroup.set_max_flow_vpspl(newfd.max_flow_vpspl);

        if(newfd.max_density_vpkpl!=null)
            lanegroup.set_max_density_vpkpl(newfd.max_density_vpkpl);
    }

    public class FDCommand {
        public Float max_speed_mps;
        public Float max_flow_vpspl;
        public Float max_density_vpkpl;
        public FDCommand(Float max_speed_mps, Float max_flow_vpspl, Float max_density_vpkpl) {
            this.max_speed_mps = max_speed_mps;
            this.max_flow_vpspl = max_flow_vpspl;
            this.max_density_vpkpl = max_density_vpkpl;
        }
    }

}

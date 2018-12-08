package actuator;

import common.Link;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.Actuator;
import runner.Scenario;

public class ActuatorFD extends AbstractActuator {

    public ActuatorFD(Scenario scenario, Actuator jaxb_actuator) throws OTMException {
        super(scenario, jaxb_actuator);

        // must be on a link
        if(target==null || !(target instanceof common.Link))
            return;
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {

    }

    @Override
    public void process_controller_command(Object command, Dispatcher dispatcher, float timestamp) throws OTMException {
//        if(command==null)
//            return;
//        ((Link) target).model.set_road_param((FDCommand) command);
    }

    /** This is the class for the controller command **/
    public class FDCommand {
        public Float max_speed_kph;
        public Float capacity_vphpl;
        public Float jam_density_vpkpl;
        public FDCommand(Float max_speed_kph, Float capacity_vphpl, Float jam_density_vpkpl) {
            this.max_speed_kph = max_speed_kph;
            this.capacity_vphpl = capacity_vphpl;
            this.jam_density_vpkpl = jam_density_vpkpl;
        }
    }

}

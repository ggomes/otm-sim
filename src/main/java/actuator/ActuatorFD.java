package actuator;

import common.Link;
import dispatch.Dispatcher;
import error.OTMException;
import jaxb.Actuator;
import common.Scenario;

public class ActuatorFD extends AbstractActuator {

    ///////////////////////////////////////////
    // construction
    ///////////////////////////////////////////

    public ActuatorFD(Scenario scenario, Actuator jaxb_actuator) throws OTMException {
        super(scenario, jaxb_actuator);

        // must be on a link
        if(target==null || !(target instanceof common.Link))
            return;
    }

    ///////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////

    @Override
    public void initialize(Scenario scenario) throws OTMException {

    }

    ///////////////////////////////////////////
    // AbstractActuator
    ///////////////////////////////////////////

    @Override
    public void process_controller_command(Object command, float timestamp) throws OTMException {
        if(command==null)
            return;
        Link link = (Link) target;
        link.model.set_road_param(link,(FDCommand) command);
    }

    ///////////////////////////////////////////
    // class
    ///////////////////////////////////////////

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

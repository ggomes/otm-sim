package actuator;

import common.Scenario;
import control.command.InterfaceCommand;
import error.OTMException;
import geometry.Side;
import jaxb.Actuator;

import java.util.Map;

public class ActuatorLaneChange extends AbstractActuator {

    protected Map<Side,Double> lanechangeprob; // side->prob

    public ActuatorLaneChange(Scenario scenario, Actuator jaxb_actuator) throws OTMException {
        super(scenario, jaxb_actuator);
    }

    @Override
    public Type getType() {
        return Type.lg_lanechange;
    }

    @Override
    public void process_controller_command(InterfaceCommand command, float timestamp) throws OTMException {

    }

    public Map<Side,Double> get_lanechange_probabilities(){
        return lanechangeprob;
    }


}

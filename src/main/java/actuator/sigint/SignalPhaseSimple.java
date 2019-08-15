package actuator.sigint;

import java.util.Set;
import java.util.HashSet;

import actuator.AbstractActuator;
import common.RoadConnection;
import error.OTMErrorLog;
import error.OTMException;
import runner.ScenarioElementType;
import runner.Scenario;
import utils.OTMUtils;


public class SignalPhaseSimple {
    public long id;
    public ActuatorSignalSimple my_signal;
    public Set<RoadConnection> road_connections;
    public float signalFlowRate;

    public SignalPhaseSimple(Scenario scenario, AbstractActuator actuator, jaxb.Phase jaxb_phase) throws OTMException {
        this.id = jaxb_phase.getId();
        this.my_signal = (ActuatorSignalSimple) actuator;
        this.signalFlowRate = Float.POSITIVE_INFINITY;

        road_connections = new HashSet<>();
        for(Long id : OTMUtils.csv2longlist(jaxb_phase.getRoadconnectionIds())) {
            RoadConnection rc = (RoadConnection) scenario.get_element(ScenarioElementType.roadconnection, id);
            if(rc==null)
                throw new OTMException("bad road connection id in actuator id=" + this.id);
            road_connections.add(rc);
        }
    }

    public void validate(OTMErrorLog errorLog) {}

    public void initialize(float time) throws OTMException {}

    private void set_rc_maxflow(float time, float rate) {        
        road_connections.forEach(rc -> rc.set_external_max_flow_vps(time, rate));
    }

    public void disable(float time) {
        // DISABLE => disable flow (e.g. waiting for other phases to finish)
        set_rc_maxflow(time, 0.0f);
    }

    public void enable(float time) {
        // ENABLE => enable flow based on intersection flow rate
        set_rc_maxflow(time, signalFlowRate);
    }

    public void turn_off(float time) {
        // Turn OFF => Disable signal (ignore intersection effect)
        set_rc_maxflow(time, Float.POSITIVE_INFINITY);
    }

}

package sensor;

import common.AbstractLaneGroupLongitudinal;
import common.Link;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.Sensor;
import runner.RunParameters;
import runner.Scenario;

import java.util.HashMap;
import java.util.Map;

public class FixedSensor extends AbstractSensor {

    public float position;
    public Map<AbstractLaneGroupLongitudinal, SubSensor> subsensors;  // because a fixed sensor may span several lanegroups

    public FixedSensor(Scenario scenario, Sensor jaxb_sensor) {
        super(scenario, jaxb_sensor);

        Link link = scenario.network.links.containsKey((jaxb_sensor.getLinkId())) ?
                scenario.network.links.get(jaxb_sensor.getLinkId()) : null;

        this.position = jaxb_sensor.getPosition();

        // read lanes
        String lanes = jaxb_sensor.getLanes();
        int start_lane;
        int end_lane;
        if(lanes==null) {
            start_lane = 1;
            end_lane = link.full_lanes;
        }
        else {
            String[] strsplit = lanes.split("#");
            if (strsplit.length == 2) {
                start_lane = Integer.parseInt(strsplit[0]);
                end_lane = Integer.parseInt(strsplit[1]);
            } else {
                start_lane = -1;
                end_lane = -1;
            }
        }

        // create subsensors
        subsensors = new HashMap<>();
        for(int lane=start_lane;lane<=end_lane;lane++){
            AbstractLaneGroupLongitudinal lg = link.get_lanegroup_for_dn_lane(lane);
            SubSensor subsensor;
            if(subsensors.containsKey(lg)){
                subsensor = subsensors.get(lg);
            } else {
                subsensor = new SubSensor();
                subsensors.put(lg,subsensor);
            }
            subsensor.lanes.add(lane);
        }

        // register flow accumulators
        for(Map.Entry<AbstractLaneGroupLongitudinal, SubSensor> e : subsensors.entrySet() ){
            AbstractLaneGroupLongitudinal lg = e.getKey();
            SubSensor subsensor = e.getValue();
            subsensor.flow_accumulator = lg.request_flow_accumulator();
        }

    }

    @Override
    public void validate(OTMErrorLog errorLog) {

    }

    @Override
    public void initialize(Scenario scenario, RunParameters runParams) throws OTMException {

    }

    @Override
    public void take_measurement(Dispatcher dispatcher, float timestamp) {

    }


}

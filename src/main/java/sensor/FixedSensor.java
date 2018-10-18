package sensor;

import common.AbstractLaneGroup;
import common.Link;
import dispatch.Dispatcher;
import error.OTMException;
import jaxb.Sensor;
import runner.RunParameters;
import runner.Scenario;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FixedSensor extends AbstractSensor {

    public float position;
    public Map<AbstractLaneGroup,SubSensor> subsensors;  // because a fixed sensor may span several lanegroups
    public Measurement measurement;

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
            AbstractLaneGroup lg = link.get_lanegroup_for_dn_lane(lane);
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
        for(Map.Entry<AbstractLaneGroup, SubSensor> e : subsensors.entrySet() ){
            AbstractLaneGroup lg = e.getKey();
            SubSensor subsensor = e.getValue();
            subsensor.flow_accumulator = lg.request_flow_accumulator();
        }

    }

    @Override
    public void initialize(Scenario scenario, RunParameters runParams) throws OTMException {
        subsensors.values().forEach(x->x.initialize());
        measurement = new Measurement();
    }

    @Override
    public void take_measurement(Dispatcher dispatcher, float timestamp) {
        double total_count = 0d;
        double total_vehicles = 0d;
        for(Map.Entry<AbstractLaneGroup, SubSensor> e :  subsensors.entrySet()){
            AbstractLaneGroup lg = e.getKey();
            SubSensor subsensor = e.getValue();
            double sub_count = subsensor.flow_accumulator.get_total_count();
            total_count += sub_count - subsensor.prev_count;
            subsensor.prev_count = sub_count;
            total_vehicles += lg.get_total_vehicles();
        }
        measurement.flow = total_count;
        measurement.density = total_vehicles;

        System.out.println(String.format("%.1f\t%d\t%f\t%f",timestamp,this.getId(),measurement.flow,measurement.density));
    }

    /////////////////////////////////////////////////////////////////
    // classes
    /////////////////////////////////////////////////////////////////

    public class SubSensor {
        public Set<Integer> lanes = new HashSet<>();
        public FlowAccumulator flow_accumulator;
        public double prev_count;
        public void initialize(){
            flow_accumulator.reset();
            prev_count = 0d;
        }
    }

    public class Measurement {
        public double flow = 0d;
        public double density = 0d;
    }

}

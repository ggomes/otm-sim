package sensor;

import common.FlowAccumulatorState;
import common.Link;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.Sensor;
import common.AbstractLaneGroup;
import common.Scenario;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FixedSensor extends AbstractSensor {

    private Link link;
    private float position;
    public int start_lane;
    public int end_lane;
    private Map<AbstractLaneGroup,SubSensor> subsensors;  // because a fixed sensor may span several lanegroups

    private Measurement measurement;

    ////////////////////////////////
    // construction
    ////////////////////////////////

    public FixedSensor(Scenario scenario, Sensor jaxb_sensor) {
        super(scenario, jaxb_sensor);

        this.link = scenario.network.links.containsKey((jaxb_sensor.getLinkId())) ?
                scenario.network.links.get(jaxb_sensor.getLinkId()) : null;

        this.position = jaxb_sensor.getPosition();

        // populate measurements
        this.measurement = new Measurement();

        // read lanes
        String lanes = jaxb_sensor.getLanes();

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
                subsensor = new SubSensor(lg);
                subsensors.put(lg,subsensor);
            }
            subsensor.lanes.add(lane);
        }

    }

    ////////////////////////////////
    // InterfaceScenarioElement
    ////////////////////////////////

    @Override
    public void validate(OTMErrorLog errorLog) {

    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        measurement.initialize();
    }


    /////////////////////////////////////////////////////////////////
    // implementation
    /////////////////////////////////////////////////////////////////

    @Override
    public void take_measurement(Dispatcher dispatcher, float timestamp) {

        double total_count = 0d;
        double total_vehicles = 0d;
        for(Map.Entry<AbstractLaneGroup, SubSensor> e2 :  subsensors.entrySet()){
            AbstractLaneGroup lg = e2.getKey();
            SubSensor subsensor = e2.getValue();
            total_count += subsensor.flow_accumulator.get_total_count();
            total_vehicles += lg.get_total_vehicles();
        }

        measurement.flow_vph = (total_count-measurement.prev_count)*dt_inv;
        measurement.prev_count = total_count;
        measurement.vehicles = total_vehicles;
    }

    /////////////////////////////////////////////////////////////////
    // get
    /////////////////////////////////////////////////////////////////

    public double get_flow_vph(){
        return measurement.flow_vph;
    }

    public double get_vehicles(){
        return measurement.vehicles;
    }

    public Link get_link(){
        return link;
    }

    public float get_position(){
        return position;
    }

    /////////////////////////////////////////////////////////////////
    // classes
    /////////////////////////////////////////////////////////////////

    public class SubSensor {
        public Set<Integer> lanes;
        public FlowAccumulatorState flow_accumulator; // commodity->fa
        public SubSensor(AbstractLaneGroup lg){
            lanes = new HashSet<>();
            flow_accumulator = lg.request_flow_accumulator(null);
        }
    }

    public class Measurement {
        public double prev_count;
        public double flow_vph;
        public double vehicles;
        public void initialize(){
            flow_vph = 0d;
            vehicles = 0d;
            prev_count = 0d;
        }
    }

}

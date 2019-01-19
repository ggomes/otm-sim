package sensor;

import common.FlowAccumulatorState;
import models.AbstractLaneGroup;
import common.Link;
import dispatch.Dispatcher;
import error.OTMException;
import jaxb.Sensor;
import runner.RunParameters;
import runner.Scenario;

import java.util.*;

public class CommoditySensor extends AbstractSensor {

    private Link link;
    private float position;
    public int start_lane;
    public int end_lane;
    private Map<AbstractLaneGroup,SubSensor> subsensors;  // because a fixed sensor may span several lanegroups

    private Map<Long,Measurement> measurements; // comm_id -> measurement

    /////////////////////////////////////////////////////////////////
    // construction
    /////////////////////////////////////////////////////////////////

    public CommoditySensor(Scenario scenario, Sensor jaxb_sensor) {
        super(scenario, jaxb_sensor);

        this.link = scenario.network.links.containsKey((jaxb_sensor.getLinkId())) ?
                scenario.network.links.get(jaxb_sensor.getLinkId()) : null;

        this.position = jaxb_sensor.getPosition();

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

    @Override
    public void initialize(Scenario scenario, RunParameters runParams) throws OTMException {
        this.measurements = null;
        measurements = new HashMap<>();
        scenario.commodities.keySet().forEach(c->measurements.put(c,new Measurement()));
    }

    /////////////////////////////////////////////////////////////////
    // implementation
    /////////////////////////////////////////////////////////////////

    @Override
    public void take_measurement(Dispatcher dispatcher, float timestamp) {

        for(Map.Entry<Long,Measurement> e : measurements.entrySet()){
            Long comm_id = e.getKey();
            Measurement m = e.getValue();

            double total_count = 0d;
            double total_vehicles = 0d;
            for(Map.Entry<AbstractLaneGroup, SubSensor> e2 :  subsensors.entrySet()){
                AbstractLaneGroup lg = e2.getKey();
                SubSensor subsensor = e2.getValue();
                total_count += subsensor.flow_accumulator.get_count_for_commodity(comm_id);
                total_vehicles += lg.get_total_vehicles();
            }

            m.flow_vph = (total_count-m.prev_count)*dt_inv;
            m.prev_count = total_count;
            m.vehicles = total_vehicles;
        }
    }

    /////////////////////////////////////////////////////////////////
    // get
    /////////////////////////////////////////////////////////////////

    public double get_flow_vph(){
        return measurements.values().stream().mapToDouble(m->m.flow_vph).sum();
    }

    public double get_flow_vph(long comm_id){
        if(!measurements.containsKey(comm_id))
            return -1d;
        return measurements.get(comm_id).flow_vph;
    }

    public double get_vehicles(){
        return measurements.values().stream().mapToDouble(m->m.vehicles).sum();
    }

    public double get_vehicles(long comm_id){
        if(!measurements.containsKey(comm_id))
            return -1d;
        return measurements.get(comm_id).vehicles;
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
            flow_accumulator = lg.request_flow_accumulator();
        }
    }

    public class Measurement {
        public double prev_count = 0d;
        public double flow_vph = 0d;
        public double vehicles = 0d;
    }

}

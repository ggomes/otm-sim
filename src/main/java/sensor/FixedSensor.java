package sensor;

import core.FlowAccumulatorState;
import error.OTMErrorLog;
import core.AbstractLaneGroup;
import core.Link;
import dispatch.Dispatcher;
import error.OTMException;
import jaxb.Sensor;
import core.Scenario;
import core.AbstractModel;
import models.fluid.FluidLaneGroup;
import utils.OTMUtils;

import java.util.*;

public class FixedSensor extends AbstractSensor {

    private Link link;
    public int start_lane;
    public int end_lane;
    private float position;     // measured in meters from the downstream edge (0 is downstream sensor)
    private Set<Long> commids;

    private Map<Long,SubSensor> subsensors;  // lgid->subsensor. because a fixed sensor may span several lanegroups

    private Map<Long,Measurement> measurements; // comm_id -> measurement

    ////////////////////////////////////////
    // construction
    ////////////////////////////////////////

    public FixedSensor(float dt, Link link, int start_lane, int end_lane, float position, Set<Long> commids) throws OTMException {
        super(null, Type.fixed, dt);
        this.link = link;
        this.start_lane = start_lane;
        this.end_lane = end_lane;
        this.position = position;
        this.commids = commids;
    }

    public FixedSensor(Scenario scenario, Sensor jaxb_sensor)  throws OTMException {
        super(jaxb_sensor);

        this.position = jaxb_sensor.getPosition();
        this.link = scenario.network.links.containsKey((jaxb_sensor.getLinkId())) ?
                scenario.network.links.get(jaxb_sensor.getLinkId()) : null;
        this.target = link;

        // read lanes
        int [] x = OTMUtils.read_lanes(jaxb_sensor.getLanes(),link.get_full_lanes());
        start_lane = x[0];
        end_lane = x[1];

        this.commids = jaxb_sensor.getCommids()==null || jaxb_sensor.getCommids().isEmpty() ?
                        scenario.commodities.keySet() :
                        new HashSet<>(OTMUtils.csv2longlist(jaxb_sensor.getCommids()));
    }

    ////////////////////////////////////////
    // InterfaceScenarioElement
    ////////////////////////////////////////

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);

        if(link.get_model().type!= AbstractModel.Type.Fluid && position!=0f)
            throw new OTMException("Currently only downstream fixed sensors are allowed for non-fluid models.");

        // create subsensors
        subsensors = new HashMap<>();
        for(int lane=start_lane;lane<=end_lane;lane++){
            AbstractLaneGroup lg = link.get_lanegroup_for_dn_lane(lane);
            SubSensor subsensor;
            if(subsensors.containsKey(lg.getId())){
                subsensor = subsensors.get(lg.getId());
            } else {
                subsensor = new SubSensor(lg,position,commids);
                subsensors.put(lg.getId(),subsensor);
            }
            subsensor.lanes.add(lane);
        }

        measurements = new HashMap<>();
        for(Long c : commids)
            measurements.put(c,new Measurement());
    }

    ////////////////////////////////////////
    // implementation
    ////////////////////////////////////////

    @Override
    public void take_measurement(Dispatcher dispatcher, float timestamp) {

        for(Map.Entry<Long,Measurement> e : measurements.entrySet()){
            Long comm_id = e.getKey();
            Measurement m = e.getValue();

            double total_count = 0d;
            double total_vehicles = 0d;
            for(SubSensor subsensor:  subsensors.values()){
                total_count += subsensor.flow_accumulator.get_count_for_commodity(comm_id);
                total_vehicles += subsensor.lg.get_total_vehicles_for_commodity(comm_id);
            }

            m.flow_vph = (total_count-m.prev_count)*dt_inv;
            m.prev_count = total_count;
            m.vehicles = total_vehicles;
        }
    }

    /////////////////////////////////////////////////////////////////
    // get
    /////////////////////////////////////////////////////////////////

    // used by otm-ui
    public float get_position(){
        return position;
    }

    public double get_flow_vph(){
        return measurements==null? 0d : measurements.values().stream().mapToDouble(m->m.flow_vph).sum();
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

    /////////////////////////////////////////////////////////////////
    // classes
    /////////////////////////////////////////////////////////////////

    public class SubSensor {
        public AbstractLaneGroup lg;
        public Set<Integer> lanes;
        public FlowAccumulatorState flow_accumulator; // commodity->fa
        public SubSensor(AbstractLaneGroup lg,float position,Set<Long> commids){
            this.lg = lg;
            lanes = new HashSet<>();
            if(position==0f)
                flow_accumulator = lg.request_flow_accumulator(commids);
            else{
                FluidLaneGroup flg = (FluidLaneGroup) lg;
                int n = flg.cells.size();
                float cell_length = flg.get_length() / n;
                int cell_index = n - ((int) (position/cell_length)) - 1;
                if(cell_index<0)
                    cell_index=0;
                if(cell_index>=n)
                    cell_index=n-1;
                flow_accumulator = flg.request_flow_accumulators_for_cell(commids,cell_index);
            }
        }
    }

    public class Measurement {
        public double prev_count = 0d;
        public double flow_vph = 0d;
        public double vehicles = 0d;
        public void initialize(){
            flow_vph = 0d;
            vehicles = 0d;
            prev_count = 0d;
        }
    }

}

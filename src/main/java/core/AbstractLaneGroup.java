package core;

import actuator.*;
import core.geometry.RoadGeometry;
import core.geometry.Side;
import error.OTMErrorLog;
import error.OTMException;
import core.packet.StateContainer;
import models.Maneuver;
import traveltime.AbstractLaneGroupTimer;
import utils.OTMUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

public abstract class AbstractLaneGroup implements Comparable<AbstractLaneGroup>, InterfaceLaneGroup, InterfaceTarget {

    public static Map<Side,Maneuver> side2maneuver;

    static {
        side2maneuver = new HashMap<>();
        side2maneuver.put(Side.in,Maneuver.lcin);
        side2maneuver.put(Side.middle,Maneuver.stay);
        side2maneuver.put(Side.out,Maneuver.lcout);
    }

    protected final long id;
    protected Link link;
    protected final core.geometry.Side side;               // inner, middle, or outer (add lane in, full, add lane out)
    protected int start_lane_up = -1;       // counted with respect to upstream boundary
    protected int start_lane_dn = -1;       // counted with respect to downstream boundary
    protected int num_lanes;
    protected float length;        // [m]

    public final jaxb.Roadparam roadparam_orig;

    protected AbstractLaneGroup neighbor_in;       // lanegroup down and in
    protected AbstractLaneGroup neighbor_out;      // lanegroup down and out

    public StateContainer buffer;

    protected double longitudinal_supply;       // [veh]

    protected ActuatorLaneGroupCapacity actuator_capacity;
    protected Map<Long, ActuatorLaneGroupAllowComm> actuator_lgrestrict;   // commid->actuator

    // flow accumulator
    protected FlowAccumulatorState flw_acc;

    // one-to-one map at the lanegroup level
    protected Map<Long, RoadConnection> outlink2roadconnection;

    // state to the road connection it must use (should be avoided in the one-to-one case)
    // I SHOULD BE ABLE TO ELIMINATE THIS SINCE IT IS SIMILAR TO OUTLINK2ROADCONNECTION
    // AND ALSO ONLY USED BY THE NODE MODEL
    protected Map<State,Long> state2roadconnection;

    // target lane group to direction
    protected Map<State,Set<Maneuver>> state2lanechangedirections = new HashMap<>();
    private Map<State,Set<Maneuver>> disallowed_state2lanechangedirections = new HashMap<>();

    // lane changing
    protected Map<State , Map<Maneuver,Double>> state2lanechangeprob; // state-> maneuver -> probability

    public AbstractLaneGroupTimer travel_timer;

    ///////////////////////////////////////////////////
    // construction / destruction
    ///////////////////////////////////////////////////

    public AbstractLaneGroup(Link link, core.geometry.Side side, float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs, jaxb.Roadparam rp) throws OTMException {
        this.link = link;
        this.side = side;
        this.length = length;
        this.num_lanes = num_lanes;
        this.id = OTMUtils.get_lanegroup_id();
        this.start_lane_dn = start_lane;
        this.state2roadconnection = new HashMap<>();
        this.state2lanechangeprob = new HashMap<>();

        this.outlink2roadconnection = new HashMap<>();
        if(out_rcs!=null) {
            for (RoadConnection rc : out_rcs) {
                rc.in_lanegroups.add(this);
                if (rc.end_link!=null) {
                    if (outlink2roadconnection.containsKey(rc.end_link.getId()))
                        throw new OTMException(String.format("Link %d has multiple road connections to link %d.",link.getId(),rc.end_link.getId()));
                    outlink2roadconnection.put(rc.end_link.getId(), rc);
                }
            }
        }

        this.roadparam_orig = rp;

        try {
            set_road_params(rp);
        } catch (OTMException e) {
            assert(false);  // This should never happen
            e.printStackTrace();
        }

    }

    public void delete(){

        for(RoadConnection rc : link.end_node.road_connections)
            if(rc.start_link==link && rc.in_lanegroups.contains(this))
                rc.in_lanegroups.remove(this);

        for(RoadConnection rc : link.start_node.road_connections)
            if(rc.end_link==link && rc.out_lanegroups.contains(this))
                rc.out_lanegroups.remove(this);


        link = null;
        start_lane_up = Integer.MIN_VALUE;
        start_lane_dn = Integer.MIN_VALUE;
        length = Float.NaN;
        neighbor_in = null;
        neighbor_out = null;
        buffer = null;
        longitudinal_supply = Double.NaN;
        actuator_capacity = null;
        actuator_lgrestrict = null;
        flw_acc = null;
        state2roadconnection = null;
        state2lanechangedirections  = null;
        disallowed_state2lanechangedirections = null;
        state2lanechangeprob = null;
        travel_timer = null;
        outlink2roadconnection = null;
    }

    ///////////////////////////////////////////////////
    // InterfaceActuatorTarget
    ///////////////////////////////////////////////////

    @Override
    public String getTypeAsTarget() {
        return "lanegroup";
    }

    @Override
    public long getIdAsTarget() {
        return id;
    }

    @Override
    public AbstractModel get_model() {
        return link.model;
    }

    @Override
    public void register_actuator(Set<Long> commids,AbstractActuator act,boolean override) throws OTMException {

        if(act instanceof ActuatorLaneGroupCapacity){
            if(!override && this.actuator_capacity!=null)
                throw new OTMException(String.format("Multiple capacity actuators on link %d, lanes %d through %d.",link.getId(),start_lane_dn,start_lane_dn+num_lanes-1));
            this.actuator_capacity = (ActuatorLaneGroupCapacity) act;
        }

        if(act instanceof ActuatorLaneGroupAllowComm){
            if(actuator_lgrestrict==null)
                actuator_lgrestrict = new HashMap<>();
            for(Long commid : commids){
                if (!override && actuator_lgrestrict.containsKey(commid))
                    throw new OTMException(String.format("Lane group closure clash for commodity %d", commid));
                this.actuator_lgrestrict.put(commid, (ActuatorLaneGroupAllowComm) act);
            }
        }

        if(act instanceof ActuatorLaneGroupSpeedLimit){
            // to do
        }

    }

    ///////////////////////////////////////////////////
    // Comparable
    ///////////////////////////////////////////////////

    @Override
    public final int compareTo(AbstractLaneGroup that) {

        int this_start = this.start_lane_up;
        int that_start = that.start_lane_up;
        if(this_start < that_start)
            return -1;
        if(that_start < this_start)
            return 1;

        int this_end = this.start_lane_up + this.num_lanes;
        int that_end = that.start_lane_up + that.num_lanes;
        if(this_end < that_end)
            return -1;
        if(that_end < this_end)
            return 1;

        System.err.println("WARNING!! FOUND EQUAL LANE GROUPS IN COMPARE TO.");
        return 0;
    }

    ///////////////////////////////////////////////////
    // overridable
    ///////////////////////////////////////////////////

    public abstract void set_road_params(jaxb.Roadparam r) throws OTMException;

    public abstract jaxb.Roadparam get_road_params();

    public final void reset_road_params() throws OTMException {
        set_road_params(roadparam_orig);
    }

    public void validate_post_init(OTMErrorLog errorLog){

    }

    public void initialize(Scenario scenario, float start_time) throws OTMException {
        if(link.is_model_source_link)
            this.buffer = new StateContainer();

        if(flw_acc!=null)
            flw_acc.reset();
    }

    public final FlowAccumulatorState request_flow_accumulator(Set<Long> comm_ids){
        if(flw_acc==null)
            flw_acc = new FlowAccumulatorState();
        for(State state : link.states)
            if(comm_ids==null || comm_ids.contains(state.commodity_id))
                flw_acc.add_state(state);
        return flw_acc;
    }


    protected final void add_stateXX(State state,Long next_link_id) {

        Set<Maneuver> maneuvers;

        if(link.is_sink){
            maneuvers = new HashSet<>();
            maneuvers.add(Maneuver.stay);
            if(this.neighbor_out!=null)
                maneuvers.add(Maneuver.lcout);
            if(this.neighbor_in!=null)
                maneuvers.add(Maneuver.lcin);
        } else {
            maneuvers = link.outlink2lanegroups.get(next_link_id).stream()
                    .map(x -> x.get_side_with_respect_to_lg(this))
                    .map(x -> side2maneuver.get(x))
                    .collect(Collectors.toSet());
        }
        state2lanechangedirections.put(state, maneuvers);

        if(link.in_barriers!=null){
            if(this.side==Side.in)
                maneuvers.remove(Side.out);
            if(this.side==Side.middle)
                maneuvers.remove(Side.in);
        }

        if(link.out_barriers!=null){
            if(this.side==Side.middle)
                maneuvers.remove(Side.out);
            if(this.side==Side.out)
                maneuvers.remove(Side.in);
        }

        // state2roadconnection
        if(!link.is_sink){
            RoadConnection my_rc = outlink2roadconnection.get(next_link_id);
            if(my_rc!=null)
                state2roadconnection.put(state, my_rc.getId());
        }

        // lane changing
        Map<Maneuver,Double> x = new HashMap<>();
        double v = 1d/maneuvers.size();
        for(Maneuver m : maneuvers)
            x.put(m,v);
        state2lanechangeprob.put(state,x);

    }

    public final void update_flow_accummulators(State state, double num_vehicles){
        if(flw_acc!=null)
            flw_acc.increment(state,num_vehicles);
    }

//    public final RoadConnection get_target_road_connection_for_state(State state){
//        Long outlink_id = state.isPath ? link.path2outlink.get(state.pathOrlink_id).getId() : state.pathOrlink_id;
//        return outlink2roadconnection.get(outlink_id);
//    }

    ///////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////

    public final long getId(){
        return id;
    }

    public final Link get_link(){
        return link;
    }

    public final core.geometry.Side get_side(){
        return side;
    }

    public final int get_start_lane_up(){
        return start_lane_up;
    }

    public final int get_start_lane_dn(){
        return start_lane_dn;
    }

    public final int get_num_lanes(){
        return num_lanes;
    }

    public final float get_length(){
        return length;
    }

    public final float get_total_vehicles_for_commodity(Long commid) {
        return vehs_dwn_for_comm(commid)+vehs_in_for_comm(commid)+vehs_out_for_comm(commid);
    }

    public final float get_total_vehicles() {
        return get_total_vehicles_for_commodity(null);
    }

    @Override
    public double get_long_supply(){
        return longitudinal_supply;
    }

    public final double get_supply_per_lane(){
        return longitudinal_supply /num_lanes;
    }

    public final List<Integer> get_dn_lanes(){
        return IntStream.range(start_lane_dn,start_lane_dn+num_lanes).boxed().collect(toList());
    }

    public final List<Integer> get_up_lanes(){
        return IntStream.range(start_lane_up,start_lane_up+num_lanes).boxed().collect(toList());
    }

    public final Side get_side_with_respect_to_lg(AbstractLaneGroup lg){

        if(!this.link.getId().equals(link.getId()))
            return null;

        if(this==lg)
            return Side.middle;

        if (this.start_lane_dn < lg.start_lane_dn)
            return Side.in;
        else
            return Side.out;
    }

    public final Set<Link> get_out_links(){
        return link.end_node.out_links;
    }

    public final AbstractLaneGroup get_neighbor_in(){
        return neighbor_in;
    }

    public final AbstractLaneGroup get_neighbor_out(){
        return neighbor_out;
    }

    public RoadConnection get_rc_for_outlink(long linkid){
        return outlink2roadconnection.get(linkid);
    }

    public Long get_rc_for_state(State state){
        return state2roadconnection.get(state);
    }

    public Set<Maneuver> get_maneuvers_for_state(State state){
        return state2lanechangedirections.get(state);
    }

    public Map<Maneuver,Double> get_maneuvprob_for_state(State state){
        return state2lanechangeprob.get(state);
    }

    public final void set_lanes(int new_numlanes) throws OTMException {
        if(new_numlanes<=0)
            throw new OTMException("Cannot set lanes to a non-positive number.");
        if(new_numlanes==num_lanes)
            return;

        int old_numlanes = num_lanes;

        this.num_lanes = new_numlanes;

        // set road parameters
        try {
            set_road_params(roadparam_orig);
        } catch (OTMException e) {
            this.num_lanes = old_numlanes;
            throw new OTMException(e);
        }

        // NOTE: We will not change the start and end lanes of the road connections
        // because these seem to only affect lane group creation.

        // set start/end lanes
        int dlanes = new_numlanes-num_lanes;
        RoadGeometry rg = get_link().road_geom;
        switch(side){
            case in:
                if(neighbor_out!=null){
                    neighbor_out.start_lane_dn += dlanes;
                    if(rg.in_is_full_length())
                        neighbor_out.start_lane_up += dlanes;
                    if(neighbor_out.neighbor_out!=null){
                        neighbor_out.neighbor_out.start_lane_dn += dlanes;
                        if(rg.in_is_full_length())
                            neighbor_out.neighbor_out.start_lane_up += dlanes;
                    }
                }
                break;
            case middle:
                if(neighbor_out!=null){
                    neighbor_out.start_lane_dn += dlanes;
                    neighbor_out.start_lane_up += dlanes;
                }
                break;
        }

    }

    ///////////////////////////////////////////////////
    // lane group closures
    ///////////////////////////////////////////////////

    @Override
    public void set_actuator_allow_comm(boolean allow, Long commid) throws OTMException {
        if(allow)
            link.states.stream()
                    .filter(s->s.commodity_id==commid)
                    .forEach(s->reallow_state(s));

        else{
            for(State s : link.states)
                if(s.commodity_id==commid)
                    disallow_state(s);
        }

    }

    private void disallow_state(State state) throws OTMException {
        // disallow movement into this lanegroup from adjacent lanegroups
        this.disallow_state_lanechangedirection(state,Maneuver.stay);
        if(neighbor_in!=null)
            neighbor_in.disallow_state_lanechangedirection(state,Maneuver.lcout);
        if(neighbor_out!=null)
            neighbor_out.disallow_state_lanechangedirection(state,Maneuver.lcin);
    }

    private void reallow_state(State state){
        // reallow movement into this lanegroup from adjacent lanegroups
        this.reallow_state_lanechangedirection(state,Maneuver.stay);
        if(neighbor_in!=null)
            neighbor_in.reallow_state_lanechangedirection(state,Maneuver.lcout);
        if(neighbor_out!=null)
            neighbor_out.reallow_state_lanechangedirection(state,Maneuver.lcin);
    }

    ///////////////////////////////////////////////////
    // toString
    ///////////////////////////////////////////////////

    @Override
    public String toString() {
        return String.format("link %d, lg %d, lanes %d, start_dn %d, start_up %d",link.getId(),id,num_lanes,start_lane_dn,start_lane_up);
    }

    ///////////////////////////////////////////////////
    // private
    ///////////////////////////////////////////////////

    private void disallow_state_lanechangedirection(State state,Maneuver maneuver) {
        if(!state2lanechangedirections.containsKey(state))
            return;
        Set<Maneuver> maneuvers = state2lanechangedirections.get(state);
        if(!maneuvers.contains(maneuver))
            return;
        if(maneuvers.size()==1)
            return;
        maneuvers.remove(maneuver);
        Set<Maneuver> dmaneuvers;
        if(disallowed_state2lanechangedirections.containsKey(state)){
            dmaneuvers = disallowed_state2lanechangedirections.get(state);
        }  else {
            dmaneuvers = new HashSet<>();
            disallowed_state2lanechangedirections.put(state,dmaneuvers);
        }
        dmaneuvers.add(maneuver);

        // adjust probabilities
        if( state2lanechangeprob.containsKey(state)){
            Map<Maneuver,Double> e = state2lanechangeprob.get(state);
            if(e.containsKey(maneuver)){
                double prob = e.size()>1 ? e.get(maneuver)/(e.size()-1) : 0d;
                e.remove(maneuver);
                for(Maneuver m : e.keySet())
                    e.put( m,e.get(m)+prob);
            }
        }

    }

    private void reallow_state_lanechangedirection(State state,Maneuver maneuver) {
        if (!disallowed_state2lanechangedirections.containsKey(state))
            return;
        Set<Maneuver> dmaneuvers = disallowed_state2lanechangedirections.get(state);
        if (!dmaneuvers.contains(maneuver))
            return;
        dmaneuvers.remove(maneuver);
        Set<Maneuver> maneuvers;
        if (state2lanechangedirections.containsKey(state)) {
            maneuvers = state2lanechangedirections.get(state);
        } else {
            maneuvers = new HashSet<>();
            state2lanechangedirections.put(state, maneuvers);
        }
        maneuvers.add(maneuver);

        // add with zero probability
        if (state2lanechangeprob.containsKey(state)) {
            Map<Maneuver, Double> e = state2lanechangeprob.get(state);
            if (!e.containsKey(maneuver))
                e.put(maneuver, 0d);
        } else {
            Map<Maneuver, Double> e = new HashMap<>();
            state2lanechangeprob.put(state, e);
            e.put(maneuver, 0d);
        }
    }

}

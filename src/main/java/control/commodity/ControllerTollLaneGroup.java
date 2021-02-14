package control.commodity;

import actuator.ActuatorLaneGroupAllowComm;
import core.*;
import control.AbstractController;
import control.command.CommandRestrictionMap;
import dispatch.Dispatcher;
import error.OTMException;
import jaxb.Controller;
import lanechange.LinkLinearLaneSelector;
import models.fluid.FluidLaneGroup;
import utils.OTMUtils;
import utils.LookupTable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ControllerTollLaneGroup extends AbstractController {

    public boolean firsttime;
    public Set<Long> free_comms = new HashSet<>();
    public Set<Long> banned_comms = new HashSet<>();
    public Set<Long> tolled_comms = new HashSet<>();
    public float toll_coef;
    public float speed_threshold_meterpdt;
    public LookupTable vplpdt_to_cents_table;

    public Set<LinkInfo> linkinfos;

    public ControllerTollLaneGroup(Scenario scenario, Controller jcnt) throws OTMException {
        super(scenario, jcnt);

        if(jcnt.getParameters()!=null)
            for(jaxb.Parameter p : jcnt.getParameters().getParameter()){
                switch(p.getName()){
                    case "free_comms":
                        free_comms.addAll(OTMUtils.csv2longlist(p.getValue()));
                        break;
                    case "disallowed_comms":
                        banned_comms.addAll(OTMUtils.csv2longlist(p.getValue()));
                        break;
                    case "tolled_comms":
                        tolled_comms.addAll(OTMUtils.csv2longlist(p.getValue()));
                        break;
                    case "vplph_to_cents_table":
                        vplpdt_to_cents_table = new LookupTable(p.getValue());
                        vplpdt_to_cents_table.scaleX(dt/3600f);
                        break;
                    case "toll_coef":
                        toll_coef = Float.parseFloat(p.getValue());
                        break;
                    case "qos_speed_threshold_kph":
                        speed_threshold_meterpdt = Float.parseFloat(p.getValue()); // fix units in initialize
                        speed_threshold_meterpdt *= 1000.0*dt/3600.0;
                        break;
                }
            }
    }

    @Override
    public void configure() throws OTMException {
        this.firsttime=true;

        ActuatorLaneGroupAllowComm act = (ActuatorLaneGroupAllowComm)actuators.values().iterator().next();
        this.linkinfos = new HashSet<>();
        for(AbstractLaneGroup hotlg : ((LaneGroupSet) act.target).lgs)
            linkinfos.add(new LinkInfo(hotlg));
    }

    @Override
    public void update_command(Dispatcher dispatcher) throws OTMException {

        float timestamp = dispatcher.current_time;
        long act_id = this.actuators.keySet().iterator().next();

        // if it is the first update, then open/close the hot lane
        if(firsttime){
            Map<Long, ControllerRestrictLaneGroup.Restriction> X = new HashMap<>();
            for (Long commid : free_comms)
                X.put(commid, ControllerRestrictLaneGroup.Restriction.Open);
            for (Long commid : tolled_comms)
                X.put(commid, ControllerRestrictLaneGroup.Restriction.Open);
            for (Long commid : banned_comms)
                X.put(commid, ControllerRestrictLaneGroup.Restriction.Closed);
            command.put(act_id,new CommandRestrictionMap(X));

            // set toll on all lanegroups and commodities
            // set the tolling coefficient
            for(LinkInfo linkinfo : linkinfos){
                linkinfo.remove_all_tolls();
                linkinfo.link_lane_selector.set_toll_coeff_all_lgs_comm(this.toll_coef);
                linkinfo.update_hot_lane_toll();
            }

            firsttime=false;
        }

        // update the toll
        else if(timestamp<end_time)
            linkinfos.forEach(l->l.update_hot_lane_toll());

        // final time: open the hot lane, turn off the toll
        else {

            // open the hot lane
            Map<Long, ControllerRestrictLaneGroup.Restriction> X = new HashMap<>();
            for(Long commid : this.scenario.commodities.keySet())
                X.put(commid, ControllerRestrictLaneGroup.Restriction.Open);
            command.put(act_id,new CommandRestrictionMap(X));

            // turn off the toll
            linkinfos.forEach(l->l.remove_all_tolls());
        }

    }

    @Override
    public Class get_actuator_class() {
        return ActuatorLaneGroupAllowComm.class;
    }

    class LinkInfo {
        FlowAccumulatorState hot_fa;
        double ffspeed_meterperdt;
        LinkLinearLaneSelector link_lane_selector;
        FluidLaneGroup hotlg;
        double hot_prev_count;

        public LinkInfo(AbstractLaneGroup abshotlg) throws OTMException{

            this.hotlg = (FluidLaneGroup) abshotlg;
            this.hot_fa = hotlg.request_flow_accumulator(null);

            hot_prev_count = hot_fa.get_total_count();

            Link link = hotlg.get_link();
            if(!(link.get_lane_selsector() instanceof LinkLinearLaneSelector))
                throw new OTMException("ControllerTollLaneGroup requires the link to have an existing LinkLinearLaneSelector");

            this.link_lane_selector = (LinkLinearLaneSelector) link.get_lane_selsector();

            int numcells = hotlg.cells.size();
            double celllength_meter = hotlg.get_length() / numcells;
            ffspeed_meterperdt = (hotlg).ffspeed_cell_per_dt * celllength_meter;
            ffspeed_meterperdt *= dt/((AbstractFluidModel)hotlg.get_link().get_model()).dt_sec;
        }

        public void remove_all_tolls(){
            for(AbstractLaneGroup lg : hotlg.get_link().get_lgs())
                link_lane_selector.set_toll_all_comm(lg.getId(),0d);
        }

        public void update_hot_lane_toll(){

            // update flow in hot lane
            double count = hot_fa.get_total_count();
            double flow_vpdt = count- hot_prev_count;
            hot_prev_count = count;

            // compute speed in hot lane
            double hot_veh = hotlg.get_total_vehicles();
            double hot_speed_meterperdt = hot_veh<1 ? ffspeed_meterperdt : hotlg.get_length()*flow_vpdt/hot_veh;
            if(hot_speed_meterperdt>ffspeed_meterperdt)
                hot_speed_meterperdt = ffspeed_meterperdt;

            // get toll from lookup table
            double toll = hot_speed_meterperdt > speed_threshold_meterpdt ?
                    Double.POSITIVE_INFINITY :
                    vplpdt_to_cents_table.get_value_for((float)flow_vpdt/hotlg.get_num_lanes());

            // set toll
            for(Long commid : tolled_comms)
                link_lane_selector.set_toll(commid, hotlg.getId(), toll);
        }

    }

}

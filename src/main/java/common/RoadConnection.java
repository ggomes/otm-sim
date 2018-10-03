/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package common;

import error.OTMErrorLog;
import runner.InterfaceScenarioElement;
import runner.ScenarioElementType;
import utils.OTMUtils;

import java.util.*;
import java.util.stream.Collectors;

public class RoadConnection implements Comparable<RoadConnection>, InterfaceScenarioElement {

    protected final long id;
    private final float length;
    public final Link start_link;
    public final int start_link_from_lane;
    public final int start_link_to_lane;
    public final Link end_link;
    public final int end_link_from_lane;
    public final int end_link_to_lane;

    public Set<AbstractLaneGroup> in_lanegroups;
    public Set<AbstractLaneGroup> out_lanegroups;

    // control
    public float external_max_flow_vps;

    public RoadConnection(Map<Long,Link> links , jaxb.Roadconnection jaxb_rc ){

        id = jaxb_rc.getId();
        length = jaxb_rc.getLength();
        start_link = links.get(jaxb_rc.getInLink())==null ? null : links.get(jaxb_rc.getInLink());
        end_link = links.get(jaxb_rc.getOutLink())==null ? null : links.get(jaxb_rc.getOutLink());
        this.external_max_flow_vps = Float.POSITIVE_INFINITY;

        int [] in_lanes = OTMUtils.int_hash_int(jaxb_rc.getInLinkLanes());
        if(in_lanes!=null && in_lanes.length==2){
            start_link_from_lane = in_lanes[0];
            start_link_to_lane = in_lanes[1];
        }
        else{
            start_link_from_lane = 0;
            start_link_to_lane = 0;
        }

        if(jaxb_rc.getOutLinkLanes()!=null) {
            int[] out_lanes = OTMUtils.int_hash_int(jaxb_rc.getOutLinkLanes());
            if (out_lanes != null && out_lanes.length == 2) {
                end_link_from_lane = out_lanes[0];
                end_link_to_lane = out_lanes[1];
            } else {
                end_link_from_lane = 0;
                end_link_to_lane = 0;
            }
        }
        else{  // out_link_lanes is not defined => assign all lanes
            List<Integer> entry_lanes = end_link.get_entry_lanes();
            end_link_from_lane = entry_lanes.get(0);
            end_link_to_lane = entry_lanes.get(entry_lanes.size()-1);
        }

    }

    // This constructor is used to make fictitious road connections for one-one nodes
    public RoadConnection(long id,Link start_link,Link end_link) {

        this.id = id;
        this.length = 0f;
        this.start_link = start_link;
        this.end_link = end_link;
        this.external_max_flow_vps = Float.POSITIVE_INFINITY;

        this.start_link_from_lane = 1;
        this.start_link_to_lane = start_link.get_num_dn_lanes();
        this.end_link_from_lane = 1;
        this.end_link_to_lane = end_link.get_num_up_lanes();
    }

    public void set_in_out_lanegroups(){
        in_lanegroups = start_link.get_lanegroups_for_dn_lanes(start_link_from_lane,start_link_to_lane);
        out_lanegroups = end_link.get_lanegroups_for_up_lanes(end_link_from_lane,end_link_to_lane);
    }

    public void validate(OTMErrorLog errorLog){

        if(start_link==null)
            errorLog.addError("Bad start link");
        if(end_link==null)
            errorLog.addError("Bad end link");
        if( start_link_from_lane<=0 | start_link_to_lane<=0 |  end_link_from_lane<=0 |  end_link_to_lane<=0 )
            errorLog.addError("positivity");
        if(start_link_from_lane>start_link_to_lane)
            errorLog.addError("start_link_from_lane>start_link_to_lane");
        if(end_link_from_lane>end_link_to_lane)
            errorLog.addError("end_link_from_lane>end_link_to_lane");

        if(start_link!=null && end_link!=null){
            Node node = start_link.end_node;
            if(!node.out_links.containsKey(end_link.id))
                errorLog.addError("!node.out_links.containsKey(end_link.id)");
        }

//        if(out_lanegroups.isEmpty() || out_lanegroup_probability.isEmpty())
//            errorLog.addError("out_lanegroup_probability.isEmpty())");

        if(in_lanegroups.stream().anyMatch(x->x==null))
            errorLog.addError("null in_lanegroup in road connection " + this.getId());

        if(out_lanegroups.stream().anyMatch(x->x==null))
            errorLog.addError("null out_lanegroups in road connection " + this.getId());

        Set<Link> all_outlink = out_lanegroups.stream().map(x->x.link).collect(Collectors.toSet());
        if(all_outlink.size()!=1 || ((Link) all_outlink.toArray()[0]).id!=end_link.id)
            errorLog.addError("all_outlink.size()!=1 || ((Link) all_outlink.toArray()[0]).id!=end_link.id");

//        if(!OTMUtils.approximately_equals(out_lanegroup_probability.stream().mapToDouble(Double::new).sum(),1d))
//            errorLog.addError("splits don't add to 1");
    }

    public void set_external_max_flow_vps(float timestamp,float rate_vps){
        this.external_max_flow_vps = rate_vps;

        // tell the incoming lanegroups
        for(AbstractLaneGroup in_lanegroup : in_lanegroups)
            in_lanegroup.exiting_roadconnection_capacity_has_been_modified(timestamp);
    }

    @Override
    public String toString() {
        String str = "";
        str += "from\t" + start_link.id + ":" + start_link_from_lane +"-"+ start_link_to_lane + "\n";
        str += "to\t" + end_link.id + ":" + end_link_from_lane +"-"+ end_link_to_lane + "\n";
        str += "out lanegroups:\n";
//        for(int i=0;i<out_lanegroups.size();i++)
//            str+= out_lanegroups.get(i).id + " (" + out_lanegroup_probability.get(i) + ")\n";
        return str;
    }

    public jaxb.Roadconnection to_jaxb(){
        jaxb.Roadconnection jrcn = new jaxb.Roadconnection();
        jrcn.setId(this.getId());
        jrcn.setInLink(this.start_link.getId());
        jrcn.setInLinkLanes(this.start_link_from_lane + "#" + this.start_link_to_lane);
//            jrcn.setLength(rcn);
        jrcn.setOutLink(this.end_link.getId());
        jrcn.setOutLinkLanes(this.end_link_from_lane + "#" + this.end_link_to_lane);
        return jrcn;
    }

    @Override
    public int compareTo(RoadConnection that) {
        if(this.id>that.id)
            return 1;
        if(this.id<that.id)
            return -1;
        return 0;
    }

    ////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public ScenarioElementType getScenarioElementType() {
        return ScenarioElementType.roadconnection;
    }

}

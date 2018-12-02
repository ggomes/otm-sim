/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package common;

import error.OTMErrorLog;
import models.AbstractLaneGroup;
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

    public RoadConnection(final Map<Long,Link> links , jaxb.Roadconnection jaxb_rc ){

        id = jaxb_rc.getId();
        length = jaxb_rc.getLength();
        start_link = links.get(jaxb_rc.getInLink())==null ? null : links.get(jaxb_rc.getInLink());
        end_link = links.get(jaxb_rc.getOutLink())==null ? null : links.get(jaxb_rc.getOutLink());
        external_max_flow_vps = Float.POSITIVE_INFINITY;

        if(jaxb_rc.getInLinkLanes()!=null) {
            int [] in_lanes = OTMUtils.int_hash_int(jaxb_rc.getInLinkLanes());
            if(in_lanes!=null && in_lanes.length==2){
                start_link_from_lane = in_lanes[0];
                start_link_to_lane = in_lanes[1];
            }
            else{
                start_link_from_lane = 0;
                start_link_to_lane = 0;
            }
        } else { // in_link_lanes is not defined => assign all lanes
            start_link_from_lane = 1;
            start_link_to_lane = start_link.get_num_dn_lanes();
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
            end_link_from_lane = 1;
            end_link_to_lane = end_link.get_num_up_lanes();
        }

    }

    // This constructor is used to make fictitious road connections for one-one nodes
    public RoadConnection(long id,Link start_link,int start_link_from_lane,int start_link_to_lane,Link end_link,int end_link_from_lane,int end_link_to_lane) {
        this.id = id;
        this.length = 0f;
        this.start_link = start_link;
        this.end_link = end_link;
        this.external_max_flow_vps = Float.POSITIVE_INFINITY;
        this.start_link_from_lane = start_link_from_lane;
        this.start_link_to_lane = start_link_to_lane;
        this.end_link_from_lane = end_link_from_lane;
        this.end_link_to_lane = end_link_to_lane;
    }

    // This constructor is used to make fictitious road connections for one-one nodes
    public RoadConnection(long id,Link start_link,Link end_link) {
        this( id, start_link, 1,start_link.get_num_dn_lanes(),end_link,1,end_link.get_num_up_lanes());
    }

//    public void set_in_out_lanegroups(){
//        in_lanegroups = start_link !=null ?
//                start_link.get_unique_lanegroups_for_dn_lanes(start_link_from_lane,start_link_to_lane) :
//                new HashSet<>();
//
//        out_lanegroups = end_link!=null ?
//                end_link.get_unique_lanegroups_for_up_lanes(end_link_from_lane,end_link_to_lane) :
//                new HashSet<>();
//    }

    public void validate(OTMErrorLog errorLog){

        if (start_link!=null && end_link!=null && start_link.end_node!=end_link.start_node ) {
            System.err.println("bad road connection: id=" + id
                    + " start_link = " + start_link.getId()
                    + " end_link = " + end_link.getId()
                    + " start_link.end_node = " + start_link.end_node.getId()
                    + " end_link.start_node = " + end_link.start_node.getId() );
        }

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

        if(!out_lanegroups.isEmpty()){

            if(out_lanegroups.stream().anyMatch(x->x==null))
                errorLog.addError("null out_lanegroups in road connection " + this.getId());

            Set<Link> all_outlink = out_lanegroups.stream().map(x->x.link).collect(Collectors.toSet());
            if(all_outlink.size()>1)
                errorLog.addError("all_outlink.size()>1");
            if(all_outlink.iterator().next().id!=end_link.id)
                errorLog.addError("all_outlink.iterator().next().id!=end_link.id");
        }

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
        return String.format("%d [%d %d] -> %d [%d %d]",start_link.getId(),start_link_from_lane,start_link_to_lane,end_link.getId(),end_link_from_lane,end_link_to_lane);
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

    ///////////////////////////////////////////
    // get
    ///////////////////////////////////////////

    public boolean has_start_link(){
        return start_link!=null;
    }

    public boolean has_end_link(){
        return end_link!=null;
    }

    public Long get_start_link_id(){
        return start_link==null ? null : start_link.getId();
    }

    public Link get_start_link(){
        return start_link;
    }

    public Link get_end_link(){
        return end_link;
    }

    public Long get_end_link_id(){
        return end_link==null ? null : end_link.getId();
    }

    ///////////////////////////////////////////
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

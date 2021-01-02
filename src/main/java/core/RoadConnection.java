package core;

import error.OTMErrorLog;
import utils.OTMUtils;

import java.util.*;

import static java.util.stream.Collectors.toSet;

public class RoadConnection implements Comparable<RoadConnection>, InterfaceScenarioElement {

    protected final long id;
    protected final Link start_link;
    protected final int start_link_from_lane;
    protected final int start_link_to_lane;
    protected final Link end_link;
    protected final int end_link_from_lane;
    protected final int end_link_to_lane;

    protected Set<AbstractLaneGroup> in_lanegroups= new HashSet<>();
    protected Set<AbstractLaneGroup> out_lanegroups= new HashSet<>();

    ///////////////////////////////////////////
    // construction
    ///////////////////////////////////////////

    public RoadConnection(final Map<Long,Link> links , jaxb.Roadconnection jaxb_rc ){

        id = jaxb_rc.getId();
        start_link = links.get(jaxb_rc.getInLink())==null ? null : links.get(jaxb_rc.getInLink());
        end_link = links.get(jaxb_rc.getOutLink())==null ? null : links.get(jaxb_rc.getOutLink());

        if(jaxb_rc.getInLinkLanes()==null) {
            start_link_from_lane = 1;
            start_link_to_lane = start_link.get_num_dn_lanes();
        } else {
            int [] in_lanes = OTMUtils.int_hash_int(jaxb_rc.getInLinkLanes());
            start_link_from_lane = in_lanes[0]!=0 ? in_lanes[0] : 1;
            start_link_to_lane   = in_lanes[1]!=0 ? in_lanes[1] : start_link.get_num_dn_lanes();
        }

        if(jaxb_rc.getOutLinkLanes()==null) {
            end_link_from_lane = 1;
            end_link_to_lane = end_link.get_num_dn_lanes();
        } else {
            int [] out_lanes = OTMUtils.int_hash_int(jaxb_rc.getOutLinkLanes());
            end_link_from_lane = out_lanes[0]!=0 ? out_lanes[0] : 1;
            end_link_to_lane   = out_lanes[1]!=0 ? out_lanes[1] : end_link.get_num_up_lanes();

        }

    }

    // This constructor is used to make fictitious road connections for one-one nodes
    public RoadConnection(long id,Link start_link,int start_link_from_lane,int start_link_to_lane,Link end_link,int end_link_from_lane,int end_link_to_lane) {
        this.id = id;
        this.start_link = start_link;
        this.end_link = end_link;
        this.start_link_from_lane = start_link_from_lane;
        this.start_link_to_lane = start_link_to_lane;
        this.end_link_from_lane = end_link_from_lane;
        this.end_link_to_lane = end_link_to_lane;
    }

    // This constructor is used to make fictitious road connections for one-one nodes
    public RoadConnection(long id,Link start_link,Link end_link) {
        this( id, start_link, 1,start_link.get_num_dn_lanes(),end_link,1,end_link.get_num_up_lanes());
    }

    ///////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////

    @Override
    public final Long getId() {
        return id;
    }

    @Override
    public final ScenarioElementType getSEType() {
        return ScenarioElementType.roadconnection;
    }

    @Override
    public void validate(OTMErrorLog errorLog) {

        if (start_link!=null && end_link!=null && start_link.end_node!=end_link.start_node ) {
            System.err.println("bad road connection: id=" + id
                    + " start_link = " + start_link.getId()
                    + " end_link = " + end_link.getId()
                    + " start_link.end_node = " + start_link.end_node.getId()
                    + " end_link.start_node = " + end_link.start_node.getId() );
        }

        if( start_link_from_lane<=0 )
            errorLog.addError(String.format("Road connection %d: non-positive start link from lane number.",id));

        if( start_link_to_lane<=0 )
            errorLog.addError(String.format("Road connection %d: non-positive start link to lane number.",id));

        if( end_link_from_lane<=0 )
            errorLog.addError(String.format("Road connection %d: non-positive end link from lane number.",id));

        if( end_link_to_lane<=0 )
            errorLog.addError(String.format("Road connection %d: non-positive end link to lane number.",id));

        if(start_link_from_lane>start_link_to_lane)
            errorLog.addError(String.format("Road connection %d: start_link_from_lane>start_link_to_lane.",id));

        if(end_link_from_lane>end_link_to_lane)
            errorLog.addError(String.format("Road connection %d: end_link_from_lane>end_link_to_lane.",id));

        if(start_link!=null && end_link!=null){
            Node node = start_link.end_node;
            if(!node.out_links.contains(end_link))
                errorLog.addError(String.format("Road connection %d: end link not an outlink of inlink's end node.",id));
        }

        if(in_lanegroups.stream().anyMatch(x->x==null))
            errorLog.addError("null in_lanegroup in road connection " + this.getId());

        if(!out_lanegroups.isEmpty()){

            if(out_lanegroups.stream().anyMatch(x->x==null))
                errorLog.addError("null out_lanegroups in road connection " + this.getId());

            Set<Link> all_outlink = out_lanegroups.stream().map(x->x.link).collect(toSet());
            if(all_outlink.size()>1)
                errorLog.addError("all_outlink.size()>1");
            if(all_outlink.iterator().next().id!=end_link.id)
                errorLog.addError("all_outlink.iterator().next().id!=end_link.id");
        }

    }

    @Override
    public jaxb.Roadconnection to_jaxb(){
        jaxb.Roadconnection jrcn = new jaxb.Roadconnection();
        jrcn.setId(this.getId());
        jrcn.setInLink(this.start_link.getId());
        jrcn.setInLinkLanes(this.start_link_from_lane + "#" + this.start_link_to_lane);
        jrcn.setOutLink(this.end_link.getId());
        jrcn.setOutLinkLanes(this.end_link_from_lane + "#" + this.end_link_to_lane);
        return jrcn;
    }

    ///////////////////////////////////////////////////
    // Comparable
    ///////////////////////////////////////////////////

    @Override
    public int compareTo(RoadConnection that) {
        if(this.id>that.id)
            return 1;
        if(this.id<that.id)
            return -1;
        return 0;
    }

    ///////////////////////////////////////
    // toString
    ///////////////////////////////////////

    @Override
    public String toString() {
        String startstr = start_link==null ? "" : String.format("%d [%d %d]",start_link.getId(), start_link_from_lane, start_link_to_lane);
        String endstr = end_link==null ? "" : String.format("%d [%d %d]",end_link.getId(),end_link_from_lane,end_link_to_lane);
        return String.format("%d: %s -> %s",id,startstr,endstr);
    }

    ///////////////////////////////////////////
    // API
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

    public int get_in_lanes(){
        return start_link_to_lane-start_link_from_lane+1;
    }

    public int get_end_link_from_lane(){
        return end_link_from_lane;
    }

    public int get_end_link_to_lane(){
        return end_link_to_lane;
    }

    public Set<AbstractLaneGroup> get_in_lanegroups(){
        return in_lanegroups;
    }

    public Set<AbstractLaneGroup> get_out_lanegroups(){
        return out_lanegroups;
    }

}

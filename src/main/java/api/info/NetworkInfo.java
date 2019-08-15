package api.info;

import common.Network;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class NetworkInfo {

    /** all node information */
    public List<NodeInfo> nodes = new ArrayList<>();

    /** all link information */
    public List<LinkInfo> links = new ArrayList<>();

    public NetworkInfo(Network network){
        network.nodes.values().forEach(x -> nodes.add(new NodeInfo(x)));
        network.links.values().forEach(x -> links.add(new LinkInfo(x)));
    }

    /**
     * Get link information for the link that contains a given lanegroup.
     * @param lanegroup_id : integer id of the lanegroup.
     * @return LinkInfo
     */
    public LinkInfo get_link_for_lanegroup(long lanegroup_id){
        for(LinkInfo linkinfo : links)
            if( linkinfo.lanegroups.stream().map(x->x.id).anyMatch(x->x==lanegroup_id) )
                return linkinfo;
        return null;
    }

    /**
     * Get link information for a given link id.
     * @param link_id : integer id of the link.
     * @return LinkInfo
     */
    public LinkInfo get_linkinfo(double link_id){
        Set<LinkInfo> link_infos = links.stream().filter(x->x.id==link_id).collect(Collectors.toSet());
        return link_infos.isEmpty() ? null : (LinkInfo) link_infos.toArray()[0];
    }

    public List<NodeInfo> getNodes() {
        return nodes;
    }

    public List<LinkInfo> getLinks() {
        return links;
    }

    @Override
    public String toString() {
        return "NetworkInfo{" +
                "nodes=" + nodes +
                ", links=" + links +
                '}';
    }
}

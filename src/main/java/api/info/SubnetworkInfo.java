package api.info;

import commodity.Path;
import commodity.Subnetwork;
import common.Link;
import error.OTMException;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class SubnetworkInfo {

    /** Integer id of the subnetwork. */
    public long id;

    /** True iff the subnetwork is a path. If true, then the links will be ordered **/
    public boolean is_path;

    /** List of link ids in this subnetwork. */
    public List<Long> link_ids =  new ArrayList<>();

    public SubnetworkInfo(Subnetwork x){
        this.id = x.getId();
        this.is_path = x.is_path;

        if( x.is_path ){
            Path path = (Path) x;
            link_ids = path.ordered_links.stream().map(z->z.getId()).collect(toList());
        } else {
            x.links.forEach(l->link_ids.add(l.getId()));
        }
    }

    public SubnetworkInfo(Path x){
        this.id = x.getId();

        for(Link link : x.ordered_links)
            link_ids.add(link.getId());

//        this.link_ids = x.ordered_links.stream().map(z->z.getId()).collect(toList());
        this.is_path = true;
    }

    /**
     * Get the id of the subntework.
     * @return Undocumented
     */
    public long getId() {
        return id;
    }

    /**
     * True if the subnetwork is a path
     * @return Undocumented
     */
    public boolean isPath() {
        return is_path;
    }

    /**
     * Get list of ids in the subnetwork. It is ordered if the subnetwork is a path
     *
     * @return Undocumented
     */
    public List<Long> get_link_ids() {
        return link_ids;
    }

    @Override
    public String toString() {
        return "SubnetworkInfo{" +
                "id=" + id +
                "is_path=" + is_path +
                ", link_ids xxx=" + link_ids +
                '}';
    }
}

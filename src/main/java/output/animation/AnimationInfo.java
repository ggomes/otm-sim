package output.animation;

import core.Link;
import error.OTMException;
import core.Scenario;

import java.util.*;
import java.util.stream.Collectors;

public class AnimationInfo {

    public float timestamp;
    public Map<Long,AbstractLinkInfo> link_info;

    //////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////

    public AnimationInfo(Scenario scenario) throws OTMException {
        this.timestamp = scenario.dispatcher.current_time;
        this.link_info = populate_link_info(scenario.network.links.values());
    }

    public AnimationInfo(Scenario scenario,List<Long> link_ids) throws OTMException {
        this.timestamp = scenario.dispatcher.current_time;
        this.link_info = populate_link_info(link_ids.stream().map(
                x->scenario.network.links.get(x)).collect(Collectors.toList()));
    }

    //////////////////////////////////////////////////
    // get
    //////////////////////////////////////////////////

    public AbstractLinkInfo get_link_info(long link_id){
        return link_info.containsKey(link_id) ? link_info.get(link_id) : null;
    }

    public Map<Long,Double> get_total_vehicles_per_link(){
        Map<Long,Double> x = new HashMap<>();
        for(Map.Entry<Long,AbstractLinkInfo> e : link_info.entrySet())
            x.put(e.getKey(),e.getValue().get_total_vehicles());
        return x;
    }

    //////////////////////////////////////////////////
    // private
    //////////////////////////////////////////////////

    private Map<Long,AbstractLinkInfo> populate_link_info(Collection<Link> links) throws OTMException {
        Map<Long,AbstractLinkInfo> x = new HashMap<>();
//        for(Link link : links){
//            switch(link.model.type){
//                case Fluid:
//                    x.put(link.getId(),new output.animation.macro.LinkInfo(link));
//                    break;
//                case Vehicle:
//                    System.err.println("This is not implemented");
//                    x.put(link.getId(),new output.animation.micro.LinkInfo(link));
//                    break;
//                default:
//                    throw new OTMException("Unknown or missing model type");
//            }
//            x.put(link.getId(),link.model.get_link_info(link));
//        }
        return x;
    }

    @Override
    public String toString() {
        String str = "";
        str += "time = " + timestamp + "\n";
        for(AbstractLinkInfo li : link_info.values())
            str += li;
        return str;
    }

}

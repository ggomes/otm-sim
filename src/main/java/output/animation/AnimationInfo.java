package output.animation;

import common.Link;
import error.OTMException;
import runner.Scenario;

import java.util.*;
import java.util.stream.Collectors;

public class AnimationInfo {

    public float timestamp;
    public Map<Long,AbstractLinkInfo> link_info;

    public AnimationInfo(Scenario scenario) throws OTMException {
        this.timestamp = scenario.get_current_time();
        this.link_info = populate_link_info(scenario.network.links.values());
    }

    public AnimationInfo(Scenario scenario,List<Long> link_ids) throws OTMException {
        this.timestamp = scenario.get_current_time();
        this.link_info = populate_link_info(link_ids.stream().map(
                x->scenario.network.links.get(x)).collect(Collectors.toList()));
    }

    private Map<Long,AbstractLinkInfo> populate_link_info(Collection<Link> links) throws OTMException {
        Map<Long,AbstractLinkInfo> x = new HashMap<>();
        for(Link link : links){
            switch(link.model_type){
                case mn:
                case ctm:
                    x.put(link.getId(),new output.animation.macro.LinkInfo(link));
                    break;
                case pq:
                    System.err.println("This is not implemented");
                    x.put(link.getId(),new output.animation.meso.LinkInfo(link));
                    break;
                case micro:
                    System.err.println("This is not implemented");
                    x.put(link.getId(),new output.animation.micro.LinkInfo(link));
                    break;
                default:
                    throw new OTMException("Unknown model_type");
            }
        }
        return x;
    }

    public Map<Long,Double> get_total_vehicles_per_link(){
        Map<Long,Double> x = new HashMap<>();
        for(Map.Entry<Long,AbstractLinkInfo> e : link_info.entrySet())
            x.put(e.getKey(),e.getValue().get_total_vehicles());
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

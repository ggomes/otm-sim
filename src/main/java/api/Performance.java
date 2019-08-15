package api;

import runner.Scenario;

public class Performance {

    private Scenario scenario;
    protected Performance(Scenario scenario){
        this.scenario = scenario;
    }


//    public PerformanceInfo get_performance(){
//        return get_performance_for_commodity(null);
//    }
//
//    public PerformanceInfo get_performance_for_commodity(Long commodity_id) {
//
//        // get commodity
//        Commodity commodity;
//        Collection<Link> links;
//        if (commodity_id == null){
//            commodity = null;
//            links = scenario.network.links.values();
//        }
//        else{
//            commodity = scenario.commodities.get(commodity_id);
//            if(commodity==null)
//                return null;
//            links = commodity.all_links;
//        }
//        return new PerformanceInfo(commodity,links);
//    }

}

package models.none;

import commodity.Commodity;
import commodity.Path;
import core.AbstractDemandGenerator;
import core.Link;
import core.RoadConnection;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import geometry.Side;
import jaxb.OutputRequest;
import core.AbstractLaneGroup;
import models.AbstractModel;
import output.AbstractOutput;
import output.animation.AbstractLinkInfo;
import core.Scenario;
import profiles.Profile1D;
import utils.StochasticProcess;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class ModelNone extends AbstractModel {

    public ModelNone(String name, boolean is_default, StochasticProcess process, jaxb.ModelParams param) {
        super(AbstractModel.Type.None,name, is_default, process);
    }

    //////////////////////////////////////////////////////////////
    // abstract in AbstractModel
    //////////////////////////////////////////////////////////////

    @Override
    public void reset(Link link){ }

    @Override
    public void build() throws OTMException { }

    @Override
    public void register_with_dispatcher(Scenario scenario, Dispatcher dispatcher, float start_time){
    }

    @Override
    public AbstractDemandGenerator create_source(Link origin, Profile1D profile, Commodity commodity, Path path){
        return new NoneDemandGenerator(origin,profile,commodity,path);
    }

    //////////////////////////////////////////////////////////////
    // InterfaceModel
    //////////////////////////////////////////////////////////////

    @Override
    public void validate(OTMErrorLog errorLog) {

    }

    @Override
    public AbstractOutput create_output_object(Scenario scenario, String prefix, String output_folder, OutputRequest jaxb_or)  throws OTMException{
        return null;
    }

    @Override
    public AbstractLaneGroup create_lane_group(Link link, Side side, Float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs,jaxb.Roadparam rp){
        return new NoneLaneGroup(link,side,length,num_lanes,start_lane,out_rcs,rp);
    }

    @Override
    public Map<AbstractLaneGroup,Double> lanegroup_proportions(Collection<? extends AbstractLaneGroup> candidate_lanegroups){
        return null;
    }

    @Override
    public AbstractLinkInfo get_link_info(Link link){
        return null;
    }

}

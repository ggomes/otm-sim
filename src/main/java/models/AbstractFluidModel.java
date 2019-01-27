package models;

import common.Link;
import dispatch.*;
import error.OTMException;
import runner.Scenario;

public abstract class AbstractFluidModel extends AbstractModel {

    public float dt;

    public AbstractFluidModel(String name, boolean is_default, float dt) {
        super(name, is_default);
        this.dt = dt;
    }

    //////////////////////////////////////////////////////////////
    // load
    //////////////////////////////////////////////////////////////

    @Override
    public void set_road_param(Link link, jaxb.Roadparam r) {
        super.set_road_param(link,r);
        // send parameters to lane groups
        for(AbstractLaneGroup lg : link.lanegroups_flwdn.values())
            lg.set_road_params(r);
    }

    //////////////////////////////////////////////////////////////
    // run
    //////////////////////////////////////////////////////////////

    abstract public void update_flux_I(float timestamp) throws OTMException;
    abstract public void update_flux_II(float timestamp) throws OTMException;
    abstract public void update_link_state(Float timestamp,Link link);

    @Override
    public void register_with_dispatcher(Scenario scenario, Dispatcher dispatcher, float start_time){
        dispatcher.register_event(new EventFluidFluxUpdate(dispatcher, start_time + dt, this));
        dispatcher.register_event(new EventFluidStateUpdate(dispatcher, start_time + dt, this));
    }

    public void update_macro_state(Float timestamp) throws OTMException {
        for(Link link : links)
            update_link_state( timestamp, link);
    }

    public void update_macro_flow(Float timestamp) throws OTMException {
        update_flux_I(timestamp);

        // -- MPI communication (in otm-mpi) -- //

        update_flux_II(timestamp);
    }

}

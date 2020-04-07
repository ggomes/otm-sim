package geometry;

import common.Scenario;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.Roadparam;
import common.InterfaceScenarioElement;
import common.ScenarioElementType;

import java.util.List;
import java.util.Map;

public class RoadGeometry implements InterfaceScenarioElement {

    final public long id;
    public AddLanes up_in;
    public AddLanes up_out;
    public AddLanes dn_in;
    public AddLanes dn_out;

    ///////////////////////////////////////////
    // construction
    ///////////////////////////////////////////

    public RoadGeometry(jaxb.Roadgeom jaxb_geom,Map<Long, Roadparam> road_params) throws OTMException {

        // null road geom
        if(jaxb_geom==null) {
            id = Long.MIN_VALUE;
            return;
        }

        this.id = jaxb_geom.getId();

        // Warning: this does not check for repetitions
        for(jaxb.AddLanes jaxb_al : jaxb_geom.getAddLanes()){

            Roadparam rp = null;
            if( jaxb_al.getRoadparam()!=null && road_params.containsKey(jaxb_al.getRoadparam()))
                rp = road_params.get(jaxb_al.getRoadparam());

            AddLanes addlane = new AddLanes(jaxb_al,rp);
            if(addlane.isUp())
                if(addlane.isIn())
                    up_in = addlane;
                else
                    up_out = addlane;
            else
                if(addlane.isIn())
                    dn_in = addlane;
                else
                    dn_out = addlane;
        }
    }

    ///////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////

    @Override
    public final Long getId() {
        return id;
    }

    @Override
    public final ScenarioElementType getType() {
        return ScenarioElementType.roadgeom;
    }

    @Override
    public void validate(OTMErrorLog errorLog) {

        if(dn_in!=null || dn_out!=null || up_in!=null || up_out!=null) {
            errorLog.addWarning("Road geometry " + id + ": Addlanes has not been implemented.");
            return;
        }

        if( dn_in !=null )
            dn_in.validate(errorLog);
        if( dn_out !=null )
            dn_out.validate(errorLog);
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {

    }

    @Override
    public void register_with_dispatcher(Dispatcher dispatcher) {

    }

    @Override
    public jaxb.Roadgeom to_jaxb(){
        jaxb.Roadgeom jgeom = new jaxb.Roadgeom();
        jgeom.setId(this.id);
        List<jaxb.AddLanes> jaddlanes = jgeom.getAddLanes();
        if(this.up_in.lanes>0)
            jaddlanes.add(this.up_in.to_jaxb());
        if(this.dn_in.lanes>0)
            jaddlanes.add(this.dn_in.to_jaxb());
        if(this.up_out.lanes>0)
            jaddlanes.add(this.up_out.to_jaxb());
        if(this.dn_out.lanes>0)
            jaddlanes.add(this.dn_out.to_jaxb());
        return jgeom;
    }

    ///////////////////////////////////////////
    // public
    ///////////////////////////////////////////

    public boolean in_is_full_length(){
        return dn_in==null ? false : Float.isNaN(dn_in.length);
    }

    public boolean out_is_full_length(){
        return dn_out==null ? false : Float.isNaN(dn_out.length);
    }

}

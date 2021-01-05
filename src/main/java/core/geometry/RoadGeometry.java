package core.geometry;

import error.OTMErrorLog;
import error.OTMException;
import jaxb.Roadparam;
import core.InterfaceScenarioElement;
import core.ScenarioElementType;

import java.util.List;
import java.util.Map;

public class RoadGeometry implements InterfaceScenarioElement {

    final public long id;
    public AddLanes in;
    public AddLanes out;

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
            if(addlane.isIn())
                in = addlane;
            else
                out = addlane;
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
    public final ScenarioElementType getSEType() {
        return ScenarioElementType.roadgeom;
    }

    @Override
    public void validate(OTMErrorLog errorLog) {
        if( in !=null )
            in.validate(errorLog);
        if( out !=null )
            out.validate(errorLog);
    }

    @Override
    public jaxb.Roadgeom to_jaxb(){
        jaxb.Roadgeom jgeom = new jaxb.Roadgeom();
        jgeom.setId(this.id);
        List<jaxb.AddLanes> jaddlanes = jgeom.getAddLanes();
        if(in !=null && in.lanes>0)
            jaddlanes.add(this.in.to_jaxb());
        if(out !=null && out.lanes>0)
            jaddlanes.add(this.out.to_jaxb());
        return jgeom;
    }

    ///////////////////////////////////////////
    // public
    ///////////////////////////////////////////

    public boolean in_is_full_length(){
        return in ==null ? false : Float.isNaN(in.length);
    }

    public boolean out_is_full_length(){
        return out ==null ? false : Float.isNaN(out.length);
    }

}

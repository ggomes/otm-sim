/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package geometry;

import error.OTMErrorLog;
import runner.InterfaceScenarioElement;
import runner.Scenario;
import runner.ScenarioElementType;

import java.util.List;

public class RoadGeometry implements InterfaceScenarioElement {

    final public long id;
    public AddLanes up_left  = new AddLanes(AddLanes.Position.up,AddLanes.Side.l);
    public AddLanes up_right = new AddLanes(AddLanes.Position.up,AddLanes.Side.r);
    public AddLanes dn_left  = new AddLanes(AddLanes.Position.dn,AddLanes.Side.l);
    public AddLanes dn_right = new AddLanes(AddLanes.Position.dn,AddLanes.Side.r);

    public RoadGeometry(jaxb.Roadgeom jaxb_geom) {

        // null road geom
        if(jaxb_geom==null) {
            id = Long.MIN_VALUE;
            return;
        }

        this.id = jaxb_geom.getId();

        // Warning: this does not check for repetitions
        for(jaxb.AddLanes jaxb_al : jaxb_geom.getAddLanes()){
            AddLanes addlane = new AddLanes(jaxb_al);
            if(addlane.isUp())
                if(addlane.isLeft())
                    up_left = addlane;
                else
                    up_right = addlane;
            else
                if(addlane.isLeft())
                    dn_left = addlane;
                else
                    dn_right = addlane;

        }
    }

    public void validate(OTMErrorLog errorLog){
        if( up_left.lanes>0 )
            errorLog.addError("Dont know how to deal with upstream addlanes");
        if( up_right.lanes>0 )
            errorLog.addError("Dont know how to deal with upstream addlanes");
        if( dn_left!=null )
            dn_left.validate(errorLog);
        if( dn_right!=null )
            dn_right.validate(errorLog);
    }

    public jaxb.Roadgeom to_jaxb(){
        jaxb.Roadgeom jgeom = new jaxb.Roadgeom();
        jgeom.setId(this.id);
        List<jaxb.AddLanes> jaddlanes = jgeom.getAddLanes();
        if(this.up_left.lanes>0)
            jaddlanes.add(this.up_left.to_jaxb());
        if(this.dn_left.lanes>0)
            jaddlanes.add(this.dn_left.to_jaxb());
        if(this.up_right.lanes>0)
            jaddlanes.add(this.up_right.to_jaxb());
        if(this.dn_right.lanes>0)
            jaddlanes.add(this.dn_right.to_jaxb());
        return jgeom;
    }

    ////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public ScenarioElementType getScenarioElementType() {
        return ScenarioElementType.roadgeom;
    }

}

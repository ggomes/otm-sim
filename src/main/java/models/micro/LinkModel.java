/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package models.micro;

import common.*;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.Roadparam;
import packet.PacketLink;
import runner.Scenario;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class LinkModel extends AbstractLinkModel {

    public LinkModel(Link link) {
        super(link);
    }

    @Override
    public void reset() {
        System.out.println("IMPLEMENT THIS");
    }

    @Override
    public void set_road_param(Roadparam r, float sim_dt_sec) {
        System.out.println("models.ctm.micro.set_road_param");
    }

    @Override
    public void add_vehicle_packet(float timestamp, PacketLink vp) throws OTMException {
        System.out.println(timestamp + "\t models.ctm.micro.add_native_vehicle_packet");
    }

    @Override
    public void validate(OTMErrorLog errorLog) {
        System.out.println("Validate models.ctm.micro link model");
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        System.out.println("Initialize models.ctm.micro link model");
    }

    @Override
    public Map<AbstractLaneGroup, Double> lanegroup_proportions(Collection<? extends AbstractLaneGroup> candidate_lanegroups) {
        return null;
    }

}

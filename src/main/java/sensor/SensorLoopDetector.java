/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package sensor;

import common.Link;
import dispatch.Dispatcher;
import error.OTMErrorLog;
import error.OTMException;
import jaxb.Sensor;
import runner.RunParameters;
import runner.Scenario;

public class SensorLoopDetector extends AbstractSensor {

    public float position;
    public int start_lane;
    public int end_lane;
    public Link link;

    public SensorLoopDetector(Scenario scenario, Sensor jaxb_sensor) {
        super(scenario, jaxb_sensor);
        this.link = scenario.network.links.containsKey((jaxb_sensor.getLinkId())) ?
                scenario.network.links.get(jaxb_sensor.getLinkId()) : null;

        this.position = jaxb_sensor.getPosition();

        String lanes = jaxb_sensor.getLanes();
        if(lanes==null) {
            start_lane = 1;
            end_lane = link.full_lanes;
        }
        else {
            String[] strsplit = lanes.split("#");
            if (strsplit.length == 2) {
                start_lane = Integer.parseInt(strsplit[0]);
                end_lane = Integer.parseInt(strsplit[1]);
            } else {
                start_lane = -1;
                end_lane = -1;
            }
        }
    }

    @Override
    public void validate(OTMErrorLog errorLog) {
        if(link==null)
            errorLog.addWarning("Sensor has no link");
        if(this.position<0 || this.position>link.length)
            errorLog.addError("Sensor is placed outside of its link");
    }

    @Override
    public void initialize(Scenario scenario, RunParameters runParams) throws OTMException {

    }

    @Override
    public void take_measurement(Dispatcher dispatcher, float timestamp) {

        // TODO Measure flow and density from link
    }

    public float get_accumulated_flow(Long commodity_id) {

        // TODO return cached flow
        System.err.println("Loop detector not implemented!");
        return 0;
    }

    public float get_current_density(Long commodity_id) {

        // TODO return cached density
        System.err.println("Loop detector not implemented!");
        return 0;
    }

}

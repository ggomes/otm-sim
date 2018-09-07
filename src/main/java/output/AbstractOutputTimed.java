/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package output;

import commodity.Commodity;
import commodity.Subnetwork;
import error.OTMErrorLog;
import error.OTMException;
import dispatch.Dispatcher;
import dispatch.EventTimedWrite;
import runner.RunParameters;
import runner.Scenario;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractOutputTimed extends AbstractOutput {

    // timed output
    public float outDt;			// output frequency in seconds
    public Commodity commodity;
    public Writer time_writer;
    public static String delim = ",";

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public AbstractOutputTimed(Scenario scenario,String prefix,String output_folder,Long commodity_id,Float outDt) throws OTMException{

        super(scenario,prefix,output_folder);

        this.outDt = outDt==null ? -1 : outDt;

        // get commodity
        if(commodity_id==null)
            commodity = null; // all commodities
        else{
            commodity = scenario.commodities.get(commodity_id);
            if(commodity==null)
                throw new OTMException("Bad commodity id (" + commodity_id + ") in output request.");
        }

    }

    public void validate(OTMErrorLog errorLog) {
        if(Float.isNaN(outDt) || outDt<=0f)
            errorLog.addError("outDt is not defined");
    }

    @Override
    public String get_output_file() {
        return  output_folder + File.separator + prefix + "_" +
                String.format("%.0f", outDt) + "_" +
                (commodity==null ? "g" : commodity.getId());
    }

    public Long get_commodity_id(){
        return commodity==null ? null : commodity.getId();
    }

    public float get_outdt(){
        return this.outDt;
    }

    //////////////////////////////////////////////////////
    // write time
    //////////////////////////////////////////////////////

    @Override
    public void open() throws OTMException {
        super.open();
        if(write_to_file){
            try {
                String filename = get_output_file();
                if(filename!=null) {
                    String subfilename = filename.substring(0,filename.length()-4);
                    time_writer = new OutputStreamWriter(new FileOutputStream(subfilename+"_time.txt"));
                }
            } catch (FileNotFoundException exc) {
                throw new OTMException(exc);
            }

        }
    }

    @Override
    public void close() throws OTMException {
        super.close();
        if(time_writer==null)
            return;
        try {
            time_writer.close();
        } catch (IOException e) {
            throw new OTMException(e);
        }
    }

    @Override
    public void write(float timestamp,Object obj) throws OTMException {
        if(write_to_file) {
            try {
                time_writer.write(timestamp + "\n");
            } catch (IOException e) {
                throw new OTMException(e);
            }
        }
    }

    //////////////////////////////////////////////////////
    // register
    //////////////////////////////////////////////////////

    @Override
    public void register(RunParameters props, Dispatcher dispatcher) {
        float start_time = props.start_time;
        float end_time = props.start_time + props.duration;
        for(float time=start_time ; time<=end_time ; time+=outDt )
            dispatcher.register_event(new EventTimedWrite(dispatcher,time,this));
    }

}

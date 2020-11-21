package output;

import commodity.Commodity;
import error.OTMErrorLog;
import error.OTMException;
import dispatch.Dispatcher;
import dispatch.EventTimedWrite;
import runner.RunParameters;
import common.Scenario;

import java.io.*;

public abstract class AbstractOutputTimed extends AbstractOutput implements InterfacePlottable {

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
            if(!scenario.commodities.containsKey(commodity_id))
                throw new OTMException("Bad commodity id (" + commodity_id + ") in output request.");
            commodity = scenario.commodities.get(commodity_id);
        }

    }

    //////////////////////////////////////////////////////
    // InterfaceOutput
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
    public void register(RunParameters props, Dispatcher dispatcher) {
        dispatcher.register_event(new EventTimedWrite(dispatcher,props.start_time,this));
    }

    //////////////////////////////////////////////////////
    // incomplete implementation
    //////////////////////////////////////////////////////

    public void validate(OTMErrorLog errorLog) {
        if(Float.isNaN(outDt) || outDt<=0f)
            errorLog.addError("outDt is not defined");
    }

    @Override
    public String get_output_file() {
        return  super.get_output_file() + "_" +
                (commodity==null ? "allcomms" : commodity.getId());
    }

    public void write(float timestamp) throws OTMException {
        if(write_to_file) {
            try {
                time_writer.write(timestamp + "\n");
            } catch (IOException e) {
                throw new OTMException(e);
            }
        }
    }

    //////////////////////////////////////////////////////
    // final
    //////////////////////////////////////////////////////

    public final Long get_commodity_id(){
        return commodity==null ? null : commodity.getId();
    }

    public final float get_outdt(){
        return this.outDt;
    }

}

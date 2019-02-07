package models.pq;

import common.Link;
import error.OTMException;
import models.AbstractLaneGroup;
import output.AbstractOutputTimed;
import profiles.Profile1D;
import runner.Scenario;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class OutputQueues extends AbstractOutputTimed {

    Model_PQ model;
    public ArrayList<models.pq.LaneGroup> ordered_lgs;               // An ordered map would be really helpful here

    public OutputQueues(Scenario scenario, String prefix, String output_folder, Long commodity_id, Float outDt) throws OTMException {
        super(scenario, prefix, output_folder, commodity_id, outDt);
    }

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public OutputQueues(Scenario scenario, Model_PQ model,String prefix, String output_folder, Long commodity_id, Float outDt) throws OTMException {
        super(scenario, prefix, output_folder, commodity_id, outDt);
        this.model = model;
        ordered_lgs = new ArrayList<>();
        for(Link link : model.links)
            for(AbstractLaneGroup lg : link.lanegroups_flwdn.values() )
                ordered_lgs.add((models.pq.LaneGroup)lg);
    }

    @Override
    public String get_output_file() {
        return  output_folder + File.separator + prefix + "_" +
                String.format("%.0f", outDt) + "_" +
                (commodity==null ? "g" : commodity.getId()) + "_" +
                model.name + "_queues.txt";
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);

        if(write_to_file){
            try {
                String filename = get_output_file();
                if(filename!=null) {
                    String subfilename = filename.substring(0,filename.length()-4);
                    Writer cells_writer = new OutputStreamWriter(new FileOutputStream(subfilename + "_queues.txt"));
                    for(models.pq.LaneGroup lg: ordered_lgs)
                        cells_writer.write(String.format("%dt\n%dw\n",lg.id,lg.id));
                    cells_writer.close();
                }
            } catch (FileNotFoundException exc) {
                throw new OTMException(exc);
            } catch (IOException e) {
                throw new OTMException(e);
            }
        } else {
            throw new OTMException("Not implemented code: 09242je");
//            for(LaneGroupProfile lgProfile : lgprofiles.values())
//                lgProfile.initialize(outDt);
        }

    }

    //////////////////////////////////////////////////////
    // write
    //////////////////////////////////////////////////////

    @Override
    public void write(float timestamp,Object obj) throws OTMException {
        if(write_to_file){
            super.write(timestamp,null);
            try {
                boolean isfirst=true;
                for(models.pq.LaneGroup lg : ordered_lgs) {
                    if(!isfirst)
                        writer.write(AbstractOutputTimed.delim);
                    isfirst = false;
                    writer.write(String.format("%d,%d",
                            lg.transit_queue.num_vehicles(),
                            lg.waiting_queue.num_vehicles()));
                }
                writer.write("\n");
            } catch (IOException e) {
                throw new OTMException(e);
            }
        } else {
            throw new OTMException("Not implemented code: 09242je");
        }
    }

}

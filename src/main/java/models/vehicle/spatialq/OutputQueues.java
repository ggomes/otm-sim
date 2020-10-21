package models.vehicle.spatialq;

import common.Link;
import error.OTMException;
import common.AbstractLaneGroup;
import output.AbstractOutputTimed;
import profiles.Profile1D;
import common.Scenario;

import java.io.*;
import java.util.*;

public class OutputQueues extends AbstractOutputTimed {

    public Set<Long> link_ids;
    public Map<Long,QueueInfo> lg2qinfo; // lane group -> 2 queues

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public OutputQueues(Scenario scenario, String prefix, String output_folder, Long commodity_id, Collection<Long> link_ids,Float outDt) throws OTMException {
        super(scenario, prefix, output_folder, commodity_id, outDt);

        this.link_ids = new HashSet<>();
        this.link_ids.addAll(link_ids==null ? scenario.network.links.keySet() : link_ids);

        lg2qinfo = new HashMap<>();
        for(Long link_id : this.link_ids){
            if(!scenario.network.links.containsKey(link_id))
                continue;
            Link link = scenario.network.links.get(link_id);
            for(AbstractLaneGroup lg : link.lanegroups_flwdn)
                lg2qinfo.put(lg.id, new QueueInfo(0f,outDt));
        }

    }

    //////////////////////////////////////////////////////
    // InterfaceOutput
    //////////////////////////////////////////////////////

    @Override
    public String get_output_file() {
        if(!write_to_file)
            return null;
        return  output_folder + File.separator + prefix + "_" +
                String.format("%.0f", outDt) + "_" +
                (commodity==null ? "g" : commodity.getId()) + "_" +
                "_queues.txt";
    }

    //////////////////////////////////////////////////////
    // AbstractOutputTimed
    //////////////////////////////////////////////////////

    @Override
    public void write(float timestamp) throws OTMException {
        super.write(timestamp);

        if(write_to_file){
//            super.write(timestamp,null);
//            try {
//                boolean isfirst=true;
//                for(LaneGroup lg : ordered_lgs) {
//                    if(!isfirst)
//                        writer.write(AbstractOutputTimed.delim);
//                    isfirst = false;
//                    writer.write(String.format("%d,%d",
//                            lg.transit_queue.num_vehicles(),
//                            lg.waiting_queue.num_vehicles()));
//                }
//                writer.write("\n");
//            } catch (IOException e) {
//                throw new OTMException(e);
//            }
        } else {
            for(Long link_id : link_ids) {
                Link link = scenario.network.links.get(link_id);
                for (AbstractLaneGroup alg : link.lanegroups_flwdn) {
                    MesoLaneGroup lg = (MesoLaneGroup) alg;
                    QueueInfo queueInfo = lg2qinfo.get(lg.id);
                    queueInfo.waiting_profile.values.add((double)lg.waiting_queue.num_vehicles());
                    queueInfo.transit_profile.values.add((double)lg.transit_queue.num_vehicles());
                }
            }
        }
    }

    //////////////////////////////////////////////////////
    // InterfacePlottable
    //////////////////////////////////////////////////////

    @Override
    public String get_yaxis_label() {
        return "veh";
    }

    @Override
    public void plot(String filename) throws OTMException {
        throw new OTMException("Plot not implemented for OutputQueues output.");
    }

    //////////////////////////////////////////////////////
    // AbstractOutput
    //////////////////////////////////////////////////////

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);

        if(write_to_file){
//            try {
//                String filename = get_output_file();
//                if(filename!=null) {
//                    String subfilename = filename.substring(0,filename.length()-4);
//                    Writer cells_writer = new OutputStreamWriter(new FileOutputStream(subfilename + "_queues.txt"));
//                    for(LaneGroup lg: ordered_lgs)
//                        cells_writer.write(String.format("%dt\n%dw\n",lg.id,lg.id));
//                    cells_writer.close();
//                }
//            } catch (FileNotFoundException exc) {
//                throw new OTMException(exc);
//            } catch (IOException e) {
//                throw new OTMException(e);
//            }
        } else {

        }
    }

    //////////////////////////////////////////////////////
    // final
    //////////////////////////////////////////////////////

    public final List<Double> get_waiting_for_link(long link_id){

        if(!scenario.network.links.containsKey(link_id))
            return null;

        Link link = scenario.network.links.get(link_id);

        Map<Queue.Type,Profile1D> X = new HashMap<>();

        Profile1D profile = null;
        for(AbstractLaneGroup lg : link.lanegroups_flwdn){
            if(!lg2qinfo.containsKey(lg.id))
                continue;

            QueueInfo queueinfo = lg2qinfo.get(lg.id);

            if(profile==null)
                profile = queueinfo.waiting_profile.clone();
            else
                profile.sum(queueinfo.waiting_profile);
        }
        return profile.get_values();
    }

    public final List<Double> get_transit_for_link(long link_id){

        if(!scenario.network.links.containsKey(link_id))
            return null;

        Link link = scenario.network.links.get(link_id);

        Map<Queue.Type,Profile1D> X = new HashMap<>();

        Profile1D profile = null;
        for(AbstractLaneGroup lg : link.lanegroups_flwdn){
            if(!lg2qinfo.containsKey(lg.id))
                continue;

            QueueInfo queueinfo = lg2qinfo.get(lg.id);

            if(profile==null)
                profile = queueinfo.transit_profile.clone();
            else
                profile.sum(queueinfo.transit_profile);
        }
        return profile.get_values();
    }

    //////////////////////////////////////////////////////
    // class
    //////////////////////////////////////////////////////

    public class QueueInfo {
        public Profile1D waiting_profile;
        public Profile1D transit_profile;
        public QueueInfo(Float start_time, Float dt){
            waiting_profile = new Profile1D(start_time, dt);
            transit_profile = new Profile1D(start_time, dt);
        }
    }
}

package output;

import common.AbstractLaneGroup;
import common.Link;
import common.Scenario;
import error.OTMErrorLog;
import error.OTMException;
import models.fluid.AbstractFluidModel;
import models.fluid.FluidLaneGroup;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import profiles.Profile1D;
import profiles.Profile2D;
import utils.OTMUtils;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractOutputTimedCell extends AbstractOutputTimed {

    public ArrayList<FluidLaneGroup> ordered_lgs;
    public Map<Long, List<CellProfile>> lgprofiles;  // lgid -> list<profiles>
    abstract protected double[] get_value_for_lanegroup(FluidLaneGroup lg);

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public AbstractOutputTimedCell(Scenario scenario, String prefix, String output_folder, Long commodity_id, Collection<Long> link_ids, Float outDt) throws OTMException {
        super(scenario,prefix,output_folder,commodity_id,outDt);

        // get lanegroup list
        if(link_ids==null)
            link_ids = new ArrayList<>(scenario.network.links.keySet());

        ordered_lgs = new ArrayList<>();
        lgprofiles = new HashMap<>();
        for(Long link_id : link_ids){
            if(!scenario.network.links.containsKey(link_id))
                continue;
            Link link = scenario.network.links.get(link_id);

            if(!(link.model instanceof AbstractFluidModel))
                throw new OTMException("Cell output cannot be generated for links with non-fluid models");

            for(AbstractLaneGroup lg : link.lanegroups_flwdn){
                FluidLaneGroup flg = (FluidLaneGroup)lg;
                ordered_lgs.add(flg);
                List<CellProfile> profs = new ArrayList<>();
                lgprofiles.put(lg.id,profs);
                for(int i=0;i<flg.cells.size();i++)
                    profs.add(new CellProfile());
            }
        }

    }

    //////////////////////////////////////////////////////
    // AbstractOutputTimed
    //////////////////////////////////////////////////////

    @Override
    public final void write(float timestamp) throws OTMException {
        super.write(timestamp);
        if(write_to_file){
            try {
                boolean isfirst=true;
                for(FluidLaneGroup lg : ordered_lgs){
                    if(!isfirst)
                        writer.write(AbstractOutputTimed.delim);
                    isfirst = false;
                    String str = OTMUtils.format_delim(get_value_for_lanegroup(lg)," ");
                    writer.write(str);  // TODO THIS WILL FAIL
                }
                writer.write("\n");
            } catch (IOException e) {
                throw new OTMException(e);
            }
        } else {
            for(FluidLaneGroup lg : ordered_lgs){
                List<CellProfile> cellprofs = lgprofiles.get(lg.id);
                double [] values = get_value_for_lanegroup(lg);
                for(int i=0;i<values.length;i++)
                    cellprofs.get(i).add_value(values[i]);
            }
        }
    }

    //////////////////////////////////////////////////////
    // AbstractOutput
    /////////////////////avail/////////////////////////////////

    @Override
    public void validate(OTMErrorLog errorLog) {
        super.validate(errorLog);
        if(lgprofiles.isEmpty())
            errorLog.addError("no lanegroups in output request");
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);

        if(write_to_file){
            try {
                String filename = get_output_file();
                if(filename!=null) {
                    String subfilename = filename.substring(0,filename.length()-4);
                    Writer cells_writer = new OutputStreamWriter(new FileOutputStream(subfilename + "_cells.txt"));
                    for(FluidLaneGroup lg: ordered_lgs)
                        for(int i=0;i<lg.cells.size();i++)
                            cells_writer.write(i+" "+lg.id+" "+lg.link.getId() + " " + lg.start_lane_dn+ " " + (lg.start_lane_dn+lg.num_lanes-1) +"\n"); // start/end dn lanes
                    cells_writer.close();
                }
            } catch (FileNotFoundException exc) {
                throw new OTMException(exc);
            } catch (IOException e) {
                throw new OTMException(e);
            }
        } else {
            for(List<CellProfile> cellprofs : lgprofiles.values())
                cellprofs.forEach(p->p.initialize(outDt));
        }

    }

    //////////////////////////////////////////////////////
    // incomplete implementation
    //////////////////////////////////////////////////////

    public Profile2D get_values_for_lg(FluidLaneGroup lg){
        Profile2D X = new Profile2D(0f,outDt);
        try {
            List<CellProfile> cellprofs = lgprofiles.get(lg.id);
            for(int i=0;i<cellprofs.size();i++)
                X.add_entry(i,cellprofs.get(i).profile.values);
        } catch (OTMException e) {
            e.printStackTrace();
        }
        return X;
    }

    public List<XYSeries> get_series_for_lg(FluidLaneGroup lg) {
        List<XYSeries> X = new ArrayList<>();
        List<CellProfile> cellprofs = lgprofiles.get(lg.id);
        for(int i=0;i<cellprofs.size();i++){
            CellProfile cellprof = cellprofs.get(i);
            String label = String.format("%d (%d-%d) cell %d",lg.link.getId(),lg.start_lane_dn,lg.start_lane_dn+lg.num_lanes-1,i);
            X.add(cellprof.profile.get_series(label));
        }
        return X;
    }

    //////////////////////////////////////////////////////
    // final
    //////////////////////////////////////////////////////

    public final ArrayList<FluidLaneGroup> get_ordered_lgs(){
        return ordered_lgs;
    }

    public final void plot_for_links(Set<Long> link_ids,String filename) throws OTMException {

        Set<FluidLaneGroup> lgs = new HashSet<>();
        if(link_ids==null)
            lgs.addAll(ordered_lgs);
        else
            lgs = ordered_lgs.stream().filter(lg->link_ids.contains(lg.link.getId())).collect(Collectors.toSet());

        XYSeriesCollection dataset = new XYSeriesCollection();

        for(FluidLaneGroup lg : lgs)
            for(XYSeries series : get_series_for_lg(lg))
                dataset.addSeries(series);

        make_time_chart(dataset,"",get_yaxis_label(),filename);
    }

    //////////////////////////////////////////////////////
    // class
    //////////////////////////////////////////////////////

    public class CellProfile {
        public Profile1D profile;
        public void initialize(float outDt){
            this.profile = new Profile1D(null, outDt);
        }
        public void add_value(double value){
            profile.add_entry(value);
        }
    }

}

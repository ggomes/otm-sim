package models.vehicle.newell;

import common.Link;
import error.OTMException;
import common.AbstractLaneGroup;
import output.AbstractOutputTimed;
import common.Scenario;

import java.io.File;
import java.io.IOException;

public class OutputMicroTrajectory extends AbstractOutputTimed {

    public float outDt;			// output frequency in seconds
    public ModelNewell model;

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public OutputMicroTrajectory(Scenario scenario, String prefix, String output_folder, Long commodity_id, Float outDt) throws OTMException {
        super(scenario, prefix, output_folder, commodity_id, outDt);
    }
//    public NewellTrajectories(Scenario scenario, ModelNewell model, String prefix, String output_folder, Float outDt) throws OTMException{
//        super(scenario,prefix,output_folder);
//        this.outDt = outDt==null ? -1 : outDt;
//        this.model = model;
//    }

    //////////////////////////////////////////////////////
    // InterfaceOutput
    //////////////////////////////////////////////////////

    @Override
    public String get_output_file() {
        if(!write_to_file)
            return null;
        return  output_folder + File.separator + prefix + "_" +
                String.format("%.0f", outDt) + "_" +
                model.name + "_traj.txt";
    }

    //////////////////////////////////////////////////////
    // AbstractOutputTimed
    //////////////////////////////////////////////////////

    @Override
    public final void write(float timestamp) throws OTMException {
        super.write(timestamp);
        if(write_to_file){
            try {
                for(Link link : model.links){
                    for(AbstractLaneGroup alg : link.lanegroups_flwdn){
                        NewellLaneGroup lg = (NewellLaneGroup) alg;
                        for(NewellVehicle vehicle : lg.vehicles)
                            writer.write(String.format("%.2f\t%d\t%d\t%.2f\n",timestamp,vehicle.getId(), lg.id,vehicle.pos));
                    }
                }
            } catch (IOException e) {
                throw new OTMException(e);
            }
        } else {
            throw new OTMException("Not implemented code: 09242je");
        }
    }

    //////////////////////////////////////////////////////
    // InterfacePlottable
    //////////////////////////////////////////////////////

    @Override
    public String get_yaxis_label() {
        return null;
    }

    @Override
    public void plot(String filename) throws OTMException {
        throw new OTMException("Plot not implemented for NewellTrajectories output type.");
    }

}

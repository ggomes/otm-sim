package output;

import common.Link;
import profiles.Profile1D;

public class LinkTravelTime {

    public int num_samples;
    public double sum_time;
    public Profile1D travel_time;
    public Link.ModelType model_type;

    public LinkTravelTime(Link link){
        num_samples = 0;
        sum_time = 0d;
        model_type = link.model_type;
    }

    public void initialize(float outDt){
        travel_time = new Profile1D(null, outDt);
    }

    public void add_sample(double travel_time_sample){
        num_samples++;
        sum_time += travel_time_sample;
    }

    public boolean has_samples(){
        return num_samples>0;
    }

    public double get_mean_and_clear(){
        double mean = sum_time / ((double) num_samples);
        num_samples = 0;
        sum_time = 0;
        return mean;
    }
}

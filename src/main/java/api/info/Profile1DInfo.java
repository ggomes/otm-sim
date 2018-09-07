/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package api.info;

import error.OTMException;
import profiles.Profile1D;

import java.util.ArrayList;
import java.util.List;

public class Profile1DInfo {

    private float start_time;
    private Float dt;
    private List<Double> values;

    public Profile1DInfo(Profile1D profile) {
        this.start_time = profile.start_time;
        this.dt = profile.dt;
        this.values = new ArrayList<>();
        for(Double x : profile.values)
            this.values.add(x);
    }


    public Profile1DInfo(double value) {
        this.start_time = 0;
        this.dt = Float.NaN;
        this.values = new ArrayList<>();
        this.values.add(value);
    }

    public float getStart_time() {
        return start_time;
    }

    public Float getDt() {
        return dt;
    }

    public List<Double> getValues() {
        return values;
    }

    public Double get_value(int k){
        if(values==null)
            return Double.NaN;
        if(k<0 || k>=values.size())
            return Double.NaN;
        return values.get(k);
    }

    public int num_values(){
        return values==null ? 0 : values.size();
    }

    //////////////////////////////////////
    // protected
    //////////////////////////////////////

    protected void add_profile(Profile1D profile) throws OTMException {
        if(start_time!=profile.start_time )
            throw new OTMException("profiles don''t match: "+start_time+"!="+profile.start_time );
        if( dt!=null && profile.dt!=null && !dt.equals(profile.dt) )
            throw new OTMException( dt + "!="+profile.dt);
        if(num_values()!=profile.get_length())
            throw new OTMException("num_values()!=profile.get_length(), numvalues="+num_values()+" profile.get_length="+profile.get_length());
        for(int i=0;i<values.size();i++)
            this.values.set(i,values.get(i) + profile.get_ith_value(i));
    }

}

package utils;

import java.util.ArrayList;
import java.util.List;

public class LookupTable {

    List<Float> x;
    List<Float> y;

    public LookupTable(String str){
        x = new ArrayList<>();
        y = new ArrayList<>();
        for(String str1 : str.split(";")){
            String [] str2 = str1.split(",");
            x.add(Float.parseFloat(str2[0]));
            y.add(Float.parseFloat(str2[1]));
        }
    }
    public Float get_value_for(float xr){
        if(xr<x.get(0))
            return 0f;
        for(int i=0;i<x.size()-1;i++)
            if(xr>=x.get(i)&& xr<x.get(i+1))
                return y.get(i);
        return y.get(y.size()-1);
    }
    public void scaleX(float alpha){
        for(int i=0;i<x.size();i++)
            x.set(i,x.get(i)*alpha);
    }
    public void scaleY(float alpha){
        for(int i=0;i<y.size();i++)
            y.set(i,y.get(i)*alpha);
    }
}

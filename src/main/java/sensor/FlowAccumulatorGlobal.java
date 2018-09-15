package sensor;

public class FlowAccumulatorGlobal {

    public double count;

    public void reset(){
        count = 0d;
    }

    public void increment(Double x){
        if(x.isNaN())
            return;
        count += x;
    }

}

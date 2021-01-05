package core.geometry;

import error.OTMErrorLog;

public class Gate implements Comparable<Gate>{
    public float start_pos;
    public float end_pos;

    public Gate(jaxb.Gate jaxb_gate) {
        this.start_pos = jaxb_gate.getStartPos();
        this.end_pos = jaxb_gate.getEndPos();
    }

    @Override
    public int compareTo(Gate that) {
        if (this.start_pos < that.start_pos)
            return -1;
        if (this.start_pos > that.start_pos)
            return 1;
        return 0;
    }

    public void validate(OTMErrorLog errorLog){
        if(start_pos<=0)
            errorLog.addError("start_pos<=0");
        if(end_pos<=start_pos)
            errorLog.addError("end_pos<=start_pos");
    }
}

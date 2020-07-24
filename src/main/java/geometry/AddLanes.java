package geometry;

import error.OTMErrorLog;
import jaxb.Roadparam;

import java.util.TreeSet;

public class AddLanes {

    public TreeSet<Gate> gates = new TreeSet<>();    // sorted set
    public boolean isopen;

    public Side side;
    public FlowPosition position;
    public boolean isfull;
    public float length;        // meters
    public int lanes;
    public jaxb.Roadparam roadparam;

    public AddLanes(FlowPosition pos, Side side, Roadparam rp){
        this.lanes = 0;
        this.side = side;
        this.position = pos;
        this.length = 0f;
        this.isfull = false;
        this.roadparam = rp;
    }

    public AddLanes(jaxb.AddLanes jaxb_al, Roadparam rp) {

        if(jaxb_al==null){
            this.lanes = 0;
            this.length = 0;
            return;
        }

        this.roadparam = rp;
        this.isopen = jaxb_al.isIsopen();
        this.side = Side.valueOf(jaxb_al.getSide().toLowerCase());
        this.position = jaxb_al.getPos()==null ? FlowPosition.dn : FlowPosition.valueOf(jaxb_al.getPos().toLowerCase());
        this.isfull = jaxb_al.getLength()==null;
        this.length = isfull ? Float.NaN : jaxb_al.getLength();
        this.lanes = jaxb_al.getLanes();

        if(jaxb_al.getGates()!=null)
            for(jaxb.Gate jaxb_gate : jaxb_al.getGates().getGate())
                gates.add(new Gate(jaxb_gate));
    }

    public float get_length(float linklength){
        return isfull ? linklength : length;
    }

    public boolean isUp(){
        return this.position.equals(FlowPosition.up);
    }

    public boolean isIn(){
        return this.side.equals(Side.in);
    }

    public void validate(OTMErrorLog errorLog){
        if(side==null)
            errorLog.addError("No side specified");
//        if(lanes<=0)
//            scenario.error_log.addError("lanes<=0");

        // gates mustn't overlap
        // assume they were correctly inserted in order
        if(!gates.isEmpty()){

            // gates are within the addlane
            if( gates.first().start_pos<0 )
                errorLog.addError("gates.first().start_pos<0");

            if( !isfull && gates.last().end_pos>this.length)
                errorLog.addError("gates.last().end_pos>this.length");

            // validate gates
            for(Gate gate : this.gates)
                gate.validate(errorLog);

            float prev_end = -1f;
            for(Gate gate : this.gates){
                if (gate.start_pos < prev_end)
                    errorLog.addError("gate.start_pos < prev_end");
                prev_end = gate.end_pos;
            }
        }
    }

    public jaxb.AddLanes to_jaxb(){
        // TODO FIX THIS
        jaxb.AddLanes j1 = new jaxb.AddLanes();
//        if(flwpos.equals(FlowDirection.dn))
//            j1.setStartPos(this.length);
//        else
//            j1.setEndPos(this.length);
        j1.setLanes(lanes);
        j1.setIsopen(isopen);
        j1.setSide(side.toString());
        if(gates!=null && !gates.isEmpty()) {
            jaxb.Gates jgates = new jaxb.Gates();
            j1.setGates(jgates);
            for (Gate gate : gates) {
                jaxb.Gate jgate = new jaxb.Gate();
                jgates.getGate().add(jgate);
                jgate.setStartPos(gate.start_pos);
                jgate.setEndPos(gate.end_pos);
            }
        }
        return j1;
    }
}

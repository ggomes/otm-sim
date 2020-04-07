package api.info;

import actuator.sigint.ActuatorSignal;
import actuator.sigint.SignalPhase;
import common.Node;

import java.util.ArrayList;
import java.util.List;

public class SignalInfo extends ActuatorInfo {

    public List<SignalPhaseInfo> signal_phases;
    public long node_id;

    public SignalInfo(ActuatorSignal x) {
        super(x);

        this.node_id = ((Node)x.target).getId();

        signal_phases = new ArrayList<>();
        for(SignalPhase s : x.signal_phases.values())
            signal_phases.add(new SignalPhaseInfo(s));
    }

    public List<SignalPhaseInfo> get_phases() {
        return signal_phases;
    }

    public long getNode_id() {
        return node_id;
    }

    @Override
    public String toString() {
        return "SignalInfo{" +
                "id=" + id +
                ", type=" + type +
                ", signal_phases=" + signal_phases +
                '}';
    }
}

package api.info.events;

import actuator.SignalPhase;

public class EventSignalPhaseInfo extends AbstractEventInfo {

    public final long signal_phase_id;
    public final SignalPhase.BulbColor bulbcolor;

    public EventSignalPhaseInfo(float timestamp, long signal_phase_id, SignalPhase.BulbColor bulbcolor) {
        super(timestamp);
        this.signal_phase_id = signal_phase_id;
        this.bulbcolor = bulbcolor;
    }

    @Override
    public String toString() {
        return new String(signal_phase_id+"\t"+bulbcolor);
    }
}

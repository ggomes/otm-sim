package output.events;

import actuator.SignalPhase;

public class EventSignalPhaseInfo extends AbstractEventWrapper {

    public final long signal_phase_id;
    public final SignalPhase.BulbColor bulbcolor;

    public EventSignalPhaseInfo(float timestamp, long signal_phase_id, SignalPhase.BulbColor bulbcolor) {
        super(timestamp);
        this.signal_phase_id = signal_phase_id;
        this.bulbcolor = bulbcolor;
    }

    @Override
    public String asString() {
        return new String(signal_phase_id+"\t"+bulbcolor);
    }
}

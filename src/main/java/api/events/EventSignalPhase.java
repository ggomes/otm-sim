package api.events;

import actuator.sigint.BulbColor;

public class EventSignalPhase extends AbstractEvent {

    public final long signal_phase_id;
    public final BulbColor bulbcolor;

    public EventSignalPhase(float timestamp, long signal_phase_id, BulbColor bulbcolor) {
        super(timestamp);
        this.signal_phase_id = signal_phase_id;
        this.bulbcolor = bulbcolor;
    }

    @Override
    public String toString() {
        return new String(signal_phase_id+"\t"+bulbcolor);
    }
}

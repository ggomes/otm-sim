package actuator.sigint;

import utils.OTMUtils;

public class PhaseTransition implements Comparable<PhaseTransition> {

    public float cycle_time;        // time within the cycle that this transition occurs
    public BulbColor from_color;
    public BulbColor to_color;

    public PhaseTransition(float cycle_time, BulbColor from_color, BulbColor to_color) {
        this.cycle_time = cycle_time;
        this.from_color = from_color;
        this.to_color = to_color;
    }

    ///////////////////////////////////////////
    // Comparable
    ///////////////////////////////////////////

    @Override
    public int compareTo(PhaseTransition other) {
        return Float.compare(this.cycle_time, other.cycle_time);
    }

    ///////////////////////////////////////////
    // toString
    ///////////////////////////////////////////

    @Override
    public String toString() {
        return String.format("%.1f\t%s\t%s",cycle_time,from_color,to_color);
    }

    ///////////////////////////////////////////
    // public static
    ///////////////////////////////////////////

    public static boolean are_equal(PhaseTransition p1, PhaseTransition p2) {
        return OTMUtils.approximately_equals(p1.cycle_time, p2.cycle_time) &&
                (p1.from_color == p2.to_color && p1.to_color == p2.from_color);
    }
}

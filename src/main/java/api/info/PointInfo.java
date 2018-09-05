package api.info;

import common.Link;
import common.Point;

public class PointInfo {

    /** X position. Unspecified units. This is not used by the simulator. */
    public float x;

    /** Y position. Unspecified units. This is not used by the simulator. */
    public float y;

    public PointInfo(Point p){
        this.x = p.x;
        this.y = p.y;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    @Override
    public String toString() {
        return "PointInfo{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}

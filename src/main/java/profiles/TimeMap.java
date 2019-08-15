package profiles;

import java.util.Map;

public class TimeMap {
    public float time;
    public Map<Long,Double> value;

    public TimeMap(float time,Map<Long,Double> value) {
        this.time = time;
        this.value = value;
    }

}

package api.info.events;

public class EventVehicleInfo extends AbstractEventInfo {

    public final long vehicle_id;
    public final long commodity_id;

    public EventVehicleInfo(long vehicle_id, long commodity_id) {
        super(-1f);
        this.vehicle_id = vehicle_id;
        this.commodity_id = commodity_id;
    }

    @Override
    public String toString() {
        return vehicle_id + "\t"+ commodity_id;
    }

}

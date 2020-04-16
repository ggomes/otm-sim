package output.events;

public class EventVehicleInfo extends AbstractEventWrapper {

    public final long vehicle_id;
    public final long commodity_id;

    public EventVehicleInfo(long vehicle_id, long commodity_id) {
        super(-1f);
        this.vehicle_id = vehicle_id;
        this.commodity_id = commodity_id;
    }

    @Override
    public String asString() {
        return vehicle_id + "\t"+ commodity_id;
    }

}

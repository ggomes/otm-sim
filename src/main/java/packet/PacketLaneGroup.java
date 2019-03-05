package packet;

import common.AbstractVehicle;
import keys.KeyCommPathOrLink;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PacketLaneGroup {

    public Set<AbstractVehicle> vehicles = new HashSet<>();

    // this buffer holds remainders of arriving fluid packets.
    // these remainders are added into the lane group packet
    public StateContainer container = new StateContainer();


    // used by newInstance (dont delete)
    public PacketLaneGroup(){
        super();
    }

//    public PacketLaneGroup(Set<AbstractVehicle> vehicles){
//        super();
//        this.vehicles.addAll(vehicles);
//    }

    public PacketLaneGroup(AbstractVehicle vehicle){
        super();
        this.vehicles.add(vehicle);
    }


    public boolean isEmpty() {
        return (vehicles==null || vehicles.isEmpty()) && container.isEmpty();
    }

    public void add_link_packet(PacketLink vp) {
        if(vp.vehicles!=null)
            vp.vehicles.forEach(v-> add_vehicle(v.get_key(),v));
        if(vp.state2vehicles!=null)
            vp.state2vehicles.forEach( (k,v)-> add_fluid(k,v));
    }

    public void add_fluid(KeyCommPathOrLink key, Double value) {
        container.set_value(key, container.get_value(key) + value);
    }

    public void add_vehicle(KeyCommPathOrLink key, AbstractVehicle vehicle) {
        vehicles.add(vehicle);
    }

    public PacketLaneGroup times(double x) {
        Map<KeyCommPathOrLink,Double> z_amount = new HashMap<>();
        for(Map.Entry<KeyCommPathOrLink,Double> e : container.amount.entrySet())
            z_amount.put(e.getKey(),e.getValue()*x);

        PacketLaneGroup z = new PacketLaneGroup();
        z.container.amount = z_amount;
        return z;
    }

}

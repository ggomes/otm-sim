package packet;

import commodity.Commodity;
import commodity.Path;
import common.AbstractVehicle;
import keys.KeyCommPathOrLink;
import models.AbstractLaneGroup;
import models.vehicle.AbstractVehicleModel;
import common.Scenario;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StateContainer {

    public Map<KeyCommPathOrLink,Double> amount;

    ////////////////////////////////////////////////////////////
    // construction
    ////////////////////////////////////////////////////////////

    public StateContainer(){
        this.amount = new HashMap<>();
    }

    ////////////////////////////////////////////////////////////
    // public
    ////////////////////////////////////////////////////////////

    public boolean isEmpty(){
        if(amount.isEmpty())
            return true;
        return amount.values().stream().allMatch(x->x==0d);
    }

    public Set<AbstractVehicle> add_packet_and_extract_vehicles(StateContainer container, AbstractLaneGroup lg){

        AbstractVehicleModel model = (AbstractVehicleModel) lg.link.model;

        Set<AbstractVehicle> vehicles = new HashSet<>();

        // iterate through all keys ion the packet
        for(Map.Entry<KeyCommPathOrLink,Double> e : container.amount.entrySet()){
            KeyCommPathOrLink key = e.getKey();
            double value = amount.containsKey(key) ? amount.get(key) + e.getValue() : e.getValue();

            if(value>=1d){
                int num_veh = (int) value;
                amount.put(key,value - num_veh);
                for(int i=0;i<num_veh;i++) {
                    Scenario scenario = lg.link.network.scenario;
                    Commodity commodity = scenario.commodities.get(key.commodity_id);
                    AbstractVehicle vehicle = model.create_vehicle(key.commodity_id, commodity.vehicle_event_listeners);
                    vehicle.set_key(key);
                    if(key.isPath)
                        vehicle.path = (Path) scenario.subnetworks.get(key.pathOrlink_id);
                    vehicles.add(vehicle);
                }
            }
            else
                amount.put(key,value);
        }

        return vehicles;
    }

    public void add_packet(PacketLaneGroup packet){

        // vehicle part
        if(packet.vehicles!=null && !packet.vehicles.isEmpty()){
            for(AbstractVehicle vehicle : packet.vehicles){
                KeyCommPathOrLink key = vehicle.get_key();
                amount.put(key, amount.containsKey(key) ? (amount.get(key)+1d) : 1d);
            }
        }

        // fluid part
        if(packet.container!=null && !packet.container.amount.isEmpty()){
            for(Map.Entry<KeyCommPathOrLink,Double> e : packet.container.amount.entrySet()){
                KeyCommPathOrLink key = e.getKey();
                amount.put(key, amount.containsKey(key) ? (amount.get(key)+e.getValue()) : e.getValue());
            }
        }

    }

    public double get_value(KeyCommPathOrLink key){
        return amount.containsKey(key) ? amount.get(key) : 0d;
    }

    public void set_value(KeyCommPathOrLink key,double val){
        amount.put(key,val);
    }

    public double get_total_veh(){
        return amount.values().stream().mapToDouble(x->x).sum();
    }

}

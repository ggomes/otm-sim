package api.info;

import runner.Scenario;

import java.util.HashSet;
import java.util.Set;

public class ScenarioInfo {

    /** Information for the network. */
    public NetworkInfo network;

    /** Information for the commodities. */
    public Set<CommodityInfo> commodities = new HashSet<>();

    /** Information for the subnetworks. */
    public Set<SubnetworkInfo> subnetworks = new HashSet<>();

    /** Information for the controllers. */
    public Set<ControllerInfo> controllers = new HashSet<>();

    /** Information for the actuators. */
    public Set<ActuatorInfo> actuators = new HashSet<>();

    /** Information for the splits. */
    public Set<SplitInfo> splits = new HashSet<>();

    /** Information for the demands. */
    public Set<DemandInfo> demands = new HashSet<>();

    public ScenarioInfo(Scenario scenario){
        network = new NetworkInfo(scenario.network);
        scenario.commodities.values().forEach(x -> commodities.add(new CommodityInfo(x)));
        scenario.subnetworks.values().forEach(x -> subnetworks.add(new SubnetworkInfo(x)));
        scenario.controllers.values().forEach(x -> controllers.add(new ControllerInfo(x)));
        scenario.actuators.values().forEach(x -> actuators.add(new ActuatorInfo(x)));
        scenario.data_demands.values().forEach(x -> demands.add(new DemandInfo(x)));
    }

    public NetworkInfo getNetwork() {
        return network;
    }

    public Set<CommodityInfo> getCommodities() {
        return commodities;
    }

    public Set<SubnetworkInfo> getSubnetworks() {
        return subnetworks;
    }

    public Set<ControllerInfo> getControllers() {
        return controllers;
    }

    public Set<ActuatorInfo> getActuators() {
        return actuators;
    }

    public Set<SplitInfo> getSplits() {
        return splits;
    }

    public Set<DemandInfo> getDemands() {
        return demands;
    }

    @Override
    public String toString() {
        return "ScenarioInfo{" +
                "network=" + network +
                ", commodities=" + commodities +
                ", subnetworks=" + subnetworks +
                ", controllers=" + controllers +
                ", actuators=" + actuators +
                ", splits=" + splits +
                ", demands=" + demands +
                '}';
    }
}

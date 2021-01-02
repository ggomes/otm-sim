package dispatch;

import error.OTMException;

// Existing events and their dispatch order.
//    0	    dispatch.EventDemandChange
//    0	    dispatch.EventSplitChange
//    5     AbstractLaneSelector.poke
//    10	AbstractSensor.poke
//    20	AbstractController.poke
//    25    ControllerSchedule.poke
//    30 	AbstractActuator.poke
//    35    EventInitializeController -- calls AbstractController.initialize
//    40	dispatch.EventCreateVehicle
//    44 	models.vehicle.spatialq.EventTransitToWaiting
//    45	models.vehicle.spatialq.EventReleaseVehicleFromLaneGroup
//    50	core.EventFluidModelUpdate
//    55	core.EventFluidStateUpdate
//    60	model.Newell.poke
//    65	dispatch.EventComputeTravelTime
//    69    models.fluid.EventUpdateTotalLanegroupVehicles
//    69    models.fluid.EventUpdateTotalCellVehicles
//    70	dispatch.EventTimedWrite
//    100	dispatch.EventStopSimulation

public abstract class AbstractEvent implements InterfaceEvent, Comparable<AbstractEvent> {

    public Dispatcher dispatcher;
    public float timestamp;
    public Object recipient;
    public int dispatch_order;

    public AbstractEvent(Dispatcher dispatcher,int dispatch_order, float timestamp, Object recipient){
        this.dispatcher = dispatcher;
        this.dispatch_order = dispatch_order;
        this.timestamp = timestamp;
        this.recipient = recipient;
    }

    ///////////////////////////////////////
    // toString
    ///////////////////////////////////////

    @Override
    public String toString() {
        return timestamp + " , " + dispatch_order + " , "+ this.getClass().getName() + " , " + this.recipient.getClass().getName();
    }

    @Override
    public int compareTo(AbstractEvent that) {
        if(this.timestamp<that.timestamp)
            return -1;
        if(that.timestamp<this.timestamp)
            return 1;
        if(this.dispatch_order<that.dispatch_order)
            return -1;
        if(that.dispatch_order<this.dispatch_order)
            return 1;
        return 0;
    }
}
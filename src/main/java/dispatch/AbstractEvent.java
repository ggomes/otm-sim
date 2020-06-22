package dispatch;

import error.OTMException;

// Existing events and their dispatch order.
//    0	dispatch.EventDemandChange
//    0	dispatch.EventSplitChange
//    10	AbstractSensor.poke
//    20	AbstractController.poke
//    25    ControllerSchedule.poke
//    30 	AbstractActuator.poke
//    40	dispatch.EventCreateVehicle
//    44 	models.vehicle.spatialq.EventTransitToWaiting
//    45	models.vehicle.spatialq.EventReleaseVehicleFromLaneGroup
//    50	models.fluid.EventFluidModelUpdate
//    55	models.fluid.EventFluidStateUpdate
//    60	model.Newell.poke
//    65	dispatch.EventComputeTravelTime
//    70	dispatch.EventTimedWrite
//    100	dispatch.EventStopSimulation

public abstract class AbstractEvent implements InterfaceEvent {

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

    @Override
    public void action(boolean verbose) throws OTMException {
        if(verbose)
            System.out.println(String.format("%.2f\t%d\t%s\t%s",timestamp,dispatch_order,getClass().getName(),recipient.getClass().getName()));
    }

    ///////////////////////////////////////
    // toString
    ///////////////////////////////////////

    @Override
    public String toString() {
        return timestamp + "\t" + dispatch_order + "\t" + this.getClass();
    }

}
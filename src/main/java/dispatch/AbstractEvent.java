/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package dispatch;

import error.OTMException;

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

    @Override
    public String toString() {
        return timestamp + "\t" + dispatch_order + "\t" + this.getClass();
    }

}
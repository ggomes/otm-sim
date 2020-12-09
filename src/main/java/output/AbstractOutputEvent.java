package output;

import output.events.AbstractEventWrapper;
import error.OTMException;
import core.Scenario;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractOutputEvent extends AbstractOutput implements InterfacePlottable {

    public List<AbstractEventWrapper> events;

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public AbstractOutputEvent(Scenario scenario,String prefix,String output_folder) {
        super(scenario,prefix,output_folder);
    }

    //////////////////////////////////////////////////////
    // InterfaceScenarioElement-like
    //////////////////////////////////////////////////////

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);
        events = new ArrayList<>();
    }

    //////////////////////////////////////////////////////
    // InterfacePlottable
    //////////////////////////////////////////////////////

    @Override
    public void plot(String filename) throws OTMException {
        System.err.println("IMPLEMENT THIS");
    }

    //////////////////////////////////////////////////////
    // final
    //////////////////////////////////////////////////////

    public final void write(AbstractEventWrapper event) throws OTMException {
        if(write_to_file){
            try {
                writer.write(event.timestamp+"\t"+event.asString()+"\n");
            } catch (IOException e) {
                throw new OTMException(e);
            }
        } else {
            events.add(event);
        }
    }

    public final List<AbstractEventWrapper> get_events(){
        return events;
    }

}

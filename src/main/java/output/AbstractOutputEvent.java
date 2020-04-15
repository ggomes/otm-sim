package output;

import api.info.events.AbstractEventInfo;
import error.OTMException;
import common.Scenario;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractOutputEvent extends AbstractOutput implements InterfacePlottable {

    public List<AbstractEventInfo> events;

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
    // InterfaceOutput
    //////////////////////////////////////////////////////

    @Override
    public void write(float timestamp,Object obj) throws OTMException {

        if(!(obj instanceof AbstractEventInfo))
            throw new OTMException("Bad object type in AbstractOutputEvent.write");

        AbstractEventInfo event = (AbstractEventInfo) obj;

        if(write_to_file){
            try {
                writer.write(timestamp+"\t"+event.toString()+"\n");
            } catch (IOException e) {
                throw new OTMException(e);
            }
        } else {
            events.add(event);
        }
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

    public final List<AbstractEventInfo> get_events(){
        return events;
    }

}

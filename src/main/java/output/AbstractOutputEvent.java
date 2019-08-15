package output;

import api.events.AbstractEvent;
import error.OTMException;
import runner.Scenario;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractOutputEvent extends AbstractOutput {

    public List<AbstractEvent> events;

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public AbstractOutputEvent(Scenario scenario,String prefix,String output_folder) {
        super(scenario,prefix,output_folder);
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);
        events = new ArrayList<>();
    }

    //////////////////////////////////////////////////////
    // get / plot
    //////////////////////////////////////////////////////

    @Override
    public void write(float timestamp,Object obj) throws OTMException {

        if(!(obj instanceof api.events.AbstractEvent))
            throw new OTMException("Bad object type in AbstractOutputEvent.write");

        api.events.AbstractEvent event = (api.events.AbstractEvent) obj;

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

    abstract public void plot(String filename) throws OTMException;

    public List<AbstractEvent> get_events(){
        return events;
    }


}

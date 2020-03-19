package tests;

import api.Scenario;
import error.OTMException;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.fail;

public class TestSignal  extends AbstractTest {

    @Ignore
    @Test
    public void test_run_from_properties() {
        try {
            String configfile = "/home/gomes/Dropbox/gabriel/work/MEng/2019-2020/_supervised/traffic/capstone-project/cfg/network_v6.xml";

            // Load ..............................
            api.OTM otm = new api.OTM(configfile,true,false);

            // initialize
            otm.initialize(0f);
            otm.advance(1f);

            // advance
            otm.advance(3000f);

            print_queues(otm);

            // my queues
            Map<Long,Scenario.Queues> my_queues = new HashMap<>();
            my_queues.put(1l,new Scenario.Queues(3,5));
            my_queues.put(2l,new Scenario.Queues(5,3));

            // set queues
            set_queues(my_queues,otm);

            print_queues(otm);

            otm.advance(2300f);

            print_queues(otm);

        } catch (OTMException e) {
            System.out.print(e);
            fail();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private Map<Long,Scenario.Queues> get_queues(api.OTM otm) throws Exception {
        Map<Long,Scenario.Queues> queues = new HashMap<>();
        for(long link_id : otm.scenario().get_link_ids())
            queues.put(link_id,otm.scenario().get_link_queues(link_id));
        return queues;
    }

    private void set_queues(Map<Long,Scenario.Queues> queues,api.OTM otm) throws Exception {
        for(Map.Entry<Long,Scenario.Queues> e : queues.entrySet()){
            Long link_id = e.getKey();
            Scenario.Queues q = e.getValue();
            otm.scenario().set_link_vehicles(link_id,q.waiting(),q.transit());
        }
    }

    private void print_queues(api.OTM otm) throws Exception{
        Map<Long,Scenario.Queues> queues = get_queues(otm);
        System.out.println("----------");
        for(Map.Entry<Long,Scenario.Queues> e : queues.entrySet()){
            Long link_id = e.getKey();
            Scenario.Queues q = e.getValue();
            System.out.println(String.format("%d\t%d\t%d",link_id,q.waiting(),q.transit()));
        }
    }

}

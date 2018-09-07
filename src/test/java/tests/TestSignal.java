package tests;

import api.API;
import error.OTMException;
import org.junit.Ignore;
import org.junit.Test;
import runner.OTM;

import static org.junit.Assert.fail;

public class TestSignal  extends AbstractTest {

    @Ignore
    @Test
    public void test_run_from_properties() {
        try {

            String prefix = "signal";
            String output_request_file = "config\\output_request.xml";
            String output_folder = "temp";

            API api = OTM.load_test("intersection_nopocket",2,true,"ctm");
            api.run(prefix,output_request_file,output_folder,0,3600);

            // TODO assert

        } catch (OTMException e) {
            System.out.print(e);
            fail();
        }
    }

}

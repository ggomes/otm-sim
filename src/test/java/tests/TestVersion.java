package tests;

import org.junit.Test;
import runner.OTM;
import utils.OTMUtils;

public class TestVersion {

    @Test
    public void test_get_version(){
        System.out.println("otm-sim: " + api.OTM.get_version());
    }

}

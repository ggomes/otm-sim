package tests;

import org.junit.Test;

public class TestVersion {

    @Test
    public void test_get_version(){
        System.out.println("otm-sim: " + api.OTM.get_version());
    }

}

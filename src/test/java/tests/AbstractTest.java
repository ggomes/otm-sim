package tests;

import org.junit.runners.Parameterized;
import xml.JaxbLoader;

import java.util.*;

public abstract class AbstractTest {

    protected static String output_folder = "temp";
    protected static HashMap<String,String> test_configs;

    static {
        test_configs = new HashMap<>();

        test_configs.put("intersection","intersection.xml");
        test_configs.put("line_ctm","line_ctm.xml");
        test_configs.put("line_newell","line_newell.xml");
        test_configs.put("line_spaceq","line_spaceq.xml");
        test_configs.put("mixing","mixing.xml");
        test_configs.put("onramp_hov","onramp_nohov.xml");
        test_configs.put("onramp_nohov","onramp_nohov.xml");
        test_configs.put("onramp_offramp","onramp_offramp.xml");
        test_configs.put("output_test","output_test.xml");
    }

    public static Collection<String> get_test_config_names() {
        return test_configs.keySet();
    }

    private static List<String> all_runparams;
    static {
        all_runparams = new ArrayList<>();
        all_runparams.add("data/props/0_5_3600.properties");
    }

    @Parameterized.Parameters
    public static Collection getConfigs(){
        ArrayList<String []> x = new ArrayList<>();

        Collection<String> all_configs = get_test_config_names();

        // TODO remove this
        all_configs.remove("signal");

        for(String s : all_configs)
            x.add(new String[]{s});
        return x;
    }

}

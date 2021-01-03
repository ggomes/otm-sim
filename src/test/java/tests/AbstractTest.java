package tests;

import org.junit.runners.Parameterized;
import xml.JaxbLoader;

import java.util.*;

public abstract class AbstractTest {

    public static String output_folder = "temp";
    public static Collection<String> get_test_config_names() {
        return JaxbLoader.test_configs.keySet();
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

        for(String s : all_configs)
            x.add(new String[]{s});
        return x;
    }
}

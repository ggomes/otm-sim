package tests;

import org.junit.runners.Parameterized;
import xml.JaxbLoader;

import java.util.*;

public abstract class AbstractTest {

    protected static String output_folder = "temp";

    private static List<String> all_runparams;
    static {
        all_runparams = new ArrayList<>();
        all_runparams.add("data/props/0_5_3600.properties");
    }

    @Parameterized.Parameters
    public static Collection getConfigs(){
        ArrayList<String []> x = new ArrayList<>();

        Collection<String> all_configs = JaxbLoader.get_test_config_names();

        // TODO remove this
        all_configs.remove("signal");

        for(String s : all_configs)
            x.add(new String[]{s});
        return x;
    }

}

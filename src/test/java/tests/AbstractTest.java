package tests;

import api.OTM;
import error.OTMException;
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

    public static OTM load_test_config(String configname,boolean validate) throws OTMException  {
        jaxb.Scenario jscn =  JaxbLoader.load_test_scenario(configname+".xml",false);
        OTM otm = new OTM();
        otm.load_from_jaxb(jscn,validate);
        return otm;
    }

}

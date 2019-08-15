package api;

import error.OTMException;
import runner.OTM;
import runner.Scenario;
import runner.ScenarioFactory;
import utils.OTMUtils;
import xml.JaxbLoader;

public class API {

    protected Scenario scn;
    public APIScenario scenario;
    public APIOutput output;
    public APIPerformance performance;

    public API(String configfile,boolean validate,boolean jaxb_only) throws OTMException {
        jaxb.Scenario jaxb_scenario = JaxbLoader.load_scenario(configfile,validate);
        if(jaxb_only)
            this.scn =  ScenarioFactory.create_scenario_for_static_traffic_assignment(jaxb_scenario);
        else
           this.scn =  ScenarioFactory.create_scenario(jaxb_scenario,validate);
        scenario = new APIScenario(scn);
        output = new APIOutput(scn);
        performance = new APIPerformance(scn);
    }

    ////////////////////////////////////////////////////////
    // static
    ////////////////////////////////////////////////////////

    /**
     * Undocumented
     * @return git hash for the current build.
     */
    public static String get_version(){
        return OTM.getGitHash();
    }

    /**
     * Undocumented
     * @param seed Undocumented
     */
    public static void set_random_seed(long seed){
        OTMUtils.set_random_seed(seed);
    }

    ////////////////////////////////////////////////////////
    // run
    ////////////////////////////////////////////////////////

    /**
     * Undocumented
     * @param prefix Undocumented
     * @param output_requests_file Undocumented
     * @param output_folder Undocumented
     * @param start_time Undocumented
     * @param duration Undocumented
     */
    public void run(String prefix,String output_requests_file,String output_folder,float start_time,float duration) {
        try {
            OTM.run(scn,prefix,output_requests_file,output_folder,start_time,duration);
        } catch (OTMException e) {
            e.printStackTrace();
        }

//        try {
//            for(FileWriter writer : scenario.writer.values()){
//                writer.close();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    /**
     * Undocumented
     * @param start_time Undocumented
     * @param duration Undocumented
     */
    public void run(float start_time,float duration) {
        run(null,null,null,start_time,duration);
    }

    /**
     * Undocumented
     * @param runfile Undocumented
     */
    public void run(String runfile) {
        try {
            OTM.run(scn,runfile);
        } catch (OTMException e) {
            e.printStackTrace();
        }
    }

    /**
     * Undocumented
     * @param start_time Undocumented
     * @throws OTMException Undocumented
     */
    public void initialize(float start_time) throws OTMException{
        OTM.initialize(scn,start_time);
    }

    /**
     * Undocumented
     * @param duration Undocumented
     * @throws OTMException Undocumented
     */
    public void advance(float duration) throws OTMException{
        OTM.advance(scn,duration);
    }

    /**
     * Undocumented
     * @return Undocumented
     */
    public float get_current_time(){
        return scn.get_current_time();
    }

}
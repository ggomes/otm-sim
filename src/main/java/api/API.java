package api;

import error.OTMException;
import runner.OTM;
import runner.Scenario;
import runner.ScenarioFactory;
import utils.OTMUtils;
import xml.JaxbLoader;

import java.util.*;

public class API {

    protected Scenario scn;
    public APIScenario scenario;
    public APIOutput output;
    public APIPerformance performance;

    public API(){
        this.scn = null;
    }

    ////////////////////////////////////////////////////////
    // miscellaneous
    ////////////////////////////////////////////////////////

    /**
     * Undocumented
     * @return git hash for the current build.
     */
    public String get_version(){
        return OTM.getGitHash();
    }

    /**
     * Undocumented
     * @param seed Undocumented
     */
    public void set_random_seed(long seed){
        OTMUtils.set_random_seed(seed);
    }

    /**
     * Undocumented
     * @return Undocumented
     */
    public boolean has_scenario(){
        return scn !=null;
    }

    ////////////////////////////////////////////////////////
    // load
    ////////////////////////////////////////////////////////

    /**
     * Undocumented
     * @param configfile Undocumented
     * @throws OTMException Undocumented
     */
    public void load(String configfile) throws OTMException{
        load(configfile,true);
    }

    /**
     * Undocumented
     * @param configfile Undocumented
     * @param validate Undocumented
     * @return Undocumented
     * @throws OTMException Undocumented
     */
    public List<Long> load(String configfile,boolean validate) throws OTMException{

        List<Long> timestamps = new ArrayList<>();
        Date now = new Date();

        timestamps.add(now.getTime());
        jaxb.Scenario jaxb_scenario = JaxbLoader.load_scenario(configfile,validate);
        now = new Date();
        timestamps.add(now.getTime());

        this.scn =  ScenarioFactory.create_scenario(jaxb_scenario,validate);

        now = new Date();
        timestamps.add(now.getTime());

        return timestamps;

    }

    /**
     * Undocumented
     * @param configfile Undocumented
     * @return Undocumented
     * @throws OTMException Undocumented
     */
    public List<Long> load_for_static_traffic_assignment(String configfile) throws OTMException{

        List<Long> timestamps = new ArrayList<>();
        Date now = new Date();

        timestamps.add(now.getTime());
        jaxb.Scenario jaxb_scenario = JaxbLoader.load_scenario(configfile,false);
        now = new Date();
        timestamps.add(now.getTime());
        System.out.println("Took " + (timestamps.get(1)-timestamps.get(0)) + " to load XML.");

        this.scn =  ScenarioFactory.create_scenario_for_static_traffic_assignment(jaxb_scenario);
//        this.scenario =  ScenarioFactory.create_scenario(jaxb_scenario,1f,true,"ctm");

        now = new Date();
        timestamps.add(now.getTime());
        System.out.println("Took " + (timestamps.get(2)-timestamps.get(1)) + " to create scenario.");

        return timestamps;

    }

    /**
     * Undocumented
     * @param testname Undocumented
     * @param validate Undocumented
     * @return Undocumented
     * @throws OTMException Undocumented
     */
    public List<Long> load_test(String testname,boolean validate) throws OTMException{

        List<Long> timestamps = new ArrayList<>();
        Date now = new Date();

        timestamps.add(now.getTime());
        jaxb.Scenario jaxb_scenario = JaxbLoader.load_test_scenario(testname,validate);
        now = new Date();
        timestamps.add(now.getTime());

        this.scn =  ScenarioFactory.create_scenario(jaxb_scenario,validate);

        now = new Date();
        timestamps.add(now.getTime());

        return timestamps;

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

    ////////////////////////////////////////////////////////
    // animation
    ////////////////////////////////////////////////////////


}
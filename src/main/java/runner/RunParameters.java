package runner;

import error.OTMErrorLog;
import error.OTMException;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class RunParameters {

    public String prefix;
    public String output_requests_file;
    public String output_folder;
    public float start_time;      // seconds after midnight
    public float duration;        // seconds

    public RunParameters(String prefix,String output_requests_file,String output_folder,float start_time,float duration){
        this.prefix = prefix;
        this.output_requests_file = output_requests_file;
        this.output_folder = output_folder;
        this.start_time = start_time;
        this.duration = duration;
    }

    public RunParameters(String prop_file_name) throws OTMException {

        Properties properties = new Properties();

        // load properties file
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(prop_file_name);
            properties.load(fis);
        } catch (IOException e) {
            System.err.println(e);
        }
        finally {
            try {
                if(fis!=null)
                    fis.close();
            } catch (IOException e) {
                System.err.println(e);
            }
        }

        if(fis==null)
            return;

        // read properties
        prefix  = properties.getProperty("PREFIX") == null ? "" : properties.getProperty("PREFIX");
        output_requests_file  = properties.getProperty("OUTPUT_REQUESTS_FILE") == null ? "" : properties.getProperty("OUTPUT_REQUESTS_FILE");
        output_folder = properties.getProperty("OUTPUT_FOLDER") == null ? "" : properties.getProperty("OUTPUT_FOLDER");
//        sim_dt = properties.getProperty("SIM_DT") == null ? 0 : Integer.parseInt(properties.getProperty("SIM_DT"));
        start_time = Integer.parseInt(properties.getProperty("START_TIME", "0"));
        duration = Integer.parseInt(properties.getProperty("DURATION", "86400"));
//        verbose = Boolean.parseBoolean(properties.getProperty("VERBOSE", "false"));
    }

    public void validate(OTMErrorLog errorLog) {
        if (output_folder!=null && output_folder.isEmpty())
            errorLog.addError("output_folder.isEmpty()");
        if (start_time < 0)
            errorLog.addError("start_time<0");
        if (duration <= 0)
            errorLog.addError("duration <= 0");
    }

    public boolean is_valid(){
        boolean is_invalid = output_folder.isEmpty() || (start_time < 0) || (duration <= 0);
        return !is_invalid;
    }

}
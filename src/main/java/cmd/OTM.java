package cmd;

import error.OTMException;

public class OTM {

    // 0:  -help      Display usage message.
    //     -version   Display version information.
    //     -load      Load and validate a config file. arguments: <configfile>
    //     -run       Run a config file with default paramters. arguments: <configfile> <prefix> <output request file> <output folder> <start_time> <duration>
    // 1: configfile: absolute location and name of the configuration file.
    // 2: prefix: string to be pre-pended to all output files.
    // 3: output request file: absolute location and name of the output request file.
    // 4: output folder: folder where the output files should go.
    // 5: start_time: [integer] start time for the simrultion in seconds after midnight.
    // 6: duration: [integer] simulation duration in seconds.
    public static void main(String[] args) {

        if (0 == args.length) {
            System.err.print(get_usage());
            return;
        }

        String cmd = args[0];
        String[] arguments = new String[args.length - 1];
        System.arraycopy(args, 1, arguments, 0, args.length - 1);

        // load and validate
        if (cmd.equals("-load")){
            try {
                core.OTM otm = new core.OTM(arguments[0],true); // config
            } catch (OTMException e) {
                e.printStackTrace();
                return;
            }

            System.out.println("Load successful!");
        } else

        // run
        //    0 configfile
        //    1 prefix
        //    2 output_request
        //    3 output folder
        //    4 start_time
        //    5 duration
        if (cmd.equals("-run")){
            try {

                if(arguments.length<6) {
                    System.err.println("Not enough input arguments.");
                    return;
                }

                String configfile = arguments[0];
                String prefix = arguments[1];
                String output_requests_file = arguments[2];
                String output_folder = arguments[3];
                int start_time = Integer.parseInt(arguments[4]);
                int duration = Integer.parseInt(arguments[5]);

                core.OTM otm = new core.OTM(configfile,true);
                otm.run(prefix, output_requests_file, output_folder,start_time,duration,true);

            } catch (OTMException e) {
                e.printStackTrace();
            }
        }

         // version
        else if (cmd.equals("-version")){
            System.out.println("otm-sim: " + core.OTM.get_version());
        }

        // help
        else if (cmd.equals("-help"))
            System.out.print(get_usage());

        else
            System.err.print(get_usage());
    }

    private static String get_usage(){
        String str =
                "Usage: [-help|-version|-load file]\n" +
                        "\t-help\t\tDisplay usage message.\n" +
                        "\t-version\tDisplay version information.\n" +
                        "\t-load\t\tLoad and validate a config file. arguments: <configfile>\n" +
                        "\t-run\t\tRun a config file with default paramters. arguments: <configfile> <prefix> <output request file> <output folder> <start_time> <duration>\n" +
                        "\t\tconfigfile: absolute location and name of the configuration file.\n" +
                        "\t\tprefix: string to be pre-pended to all output files.\n" +
                        "\t\toutput request file: absolute location and name of the output request file.\n" +
                        "\t\toutput folder: folder where the output files should go.\n" +
                        "\t\tstart_time: [integer] start time for the simrultion in seconds after midnight.\n" +
                        "\t\tduration: [integer] simulation duration in seconds.\n";
        return str;
    }

}

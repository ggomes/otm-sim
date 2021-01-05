package output;

import error.OTMException;
import dispatch.Dispatcher;
import cmd.RunParameters;

public interface InterfaceOutput {
    String get_output_file();
    void open() throws OTMException;
    void close() throws OTMException;
//    void write(float timestamp,Object obj) throws OTMException;
    void register(RunParameters props, Dispatcher dispatcher) throws OTMException;
}

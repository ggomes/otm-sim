package core;

import error.OTMErrorLog;

public interface InterfaceScenarioElement {

    Long getId();
    ScenarioElementType getSEType();
    void validate(OTMErrorLog errorLog);
//    void initialize(Scenario scenario) throws OTMException;
//    void register_with_dispatcher(Dispatcher dispatcher);
    Object to_jaxb();

}

package core;

public interface InterfaceScenarioElement {

    Long getId();
    ScenarioElementType getSEType();
    Object to_jaxb();

}

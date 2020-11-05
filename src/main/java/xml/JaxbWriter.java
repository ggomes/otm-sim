package xml;

import error.OTMException;
import jaxb.Scenario;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;

public class JaxbWriter {

    public static void save_scenario(jaxb.Scenario scn,String filename) throws OTMException {
        try {
            create_marshaller().marshal(scn,new File(filename));
        } catch (Exception e) {
            throw new OTMException(e.getMessage());
        }
    }

    private static Marshaller create_marshaller() throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(Scenario.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        return marshaller;
    }

}

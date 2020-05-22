/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package xml;

import error.OTMException;
import jaxb.Scenario;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;

public class JaxbLoader {

    private static HashMap<String,String> test_configs;
    static {
        test_configs = new HashMap<>();

        test_configs.put("line_ctm","line_ctm.xml");
        test_configs.put("line_spaceq","line_spaceq.xml");
        test_configs.put("line_newell","line_newell.xml");
        test_configs.put("intersection","intersection.xml");
        test_configs.put("onramp_nohov","onramp_nohov.xml");
        test_configs.put("onramp_hov","onramp_hov.xml");
        test_configs.put("mixing","mixing.xml");
        test_configs.put("onramp_hov_2link","onramp_hov_2link.xml");
        test_configs.put("onramp_offramp","onramp_offramp.xml");

    }

    public static Collection<String> get_test_config_names() {
        return test_configs.keySet();
    }

    public static jaxb.Scenario load_scenario(String filename, boolean validate) throws OTMException {
        try {
            return (Scenario) create_unmarshaller(validate).unmarshal(new File(filename));
        } catch(org.xml.sax.SAXException e){
            throw new OTMException(e);
        }  catch (JAXBException e) {
            throw new OTMException(e);
        }
    }

    public static jaxb.Scenario load_scenario(InputStream stream, boolean validate) throws OTMException {
        try {
            return (Scenario) create_unmarshaller(validate).unmarshal(stream);
        } catch(org.xml.sax.SAXException e){
            throw new OTMException(e);
        }  catch (JAXBException e) {
            throw new OTMException(e);
        }
    }

    public static jaxb.Scenario load_test_scenario(String configname, boolean validate) throws OTMException {
        InputStream stream = JaxbLoader.class.getClassLoader().getResourceAsStream("test_configs/" + configname);
        return load_scenario(stream,validate);
    }

    private static Unmarshaller create_unmarshaller(boolean validate) throws JAXBException, SAXException {
        JAXBContext jaxbContext = JAXBContext.newInstance(Scenario.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        if(validate){
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            InputStream resourceAsStream = JaxbLoader.class.getResourceAsStream("/otm.xsd");
            Schema schema = sf.newSchema(new StreamSource(resourceAsStream));
            unmarshaller.setSchema(schema);
        }
        return unmarshaller;
    }

}
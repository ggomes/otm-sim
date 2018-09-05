package plugin;

import control.AbstractController;
import error.OTMException;
import runner.Scenario;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

public class PluginLoader {

    public static Map<String,Class<?>> loaded_plugins = new HashMap<>();

    public static void load_plugins( jaxb.Plugins plugins ) {

        if(plugins==null)
            return;

        try {
            for (jaxb.Plugin plugin : plugins.getPlugin()) {
                File folder = new File(plugin.getFolder());
                String clazz_name = plugin.getClazz();
                URL[] urls = new URL[]{folder.toURI().toURL()};
                Class<?> clazz = Class.forName(clazz_name, true, new URLClassLoader(urls));
                loaded_plugins.put(plugin.getName(), clazz);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public static AbstractController get_controller_instance(String plugin_name, Scenario scenario, jaxb.Controller jaxb_controller) throws OTMException {
//        try {


        try {
            Class[] cArg = new Class[2];
            cArg[0] = Scenario.class; //First argument is of *object* type Long
            cArg[1] = jaxb.Controller.class; //Second argument is of *object* type String

            Class<?> clazz = loaded_plugins.get(plugin_name);
            Constructor<?> cnstr = clazz.getDeclaredConstructor(cArg);
            AbstractController controller = (AbstractController) cnstr.newInstance(scenario, jaxb_controller);
            return controller;
        } catch (InstantiationException e) {
            e.printStackTrace();
            return null;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return null;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }

//            return (AbstractController) loaded_plugins.get(plugin_name).newInstance(scenario,jaxb_controller);
//        } catch(InstantiationException e) {
//            throw new OTMException(e);
//        } catch(IllegalAccessException e) {
//            throw new OTMException(e);
//        }
//    }
    }

}

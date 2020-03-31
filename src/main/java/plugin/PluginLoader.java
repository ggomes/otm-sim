package plugin;

import control.AbstractController;
import error.OTMException;
import models.AbstractModel;
import runner.Scenario;
import utils.StochasticProcess;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

public class PluginLoader {

    public static Map<String,Class<AbstractModel>> model_plugins = new HashMap<>();
    public static Map<String,Class<AbstractController>> controller_plugins = new HashMap<>();

    public static void load_plugins( jaxb.Plugins plugins ) throws OTMException {

        if(plugins==null)
            return;

        try {
            for (jaxb.Plugin plugin : plugins.getPlugin()) {
                File folder = new File(plugin.getFolder());
                String clazz_name = plugin.getClazz();
                URL[] urls = new URL[]{folder.toURI().toURL()};
                URLClassLoader cl = URLClassLoader.newInstance(urls);
                Class clazz = cl.loadClass(clazz_name);

                if(AbstractModel.class.isAssignableFrom(clazz)){
                    model_plugins.put(plugin.getName(), clazz);
                } else if(AbstractController.class.isAssignableFrom(clazz)){
                    controller_plugins.put(plugin.getName(), clazz);
                } else {
                    throw new OTMException(String.format("Plugin %s does not extend either AbstractModel or AbstractController.",plugin.getName()));
                }
            }
        } catch (ClassNotFoundException e) {
            throw new OTMException(e);
        } catch (MalformedURLException e) {
            throw new OTMException(e);
        }

    }

    public static AbstractController get_controller_instance(String plugin_name, Scenario scenario, jaxb.Controller jaxb_controller) throws OTMException {

        try {
            Class<AbstractModel> xxx = model_plugins.get("ctmplugin");
            Class[] cArg = new Class[4];
            cArg[0] = AbstractModel.Type.class;
            cArg[1] = String.class;
            cArg[2] = boolean.class;
            cArg[3] = StochasticProcess.class;


            Constructor<?> cnstr = xxx.getDeclaredConstructor(cArg);
            AbstractModel model = (AbstractModel) cnstr.newInstance(null, null, true,null);

        } catch (NoSuchMethodException e) {
            throw new OTMException(e);
        } catch (IllegalAccessException e) {
            throw new OTMException(e);
        } catch (InstantiationException e) {
            throw new OTMException(e);
        } catch (InvocationTargetException e) {
            throw new OTMException(e);
        }


//        try {


//            Class[] cArg = new Class[4];
//            cArg[0] = AbstractModel.Type.class;
//            cArg[1] = String.class;
//        zzzzzcArg[1] = boolean.class;

//            Class<?> clazz = controller_plugins.get(plugin_name);
//            Constructor<?> cnstr = clazz.getDeclaredConstructor(cArg);
//            AbstractController controller = (AbstractController) cnstr.newInstance(scenario, jaxb_controller);
            return null;
//        } catch (InstantiationException e) {
//            e.printStackTrace();
//            return null;
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//            return null;
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//            return null;
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//            return null;
//        }

//            return (AbstractController) loaded_plugins.get(plugin_name).newInstance(scenario,jaxb_controller);
//        } catch(InstantiationException e) {
//            throw new OTMException(e);
//        } catch(IllegalAccessException e) {
//            throw new OTMException(e);
//        }
//    }
    }

    public static AbstractModel get_model_instance(jaxb.Model jaxb_model,StochasticProcess process) throws OTMException {

        String plugin_name = jaxb_model.getType();

        try {

            Class<AbstractModel> xxx = model_plugins.get(plugin_name);
            Class[] cArg = new Class[4];
            cArg[0] = String.class;                // String name
            cArg[1] = boolean.class;               // boolean is_default
            cArg[2] = StochasticProcess.class;     // StochasticProcess process
            cArg[3] = jaxb.ModelParams.class;      // jaxb.ModelParams param

            Constructor<?> cnstr = xxx.getDeclaredConstructor(cArg);
            AbstractModel model = (AbstractModel) cnstr.newInstance(
                    jaxb_model.getName(),
                    jaxb_model.isIsDefault(),
                    process,
                    jaxb_model.getModelParams());

            return model;

        } catch (NoSuchMethodException e) {
            throw new OTMException(e);
        } catch (IllegalAccessException e) {
            throw new OTMException(e);
        } catch (InstantiationException e) {
            throw new OTMException(e);
        } catch (InvocationTargetException e) {
            throw new OTMException(e);
        }
    }
}

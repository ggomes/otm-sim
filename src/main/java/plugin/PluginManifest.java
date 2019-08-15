package plugin;

public class PluginManifest {
    public String clazz;
    public String jarfile;
    public PluginManifest(String clazz, String jarfile) {
        this.clazz = clazz;
        this.jarfile = jarfile;
    }
}

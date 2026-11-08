package legacyconnector.vb1_7_3.net.minecraft.src;

import java.io.*;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.client.Minecraft;
/** GuiAPI 0.10/0.11 settings storage, rooted in the active game instance. */
public class ModSettings {
    public static ArrayList<ModSettings> all=new ArrayList<>();
    public static HashMap<String,String> contextDatadirs=new HashMap<>();
    public static String currentContext="";
    static {contextDatadirs.put("","mods");}
    public ArrayList<Setting<?>> Settings=new ArrayList<>();
    public String backendname;public boolean have_loaded,settingsLoaded;
    private static Minecraft mcinst;
    public ModSettings(String name){backendname=name;all.add(this);}
    public static void presetMcint(Minecraft mc){mcinst=mc;}public static Minecraft getMcinst(){return mcinst==null?Minecraft.mcc$instance():mcinst;}
    public static File getAppDir(String path){return Minecraft.getApplicationDirectory(path);}
    public void append(Setting setting){Settings.add(setting);setting.parent=this;}
    public void remove(Setting setting){Settings.remove(setting);setting.parent=null;}public int size(){return Settings.size();}
    public void resetAll(String context){for(Setting<?> s:Settings)s.reset(context);}public void resetAll(){resetAll(currentContext);}
    public void copyContextAll(String from,String to){for(Setting<?> s:Settings)s.copyContext(from,to);}
    public static void setContext(String context,String directory){if(context==null){currentContext="";return;}contextDatadirs.put(context,directory);currentContext=context;if(!context.isEmpty())loadAll(context);}
    private File file(String context){String dir=contextDatadirs.get(context);return dir==null?null:new File(getAppDir("minecraft/"+dir+"/"+backendname),"guiconfig.properties");}
    public void save(String context){
        if(!have_loaded&&!settingsLoaded)return;File file=file(context);if(file==null)return;
        try{java.nio.file.Files.createDirectories(file.getParentFile().toPath());Properties props=new Properties();for(Setting<?> s:Settings)props.setProperty(s.backendname,s.toString(context));try(OutputStream out=new FileOutputStream(file)){props.store(out,"");}}
        catch(IOException e){System.err.println("[GuiAPI] Cannot save "+file+": "+e);}
    }
    public void load(String context){
        File file=file(context);if(file==null||!file.isFile())return;
        try(InputStream in=new FileInputStream(file)){Properties props=new Properties();props.load(in);for(Setting<?> s:Settings){String text=props.getProperty(s.backendname);if(text!=null)s.fromString(text,context);}}
        catch(IOException|IllegalArgumentException e){System.err.println("[GuiAPI] Cannot load "+file+": "+e);}
    }
    public void load(){load("");have_loaded=true;settingsLoaded=true;}
    public static void loadAll(String context){for(ModSettings settings:new ArrayList<>(all))settings.load(context);}
    public static void dbgout(String text){}
}

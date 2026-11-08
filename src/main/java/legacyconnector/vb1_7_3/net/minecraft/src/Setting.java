package legacyconnector.vb1_7_3.net.minecraft.src;

import java.util.HashMap;
import legacyconnector.vb1_7_3.de.matthiasmann.twl.Widget;
/** GuiAPI typed defaults with per-context overrides. */
public abstract class Setting<T> extends Widget {
    public HashMap<String,T> values=new HashMap<>();
    public T defvalue;public String backendname;public WidgetSetting gui;public ModSettings parent;
    public abstract String toString(String context);
    public abstract void fromString(String value,String context);
    public abstract void set(T value,String context);
    public abstract T get(String context);
    public T get(){return get(ModSettings.currentContext);}
    public void set(T value){set(value,ModSettings.currentContext);}
    public void reset(){reset(ModSettings.currentContext);}public void reset(String context){set(defvalue,context);}
    public void copyContext(String from,String to){values.put(to,values.get(from));}
    protected T value(String context){T v=values.get(context);if(v==null)v=values.get("");return v==null?defvalue:v;}
    protected void initialize(String name,T initial){backendname=name;defvalue=initial;values.put("",initial);}
    protected void updated(){if(gui!=null)gui.update();}
    protected void changed(String context){if(parent!=null)parent.save(context);updated();}
}

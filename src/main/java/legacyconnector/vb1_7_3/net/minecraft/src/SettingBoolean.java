package legacyconnector.vb1_7_3.net.minecraft.src;

public class SettingBoolean extends Setting<Boolean> {
    public SettingBoolean(String name){this(name,false);}public SettingBoolean(String name,Boolean value){initialize(name,value);}
    public Boolean get(String context){return value(context);}
    public void set(Boolean v,String context){values.put(context,v);changed(context);}
    public String toString(String context){return get(context).toString();}
    public void fromString(String text,String context){values.put(context,text.equals("true"));updated();}
}

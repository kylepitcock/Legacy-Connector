package legacyconnector.vb1_7_3.net.minecraft.src;

public class SettingFloat extends Setting<Float> {
    public float min,max,step;
    public SettingFloat(String name){this(name,0f);}
    public SettingFloat(String name,float value){this(name,value,0f,.1f,1f);}
    public SettingFloat(String name,float value,float min,float max){this(name,value,min,.1f,max);}
    public SettingFloat(String name,float value,float min,float step,float max){initialize(name,value);this.min=Math.min(min,max);this.max=Math.max(min,max);this.step=step;}
    public Float get(String context){return value(context);}
    public void set(Float v,String context){values.put(context,step>0?Math.round(v/step)*step:v);changed(context);}
    public String toString(String context){return get(context).toString();}
    public void fromString(String text,String context){values.put(context,Float.valueOf(text));updated();}
}

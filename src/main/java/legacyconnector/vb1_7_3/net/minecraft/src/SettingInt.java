package legacyconnector.vb1_7_3.net.minecraft.src;

public class SettingInt extends Setting<Integer> {
    public int min,max,step;
    public SettingInt(String name){this(name,0);}
    public SettingInt(String name,int value){this(name,value,0,1,100);}
    public SettingInt(String name,int value,int min,int max){this(name,value,min,1,max);}
    public SettingInt(String name,int value,int min,int step,int max){initialize(name,value);this.min=Math.min(min,max);this.max=Math.max(min,max);this.step=step;}
    public Integer get(String context){return value(context);}
    public void set(Integer v,String context){values.put(context,step>1?Math.round(v.floatValue()/step)*step:v);changed(context);}
    public String toString(String context){return get(context).toString();}
    public void fromString(String text,String context){values.put(context,Integer.valueOf(text));updated();}
}

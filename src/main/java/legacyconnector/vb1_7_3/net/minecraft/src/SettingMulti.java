package legacyconnector.vb1_7_3.net.minecraft.src;

public class SettingMulti extends Setting<Integer> {
    public String[] labels;
    public SettingMulti(String name,String... labels){this(name,0,labels);}
    public SettingMulti(String name,int value,String... labels){this.labels=labels;initialize(name,value);}
    public Integer get(String context){return value(context);}
    public void set(Integer v,String context){values.put(context,v);changed(context);}
    public void set(String label,String context){for(int i=0;i<labels.length;i++)if(labels[i].equals(label)){set(i,context);return;}}
    public void set(String label){set(label,ModSettings.currentContext);}
    public String getLabel(String context){return labels[get(context)];}public String getLabel(){return getLabel(ModSettings.currentContext);}
    public String toString(String context){return getLabel(context);}
    public void fromString(String text,String context){int i;for(i=0;i<labels.length;i++)if(labels[i].equals(text))break;values.put(context,i<labels.length?i:Integer.parseInt(text));updated();}
    public void next(String context){int i=get(context)+1;set(i>=labels.length?0:i,context);}public void next(){next(ModSettings.currentContext);}
}

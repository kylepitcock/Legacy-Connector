package legacyconnector.vb1_7_3.net.minecraft.src;

public class WidgetInt extends WidgetSetting implements Runnable {
    public int displaylen;public String formatstring="%s";public WidgetSlider s;public SettingInt value;
    private boolean updating;
    public WidgetInt(SettingInt value,String name){this(value,name,0);}
    public WidgetInt(SettingInt value,String name,int length){super(name);this.value=value;displaylen=length;value.gui=this;s=new WidgetSlider();s.setMinMaxValue(value.min,value.max);add(s);update();s.addCallback(this);}
    public String userString(){return nicename+": "+value.get(ModSettingScreen.guicontext);}
    public void update(){updating=true;try{s.setValue(value.get(ModSettingScreen.guicontext));s.setFormat(userString());}finally{updating=false;}}
    public void run(){if(!updating)value.set(Math.round(s.getValue()),ModSettingScreen.guicontext);}
}

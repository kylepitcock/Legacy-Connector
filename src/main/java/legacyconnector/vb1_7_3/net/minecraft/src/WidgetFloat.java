package legacyconnector.vb1_7_3.net.minecraft.src;

public class WidgetFloat extends WidgetSetting implements Runnable {
    public int displaylen;public String formatstring="%s";public WidgetSlider slider;public SettingFloat value;
    private boolean updating;
    public WidgetFloat(SettingFloat value,String name){this(value,name,2);}
    public WidgetFloat(SettingFloat value,String name,int length){this(value,name,length,"%s");}
    public WidgetFloat(SettingFloat value,String name,int length,String format){super(name);this.value=value;displaylen=length;formatstring=format;value.gui=this;slider=new WidgetSlider();slider.setMinMaxValue(value.min,value.max);add(slider);update();slider.addCallback(this);}
    public String userString(){return nicename+": "+value.get(ModSettingScreen.guicontext);}
    public void update(){updating=true;try{slider.setValue(value.get(ModSettingScreen.guicontext));slider.setFormat(userString());}finally{updating=false;}}
    public void run(){if(!updating)value.set(slider.getValue(),ModSettingScreen.guicontext);}
}

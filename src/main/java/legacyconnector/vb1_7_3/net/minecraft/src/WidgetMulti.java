package legacyconnector.vb1_7_3.net.minecraft.src;

import legacyconnector.vb1_7_3.de.matthiasmann.twl.Button;
public class WidgetMulti extends WidgetSetting implements Runnable {
    public SettingMulti value;public Button b;
    public WidgetMulti(SettingMulti value,String name){super(name);this.value=value;value.gui=this;b=new Button();b.addCallback(this);add(b);update();}
    public String userString(){return nicename+": "+value.getLabel(ModSettingScreen.guicontext);}
    public void update(){b.setText(userString());}public void run(){value.next(ModSettingScreen.guicontext);}
}

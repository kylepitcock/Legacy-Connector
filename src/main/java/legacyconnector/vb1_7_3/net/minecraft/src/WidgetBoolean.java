package legacyconnector.vb1_7_3.net.minecraft.src;

import legacyconnector.vb1_7_3.de.matthiasmann.twl.Button;
public class WidgetBoolean extends WidgetSetting implements Runnable {
    public String ttext,ftext;public Button b;public SettingBoolean value;
    public WidgetBoolean(SettingBoolean value,String name){this(value,name,"true","false");}
    public WidgetBoolean(SettingBoolean value,String name,String yes,String no){super(name);this.value=value;ttext=yes;ftext=no;value.gui=this;b=new Button();b.addCallback(this);add(b);update();}
    public String userString(){return nicename+": "+(value.get(ModSettingScreen.guicontext)?ttext:ftext);}
    public void update(){b.setText(userString());}
    public void run(){value.set(!value.get(ModSettingScreen.guicontext),ModSettingScreen.guicontext);}
}

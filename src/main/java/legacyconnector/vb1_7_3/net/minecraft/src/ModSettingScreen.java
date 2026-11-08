package legacyconnector.vb1_7_3.net.minecraft.src;

import java.util.ArrayList;
import legacyconnector.vb1_7_3.de.matthiasmann.twl.Widget;
public class ModSettingScreen {
    public static ArrayList<ModSettingScreen> modscreens=new ArrayList<>();public static String guicontext="";
    public Widget thewidget;public WidgetClassicTwocolumn w;public String buttontitle,nicename;
    public ModSettingScreen(String name){this(name,name);}
    public ModSettingScreen(String name,String button){nicename=name;buttontitle=button;w=new WidgetClassicTwocolumn();thewidget=new WidgetSimplewindow(w,name);modscreens.add(this);}
    public ModSettingScreen(Widget widget,String name){thewidget=widget;nicename=buttontitle=name;modscreens.add(this);}
    public void append(Widget widget){(w==null?thewidget:w).add(widget);}
    public void remove(Widget widget){(w==null?thewidget:w).removeChild(widget);}
}

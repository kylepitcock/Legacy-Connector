package legacyconnector.vb1_7_3.net.minecraft.src;

import java.util.ArrayList;
import legacyconnector.vb1_7_3.de.matthiasmann.twl.Widget;
/**
 * GuiAPI's {@code ModSettingScreen}. Both GuiAPI generations are served: early names ({@code thewidget}, {@code w}, {@code nicename},
 * {@code buttontitle}, {@code modscreens}) and 0.11's ({@code theWidget}, {@code widgetColumn}, {@code niceName}, {@code buttonTitle},
 * {@code modScreens}); each pair holds the same object.
 */
public class ModSettingScreen {
    public static ArrayList<ModSettingScreen> modscreens=new ArrayList<>();public static ArrayList<ModSettingScreen> modScreens=modscreens;
    public static String guicontext="";public static String guiContext="";
    public Widget thewidget;public WidgetClassicTwocolumn w;public String buttontitle,nicename;
    public Widget theWidget;public WidgetClassicTwocolumn widgetColumn;public String buttonTitle,niceName;
    public ModSettingScreen(String name){this(name,name);}
    public ModSettingScreen(String name,String button){nicename=niceName=name;buttontitle=buttonTitle=button;w=widgetColumn=new WidgetClassicTwocolumn();thewidget=theWidget=new WidgetSimplewindow(w,name);modscreens.add(this);}
    public ModSettingScreen(Widget widget,String name){thewidget=theWidget=widget;nicename=niceName=buttontitle=buttonTitle=name;modscreens.add(this);}
    public void append(Widget widget){(w==null?thewidget:w).add(widget);}
    public void remove(Widget widget){(w==null?thewidget:w).removeChild(widget);}
}

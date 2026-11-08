package legacyconnector.vb1_7_3.net.minecraft.src;

import java.util.ArrayList;
import legacyconnector.vb1_7_3.de.matthiasmann.twl.Widget;
public abstract class WidgetSetting extends Widget {
    public String nicename;public static ArrayList<WidgetSetting> all=new ArrayList<>();
    public WidgetSetting(String name){nicename=name;all.add(this);}
    public void layout(){}
    public static void updateAll(){for(WidgetSetting widget:new ArrayList<>(all))widget.update();}
    public abstract void update();
}

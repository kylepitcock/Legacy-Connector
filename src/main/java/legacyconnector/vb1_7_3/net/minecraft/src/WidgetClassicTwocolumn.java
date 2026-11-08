package legacyconnector.vb1_7_3.net.minecraft.src;

import legacyconnector.vb1_7_3.de.matthiasmann.twl.Widget;
public class WidgetClassicTwocolumn extends Widget {
    public int defaultwidth=150,defaultheight=20,defaultpad=4,split=10,vpad=0;public boolean overridewidth=true,overrideheight=true;
    public WidgetClassicTwocolumn(Widget... widgets){for(Widget widget:widgets)add(widget);}
    public void layout(){}
}

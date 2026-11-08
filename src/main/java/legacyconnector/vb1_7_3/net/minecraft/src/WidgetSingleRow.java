package legacyconnector.vb1_7_3.net.minecraft.src;

import legacyconnector.vb1_7_3.de.matthiasmann.twl.Widget;
public class WidgetSingleRow extends Widget {
    public WidgetSingleRow(){}
    public WidgetSingleRow(int width,int height,Widget... widgets){for(Widget widget:widgets){widget.setSize(width,height);add(widget);}}
}

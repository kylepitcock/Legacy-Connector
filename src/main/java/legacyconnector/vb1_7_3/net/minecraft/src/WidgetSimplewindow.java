package legacyconnector.vb1_7_3.net.minecraft.src;

import legacyconnector.vb1_7_3.de.matthiasmann.twl.*;
public class WidgetSimplewindow extends Widget {
    public String Title;public Widget mainwidget;public Label TitleWidget;public Button BackButton;public WidgetSingleRow buttonbar;
    public int hpad=30,vtop=40,vbot=40;
    public WidgetSimplewindow(){this(new Widget(),"");}public WidgetSimplewindow(Widget widget){this(widget,"");}
    public WidgetSimplewindow(Widget widget,String title){mainwidget=widget;Title=title;init();}
    protected void init(){TitleWidget=new Label(Title);BackButton=new Button("Back");BackButton.addCallback(GuiModScreen::back);buttonbar=new WidgetSingleRow(200,20,BackButton);add(mainwidget);add(TitleWidget);add(buttonbar);}
    public void layout(){}
}

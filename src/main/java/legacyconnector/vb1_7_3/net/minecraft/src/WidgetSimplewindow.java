package legacyconnector.vb1_7_3.net.minecraft.src;
import legacyconnector.vb1_7_3.de.matthiasmann.twl.*;
/**
 * GuiAPI's {@code WidgetSimplewindow}: a title, the content and a back button. Both GuiAPI generations are served: the early names
 * ({@code BackButton}, {@code mainwidget}, {@code TitleWidget}, {@code Title}) and 0.11's ({@code backButton}, {@code mainWidget},
 * {@code titleWidget}, the constructor that leaves the back button out); each pair holds the same widget.
 */
public class WidgetSimplewindow extends Widget {
    public String Title;public Widget mainwidget;public Label TitleWidget;public Button BackButton;public WidgetSingleRow buttonbar;
    public Widget mainWidget;public Label titleWidget;public Button backButton;
    public int hpad=30,vtop=40,vbot=40;
    public int hPadding=30,vTopPadding=40,vBottomPadding=40;
    private final boolean showBack;
    public WidgetSimplewindow(){this(new Widget(),"");}public WidgetSimplewindow(Widget widget){this(widget,"");}
    public WidgetSimplewindow(Widget widget,String title){this(widget,title,Boolean.TRUE);}
    public WidgetSimplewindow(Widget widget,String title,Boolean showBackButton){mainwidget=mainWidget=widget;Title=title;showBack=showBackButton==null||showBackButton;init();}
    protected void init(){
        TitleWidget=titleWidget=new Label(Title);add(mainwidget);add(TitleWidget);
        BackButton=backButton=new Button("Back");BackButton.addCallback(GuiModScreen::back);
        if(showBack){buttonbar=new WidgetSingleRow(200,20,BackButton);add(buttonbar);}
    }
    public void layout(){}
}

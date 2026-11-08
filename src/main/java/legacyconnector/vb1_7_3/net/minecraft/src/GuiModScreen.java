package legacyconnector.vb1_7_3.net.minecraft.src;

import legacyconnector.vb1_7_3.de.matthiasmann.twl.Widget;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.Screen;
import legacyconnector.vb1_7_3.net.minecraft.client.Minecraft;
/** GuiAPI widget trees drawn and controlled by the native legacy screen bridge. */
public class GuiModScreen extends Screen {
    public Screen parentScreen;public boolean drawbg=true;public Widget mainwidget;public static GuiModScreen currentscreen;
    protected GuiModScreen(Screen parent){parentScreen=parent;}
    public GuiModScreen(Screen parent,Widget widget){this(parent);mainwidget=widget;}
    public static void back(){if(currentscreen!=null){Minecraft.mcc$instance().setScreen(currentscreen.parentScreen);currentscreen=currentscreen.parentScreen instanceof GuiModScreen s?s:null;}}
    public static void show(Widget widget){show(new GuiModScreen(currentscreen==null?Minecraft.mcc$instance().currentScreen:currentscreen,widget));}
    private org.kylepitcock.legacyconnector.vb1_7_3.GuiWidgetsB173 controls;
    public static void show(GuiModScreen screen){currentscreen=screen;Minecraft.mcc$instance().setScreen(screen);}
    public static void showSettings(){
        WidgetClassicTwocolumn choices=new WidgetClassicTwocolumn();
        for(var settings:ModSettingScreen.modscreens){var button=new legacyconnector.vb1_7_3.de.matthiasmann.twl.Button(settings.buttontitle);button.addCallback(()->show(settings.thewidget));choices.add(button);}
        show(new WidgetSimplewindow(choices,"Mod settings"));
    }
    @Override public void init(){controls=new org.kylepitcock.legacyconnector.vb1_7_3.GuiWidgetsB173(mainwidget);controls.resize(width,height);}
    @Override public void render(int x,int y,float delta){if(drawbg)org.kylepitcock.legacyconnector.vb1_7_3.GuiDrawB173.fill(0,0,width,height,0xB0101010);if(controls!=null)controls.render(x,y);}
    @Override protected void mouseClicked(int x,int y,int button){if(controls!=null)controls.press(x,y,button);}
    @Override protected void mouseReleased(int x,int y,int button){if(controls!=null)controls.release(x,y,button);}
    @Override public void mcc$mouseDragged(int x,int y,int button){if(controls!=null)controls.drag(x,y,button);}
    @Override public void mcc$mouseScrolled(double amount){if(controls!=null)controls.scroll(amount);}
    @Override protected void keyPressed(char character,int key){if(key==1)back();else if(controls!=null)controls.key(key);}
    public static void clicksound(){org.kylepitcock.legacyconnector.vb1_7_3.GuiDrawB173.click();}
}

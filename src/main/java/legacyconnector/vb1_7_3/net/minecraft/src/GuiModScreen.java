package legacyconnector.vb1_7_3.net.minecraft.src;

import legacyconnector.vb1_7_3.de.matthiasmann.twl.Widget;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.Screen;
import legacyconnector.vb1_7_3.net.minecraft.client.Minecraft;
/** GuiAPI screen identity only. TWL rendering must not silently open a blank screen. */
public class GuiModScreen extends Screen {
    public Screen parentScreen;public boolean drawbg=true;public Widget mainwidget;public static GuiModScreen currentscreen;
    protected GuiModScreen(Screen parent){parentScreen=parent;}
    public GuiModScreen(Screen parent,Widget widget){this(parent);mainwidget=widget;}
    public static void back(){if(currentscreen!=null){Minecraft.mcc$instance().setScreen(currentscreen.parentScreen);currentscreen=currentscreen.parentScreen instanceof GuiModScreen s?s:null;}}
    public static void show(Widget widget){show(new GuiModScreen(Minecraft.mcc$instance().currentScreen,widget));}
    public static void show(GuiModScreen screen){throw new UnsupportedOperationException("GuiAPI settings are available, but TWL screen rendering is not implemented");}
    public static void clicksound(){org.kylepitcock.legacyconnector.vb1_7_3.GuiDrawB173.click();}
}

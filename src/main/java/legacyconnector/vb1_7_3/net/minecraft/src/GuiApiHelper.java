package legacyconnector.vb1_7_3.net.minecraft.src;
import legacyconnector.vb1_7_3.de.matthiasmann.twl.Button;
import legacyconnector.vb1_7_3.de.matthiasmann.twl.model.SimpleButtonModel;
/** GuiAPI 0.11's {@code GuiApiHelper}: shared actions (go back a screen, the click sound) and the button factories. */
public class GuiApiHelper {
    public static final ModAction backModAction=new ModAction(GuiApiHelper.class,"back").setDefaultArguments();
    public static final ModAction clickModAction=new ModAction(GuiApiHelper.class,"click").setDefaultArguments();
    private static void back(){GuiModScreen.back();}
    private static void click(){GuiModScreen.clicksound();}
    public static Button makeButton(String text,ModAction action,Boolean doClickSound){
        var model=new SimpleButtonModel();if(doClickSound==null||doClickSound)model.addActionCallback(clickModAction);if(action!=null)model.addActionCallback(action);
        var button=new Button(model);button.setText(text);return button;
    }
    public static Button makeButton(String text,String methodName,Object target,Boolean doClickSound){return makeButton(text,new ModAction(target,methodName),doClickSound);}
    public static Button makeButton(String text,String methodName,Object target,Boolean doClickSound,Class[] types,Object... arguments){
        return makeButton(text,new ModAction(target,methodName,types).setDefaultArguments(arguments),doClickSound);
    }
}

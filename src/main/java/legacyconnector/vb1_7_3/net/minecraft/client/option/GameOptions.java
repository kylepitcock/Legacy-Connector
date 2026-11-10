package legacyconnector.vb1_7_3.net.minecraft.client.option;

/** Beta keyboard code view; the client adapter updates bindings from native options. */
public class GameOptions {
    public KeyBinding jumpKey=new KeyBinding("key.jump",57);
    public boolean hideHud,thirdPerson,anaglyph3d;
    public int guiScale;
    public boolean fancyGraphics;
    public GameOptions(){mcc$set$fancyGraphics(this,true);}
    private static volatile java.util.function.BooleanSupplier GRAPHICS;
    /** Installed by the client only; dedicated servers keep the original option value. */
    public static void mcc$graphics(java.util.function.BooleanSupplier graphics){GRAPHICS=graphics;}
    public static boolean mcc$get$fancyGraphics(GameOptions options){var graphics=GRAPHICS;return graphics==null?options.fancyGraphics:graphics.getAsBoolean();}
    public static void mcc$set$fancyGraphics(GameOptions options,boolean value){options.fancyGraphics=value;}
}

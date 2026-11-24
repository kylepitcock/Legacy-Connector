package legacyconnector.vb1_7_3.net.minecraft.client.option;

/**
 * Beta's {@code GameSettings}. The native options are the source of truth: on the client the volume, sensitivity, view distance, bobbing,
 * ambient occlusion, frame limit, difficulty and debug-screen values are read from and written to the game's own options through
 * {@link NativeView} (installed by the client); without it (a dedicated server) the plain fields hold the value.
 */
public class GameOptions {
    public KeyBinding jumpKey=new KeyBinding("key.jump",57);
    public KeyBinding forwardKey=new KeyBinding("key.forward",17),leftKey=new KeyBinding("key.left",30),backKey=new KeyBinding("key.back",31),rightKey=new KeyBinding("key.right",32);
    /** Beta's other bindings (LWJGL 2 codes): a mod's screen closes on the inventory key as Beta's container screens did. */
    public KeyBinding inventoryKey=new KeyBinding("key.inventory",18),dropKey=new KeyBinding("key.drop",16),chatKey=new KeyBinding("key.chat",20),
            fogKey=new KeyBinding("key.fog",33),sneakKey=new KeyBinding("key.sneak",42);
    /** Beta's {@code keyBindings}: every binding of the game, for a controls screen. */
    public KeyBinding[] allKeys={forwardKey,leftKey,backKey,rightKey,jumpKey,inventoryKey,dropKey,chatKey,fogKey,sneakKey};
    /** The native options are the source of truth; nothing is read from a Beta options file. */
    public void load(){}
    /** Nothing to write: the native options save themselves. */
    public void save(){}
    public boolean hideHud,thirdPerson,anaglyph3d,advancedOpengl,cinematicMode,debugCamera,discreteScroll;
    public float totalDiscreteScroll;
    public int guiScale;
    public String lastServer="";
    public boolean fancyGraphics;
    // values the client reads from the native options (see NativeView); the fields are what a dedicated server holds
    // (declared so the layer knows the legacy fields; reads and writes of them go through the mcc$ accessors, which keep the values below)
    public float musicVolume,soundVolume,mouseSensitivity;
    public boolean invertYMouse,bobView,ao,debugHud;
    public int viewDistance,fpsLimit,difficulty;
    private float vMusic=1.0F,vSound=1.0F,vSensitivity=0.5F;
    private boolean vInvertY,vBob=true,vAo,vDebug;
    private int vDistance=1,vFps=1,vDifficulty=2;

    public GameOptions(){mcc$set$fancyGraphics(this,true);}
    public GameOptions(legacyconnector.vb1_7_3.net.minecraft.client.Minecraft minecraft,java.io.File directory){this();}

    private static volatile java.util.function.BooleanSupplier GRAPHICS;
    /** Installed by the client only; dedicated servers keep the original option value. */
    public static void mcc$graphics(java.util.function.BooleanSupplier graphics){GRAPHICS=graphics;}
    public static boolean mcc$get$fancyGraphics(GameOptions options){var graphics=GRAPHICS;return graphics==null?options.fancyGraphics:graphics.getAsBoolean();}
    public static void mcc$set$fancyGraphics(GameOptions options,boolean value){options.fancyGraphics=value;}

    /** The game's own options by key: "music", "sound", "sensitivity" (Float), "invertY", "bob", "ao", "debug" (Boolean), "viewDistance", "fps", "difficulty" (Integer, Beta's scale). */
    public interface NativeView {
        Object get(String key);
        void set(String key,Object value);
    }
    private static volatile NativeView NATIVE;
    public static void mcc$native(NativeView view){NATIVE=view;}
    private static float nf(GameOptions o,String key,float own){var n=NATIVE;return n==null?own:((Number)n.get(key)).floatValue();}
    private static int ni(GameOptions o,String key,int own){var n=NATIVE;return n==null?own:((Number)n.get(key)).intValue();}
    private static boolean nb(GameOptions o,String key,boolean own){var n=NATIVE;return n==null?own:(Boolean)n.get(key);}
    private static void put(GameOptions o,String key,Object value){var n=NATIVE;if(n!=null)n.set(key,value);}
    public static float mcc$get$musicVolume(GameOptions o){return nf(o,"music",o.vMusic);}
    public static void mcc$set$musicVolume(GameOptions o,float v){o.vMusic=v;put(o,"music",v);}
    public static float mcc$get$soundVolume(GameOptions o){return nf(o,"sound",o.vSound);}
    public static void mcc$set$soundVolume(GameOptions o,float v){o.vSound=v;put(o,"sound",v);}
    public static float mcc$get$mouseSensitivity(GameOptions o){return nf(o,"sensitivity",o.vSensitivity);}
    public static void mcc$set$mouseSensitivity(GameOptions o,float v){o.vSensitivity=v;put(o,"sensitivity",v);}
    public static boolean mcc$get$invertYMouse(GameOptions o){return nb(o,"invertY",o.vInvertY);}
    public static void mcc$set$invertYMouse(GameOptions o,boolean v){o.vInvertY=v;put(o,"invertY",v);}
    public static boolean mcc$get$bobView(GameOptions o){return nb(o,"bob",o.vBob);}
    public static void mcc$set$bobView(GameOptions o,boolean v){o.vBob=v;put(o,"bob",v);}
    public static boolean mcc$get$ao(GameOptions o){return nb(o,"ao",o.vAo);}
    public static void mcc$set$ao(GameOptions o,boolean v){o.vAo=v;put(o,"ao",v);}
    public static boolean mcc$get$debugHud(GameOptions o){return nb(o,"debug",o.vDebug);}
    public static void mcc$set$debugHud(GameOptions o,boolean v){o.vDebug=v;put(o,"debug",v);}
    /** Beta's render distance: 0 far, 1 normal, 2 short, 3 tiny. */
    public static int mcc$get$viewDistance(GameOptions o){return ni(o,"viewDistance",o.vDistance);}
    public static void mcc$set$viewDistance(GameOptions o,int v){o.vDistance=v;put(o,"viewDistance",v);}
    /** Beta's frame limit: 0 as fast as possible, 1 balanced, 2 power saver. */
    public static int mcc$get$fpsLimit(GameOptions o){return ni(o,"fps",o.vFps);}
    public static void mcc$set$fpsLimit(GameOptions o,int v){o.vFps=v;put(o,"fps",v);}
    public static int mcc$get$difficulty(GameOptions o){return ni(o,"difficulty",o.vDifficulty);}
    public static void mcc$set$difficulty(GameOptions o,int v){o.vDifficulty=v&3;put(o,"difficulty",v&3);}

    /** Beta's {@code getOptionFloatValue}: the value of a slider option (0..1), 0 for the others. */
    public float getFloat(Option option){
        if(option==Option.MUSIC)return mcc$get$musicVolume(this);
        if(option==Option.SOUND)return mcc$get$soundVolume(this);
        if(option==Option.SENSITIVITY)return mcc$get$mouseSensitivity(this);
        return 0.0F;
    }
    public void setFloat(Option option,float value){
        if(option==Option.MUSIC)mcc$set$musicVolume(this,value);
        else if(option==Option.SOUND)mcc$set$soundVolume(this,value);
        else if(option==Option.SENSITIVITY)mcc$set$mouseSensitivity(this,value);
    }
    /** Beta's {@code setOptionValue}: steps a button option to its next value. */
    public void setInt(Option option,int step){
        if(option==Option.INVERT_MOUSE)mcc$set$invertYMouse(this,!mcc$get$invertYMouse(this));
        else if(option==Option.RENDER_DISTANCE)mcc$set$viewDistance(this,(mcc$get$viewDistance(this)+step)&3);
        else if(option==Option.GUI_SCALE)guiScale=(guiScale+step)&3;
        else if(option==Option.VIEW_BOBBING)mcc$set$bobView(this,!mcc$get$bobView(this));
        else if(option==Option.ADVANCED_OPENGL)advancedOpengl=!advancedOpengl;
        else if(option==Option.ANAGLYPH)anaglyph3d=!anaglyph3d;
        else if(option==Option.FRAMERATE_LIMIT)mcc$set$fpsLimit(this,(mcc$get$fpsLimit(this)+step+3)%3);
        else if(option==Option.DIFFICULTY)mcc$set$difficulty(this,(mcc$get$difficulty(this)+step)&3);
        else if(option==Option.GRAPHICS)mcc$set$fancyGraphics(this,!mcc$get$fancyGraphics(this));
        else if(option==Option.AMBIENT_OCCLUSION)mcc$set$ao(this,!mcc$get$ao(this));
    }
    private static String onOff(boolean on){return on?"ON":"OFF";}
    /** Beta's {@code getKeyBinding}: the caption of an option's button. */
    public String getString(Option option){
        String label=switch(option.getName()){
            case "options.music"->"Music";case "options.sound"->"Sound";case "options.invertMouse"->"Invert Mouse";case "options.sensitivity"->"Sensitivity";
            case "options.renderDistance"->"Render Distance";case "options.viewBobbing"->"View Bobbing";case "options.anaglyph"->"3D Anaglyph";
            case "options.advancedOpengl"->"Advanced OpenGL";case "options.framerateLimit"->"Max Framerate";case "options.difficulty"->"Difficulty";
            case "options.graphics"->"Graphics";case "options.ao"->"Smooth Lighting";default->"GUI Scale";};
        if(option.isSlider()){float v=getFloat(option);return label+": "+(v==0.0F?"OFF":(int)(v*100.0F)+"%");}
        return label+": "+switch(option.getName()){
            case "options.invertMouse"->onOff(mcc$get$invertYMouse(this));
            case "options.viewBobbing"->onOff(mcc$get$bobView(this));
            case "options.anaglyph"->onOff(anaglyph3d);
            case "options.advancedOpengl"->onOff(advancedOpengl);
            case "options.ao"->onOff(mcc$get$ao(this));
            case "options.renderDistance"->new String[]{"Far","Normal","Short","Tiny"}[mcc$get$viewDistance(this)&3];
            case "options.framerateLimit"->new String[]{"Max","Balanced","Power saver"}[Math.floorMod(mcc$get$fpsLimit(this),3)];
            case "options.difficulty"->new String[]{"Peaceful","Easy","Normal","Hard"}[mcc$get$difficulty(this)&3];
            case "options.graphics"->mcc$get$fancyGraphics(this)?"Fancy":"Fast";
            default->guiScale==0?"Auto":String.valueOf(guiScale);
        };
    }
}

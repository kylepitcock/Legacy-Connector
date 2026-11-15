package legacyconnector.vb1_7_3.org.lwjgl.input;

/** LWJGL2 repeat preference; native screen input already forwards repeated key events. */
public final class Keyboard {
    private static final ThreadLocal<Boolean> REPEAT=ThreadLocal.withInitial(()->false);
    private static volatile java.util.function.IntPredicate KEYS=key->false;
    public static void mcc$keys(java.util.function.IntPredicate keys){KEYS=keys;}
    public static boolean isKeyDown(int code){
        var minecraft=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance();
        var player=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$get$player(minecraft);
        if(player!=null&&player.mcc$modern instanceof net.minecraft.server.level.ServerPlayer nativePlayer&&code==legacyconnector.vb1_7_3.net.minecraft.client.option.KeyBinding.mcc$get$code(minecraft.options.jumpKey))return org.kylepitcock.legacyconnector.vb1_7_3.EntityFieldsB173.jumpInput(nativePlayer);
        // a server-side tick of a player (a mod's OnTickInGame for that player) reads the keys that player's client reports
        if(player!=null&&player.mcc$modern instanceof net.minecraft.server.level.ServerPlayer nativePlayer)
            return org.kylepitcock.legacyconnector.vb1_7_3.KeyBindingsB173.rawHeld(nativePlayer,code);
        return KEYS.test(code);
    }
    private Keyboard(){}
    public static void enableRepeatEvents(boolean enabled){REPEAT.set(enabled);}
    public static boolean areRepeatEventsEnabled(){return REPEAT.get();}

    // ---- LWJGL2 event state: legacy screens and handlers read the key event being dispatched (the layer dispatches one event at a time)
    private static final ThreadLocal<int[]> EVENT=ThreadLocal.withInitial(()->new int[]{0,0,0});
    private static final ThreadLocal<java.util.ArrayDeque<int[]>> QUEUE=ThreadLocal.withInitial(java.util.ArrayDeque::new);
    private static final java.util.Map<Integer,String> NAMES=new java.util.HashMap<>();
    private static final java.util.Map<String,Integer> INDEX=new java.util.HashMap<>();
    static{
        Object[] names={1,"ESCAPE",2,"1",3,"2",4,"3",5,"4",6,"5",7,"6",8,"7",9,"8",10,"9",11,"0",12,"MINUS",13,"EQUALS",14,"BACK",15,"TAB",
                16,"Q",17,"W",18,"E",19,"R",20,"T",21,"Y",22,"U",23,"I",24,"O",25,"P",26,"LBRACKET",27,"RBRACKET",28,"RETURN",29,"LCONTROL",
                30,"A",31,"S",32,"D",33,"F",34,"G",35,"H",36,"J",37,"K",38,"L",39,"SEMICOLON",40,"APOSTROPHE",41,"GRAVE",42,"LSHIFT",43,"BACKSLASH",
                44,"Z",45,"X",46,"C",47,"V",48,"B",49,"N",50,"M",51,"COMMA",52,"PERIOD",53,"SLASH",54,"RSHIFT",55,"MULTIPLY",56,"LMENU",57,"SPACE",
                58,"CAPITAL",59,"F1",60,"F2",61,"F3",62,"F4",63,"F5",64,"F6",65,"F7",66,"F8",67,"F9",68,"F10",69,"NUMLOCK",70,"SCROLL",
                71,"NUMPAD7",72,"NUMPAD8",73,"NUMPAD9",74,"SUBTRACT",75,"NUMPAD4",76,"NUMPAD5",77,"NUMPAD6",78,"ADD",79,"NUMPAD1",80,"NUMPAD2",
                81,"NUMPAD3",82,"NUMPAD0",83,"DECIMAL",87,"F11",88,"F12",156,"NUMPADENTER",157,"RCONTROL",179,"NUMPADCOMMA",181,"DIVIDE",
                183,"SYSRQ",184,"RMENU",197,"PAUSE",199,"HOME",200,"UP",201,"PRIOR",203,"LEFT",205,"RIGHT",207,"END",208,"DOWN",209,"NEXT",
                210,"INSERT",211,"DELETE",219,"LMETA",220,"RMETA"};
        for(int i=0;i<names.length;i+=2){NAMES.put((Integer)names[i],(String)names[i+1]);INDEX.put((String)names[i+1],(Integer)names[i]);}
    }
    /** LWJGL2 key name of a code ({@code null} when the code names no key), as {@code Keyboard.getKeyName}. */
    public static String getKeyName(int key){return NAMES.get(key);}
    public static int getKeyIndex(String name){Integer code=name==null?null:INDEX.get(name);return code==null?0:code;}
    public static int getKeyCount(){return 256;}
    public static boolean isCreated(){return true;}
    public static void create(){}
    public static void destroy(){}
    public static void poll(){}
    /** Sets the event {@code getEventKey/getEventKeyState/getEventCharacter} report (what one LWJGL2 event looked like to the code handling it). */
    public static void mcc$setEvent(int key,boolean state,char character){var e=EVENT.get();e[0]=key;e[1]=state?1:0;e[2]=character;}
    /** Queues an event for {@link #next()}: a mod loop {@code while(Keyboard.next())} then reads exactly the events fed to it. */
    public static void mcc$queueEvent(int key,boolean state,char character){QUEUE.get().add(new int[]{key,state?1:0,character});}
    public static boolean next(){var event=QUEUE.get().poll();if(event==null)return false;System.arraycopy(event,0,EVENT.get(),0,3);return true;}
    public static int getNumKeyboardEvents(){return QUEUE.get().size();}
    public static int getEventKey(){return EVENT.get()[0];}
    public static boolean getEventKeyState(){return EVENT.get()[1]!=0;}
    public static char getEventCharacter(){return (char)EVENT.get()[2];}
    public static long getEventNanoseconds(){return System.nanoTime();}
    public static boolean isRepeatEvent(){return false;}
}

package legacyconnector.vb1_7_3.net.minecraft.client.option;

public class KeyBinding {
    public String translationKey;
    public int code;
    public volatile boolean pressed;
    private volatile boolean clientPressed;
    private volatile java.util.function.IntSupplier liveCode;
    private volatile java.util.function.BooleanSupplier livePressed;
    private volatile java.util.function.IntConsumer changeCode;
    public KeyBinding(String key,int code){translationKey=key;mcc$set$code(this,code);}
    public void mcc$bind(java.util.function.IntSupplier code,java.util.function.BooleanSupplier pressed,java.util.function.IntConsumer change){liveCode=code;livePressed=pressed;changeCode=change;}
    public static int mcc$get$code(KeyBinding key){return key.liveCode==null?key.code:key.liveCode.getAsInt();}
    public static void mcc$set$code(KeyBinding key,int code){key.code=code;var change=key.changeCode;if(change!=null)change.accept(code);}
    public static boolean mcc$get$pressed(KeyBinding key){
        Boolean actor=org.kylepitcock.legacyconnector.vb1_7_3.KeyBindingsB173.actorPressed(key);
        return actor!=null?actor:key.livePressed==null?key.clientPressed:key.livePressed.getAsBoolean();
    }
    public static void mcc$set$pressed(KeyBinding key,boolean pressed){key.clientPressed=pressed;}
    public boolean mcc$clientPressed(){return clientPressed;}
    public boolean isPressed(){return mcc$get$pressed(this);}
}

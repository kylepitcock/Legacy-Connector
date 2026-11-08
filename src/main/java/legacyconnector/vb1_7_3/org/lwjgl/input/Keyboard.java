package legacyconnector.vb1_7_3.org.lwjgl.input;

/** LWJGL2 repeat preference; native screen input already forwards repeated key events. */
public final class Keyboard {
    private static final ThreadLocal<Boolean> REPEAT=ThreadLocal.withInitial(()->false);
    private static volatile java.util.function.IntPredicate KEYS=key->false;
    public static void mcc$keys(java.util.function.IntPredicate keys){KEYS=keys;}
    public static boolean isKeyDown(int code){
        var minecraft=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance();
        var player=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$get$player(minecraft);
        if(player!=null&&player.mcc$modern instanceof net.minecraft.server.level.ServerPlayer nativePlayer&&code==minecraft.options.jumpKey.code)return org.kylepitcock.legacyconnector.vb1_7_3.EntityFieldsB173.jumpInput(nativePlayer);
        return KEYS.test(code);
    }
    private Keyboard(){}
    public static void enableRepeatEvents(boolean enabled){REPEAT.set(enabled);}
    public static boolean areRepeatEventsEnabled(){return REPEAT.get();}
}

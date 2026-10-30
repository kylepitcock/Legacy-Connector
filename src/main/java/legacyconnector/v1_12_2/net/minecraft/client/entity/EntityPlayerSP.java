package legacyconnector.v1_12_2.net.minecraft.client.entity;

/** The local player: a view of the modern local player. */
public class EntityPlayerSP extends AbstractClientPlayer {
    public EntityPlayerSP(net.minecraft.world.entity.player.Player modern) {
        super(modern);
    }

    private legacyconnector.v1_12_2.net.minecraft.stats.StatisticsManager mcc$stats;
    private legacyconnector.v1_12_2.net.minecraft.stats.RecipeBook mcc$recipeBook;

    /**
     * A client player a mod makes (a free camera, a replay viewer): an entity of the signed-in profile in the world given, with the
     * statistics and recipe book given.
     */
    public EntityPlayerSP(legacyconnector.v1_12_2.net.minecraft.client.Minecraft p_i47378_1_, legacyconnector.v1_12_2.net.minecraft.world.World p_i47378_2_,
            legacyconnector.v1_12_2.net.minecraft.client.network.NetHandlerPlayClient p_i47378_3_, legacyconnector.v1_12_2.net.minecraft.stats.StatisticsManager p_i47378_4_,
            legacyconnector.v1_12_2.net.minecraft.stats.RecipeBook p_i47378_5_) {
        super(p_i47378_2_, (p_i47378_1_ != null ? p_i47378_1_ : legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft()).getSession().getProfile());
        this.mcc$stats = p_i47378_4_;
        this.mcc$recipeBook = p_i47378_5_;
    }

    /** The recipe book: the local player's modern one, or the one a mod-made player was given. */
    public legacyconnector.v1_12_2.net.minecraft.stats.RecipeBook getRecipeBook() {
        if (mcc$entity instanceof net.minecraft.client.player.LocalPlayer lp) return legacyconnector.v1_12_2.net.minecraft.stats.RecipeBook.mcc$of(lp.getRecipeBook());
        return mcc$recipeBook;
    }

    /** The statistics: the local player's (as the server last sent them), or the ones a mod-made player was given. */
    public legacyconnector.v1_12_2.net.minecraft.stats.StatisticsManager getStatFileWriter() {
        if (mcc$entity instanceof net.minecraft.client.player.LocalPlayer lp) return legacyconnector.v1_12_2.net.minecraft.stats.StatisticsManager.mcc$of(lp.getStats());
        return mcc$stats;
    }

    @Override
    public boolean isUser() {
        return true;
    }

    @Override
    public void sendMessage(legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent component) {
        if (component != null) mcc$player().displayClientMessage(component.mcc$modern(), false);
    }

    // ------------------------------------------------------------------ area R2b: the local player's client side

    /** Phantom: the connection to the server. */
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.client.network.NetHandlerPlayClient connection;
    /** Phantom: the movement keys read this tick (a live view of the game's input). */
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.util.MovementInput movementInput;
    /** Phantom: the client. */
    @Deprecated
    public legacyconnector.v1_12_2.net.minecraft.client.Minecraft mc;
    /** Phantoms: the nether portal's screen effect (0-1) now and last tick. */
    @Deprecated
    public float timeInPortal;
    @Deprecated
    public float prevTimeInPortal;
    /** Phantoms: the hand sway following the camera. */
    @Deprecated
    public float renderArmYaw;
    @Deprecated
    public float renderArmPitch;
    @Deprecated
    public float prevRenderArmYaw;
    @Deprecated
    public float prevRenderArmPitch;
    /** Phantoms: the rotation last sent to the server, and the double-tap sprint timer. */
    @Deprecated
    public float lastReportedYaw;
    @Deprecated
    public float lastReportedPitch;
    @Deprecated
    public int sprintToggleTimer;

    private net.minecraft.client.player.LocalPlayer mcc$local() {
        return (net.minecraft.client.player.LocalPlayer) mcc$player();
    }

    public static legacyconnector.v1_12_2.net.minecraft.client.network.NetHandlerPlayClient mcc$get$connection(EntityPlayerSP p) {
        return legacyconnector.v1_12_2.net.minecraft.client.network.NetHandlerPlayClient.mcc$INSTANCE;
    }

    public static legacyconnector.v1_12_2.net.minecraft.util.MovementInput mcc$get$movementInput(EntityPlayerSP p) {
        return legacyconnector.v1_12_2.net.minecraft.util.MovementInput.mcc$of(p.mcc$local().input);
    }

    public static legacyconnector.v1_12_2.net.minecraft.client.Minecraft mcc$get$mc(EntityPlayerSP p) {
        return legacyconnector.v1_12_2.net.minecraft.client.Minecraft.getMinecraft();
    }

    public static float mcc$get$timeInPortal(EntityPlayerSP p) {
        return p.mcc$local().spinningEffectIntensity;
    }

    public static void mcc$set$timeInPortal(EntityPlayerSP p, float v) {
        p.mcc$local().spinningEffectIntensity = v;
    }

    public static float mcc$get$prevTimeInPortal(EntityPlayerSP p) {
        return p.mcc$local().oSpinningEffectIntensity;
    }

    public static void mcc$set$prevTimeInPortal(EntityPlayerSP p, float v) {
        p.mcc$local().oSpinningEffectIntensity = v;
    }

    public static float mcc$get$renderArmYaw(EntityPlayerSP p) {
        return p.mcc$local().yBob;
    }

    public static void mcc$set$renderArmYaw(EntityPlayerSP p, float v) {
        p.mcc$local().yBob = v;
    }

    public static float mcc$get$renderArmPitch(EntityPlayerSP p) {
        return p.mcc$local().xBob;
    }

    public static void mcc$set$renderArmPitch(EntityPlayerSP p, float v) {
        p.mcc$local().xBob = v;
    }

    public static float mcc$get$prevRenderArmYaw(EntityPlayerSP p) {
        return p.mcc$local().yBobO;
    }

    public static void mcc$set$prevRenderArmYaw(EntityPlayerSP p, float v) {
        p.mcc$local().yBobO = v;
    }

    public static float mcc$get$prevRenderArmPitch(EntityPlayerSP p) {
        return p.mcc$local().xBobO;
    }

    public static void mcc$set$prevRenderArmPitch(EntityPlayerSP p, float v) {
        p.mcc$local().xBobO = v;
    }

    private static final java.util.Map<String, java.lang.reflect.Field> LOCAL_FIELDS = new java.util.concurrent.ConcurrentHashMap<>();

    /** A non-public field of the game's local player (mojmap names at run time). */
    private static java.lang.reflect.Field localField(String name) {
        return LOCAL_FIELDS.computeIfAbsent(name, n -> {
            try {
                var f = net.minecraft.client.player.LocalPlayer.class.getDeclaredField(n);
                f.setAccessible(true);
                return f;
            } catch (ReflectiveOperationException | RuntimeException e) {
                return null;
            }
        });
    }

    private Object mcc$read(String name, Object fallback) {
        var f = localField(name);
        try {
            return f == null ? fallback : f.get(mcc$local());
        } catch (ReflectiveOperationException e) {
            return fallback;
        }
    }

    private void mcc$write(String name, Object value) {
        var f = localField(name);
        try {
            if (f != null) f.set(mcc$local(), value);
        } catch (ReflectiveOperationException e) {
            // not writable: the value is lost
        }
    }

    public static float mcc$get$lastReportedYaw(EntityPlayerSP p) {
        return (Float) p.mcc$read("yRotLast", 0.0F);
    }

    public static void mcc$set$lastReportedYaw(EntityPlayerSP p, float v) {
        p.mcc$write("yRotLast", v);
    }

    public static float mcc$get$lastReportedPitch(EntityPlayerSP p) {
        return (Float) p.mcc$read("xRotLast", 0.0F);
    }

    public static void mcc$set$lastReportedPitch(EntityPlayerSP p, float v) {
        p.mcc$write("xRotLast", v);
    }

    public static int mcc$get$sprintToggleTimer(EntityPlayerSP p) {
        return (Integer) p.mcc$read("sprintTriggerTime", 0);
    }

    public static void mcc$set$sprintToggleTimer(EntityPlayerSP p, int v) {
        p.mcc$write("sprintTriggerTime", v);
    }

    /** Sends a chat line to the server ("/..." as a command). */
    public void sendChatMessage(String message) {
        var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
        if (connection == null || message == null) return;
        if (message.startsWith("/")) connection.sendCommand(message.substring(1));
        else connection.sendChat(message);
    }

    /** The player's operator level (what commands the client may suggest). */
    public int getPermissionLevel() {
        return mcc$local().getPermissionLevel();
    }

    /** Whether the player rides something they can make jump (a horse). */
    public boolean isRidingHorse() {
        return mcc$local().jumpableVehicle() != null;
    }

    /** What the player looks at within the distance (blocks only, as 1.12's rayTrace). */
    public legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult rayTrace(double blockReachDistance, float partialTicks) {
        return legacyconnector.v1_12_2.net.minecraft.util.math.RayTraceResult.mcc$of(mcc$player().pick(blockReachDistance, partialTicks, false));
    }

    /** Whether the player is paddling the boat they ride (steering it, which keeps the hands busy). */
    public boolean isRowingBoat() {
        var p = mcc$local();
        return p.getControlledVehicle() instanceof net.minecraft.world.entity.vehicle.Boat && p.isHandsBusy();
    }
}

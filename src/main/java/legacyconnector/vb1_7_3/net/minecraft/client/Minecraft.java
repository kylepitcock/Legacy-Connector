package legacyconnector.vb1_7_3.net.minecraft.client;

import java.io.File;

import legacyconnector.vb1_7_3.net.minecraft.client.font.TextRenderer;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.Screen;
import legacyconnector.vb1_7_3.net.minecraft.client.texture.TextureManager;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.vb1_7_3.MenusB173;

/**
 * Beta 1.7.3 {@code Minecraft}: the static directory helpers and one instance ({@code ModLoader.getMinecraftInstance()}). A ModLoader mod
 * ran on one world, the client's; here its code runs on the server's and the client's level, so {@link #world} is the legacy view of the
 * level of the callback being run (IndustrialCraft 2 decides by {@code mc.theWorld.isRemote} whether it simulates). GUIs find the texture
 * manager and the font here and open screens with {@link #setScreen}; the player, renderer and settings fields are synthesized for now.
 */
public class Minecraft {
    private static final Minecraft INSTANCE = new Minecraft();
    private static final ThreadLocal<Minecraft> SCOPED=new ThreadLocal<>();
    public static void mcc$scope(Minecraft minecraft,Runnable callback){Minecraft previous=SCOPED.get();SCOPED.set(minecraft);try{callback.run();}finally{if(previous==null)SCOPED.remove();else SCOPED.set(previous);}}

    public World world;
    /** Beta single player ran the server in-process: a client world is remote only when it is not the integrated server's. */
    public boolean isWorldRemote(){return world!=null&&world.isRemote;}
    public legacyconnector.vb1_7_3.net.minecraft.client.util.Session session=legacyconnector.vb1_7_3.net.minecraft.client.util.Session.mcc$current();
    private legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity explicitPlayer;
    private record Context(World world,legacyconnector.vb1_7_3.net.minecraft.entity.Entity focus){}
    private static final ThreadLocal<Context> CONTEXT=new ThreadLocal<>();
    private static final ThreadLocal<legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity> PLAYER_OVERRIDE=new ThreadLocal<>();
    public legacyconnector.vb1_7_3.net.minecraft.client.gui.hud.InGameHud inGameHud=new legacyconnector.vb1_7_3.net.minecraft.client.gui.hud.InGameHud();
    public legacyconnector.vb1_7_3.net.minecraft.client.option.GameOptions options=new legacyconnector.vb1_7_3.net.minecraft.client.option.GameOptions();
    public InteractionManager interactionManager = new InteractionManager(this);
    public boolean focused;
    public legacyconnector.vb1_7_3.net.minecraft.stat.PlayerStats stats=new legacyconnector.vb1_7_3.net.minecraft.stat.PlayerStats();
    public static final class ContextScope implements AutoCloseable {
        private final Context previous;private final legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity override;
        private final org.kylepitcock.legacyconnector.vb1_7_3.HookActorStateB173.Scope actor;
        private ContextScope(World world,legacyconnector.vb1_7_3.net.minecraft.entity.Entity focus){
            previous=CONTEXT.get();override=PLAYER_OVERRIDE.get();World previousWorld=mcc$instance().world;
            CONTEXT.set(new Context(world,focus));if(world!=null)mcc$instance().world=world;PLAYER_OVERRIDE.remove();
            try{actor=org.kylepitcock.legacyconnector.vb1_7_3.HookActorStateB173.forContext(world,focus);}
            catch(RuntimeException|Error error){mcc$instance().world=previousWorld;if(previous==null)CONTEXT.remove();else CONTEXT.set(previous);if(override==null)PLAYER_OVERRIDE.remove();else PLAYER_OVERRIDE.set(override);throw error;}
        }
        @Override public void close(){try{actor.close();}finally{if(previous==null)CONTEXT.remove();else CONTEXT.set(previous);if(override==null)PLAYER_OVERRIDE.remove();else PLAYER_OVERRIDE.set(override);}}
    }
    public static ContextScope mcc$context(World world,legacyconnector.vb1_7_3.net.minecraft.entity.Entity focus){return new ContextScope(world,focus);}
    /** Existing callback actor only: directory lookup must never refresh a player view midway through a callback. */
    public static legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity mcc$currentPlayerContext(){
        Context context=CONTEXT.get();
        if(context!=null&&context.focus instanceof legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity player)return player;
        var overridden=PLAYER_OVERRIDE.get();if(overridden!=null)return overridden;
        Minecraft active=SCOPED.get();return active==null?null:active.explicitPlayer;
    }
    /** Phantom field accessor: cached Minecraft references resolve the actor of this callback, rather than another world's player. */
    public static legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity mcc$get$player(Minecraft minecraft){
        return org.kylepitcock.legacyconnector.vb1_7_3.PlayerClassOverridesB173.present(mcc$basePlayer(minecraft));
    }
    private static legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity mcc$basePlayer(Minecraft minecraft){
        Minecraft active=SCOPED.get()==null?minecraft:SCOPED.get();
        if(active.explicitPlayer!=null)return active.explicitPlayer;
        var overridden=PLAYER_OVERRIDE.get();if(overridden!=null)return overridden;
        var actor=MenusB173.currentActor();
        if(actor!=null)return (legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity)org.kylepitcock.legacyconnector.vb1_7_3.EntityViewsB173.wrap(actor);
        Context context=CONTEXT.get();World world=context==null?active.world:context.world;
        if(world!=null&&world.mcc$level()!=null&&!world.isRemote){
            if(context!=null&&context.focus instanceof legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity player)return player;
            net.minecraft.world.entity.player.Player closest=mcc$contextPlayer(world.mcc$level().players(),
                    ()->world.mcc$level() instanceof net.minecraft.server.level.ServerLevel level
                            ?level.getServer().getPlayerList().getPlayers():java.util.List.of(),
                    player->player.level()==world.mcc$level(),
                    player->context==null||context.focus==null?0:player.distanceToSqr(context.focus.x,context.focus.y,context.focus.z));
            return closest==null?null:(legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity)org.kylepitcock.legacyconnector.vb1_7_3.EntityViewsB173.wrap(closest);
        }
        var local=MenusB173.localPlayer();return local==null?null:(legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity)org.kylepitcock.legacyconnector.vb1_7_3.EntityViewsB173.wrap(local);
    }
    /** Beta keeps Minecraft.player during the death screen, after level membership ends. */
    static <P> P mcc$contextPlayer(Iterable<? extends P> levelPlayers,
            java.util.function.Supplier<? extends Iterable<? extends P>> registeredPlayers,
            java.util.function.Predicate<? super P> sameLevel,java.util.function.ToDoubleFunction<? super P> distance){
        P closest=null;double best=Double.POSITIVE_INFINITY;
        for(P player:levelPlayers)if(sameLevel.test(player)){double value=distance.applyAsDouble(player);if(value<best){best=value;closest=player;}}
        if(closest!=null)return closest;
        // The server's connected-player registry retains the real death-screen actor.
        // Do not cache it beyond logout or borrow an actor from another dimension.
        for(P player:registeredPlayers.get())if(sameLevel.test(player)){double value=distance.applyAsDouble(player);if(value<best){best=value;closest=player;}}
        return closest;
    }
    public static void mcc$set$player(Minecraft minecraft,legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity player){
        Minecraft active=SCOPED.get()==null?minecraft:SCOPED.get();
        if(active!=INSTANCE)active.explicitPlayer=player;else if(player==null)PLAYER_OVERRIDE.remove();else PLAYER_OVERRIDE.set(player);
    }
    public int displayWidth = 854;
    public int displayHeight = 480;
    public volatile boolean paused;
    public TextureManager textureManager = TextureManager.mcc$instance();
    public legacyconnector.vb1_7_3.net.minecraft.client.particle.ParticleManager particleManager=new legacyconnector.vb1_7_3.net.minecraft.client.particle.ParticleManager();
    public TextRenderer textRenderer = TextRenderer.mcc$instance();
    public legacyconnector.vb1_7_3.net.minecraft.client.sound.SoundManager soundManager = new legacyconnector.vb1_7_3.net.minecraft.client.sound.SoundManager();
    /** The legacy screen shown, if any. */
    public Screen currentScreen;

    protected Minecraft() {}

    /**
     * Beta's {@code Minecraft.changeWorld(World, String, EntityPlayer)}: a mod that makes the player an instance of its own subclass of the
     * client player (a different gravity, a suit) hands it over here. The subclass's overrides then run for the player of this callback; a
     * plain client player puts the native behaviour back.
     */
    public void setWorld(World world,String message,legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity player){
        org.kylepitcock.legacyconnector.vb1_7_3.PlayerClassOverridesB173.hand(player);
    }

    public static Minecraft mcc$instance() {
        Minecraft scoped=SCOPED.get();return scoped==null?INSTANCE:scoped;
    }

    /** The world legacy code is about to run on (set before a block callback or a mod's tick). */
    public static void mcc$use(World world) {
        if (world != null){mcc$instance().world=world;CONTEXT.set(new Context(world,null));}
    }
    public static void mcc$use(World world,legacyconnector.vb1_7_3.net.minecraft.entity.Entity focus){
        if(world!=null){mcc$instance().world=world;CONTEXT.set(new Context(world,focus));}
    }

    /** MCP {@code displayGuiScreen}: shows a legacy screen on the client (null closes it). */
    public void setScreen(Screen screen) {
        MenusB173.setScreen(screen);
    }

    /** MCP {@code getMinecraftDir()}: the game directory. */
    public static File getRunDirectory() {
        return net.neoforged.fml.loading.FMLPaths.GAMEDIR.get().toFile();
    }

    /** MCP {@code getAppDir(String)}: resolve the old minecraft prefix within the active instance. */
    public static File getApplicationDirectory(String name) {
        String path=name.replace('\\','/');
        if(path.equals("minecraft")||path.equals(".minecraft"))return getRunDirectory();
        if(path.startsWith("minecraft/"))path=path.substring("minecraft/".length());
        else if(path.startsWith(".minecraft/"))path=path.substring(".minecraft/".length());
        return getRunDirectory().toPath().resolve(path).normalize().toFile();
    }
    public void loadResource(String name,File file){
        String path=name.replace('\\','/');int slash=path.indexOf('/');if(slash<0)return;
        String kind=path.substring(0,slash),entry=path.substring(slash+1);
        switch(kind){case "sound","newsound"->soundManager.loadSound(entry,file);case "streaming"->soundManager.loadStreaming(entry,file);case "music","newmusic"->soundManager.loadMusic(entry,file);default->{}}
    }
}

package org.kylepitcock.legacyconnector.vb1_7_3.client;

import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.Screen;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.kylepitcock.legacyconnector.vb1_7_3.GuiDrawB173;
import org.kylepitcock.legacyconnector.vb1_7_3.MenusB173;

/** Client side of b1.7.3 mods: the menu screen, the GUI drawing sink, the client half of {@link MenusB173}, block item subtype models. */
public final class ClientB173 implements MenusB173.Client {
    /** How long a GUI the client's callback built waits for its menu. */
    private static final long PENDING_MS = 10_000;
    /** LWJGL2 codes of shift, control, alt and the meta keys (left and right). */
    static boolean modifier(int code){return code==42||code==54||code==29||code==157||code==56||code==184||code==219||code==220;}

    private Screen pending;
    private long pendingAt;

    private ClientB173() {}

    public static void register(IEventBus modBus) {
        org.kylepitcock.legacyconnector.vb1_7_3.RendererContextB173.VIEW_ROTATION=()->new org.joml.Matrix4f().rotation(
                net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().rotation().conjugate(new org.joml.Quaternionf()));
        // single player: client-side original code reads tile entities from the client's copy of the world, not by waiting for the server
        legacyconnector.vb1_7_3.net.minecraft.world.World.CLIENT_MIRROR=(server,pos)->{
            var level=net.minecraft.client.Minecraft.getInstance().level;
            return level==null||!level.dimension().equals(server.dimension())?null:level.getBlockEntity(pos);
        };
        WorldStorageClientB173.install();
        PlayerPresentationClientB173.install();
        PlayerRendererClientB173.install();
        org.kylepitcock.legacyconnector.vb1_7_3.HudOverlayB173.installClient(text->{
            var mc=Minecraft.getInstance();mc.execute(()->mc.gui.setOverlayMessage(net.minecraft.network.chat.Component.literal(text),true));
        });
        KeyClientB173.register(modBus);
        HighlightClientB173.install();
        LoadingProgressB173.register();
        legacyconnector.vb1_7_3.net.minecraft.client.option.GameOptions.mcc$graphics(Minecraft::useFancyGraphics);
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent event) ->
                event.register(org.kylepitcock.legacyconnector.vb1_7_3.DimensionsB173.EFFECTS, new DimensionEffectsB173()));
        legacyconnector.vb1_7_3.org.lwjgl.input.Keyboard.mcc$keys(code->{
            if(code==0)return false;
            for(var binding:Minecraft.getInstance().options.keyMappings)
                if(KeysB173.lwjgl(binding.getKey().getValue())==code&&binding.isDown())return true;
            // LWJGL2 polled any key, bound or not (mods read their own keys from a properties file); only while no screen takes the input.
            // Modifier keys are read inside screens too: Beta's own container shift-click and mods' trade screens (shift and ctrl change
            // the amount) poll them while their screen is open, and a held modifier types nothing.
            var mc=Minecraft.getInstance();
            if(mc.getWindow()==null||mc.screen!=null&&!modifier(code))return false;
            int glfw=KeysB173.glfw(code);
            return glfw>0&&com.mojang.blaze3d.platform.InputConstants.isKeyDown(mc.getWindow().getWindow(),glfw);
        });
        AnimatedTexturesB173.register(modBus);
        BlockColorsB173.register(modBus);
        ModTicksClientB173.install();
        PlayerInventoryClientB173.install();
        MobClientB173.register(modBus);
        modBus.addListener((net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e) ->
                e.registerEntityRenderer(org.kylepitcock.legacyconnector.LegacyConnector.BETA_VANILLA_ARROW.get(), context ->
                        new net.minecraft.client.renderer.entity.ArrowRenderer<org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173VanillaArrow>(context) {
                            @Override public net.minecraft.resources.ResourceLocation getTextureLocation(org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173VanillaArrow arrow) {
                                return org.kylepitcock.legacyconnector.vb1_7_3.ProjectileTexturesB173.FALLBACK;
                            }
                        }));
        modBus.addListener((net.neoforged.neoforge.client.event.ModelEvent.ModifyBakingResult e)->WorldTextureModelB173.wrap(e.getModels()));
        modBus.addListener((net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e) ->
                e.registerEntityRenderer(org.kylepitcock.legacyconnector.LegacyConnector.BETA_ARROW.get(), context ->
                        new net.minecraft.client.renderer.entity.ArrowRenderer<org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Arrow>(context) {
                            @Override public net.minecraft.resources.ResourceLocation getTextureLocation(org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Arrow arrow) {
                                return arrow.texture();
                            }
                        }));
        modBus.addListener((net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e) ->
                e.registerEntityRenderer(org.kylepitcock.legacyconnector.LegacyConnector.BETA_THROWN.get(),ThrownRendererB173::new));
        modBus.addListener((RegisterMenuScreensEvent e) -> {
            var type = org.kylepitcock.legacyconnector.LegacyConnector.LEGACY_MENU_B1_7_3.get();
            e.register(type, LegacyB173Screen::new);
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").info("b1.7.3 menu screen registered for {}",
                    net.minecraft.core.registries.BuiltInRegistries.MENU.getKey(type));
        });
        // a mod that draws its own block as an item (RenderInvBlock) gets a custom item renderer for those items
        modBus.addListener((net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent e) -> {
            LegacyB173InventoryBlockRendererB173[] renderer = new LegacyB173InventoryBlockRendererB173[1];
            for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                if (item instanceof org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173BlockItem bi
                        && bi.getBlock() instanceof org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Block hosted
                        && org.kylepitcock.legacyconnector.vb1_7_3.BlockRenderB173.inventoryRenderer(hosted.legacy()) != null) {
                    e.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
                        @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                            if (renderer[0] == null) renderer[0] = new LegacyB173InventoryBlockRendererB173();
                            return renderer[0];
                        }
                    }, item);
                }
            }
        });
        // block items with subtypes pick their model per metadata (Minefactory's machines are one block item)
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e) -> e.enqueueWork(() -> {
            for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                if (item instanceof org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Item||item instanceof org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173ArmorItem) {
                    net.minecraft.client.renderer.item.ItemProperties.register(item, org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173BlockItem.META,
                            (stack,level,entity,seed) -> {
                                var legacy=org.kylepitcock.legacyconnector.vb1_7_3.StacksB173.toLegacy(stack);
                                return legacy == null ? 0 : legacy.getDamage()/16.0F;
                            });
                }
                if (item instanceof org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173BlockItem bi && bi.hasSubtypes()) {
                    net.minecraft.client.renderer.item.ItemProperties.register(bi, org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173BlockItem.META,
                            (stack, level, entity, seed) -> org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173BlockItem.meta(stack) / 16.0F);
                }
            }
        }));
        GuiDrawB173.install(new ClientGuiB173());
        MenusB173.installClient(new ClientB173());
        AudioClientB173.install();
        ParticleClientB173.install();
        org.kylepitcock.legacyconnector.vb1_7_3.GuiSessionsB173.install();
    }

    @Override
    public synchronized void openedOnClient(Screen gui) {
        Minecraft mc = Minecraft.getInstance();
        // single player: the server's own call opens it (with the GUI object the server built)
        if (mc.hasSingleplayerServer()) return;
        if (gui instanceof HandledScreen) {
            pending = gui;
            pendingAt = System.currentTimeMillis();
        } else {
            mc.execute(() -> mc.setScreen(new LegacyB173PlainScreen(gui)));
        }
    }

    @Override public net.minecraft.world.entity.player.Player localPlayer(){
        var mc=Minecraft.getInstance();
        legacyconnector.vb1_7_3.net.minecraft.client.option.KeyBinding.mcc$set$code(legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance().options.jumpKey,KeysB173.lwjgl(mc.options.keyJump.getKey().getValue()));
        var legacyOptions=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance().options;
        var set=(java.util.function.BiConsumer<legacyconnector.vb1_7_3.net.minecraft.client.option.KeyBinding,net.minecraft.client.KeyMapping>)(legacy,nativeKey)->
                legacyconnector.vb1_7_3.net.minecraft.client.option.KeyBinding.mcc$set$code(legacy,KeysB173.lwjgl(nativeKey.getKey().getValue()));
        set.accept(legacyOptions.forwardKey,mc.options.keyUp);set.accept(legacyOptions.leftKey,mc.options.keyLeft);
        set.accept(legacyOptions.backKey,mc.options.keyDown);set.accept(legacyOptions.rightKey,mc.options.keyRight);
        legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity.mcc$keyUpdate=(code,pressed)->{
            for(var key:new net.minecraft.client.KeyMapping[]{mc.options.keyUp,mc.options.keyLeft,mc.options.keyDown,mc.options.keyRight,mc.options.keyJump})
                if(KeysB173.lwjgl(key.getKey().getValue())==code)key.setDown(pressed);
        };
        return mc.player;
    }
    private static final class AdvancementAccess {
        static final java.lang.reflect.Field PROGRESS=net.neoforged.fml.util.ObfuscationReflectionHelper.findField(net.minecraft.client.multiplayer.ClientAdvancements.class,"progress");
    }
    @Override public boolean hasAchievement(net.minecraft.resources.ResourceLocation id){
        var connection=Minecraft.getInstance().getConnection();if(connection==null)return false;
        try {
            var progress=(java.util.Map<?,?>)AdvancementAccess.PROGRESS.get(connection.getAdvancements());
            for(var entry:progress.entrySet())if(entry.getKey() instanceof net.minecraft.advancements.AdvancementHolder holder&&holder.id().equals(id)
                    &&entry.getValue() instanceof net.minecraft.advancements.AdvancementProgress value)return value.isDone();
            return false;
        }catch(IllegalAccessException error){throw new IllegalStateException("Cannot read native client advancement progress",error);}
    }

    @Override
    public void showFromServer(Screen gui) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.setScreen(new LegacyB173PlainScreen(gui)));
    }

    @Override
    public synchronized Screen takePending(String guiClass) {
        Screen p = pending;
        if (p == null || !p.getClass().getName().equals(guiClass) || System.currentTimeMillis() - pendingAt > PENDING_MS) return null;
        pending = null;
        return p;
    }

    @Override
    public void setScreen(Screen screen) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (screen == null) {
                if (mc.screen instanceof AbstractContainerScreen<?> && mc.player != null) mc.player.closeContainer();
                else if(mc.screen instanceof LegacyB173PlainScreen plain)plain.onClose();
                else mc.setScreen(null);
            } else if(screen.getClass()==legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.TitleScreen.class){
                mc.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
            } else if (screen.getClass() == legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.ingame.InventoryScreen.class) {
                if (mc.player != null) mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
            } else if(org.kylepitcock.legacyconnector.vb1_7_3.NativeScreenRoutesB173.route(screen)==org.kylepitcock.legacyconnector.vb1_7_3.NativeScreenRoutesB173.Route.ACHIEVEMENTS){
                if(mc.getConnection()!=null)mc.setScreen(new net.minecraft.client.gui.screens.advancements.AdvancementsScreen(mc.getConnection().getAdvancements(),mc.screen));
            } else if(org.kylepitcock.legacyconnector.vb1_7_3.NativeScreenRoutesB173.route(screen)==org.kylepitcock.legacyconnector.vb1_7_3.NativeScreenRoutesB173.Route.STATS){
                if(mc.player!=null)mc.setScreen(new net.minecraft.client.gui.screens.achievement.StatsScreen(mc.screen,mc.player.getStats()));
            } else {
                var parent=mc.screen instanceof LegacyB173PlainScreen plain?plain.returnScreen():
                        mc.screen instanceof net.minecraft.client.gui.screens.options.OptionsScreen?mc.screen:null;
                mc.setScreen(new LegacyB173PlainScreen(screen,parent));
            }
        });
    }

    @Override public void remoteFrame(net.minecraft.nbt.CompoundTag frame){
        Minecraft mc=Minecraft.getInstance();mc.execute(()->{
            if(frame.contains("nativeScreen")){
                Screen target=switch(frame.getString("nativeScreen")){
                    case "ACHIEVEMENTS"->new legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.AchievementsScreen(null);
                    case "STATS"->new legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.StatsScreen(null,null);
                    default->null;
                };
                if(target!=null){if(mc.screen instanceof ServerGuiScreenB173 remote)remote.suspendForNativeScreen();setScreen(target);}return;
            }
            // a menu's screen drawn by the server: to the open container screen of that menu, never a screen of its own
            if(frame.contains("menu")){if(mc.screen instanceof LegacyB173Screen menu&&menu.getMenu().containerId==frame.getInt("menu")&&!frame.getBoolean("closed"))menu.remoteFrame(frame);return;}
            if(frame.getBoolean("closed")){if(mc.screen instanceof ServerGuiScreenB173 remote&&(frame.getInt("id")==0||remote.sessionId()==frame.getInt("id")))remote.closeFromServer();return;}
            if(mc.screen instanceof ServerGuiScreenB173 remote&&remote.sessionId()==frame.getInt("id"))remote.frame(frame);
            else mc.setScreen(new ServerGuiScreenB173(frame));
        });
    }
    @Override
    public String clipboard() {
        try {
            return Minecraft.getInstance().keyboardHandler.getClipboard();
        } catch (RuntimeException e) {
            return "";
        }
    }
}

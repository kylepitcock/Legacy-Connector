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

    private Screen pending;
    private long pendingAt;

    private ClientB173() {}

    public static void register(IEventBus modBus) {
        AnimatedTexturesB173.register(modBus);
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
        // block items with subtypes pick their model per metadata (Minefactory's machines are one block item)
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e) -> e.enqueueWork(() -> {
            for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                if (item instanceof org.kylepitcock.legacyconnector.vb1_7_3.LegacyB173Item) {
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
                else mc.setScreen(null);
            } else {
                mc.setScreen(new LegacyB173PlainScreen(screen));
            }
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

package legacyconnector.v1_6_4.net.minecraft.src.forge;

import legacyconnector.v1_7_10.cpw.mods.fml.common.eventhandler.SubscribeEvent;
import legacyconnector.v1_7_10.net.minecraftforge.common.MinecraftForge;

/** The client-only handler registrations of Forge 3.x as subscribers of the shim's client events (kept apart so a server never loads client types). */
final class ForgeClientEvents {
    private ForgeClientEvents() {}

    static void highlight(IHighlightHandler handler) {
        MinecraftForge.EVENT_BUS.register(new Object() {
            @SubscribeEvent
            public void onHighlight(legacyconnector.v1_7_10.net.minecraftforge.client.event.DrawBlockHighlightEvent e) {
                if (handler.onBlockHighlight(e.context, e.player, e.target, e.subID, e.currentItem, e.partialTicks)) e.setCanceled(true);
            }
        });
    }

    static void renderLast(IRenderWorldLastHandler handler) {
        MinecraftForge.EVENT_BUS.register(new Object() {
            @SubscribeEvent
            public void onRenderLast(legacyconnector.v1_7_10.net.minecraftforge.client.event.RenderWorldLastEvent e) {
                handler.onRenderWorldLast(e.context, e.partialTicks);
            }
        });
    }

    static void sound(ISoundHandler handler) {
        MinecraftForge.EVENT_BUS.register(new Object() {
            @SubscribeEvent
            public void onLoad(legacyconnector.v1_7_10.net.minecraftforge.client.event.sound.SoundLoadEvent e) {
                handler.onSetupAudio(e.manager);
                handler.onLoadSoundSettings(e.manager);
            }

            @SubscribeEvent
            public void onEntitySound(legacyconnector.v1_7_10.net.minecraftforge.event.entity.PlaySoundAtEntityEvent e) {
                String name = handler.onPlaySoundAtEntity(e.entity, e.name, e.volume, e.pitch);
                if (name == null) e.setCanceled(true);
                else e.name = name;
            }
        });
    }

    /** {@code onClientChatRecv}: the text of a chat line that reaches the client, replaced or dropped (null). */
    static void chat(IChatHandler handler) {
        MinecraftForge.EVENT_BUS.register(new Object() {
            @SubscribeEvent
            public void onReceived(legacyconnector.v1_7_10.net.minecraftforge.client.event.ClientChatReceivedEvent e) {
                if (e.message == null) return;
                String text = e.message.getUnformattedText();
                String result = handler.onClientChatRecv(text);
                if (result == null) e.setCanceled(true);
                else if (!result.equals(text)) e.message = new legacyconnector.v1_7_10.net.minecraft.util.ChatComponentText(result);
            }
        });
    }
}

package org.kylepitcock.legacyconnector.v1_7_10.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 1.7.10 had no offhand, advancements, social screen or saved toolbars, so legacy mods default their keys to the ones vanilla
 * took for them later: Thaumcraft's "Change Wand Focus" is F, which now also swaps the held wand into the offhand, and the
 * focus menu never opens. When a legacy binding shares its key with one of those later vanilla bindings and that binding is
 * still on its default key, the vanilla binding is unbound for the session (the player can rebind either in Controls).
 */
public final class LegacyKeyConflicts {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Keys");
    private static boolean done;

    private LegacyKeyConflicts() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            if (!done) {
                done = true;
                resolve(Minecraft.getInstance());
            }
        });
    }

    private static void resolve(Minecraft mc) {
        var o = mc.options;
        List<KeyMapping> later = List.of(o.keySwapOffhand, o.keyAdvancements, o.keySocialInteractions, o.keySaveHotbarActivator, o.keyLoadHotbarActivator);
        boolean changed = false;
        for (var legacy : legacyconnector.v1_7_10.net.minecraft.client.settings.KeyBinding.mcc$registered()) {
            KeyMapping mine = legacy.mcc$modern();
            if (mine.isUnbound()) continue;
            for (KeyMapping vanilla : later) {
                if (vanilla.isDefault() && vanilla.getKey().equals(mine.getKey())) {
                    LOG.info("Key {}: legacy binding '{}' takes it from vanilla '{}' (1.7.10 had no such binding); rebind either in Controls",
                            mine.getKey().getDisplayName().getString(), mine.getName(), vanilla.getName());
                    vanilla.setKey(InputConstants.UNKNOWN);
                    changed = true;
                }
            }
        }
        if (changed) KeyMapping.resetMapping();
    }
}

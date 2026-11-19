package org.kylepitcock.legacyconnector.vb1_7_3;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.EntityMountEvent;

/**
 * Beta had no sneak-to-dismount: a player left a boat or a mod's mount by using it again, which the original code handles. A held sneak
 * key is often one of a Beta vehicle's own controls (an airship descends with it), so the native sneak dismount does not apply to a Beta
 * mod's entity; every other way off (original code, death, teleport, the vehicle leaving) is untouched.
 */
public final class BetaRidingB173 {
    private BetaRidingB173() {}

    public static void init() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(BetaRidingB173::onMount);
    }

    private static void onMount(EntityMountEvent event) {
        if (event.isMounting() || !(event.getEntityMounting() instanceof Player player) || !player.isShiftKeyDown()) return;
        Entity vehicle = event.getEntityBeingMounted();
        if (!(vehicle instanceof B173EntityHost) || vehicle.isRemoved() || !player.isAlive()) return;
        if (sneakDismount()) event.setCanceled(true);
    }

    /** The native dismount of a passenger holding sneak runs from Player.rideTick; original code's dismounts come from elsewhere. */
    static boolean sneakDismount() {
        return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk(frames -> frames.limit(12)
                .anyMatch(frame -> frame.getMethodName().equals("rideTick") && Player.class.isAssignableFrom(frame.getDeclaringClass())));
    }
}

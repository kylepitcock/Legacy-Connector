package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import java.nio.charset.StandardCharsets;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayerMP;

/**
 * Vanilla's own 1.7.10 custom payload channels a legacy client sends ({@code new C17PacketCustomPayload("MC|ItemName",
 * ...)}: Railcraft's anvil): handled on the server as 1.7.10 did, for legacy containers and modern menus alike.
 */
public final class VanillaPayloads1710 {
    private VanillaPayloads1710() {}

    public static void handleServer(String channel, byte[] data, EntityPlayerMP player) {
        if (player == null) return;
        net.minecraft.server.level.ServerPlayer sp = player.mcc$serverPlayer();
        switch (channel) {
            case "MC|ItemName" -> {
                String name = data == null || data.length == 0 ? "" : net.minecraft.util.StringUtil.filterText(new String(data, StandardCharsets.UTF_8));
                if (name.length() > 30) return;
                if (EntityPlayer.mcc$get$openContainer(player) instanceof legacyconnector.v1_7_10.net.minecraft.inventory.ContainerRepair repair) {
                    repair.updateItemName(name);
                } else if (sp.containerMenu instanceof net.minecraft.world.inventory.AnvilMenu anvil && anvil.stillValid(sp)) {
                    anvil.setItemName(name);
                }
            }
            case "MC|TrSel" -> {
                if (data == null || data.length < 4) return;
                int trade = java.nio.ByteBuffer.wrap(data).getInt();
                if (sp.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu merchant && merchant.stillValid(sp)) {
                    merchant.setSelectionHint(trade);
                    merchant.tryMoveItems(trade);
                }
            }
            case "MC|Beacon" -> {
                if (data == null || data.length < 8) return;
                var buf = java.nio.ByteBuffer.wrap(data);
                int primary = buf.getInt(), secondary = buf.getInt();
                if (sp.containerMenu instanceof net.minecraft.world.inventory.BeaconMenu beacon && beacon.stillValid(sp)) {
                    beacon.updateEffects(effect(primary), effect(secondary));
                }
            }
            default -> {
            }
        }
    }

    private static java.util.Optional<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> effect(int legacyId) {
        if (legacyId <= 0) return java.util.Optional.empty();
        var inst = new legacyconnector.v1_7_10.net.minecraft.potion.PotionEffect(legacyId, 1).mcc$modern();
        return inst == null ? java.util.Optional.empty() : java.util.Optional.of(inst.getEffect());
    }
}

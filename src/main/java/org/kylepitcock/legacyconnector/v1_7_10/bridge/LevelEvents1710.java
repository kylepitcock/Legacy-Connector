package org.kylepitcock.legacyconnector.v1_7_10.bridge;

/**
 * 1.7.10 world effect ids ({@code playAuxSFX}, {@code S28PacketEffect}) to modern level events. Most sounds were
 * renumbered; the block-break effect's data changed from {@code id + (meta << 12)} to the modern block state id and the
 * smoke effect's from a 3x3 offset index to a direction.
 */
public final class LevelEvents1710 {
    private LevelEvents1710() {}

    /** {type, data} of the modern event, or null when the legacy effect has no modern counterpart. */
    public static int[] toModern(int type, int data) {
        return switch (type) {
            case 1000 -> new int[] {1000, data};
            case 1001 -> new int[] {1001, data};
            case 1002 -> new int[] {1002, data};
            case 1003 -> new int[] {1006, data};
            case 1004 -> new int[] {1009, data};
            case 1005 -> record(data);
            case 1007 -> new int[] {1015, data};
            case 1008 -> new int[] {1016, data};
            case 1009 -> new int[] {1018, data};
            case 1010 -> new int[] {1019, data};
            case 1011 -> new int[] {1020, data};
            case 1012 -> new int[] {1021, data};
            case 1014 -> new int[] {1024, data};
            case 1015 -> new int[] {1025, data};
            case 1016 -> new int[] {1026, data};
            case 1017 -> new int[] {1027, data};
            case 1018 -> new int[] {1028, data};
            case 1020 -> new int[] {1029, data};
            case 1021 -> new int[] {1030, data};
            case 1022 -> new int[] {1031, data};
            case 2000 -> new int[] {2000, smokeDirection(data)};
            case 2001 -> blockBreak(data);
            case 2002 -> new int[] {2002, potionColor(data)};
            case 2003 -> new int[] {2003, data};
            case 2004 -> new int[] {2004, data};
            case 2005 -> new int[] {1505, data <= 0 ? 15 : data};
            default -> null;
        };
    }

    private static int[] record(int legacyItemId) {
        if (legacyItemId == 0) return new int[] {1011, 0};
        try {
            var item = ItemBridge.byId(legacyItemId);
            if (item == null) return null;
            var modern = ItemBridge.toModern(item, 1, 0);
            return new int[] {1010, net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(modern.getItem())};
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    private static int[] blockBreak(int data) {
        try {
            var block = BlockBridge.byId(data & 4095);
            if (block == null) return null;
            var state = BlockBridge.toModern(block, (data >> 12) & 15);
            return state == null ? null : new int[] {2001, net.minecraft.world.level.block.Block.getId(state)};
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    /** Legacy smoke data is {@code (dx + 1) + (dz + 1) * 3}. */
    private static int smokeDirection(int data) {
        int dx = data % 3 - 1;
        int dz = data / 3 % 3 - 1;
        net.minecraft.core.Direction d = dz < 0 ? net.minecraft.core.Direction.NORTH : dz > 0 ? net.minecraft.core.Direction.SOUTH
                : dx < 0 ? net.minecraft.core.Direction.WEST : dx > 0 ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.UP;
        return d.get3DDataValue();
    }

    /** The splash colour of a legacy potion damage value (its effect's liquid colour). */
    private static int potionColor(int damage) {
        try {
            var effects = legacyconnector.v1_7_10.net.minecraft.potion.PotionHelper.getPotionEffects(damage, false);
            if (effects != null && !effects.isEmpty()) {
                var inst = ((legacyconnector.v1_7_10.net.minecraft.potion.PotionEffect) effects.get(0)).mcc$modern();
                if (inst != null) return inst.getEffect().value().getColor();
            }
        } catch (RuntimeException | LinkageError ignored) {
            // an unknown potion splashes in the default colour
        }
        return 0x385DC6;
    }
}

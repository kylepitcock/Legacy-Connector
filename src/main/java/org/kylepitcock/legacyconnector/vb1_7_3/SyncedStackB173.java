package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;

/**
 * The modern stack shown for one legacy inventory place. b1.7.3 stacks are converted, not shared, so the modern stack handed out stays the
 * same object while the legacy stack is unchanged: modern menu code changes it in place and then calls {@code setChanged}, and
 * {@link #changedInPlace} tells that the change has to be written back.
 */
final class SyncedStackB173 {
    private ItemStack seen;
    private int seenId;
    private int seenCount;
    private int seenDamage;
    private boolean valid;
    private net.minecraft.world.item.ItemStack modern = net.minecraft.world.item.ItemStack.EMPTY;
    private int handedCount;

    /** The modern form of the legacy stack now in place. */
    net.minecraft.world.item.ItemStack get(ItemStack now) {
        if (!valid || !same(now)) {
            seen = now;
            if (now != null) {
                seenId = now.itemId;
                seenCount = now.count;
                seenDamage = now.getDamage();
            }
            modern = StacksB173.toModern(now);
            handedCount = modern.getCount();
            valid = true;
        }
        return modern;
    }

    /** True if modern code changed the stack it was given while the legacy stack stayed as it was. */
    boolean changedInPlace(ItemStack now) {
        return valid && same(now) && modern.getCount() != handedCount;
    }

    net.minecraft.world.item.ItemStack modern() {
        return modern;
    }

    void invalidate() {
        valid = false;
        seen = null;
    }

    private boolean same(ItemStack now) {
        if (now != seen) return false;
        return now == null || now.itemId == seenId && now.count == seenCount && now.getDamage() == seenDamage;
    }
}

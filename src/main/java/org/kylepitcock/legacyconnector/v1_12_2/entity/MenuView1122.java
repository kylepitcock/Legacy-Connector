package org.kylepitcock.legacyconnector.v1_12_2.entity;

import legacyconnector.v1_12_2.net.minecraft.inventory.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.kylepitcock.legacyconnector.v1_12_2.gui.LegacyMenu1122;
import org.kylepitcock.legacyconnector.v1_12_2.gui.ModernContainer1122;

/**
 * The legacy container of a player's modern menu ({@code player.openContainer}, {@code player.inventoryContainer}): a legacy menu's own
 * container, otherwise the GUI layer's view of the modern menu ({@link ModernContainer1122}, one per menu, so legacy identity checks such
 * as {@code openContainer == inventoryContainer} hold).
 */
public final class MenuView1122 {
    private MenuView1122() {}

    public static Container of(AbstractContainerMenu menu) {
        if (menu == null) return null;
        if (menu instanceof LegacyMenu1122 legacy) return legacy.legacy();
        return ModernContainer1122.of(menu);
    }

    /** The modern menu behind a legacy container (its hosting menu or the menu it views), or null. */
    public static AbstractContainerMenu modern(Container container) {
        if (container instanceof ModernContainer1122 view) return view.mcc$modern();
        return container != null && container.mcc$menu instanceof AbstractContainerMenu m ? m : null;
    }
}

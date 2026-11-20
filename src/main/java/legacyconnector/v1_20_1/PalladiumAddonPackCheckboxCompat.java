package legacyconnector.v1_20_1;

import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Replaces Palladium's old Checkbox subclass constructor with the 1.21.1
 * Checkbox builder while retaining its filter callback and list refresh.
 */
public final class PalladiumAddonPackCheckboxCompat {
    private PalladiumAddonPackCheckboxCompat() {}

    public static Checkbox create(Screen screen,
            int x, int y, int width, int ignoredHeight,
            Component label, boolean selected, Consumer<Boolean> consumer, Runnable refresh) {
        Objects.requireNonNull(screen, "screen");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(consumer, "consumer");
        Objects.requireNonNull(refresh, "refresh");
        return Checkbox.builder(label, Minecraft.getInstance().font)
                .pos(x, y)
                .maxWidth(Math.max(1, width))
                .selected(selected)
                .onValueChange((checkbox, value) -> {
                    consumer.accept(value);
                    refresh.run();
                })
                .build();
    }
}

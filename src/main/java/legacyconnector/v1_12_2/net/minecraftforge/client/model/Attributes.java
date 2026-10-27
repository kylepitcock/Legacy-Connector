package legacyconnector.v1_12_2.net.minecraftforge.client.model;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormat;

/** Forge 1.12.2 vertex format helpers: the format models are baked in (position, colour, uv, normal, as the item format). */
public class Attributes {
    public static final VertexFormat DEFAULT_BAKED_FORMAT = DefaultVertexFormats.ITEM;

    public static boolean moreSpecific(VertexFormat first, VertexFormat second) {
        int size = first.getSize();
        if (size != second.getSize()) return false;
        int padding = 0, j = 0;
        for (var element : first.getElements()) {
            while (j < second.getElementCount() && second.getElement(j).getUsage() == legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormatElement.EnumUsage.PADDING) {
                padding += second.getElement(j++).getSize();
            }
            if (j >= second.getElementCount() && padding == 0) {
                if (element.getUsage() != legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormatElement.EnumUsage.PADDING) return false;
                padding -= element.getSize();
                continue;
            }
            if (padding > 0) {
                padding -= element.getSize();
                continue;
            }
            if (!element.equals(second.getElement(j++))) return false;
        }
        return padding == 0 && j == second.getElementCount();
    }
}

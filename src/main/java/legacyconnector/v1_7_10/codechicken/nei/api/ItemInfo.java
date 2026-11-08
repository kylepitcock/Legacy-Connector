package legacyconnector.v1_7_10.codechicken.nei.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.MovingObjectPosition;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** NEI's item information: block highlight handlers (who names the block under the cursor). */
public class ItemInfo {
    public enum Layout {
        HEADER, BODY, FOOTER
    }

    public static final Map<Block, List<IHighlightHandler>> highlightIdentifiers = new LinkedHashMap<>();
    public static final List<IHighlightHandler> highlightHandlers = new ArrayList<>();

    /** The stacks that identify the target, from the highlight handlers registered for its block, in registration order. */
    public static ArrayList<ItemStack> getIdentifierItems(World world, EntityPlayer player, MovingObjectPosition hit) {
        ArrayList<ItemStack> items = new ArrayList<>();
        if (hit == null || world == null) return items;
        Block block = world.getBlock(hit.blockX, hit.blockY, hit.blockZ);
        List<IHighlightHandler> handlers = highlightIdentifiers.get(block);
        if (handlers != null) {
            for (IHighlightHandler h : handlers) {
                ItemStack s = h.identifyHighlight(world, player, hit);
                if (s != null) items.add(s);
            }
        }
        return items;
    }

    /** The tooltip lines the highlight handlers add for the stack (header, body, footer in order). */
    public static List<String> getText(ItemStack itemStack, World world, EntityPlayer player, MovingObjectPosition hit) {
        List<String> lines = new ArrayList<>();
        for (Layout layout : Layout.values()) {
            for (IHighlightHandler h : highlightHandlers) lines = h.handleTextData(itemStack, world, player, hit, lines, layout);
        }
        return lines;
    }
}

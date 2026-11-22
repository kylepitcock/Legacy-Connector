package org.kylepitcock.legacyconnector.core.host;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Creates the property builders of new modern items and blocks. 1.21.1 builds them without an id; from 1.21.2 on both builders
 * need the registry key of the thing being built (setId), so a host bump adds the id parameter here instead of at every bridge.
 * See docs/wiki/Host-Version-Upgrades.md.
 */
public final class HostProperties {
    private HostProperties() {}

    public static Item.Properties item() {
        return new Item.Properties();
    }

    public static BlockBehaviour.Properties block() {
        return BlockBehaviour.Properties.of();
    }
}

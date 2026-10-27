package legacyconnector.v1_12_2.net.minecraftforge.event;

import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.LootTable;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.LootTableManager;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/**
 * A loot table was loaded: handlers add pools to it, edit its pools or replace it; canceling empties it. Fed from the modern
 * {@code LootTableLoadEvent} with a view of the modern table (see {@code Loot1122}).
 */
@Cancelable
public class LootTableLoadEvent extends Event {
    private final ResourceLocation name;
    private LootTable table;
    private final LootTableManager lootTableManager;

    public LootTableLoadEvent(ResourceLocation name, LootTable table, LootTableManager lootTableManager) {
        this.name = name;
        this.table = table;
        this.lootTableManager = lootTableManager;
    }

    public ResourceLocation getName() {
        return name;
    }

    public LootTable getTable() {
        return table;
    }

    public void setTable(LootTable table) {
        this.table = table;
    }

    public LootTableManager getLootTableManager() {
        return lootTableManager;
    }
}

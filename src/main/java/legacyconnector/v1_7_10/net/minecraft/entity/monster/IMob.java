package legacyconnector.v1_7_10.net.minecraft.entity.monster;

import legacyconnector.v1_7_10.net.minecraft.command.IEntitySelector;
import legacyconnector.v1_7_10.net.minecraft.entity.passive.IAnimals;

/** Marker for hostile mobs: their hosts are modern {@code Enemy} monsters. */
public interface IMob extends IAnimals {
    /** Selects hostile mobs (vanilla monsters' views are IMobs too). */
    IEntitySelector mobSelector = e -> e instanceof IMob;
}

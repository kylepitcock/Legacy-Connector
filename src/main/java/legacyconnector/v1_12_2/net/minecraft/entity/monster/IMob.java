package legacyconnector.v1_12_2.net.minecraft.entity.monster;

/** A hostile creature (spawns in the dark, despawns on Peaceful). */
public interface IMob extends legacyconnector.v1_12_2.net.minecraft.entity.passive.IAnimals {
    com.google.common.base.Predicate<legacyconnector.v1_12_2.net.minecraft.entity.Entity> MOB_SELECTOR = e -> e instanceof IMob;
    com.google.common.base.Predicate<legacyconnector.v1_12_2.net.minecraft.entity.Entity> VISIBLE_MOB_SELECTOR = e -> e instanceof IMob && !e.isInvisible();
}

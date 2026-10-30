package legacyconnector.v1_12_2.net.minecraftforge.event;

import legacyconnector.v1_12_2.net.minecraft.world.EnumDifficulty;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** The world's difficulty changed (posted from the modern event). */
public class DifficultyChangeEvent extends Event {
    private final EnumDifficulty difficulty;
    private final EnumDifficulty oldDifficulty;

    public DifficultyChangeEvent(EnumDifficulty difficulty, EnumDifficulty oldDifficulty) {
        this.difficulty = difficulty;
        this.oldDifficulty = oldDifficulty;
    }

    public EnumDifficulty getDifficulty() {
        return difficulty;
    }

    public EnumDifficulty getOldDifficulty() {
        return oldDifficulty;
    }
}

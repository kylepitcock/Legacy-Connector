package legacyconnector.v1_20_1;

import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Supplies the same symlink policy Minecraft uses for discovered pack folders.
 * Do not replace this with a permissive matcher: addon packs must obey the
 * user's allowed_symlinks.txt configuration and Minecraft's default policy.
 */
public final class PalladiumAddonPackCompat {
    private PalladiumAddonPackCompat() {
    }

    public static DirectoryValidator addonPackDirectoryValidator() {
        return LevelStorageSource.parseValidator(
                FMLPaths.GAMEDIR.get().resolve(LevelStorageSource.ALLOWED_SYMLINKS_CONFIG_NAME));
    }
}

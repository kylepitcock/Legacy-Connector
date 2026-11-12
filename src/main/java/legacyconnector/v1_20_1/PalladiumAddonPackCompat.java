package legacyconnector.v1_20_1;

import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.LevelStorageSource;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.neoforged.fml.loading.FMLPaths;
import net.threetag.palladium.addonpack.AddonPackManager;

/**
 * Palladium 4.5.10 addon-pack compatibility for Minecraft 1.21.1.
 *
 * The legacy manager mutates Pack.id to add the "addonpack:" namespace and
 * marks discovered packs as required. In 1.21.1 those values are immutable
 * PackLocationInfo and PackSelectionConfig record data, so create an equivalent
 * Pack that delegates resource opening to the original Pack.
 */
public final class PalladiumAddonPackCompat {
    private PalladiumAddonPackCompat() {
    }

    /** Use Minecraft's configured symlink policy for discovered addon folders. */
    public static DirectoryValidator addonPackDirectoryValidator() {
        return LevelStorageSource.parseValidator(
                FMLPaths.GAMEDIR.get().resolve(LevelStorageSource.ALLOWED_SYMLINKS_CONFIG_NAME));
    }

    /**
     * Rebuild the discovered pack with Palladium's legacy ID prefix and
     * required-selection flag. The original pack remains the resource source;
     * each open obtains a fresh PackResources instance through original.open().
     */
    public static Pack prefixAndRequireAddonPack(Pack original) {
        if (original == null) {
            throw new IllegalArgumentException("Palladium discovered a null addon pack");
        }

        PackLocationInfo previousLocation = original.location();
        PackLocationInfo location = new PackLocationInfo(
                "addonpack:" + original.getId(),
                previousLocation.title(),
                previousLocation.source(),
                previousLocation.knownPackInfo());

        PackSelectionConfig previousSelection = original.selectionConfig();
        PackSelectionConfig selection = new PackSelectionConfig(
                true, previousSelection.defaultPosition(), previousSelection.fixedPosition());

        Pack.ResourcesSupplier resources = new Pack.ResourcesSupplier() {
            @Override
            public PackResources openPrimary(PackLocationInfo ignoredLocation) {
                return original.open();
            }

            @Override
            public PackResources openFull(PackLocationInfo ignoredLocation, Pack.Metadata ignoredMetadata) {
                return original.open();
            }
        };

        Pack migrated = Pack.readMetaAndCreate(
                location, resources, AddonPackManager.getPackType(), selection);
        if (migrated == null) {
            throw new IllegalStateException(
                    "Could not recreate Palladium addon pack after applying its legacy ID and required settings: "
                            + original.getId());
        }
        return migrated;
    }
}

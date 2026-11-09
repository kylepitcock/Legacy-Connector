package legacyconnector.v1_20_1;

import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.IEventBus;

/** Static replacements for Forge 1.20.1 calls whose meaning changed; the 1.20.1 translator redirects the call sites here ({@code CallRewrites1201}). */
public final class Compat1201 {
    private Compat1201() {
    }

    /** Forge's {@code IEventBus.post} returned true when a listener cancelled the event; NeoForge's returns the event. */
    public static boolean post(IEventBus bus, Event event) {
        bus.post(event);
        return event instanceof ICancellableEvent cancellable && cancellable.isCanceled();
    }

    /** {@code new CompoundIngredient(list) {}} (an anonymous subclass that opened the constructor): NeoForge's final class through its factory. */
    public static net.minecraft.world.item.crafting.Ingredient compoundIngredient(java.util.List<net.minecraft.world.item.crafting.Ingredient> children) {
        return net.neoforged.neoforge.common.crafting.CompoundIngredient.of(children.toArray(new net.minecraft.world.item.crafting.Ingredient[0]));
    }

    /**
     * Palladium 4.5.x's addon-pack folder is a child of the game directory. Minecraft
     * 1.21.1 requires the repository's symlink validator explicitly; use the same
     * allowed_symlinks.txt policy as the game's own pack discovery.
     */
    public static net.minecraft.server.packs.repository.FolderRepositorySource folderRepositorySource(
            java.nio.file.Path folder,
            net.minecraft.server.packs.PackType packType,
            net.minecraft.server.packs.repository.PackSource packSource) {
        java.nio.file.Path gameDirectory = folder.getParent();
        if (gameDirectory == null) {
            throw new IllegalArgumentException("Palladium addon-pack directory has no game-directory parent: " + folder);
        }
        net.minecraft.world.level.validation.DirectoryValidator validator =
                net.minecraft.world.level.storage.LevelStorageSource.parseValidator(
                        gameDirectory.resolve("allowed_symlinks.txt"));
        return new net.minecraft.server.packs.repository.FolderRepositorySource(
                folder, packType, packSource, validator);
    }

    
    /**
     * Minecraft 1.21.1 uses resource-pack format 34 and data-pack format 48.
     * Keep Palladium's generated pack.mcmeta accurate for the PackType being loaded.
     */
    public static int packFormat(net.minecraft.server.packs.PackType packType) {
        if (packType == net.minecraft.server.packs.PackType.CLIENT_RESOURCES) return 34;
        if (packType == net.minecraft.server.packs.PackType.SERVER_DATA) return 48;
        throw new IllegalArgumentException("Unsupported Palladium addon-pack type: " + packType);
    }

}

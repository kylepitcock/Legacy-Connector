package legacyconnector.v1_12_2.net.minecraft.world.storage;

import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;

/** The saved data of players ({@code playerdata/<uuid>.dat} of the world). */
public interface IPlayerFileData {
    void writePlayerData(EntityPlayer player);

    /** Loads the player's saved data into the player; the data, or null when there is none. */
    NBTTagCompound readPlayerData(EntityPlayer player);

    /** The ids (UUIDs) of the players with saved data. */
    String[] getAvailablePlayerDat();

    /** The running server's player data, read and written like the modern server does. */
    static IPlayerFileData mcc$server() {
        return PlayerFiles.INSTANCE;
    }

    final class PlayerFiles implements IPlayerFileData {
        static final PlayerFiles INSTANCE = new PlayerFiles();
        private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("LegacyConnector/PlayerData");

        private PlayerFiles() {
        }

        private static java.io.File dir() {
            var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            return server == null ? null : server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).toFile();
        }

        @Override
        public void writePlayerData(EntityPlayer player) {
            var p = player == null ? null : player.mcc$player();
            java.io.File dir = dir();
            if (p == null || dir == null) return;
            try {
                dir.mkdirs();
                var tag = p.saveWithoutId(new net.minecraft.nbt.CompoundTag());
                java.io.File tmp = java.io.File.createTempFile(p.getStringUUID() + "-", ".dat", dir);
                net.minecraft.nbt.NbtIo.writeCompressed(tag, tmp.toPath());
                java.io.File target = new java.io.File(dir, p.getStringUUID() + ".dat");
                java.io.File old = new java.io.File(dir, p.getStringUUID() + ".dat_old");
                net.minecraft.Util.safeReplaceFile(target.toPath(), tmp.toPath(), old.toPath());
            } catch (Exception e) {
                LOG.warn("Failed to save player data for {}", p.getName().getString(), e);
            }
        }

        @Override
        public NBTTagCompound readPlayerData(EntityPlayer player) {
            var p = player == null ? null : player.mcc$player();
            java.io.File dir = dir();
            if (p == null || dir == null) return null;
            java.io.File file = new java.io.File(dir, p.getStringUUID() + ".dat");
            if (!file.isFile()) return null;
            try {
                var tag = net.minecraft.nbt.NbtIo.readCompressed(file.toPath(), net.minecraft.nbt.NbtAccounter.unlimitedHeap());
                p.load(tag);
                return new NBTTagCompound(tag);
            } catch (Exception e) {
                LOG.warn("Failed to load player data for {}", p.getName().getString(), e);
                return null;
            }
        }

        @Override
        public String[] getAvailablePlayerDat() {
            java.io.File dir = dir();
            String[] files = dir == null ? null : dir.list((d, name) -> name.endsWith(".dat"));
            if (files == null) return new String[0];
            for (int i = 0; i < files.length; i++) files[i] = files[i].substring(0, files[i].length() - 4);
            return files;
        }
    }
}

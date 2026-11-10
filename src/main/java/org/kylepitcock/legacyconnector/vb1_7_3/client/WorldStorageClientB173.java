package org.kylepitcock.legacyconnector.vb1_7_3.client;

import net.minecraft.client.Minecraft;
import org.kylepitcock.legacyconnector.vb1_7_3.WorldStorageB173;

/** Installed only on the physical client; never exposes a remote server filesystem. */
public final class WorldStorageClientB173 {
    private WorldStorageClientB173(){}
    public static void install(){WorldStorageB173.installClient(level->{
        var mc=Minecraft.getInstance();
        if(!(level instanceof net.minecraft.client.multiplayer.ClientLevel)||mc.level!=level)return null;
        var integrated=mc.getSingleplayerServer();
        if(integrated!=null)return WorldStorageB173.boundClientRoot(true,integrated.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath(),null,null);
        var server=mc.getCurrentServer();
        if(server==null||mc.getConnection()==null)return null;
        return WorldStorageB173.boundClientRoot(true,null,mc.gameDirectory.toPath().toAbsolutePath(),server.ip);
    });}
}

package org.kylepitcock.legacyconnector.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.Level;
import org.kylepitcock.legacyconnector.v1_7_10.dimension.LegacyDimensions;
import org.kylepitcock.legacyconnector.v1_7_10.dimension.RuntimeDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A player who quit inside a mod dimension that a mod registers only while the server runs (Mystcraft ages, registered from the
 * saved ages when the server starts) logs in before anything asked for that dimension's level. The level does not exist yet, and
 * the server puts the player in the overworld. 1.7.10 created such a world when the player's saved dimension was asked for
 * ({@code WorldServer} of the id), so the level is created here when the saved dimension is a registered mod dimension.
 */
@Mixin(PlayerList.class)
public abstract class PlayerListRuntimeDimensionMixin {
    @WrapOperation(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getLevel(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/server/level/ServerLevel;"), require = 0)
    private ServerLevel legacyconnector$runtimeDimensionLevel(MinecraftServer server, ResourceKey<Level> key, Operation<ServerLevel> original) {
        ServerLevel level = original.call(server, key);
        if (level != null || key == null) return level;
        Integer id = LegacyDimensions.idOf(key.location());
        return id == null ? null : RuntimeDimensions.getOrCreate(server, id);
    }
}

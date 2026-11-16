package org.kylepitcock.legacyconnector.v1_6_4;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * The members of Minecraft 1.2.5's game classes that have no 1.4.7 counterpart (named {@code mcc125$<Feather name>} by the loader) exist on the shim
 * classes under exactly that name and shape: a reference the loader emits links to the real member instead of a synthesized empty stub.
 */
class Members125Test {
    private static final String B = "legacyconnector.v1_7_10.net.minecraft.";

    /** class, member name, parameter classes (shim or JDK names, comma separated, empty for none). */
    private static final String[][] METHODS = {
        {"world.World", "mcc125$getEntity", "int"},
        {"world.World", "getMoonPhase", "float"},
        {"world.World", "mcc125$save", "boolean," + B + "util.IProgressUpdate"},
        {"world.World", "mcc125$forceSave", B + "util.IProgressUpdate"},
        {"world.World", "mcc$get$mcc125$isNew", B + "world.World"},
        {"world.storage.WorldInfo", "mcc125$getDefaultGamemode", ""},
        {"util.AxisAlignedBB", "mcc125$resetPool", ""},
        {"util.AxisAlignedBB", "mcc125$clearCache", ""},
        {"util.Vec3", "mcc125$resetPool", ""},
        {"entity.item.EntityItem", "mcc$get$mcc125$item", B + "entity.item.EntityItem"},
        {"entity.item.EntityItem", "mcc$set$mcc125$item", B + "entity.item.EntityItem," + B + "item.ItemStack"},
        {"entity.EntityLivingBase", "mcc$get$mcc125$stuckArrows", B + "entity.EntityLivingBase"},
        {"entity.EntityLivingBase", "mcc$get$mcc125$swingArm", B + "entity.EntityLivingBase"},
        {"entity.EntityLivingBase", "mcc125$beforeRemove", ""},
        {"entity.EntityLivingBase", "onFinishedPotionEffect", B + "potion.PotionEffect"},
        {"entity.player.EntityPlayer", "mcc$get$mcc125$dimension", B + "entity.player.EntityPlayer"},
        {"entity.player.EntityPlayer", "mcc$get$mcc125$portalTime", B + "entity.player.EntityPlayer"},
        {"entity.player.EntityPlayer", "mcc$get$mcc125$lastPortalTime", B + "entity.player.EntityPlayer"},
        {"entity.player.EntityPlayer", "mcc$get$mcc125$portalCooldown", B + "entity.player.EntityPlayer"},
        {"entity.player.EntityPlayer", "mcc125$pickUp", B + "entity.Entity,int"},
        {"entity.player.EntityPlayer", "faceEntity", B + "entity.Entity,float,float"},
        {"client.entity.EntityPlayerSP", "mcc125$sendChat", "java.lang.String"},
        {"client.multiplayer.PlayerControllerMP", "mcc125$closeMenu", "int," + B + "entity.player.EntityPlayer"},
        {"client.multiplayer.PlayerControllerMP", "mcc125$setGameMode", "boolean"},
        {"client.multiplayer.PlayerControllerMP", "mcc$get$mcc125$creative", B + "client.multiplayer.PlayerControllerMP"},
        {"client.gui.GuiIngame", "mcc125$clearChat", ""},
        {"entity.boss.EntityDragon", "mcc125$getDisplayHealth", ""},
        {"entity.projectile.EntityArrow", "mcc125$shoot", "double,double,double,float,float"},
        {"entity.projectile.EntityArrow", "mcc$get$mcc125$player", B + "entity.projectile.EntityArrow"},
        {"entity.projectile.EntityThrowable", "mcc125$thrown", "double,double,double,float,float"},
        {"entity.item.EntityPainting", "mcc125$setDirection", "int"},
        {"entity.item.EntityPainting", "mcc125$canSurvive", ""},
        {"entity.item.EntityPainting", "mcc$get$mcc125$blockX", B + "entity.item.EntityPainting"},
        {"block.Block", "mcc125$getMiningTime", ""},
        {"block.Block", "getBlockTexture", B + "world.IBlockAccess,int,int,int,int"},
        {"block.BlockContainer", "mcc125$createBlockEntity", ""},
        {"block.BlockContainer", "getBlockEntity", "int"},
        {"item.Item", "onItemUseFirst", B + "item.ItemStack," + B + "entity.player.EntityPlayer," + B + "world.World,int,int,int,int"},
        {"item.Item", "itemInteractionForEntity", B + "item.ItemStack," + B + "entity.EntityLivingBase"},
        {"client.model.ModelBiped", "<init>", "float,float"},
        {"entity.item.EntityTNTPrimed", "<init>", B + "world.World,double,double,double"},
        {"world.WorldSettings", "<init>", "long,int,boolean,boolean," + B + "world.WorldType"},
        {"client.multiplayer.PlayerControllerMP", "<init>", B + "client.Minecraft"},
        {"world.WorldProvider", "getWelcomeMessage", ""},
        {"client.renderer.entity.RenderManager", "cacheActiveRenderInfo", B + "world.World," + B + "client.renderer.texture.TextureManager," + B + "client.gui.FontRenderer," + B + "entity.EntityLivingBase," + B + "client.settings.GameSettings,float"},
        {"client.renderer.tileentity.TileEntityRendererDispatcher", "mcc$get$playerYaw", B + "client.renderer.tileentity.TileEntityRendererDispatcher"},
        {"client.audio.SoundManager", "getSoundsPool", ""},
        {"world.chunk.Chunk", "updateSkylight", ""},
        {"entity.player.EntityPlayer", "addMovementStat", "double,double,double"},
        {"block.BlockPane", "canThisPaneConnectToThisBlockID", "int"},
        {"block.Block", "isBlockSolidOnSide", B + "world.World,int,int,int,int"},
    };

    private static Class<?> cls(String name) throws ClassNotFoundException {
        return switch (name) {
            case "int" -> int.class;
            case "float" -> float.class;
            case "double" -> double.class;
            case "long" -> long.class;
            case "boolean" -> boolean.class;
            default -> Class.forName(name, false, Members125Test.class.getClassLoader());
        };
    }

    @Test
    void everyMemberExistsWithTheShapeTheLoaderEmits() throws Exception {
        for (String[] row : METHODS) {
            Class<?> owner = Class.forName(B + row[0], false, Members125Test.class.getClassLoader());
            String[] names = row[2].isEmpty() ? new String[0] : row[2].split(",");
            Class<?>[] params = new Class<?>[names.length];
            for (int i = 0; i < names.length; i++) params[i] = cls(names[i]);
            if (row[1].equals("<init>")) assertNotNull(owner.getConstructor(params), row[0] + ".<init>");
            else assertNotNull(owner.getMethod(row[1], params), row[0] + "." + row[1]);
        }
    }
}

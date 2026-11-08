package legacyconnector.v1_6_4.forward;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.pathfinding.PathNavigate;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * Call and constructor adapters for members whose signature changed between 1.4.7/1.5.2 and 1.7.10 (the {@code R ... static} and {@code C ... args} rules of
 * {@code forward/1.5.2/member-manual.tsv}, inherited by 1.4.7). Each one used to link to a synthesized empty member.
 */
public final class Forward152 {
    private Forward152() {}

    /** A constant argument for a constructor rule ({@code -1:...#no#()Z}). */
    public static boolean no() {
        return false;
    }

    /** A constant empty name for a constructor rule. */
    public static String empty() {
        return "";
    }

    public static legacyconnector.v1_7_10.net.minecraftforge.common.config.Property.Type stringType() {
        return legacyconnector.v1_7_10.net.minecraftforge.common.config.Property.Type.STRING;
    }

    public static double widen(float f) {
        return f;
    }

    /** 1.5.2 item ids to the item (EntityAITempt took the id of the bait item). */
    public static Item item(int id) {
        return id <= 0 ? null : legacyconnector.v1_6_4.net.minecraft.util.Ids.item(id);
    }

    /** {@code navigator.tryMoveToEntityLiving(target, float speed)}: the speed was a float. */
    public static boolean tryMoveToEntityLiving(PathNavigate nav, EntityLivingBase target, float speed) {
        return nav.tryMoveToEntityLiving(target, speed);
    }

    /** {@code world.getAllCollidingBoundingBoxes(box)}: every block and entity box, no entity excluded. */
    public static List getAllCollidingBoundingBoxes(World world, AxisAlignedBB box) {
        return world.getCollidingBoundingBoxes(null, box);
    }

    /** {@code BlockPistonBase.determineOrientation(world, x, y, z, player)} (static, the placing entity was an EntityPlayer). */
    public static int determineOrientation(World world, int x, int y, int z, EntityPlayer player) {
        return legacyconnector.v1_7_10.net.minecraft.block.BlockPistonBase.determineOrientation(world, x, y, z, player);
    }

    /** {@code FMLRelaunchLog.log.getLogger()}: 1.4.7 answered a java.util.logging logger. */
    public static java.util.logging.Logger relaunchLogger(legacyconnector.v1_7_10.cpw.mods.fml.relauncher.FMLRelaunchLog log) {
        return java.util.logging.Logger.getLogger("ForgeModLoader");
    }
}

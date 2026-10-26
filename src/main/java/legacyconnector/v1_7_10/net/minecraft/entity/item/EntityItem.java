package legacyconnector.v1_7_10.net.minecraft.entity.item;

import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.world.entity.item.ItemEntity;

public class EntityItem extends Entity {
    @Deprecated
    public int delayBeforeCanPickup;
    @Deprecated
    public int age;
    public int lifespan = 6000;
    /** Phantom: render bobbing phase. */
    @Deprecated
    public float hoverStart;
    private float mcc$hover = (float) (Math.random() * Math.PI * 2.0D);

    public EntityItem(World world, double x, double y, double z, ItemStack stack) {
        super(mcc$make(world, x, y, z, ItemStack.mcc$toModern(stack).copy()));
    }

    public EntityItem(World world, double x, double y, double z) {
        super(mcc$make(world, x, y, z, net.minecraft.world.item.ItemStack.EMPTY));
    }

    public EntityItem(World world) {
        this(world, 0, 0, 0);
    }

    /** Mods build detached items for rendering (no world yet): the entity then has no level until it is spawned. */
    private static ItemEntity mcc$make(World world, double x, double y, double z, net.minecraft.world.item.ItemStack stack) {
        if (world == null) {
            ItemEntity e = new ItemEntity(net.minecraft.world.entity.EntityType.ITEM, null);
            e.setPos(x, y, z);
            e.setItem(stack);
            return e;
        }
        return new ItemEntity(world.mcc$fullLevel(), x, y, z, stack);
    }

    public EntityItem(ItemEntity modern) {
        super(modern);
    }

    /** Backing item entity for a subclass hosted as a mod entity (no modern item entity behind it). */
    private ItemEntity mcc$detached;

    public ItemEntity mcc$item() {
        if (mcc$entity instanceof ItemEntity ie) return ie;
        if (mcc$detached == null) {
            mcc$detached = new ItemEntity(net.minecraft.world.entity.EntityType.ITEM, null);
            mcc$detached.setItem(net.minecraft.world.item.ItemStack.EMPTY);
        }
        return mcc$detached;
    }

    public static int mcc$get$delayBeforeCanPickup(EntityItem e) {
        return e.mcc$item().hasPickUpDelay() ? 10 : 0;
    }

    public static void mcc$set$delayBeforeCanPickup(EntityItem e, int v) {
        e.mcc$item().setPickUpDelay(v);
    }

    public static float mcc$get$hoverStart(EntityItem e) {
        return e.mcc$entity instanceof ItemEntity ie ? ie.bobOffs : e.mcc$hover;
    }

    public static void mcc$set$hoverStart(EntityItem e, float v) {
        e.mcc$hover = v;
    }

    public static int mcc$get$age(EntityItem e) {
        return e.mcc$item().getAge();
    }

    public static void mcc$set$age(EntityItem e, int v) {
    }

    public ItemStack getEntityItem() {
        ItemStack stack = ItemStack.mcc$wrap(mcc$item().getItem());
        // 1.7.10 answered an item entity without an item with a stone stack (mods' item entities spawned bare: Botania's
        // thrown items, AE2's growing crystals, and every mob that looks at them)
        if (stack == null && !(mcc$entity instanceof ItemEntity)) {
            return new ItemStack(legacyconnector.v1_7_10.net.minecraft.init.Blocks.stone);
        }
        return stack;
    }

    public void setEntityItemStack(ItemStack stack) {
        mcc$item().setItem(ItemStack.mcc$toModern(stack));
    }

    public String getThrower() {
        var t = mcc$item().getOwner();
        return t == null ? null : t.getName().getString();
    }

    /** The player the item is reserved for, by name (1.7.10 compared names; the modern item keeps the player's id). */
    private String mcc$owner;

    public String getOwner() {
        if (mcc$owner != null) return mcc$owner;
        java.util.UUID target = mcc$item().getTarget();
        if (target == null) return null;
        var server = mcc$server();
        var player = server == null ? null : server.getPlayerList().getPlayer(target);
        return player != null ? player.getGameProfile().getName() : net.neoforged.neoforge.common.UsernameCache.getLastKnownUsername(target);
    }

    /**
     * Reserves the item for one player: only they can pick it up (1.7.10 also let anyone take it in its last ten seconds).
     * The name is resolved to an online player, else to a name the server has seen before; an unknown name reserves nothing.
     */
    public void setOwner(String name) {
        this.mcc$owner = name;
        mcc$item().setTarget(name == null ? null : mcc$uuidOf(name));
    }

    private net.minecraft.server.MinecraftServer mcc$server() {
        var level = mcc$item().level();
        return level == null ? null : level.getServer();
    }

    private java.util.UUID mcc$uuidOf(String name) {
        var server = mcc$server();
        if (server == null) return null;
        var online = server.getPlayerList().getPlayerByName(name);
        if (online != null) return online.getUUID();
        for (var e : net.neoforged.neoforge.common.UsernameCache.getMap().entrySet()) {
            if (name.equalsIgnoreCase(e.getValue())) return e.getKey();
        }
        return null;
    }

    public void setAgeToCreativeDespawnTime() {
        mcc$item().setExtendedLifetime();
    }

    public boolean combineItems(EntityItem other) {
        return false;
    }
}

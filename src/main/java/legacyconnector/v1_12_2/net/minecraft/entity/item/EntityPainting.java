package legacyconnector.v1_12_2.net.minecraft.entity.item;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityHanging;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/** 1.12.2 painting: a view of the modern painting, whose variant is one of the 1.12 arts of the same name. */
public class EntityPainting extends EntityHanging {
    @Deprecated
    public EnumArt art;
    private EnumArt mcc$art = EnumArt.KEBAB;

    public EntityPainting(World world) {
        super(world);
    }

    public EntityPainting(World world, BlockPos pos, EnumFacing facing) {
        super(world, pos);
        updateFacingWithBoundingBox(facing);
    }

    public EntityPainting(World world, BlockPos pos, EnumFacing facing, String title) {
        this(world, pos, facing);
        for (EnumArt a : EnumArt.values()) {
            if (a.title.equals(title)) {
                mcc$set$art(this, a);
                break;
            }
        }
        updateFacingWithBoundingBox(facing);
    }

    protected EntityPainting(net.minecraft.world.entity.Entity modern) {
        super(modern);
    }

    private net.minecraft.world.entity.decoration.Painting mcc$painting() {
        return !mcc$hosted() && mcc$entity instanceof net.minecraft.world.entity.decoration.Painting p ? p : null;
    }

    public static EnumArt mcc$get$art(EntityPainting e) {
        var p = e.mcc$painting();
        if (p != null) {
            String path = p.getVariant().unwrapKey().map(k -> k.location().getPath()).orElse("");
            for (EnumArt a : EnumArt.values()) if (a.name().toLowerCase(java.util.Locale.ROOT).equals(path)) return a;
        }
        return e.mcc$art;
    }

    public static void mcc$set$art(EntityPainting e, EnumArt v) {
        if (v == null) return;
        e.mcc$art = v;
        var p = e.mcc$painting();
        if (p != null) {
            var id = net.minecraft.resources.ResourceLocation.withDefaultNamespace(v.name().toLowerCase(java.util.Locale.ROOT));
            p.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.PAINTING_VARIANT).getHolder(id).ifPresent(h -> {
                var tag = new net.minecraft.nbt.CompoundTag();
                p.saveWithoutId(tag);
                tag.putString("variant", id.toString());
                p.load(tag);
            });
        }
    }

    @Override
    public int getWidthPixels() {
        return mcc$get$art(this).sizeX;
    }

    @Override
    public int getHeightPixels() {
        return mcc$get$art(this).sizeY;
    }

    @Override
    public void onBroken(Entity brokenEntity) {
        if (mcc$entity == null || !mcc$entity.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOENTITYDROPS)) return;
        playSound(org.kylepitcock.legacyconnector.v1_12_2.entity.Sounds1122.legacy(net.minecraft.sounds.SoundEvents.PAINTING_BREAK), 1.0F, 1.0F);
        if (brokenEntity instanceof legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer p && p.isCreative()) return;
        entityDropItem(new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(legacyconnector.v1_12_2.net.minecraft.item.Item.getByNameOrId("minecraft:painting")), 0.0F);
    }

    @Override
    public void playPlaceSound() {
        playSound(org.kylepitcock.legacyconnector.v1_12_2.entity.Sounds1122.legacy(net.minecraft.sounds.SoundEvents.PAINTING_PLACE), 1.0F, 1.0F);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        tag.setString("Motive", mcc$get$art(this).title);
        super.writeEntityToNBT(tag);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        String motive = tag.getString("Motive");
        for (EnumArt a : EnumArt.values()) if (a.title.equals(motive)) mcc$art = a;
        super.readEntityFromNBT(tag);
    }

    /** The 1.12 paintings: title, size in pixels and texture offset on the painting sheet. */
    public enum EnumArt {
        KEBAB("Kebab", 16, 16, 0, 0), AZTEC("Aztec", 16, 16, 16, 0), ALBAN("Alban", 16, 16, 32, 0), AZTEC_2("Aztec2", 16, 16, 48, 0),
        BOMB("Bomb", 16, 16, 64, 0), PLANT("Plant", 16, 16, 80, 0), WASTELAND("Wasteland", 16, 16, 96, 0), POOL("Pool", 32, 16, 0, 32),
        COURBET("Courbet", 32, 16, 32, 32), SEA("Sea", 32, 16, 64, 32), SUNSET("Sunset", 32, 16, 96, 32), CREEBET("Creebet", 32, 16, 128, 32),
        WANDERER("Wanderer", 16, 32, 0, 64), GRAHAM("Graham", 16, 32, 16, 64), MATCH("Match", 32, 32, 0, 128), BUST("Bust", 32, 32, 32, 128),
        STAGE("Stage", 32, 32, 64, 128), VOID("Void", 32, 32, 96, 128), SKULL_AND_ROSES("SkullAndRoses", 32, 32, 128, 128),
        WITHER("Wither", 32, 32, 160, 128), FIGHTERS("Fighters", 64, 32, 0, 96), POINTER("Pointer", 64, 64, 0, 192),
        PIGSCENE("Pigscene", 64, 64, 64, 192), BURNING_SKULL("BurningSkull", 64, 64, 128, 192), SKELETON("Skeleton", 64, 48, 192, 64),
        DONKEY_KONG("DonkeyKong", 64, 48, 192, 112);

        public static final int MAX_NAME_LENGTH = "SkullAndRoses".length();
        public final String title;
        public final int sizeX;
        public final int sizeY;
        public final int offsetX;
        public final int offsetY;

        EnumArt(String title, int sizeX, int sizeY, int offsetX, int offsetY) {
            this.title = title;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }
    }
}

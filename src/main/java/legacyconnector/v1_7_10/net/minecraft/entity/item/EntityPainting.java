package legacyconnector.v1_7_10.net.minecraft.entity.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityHanging;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.entity.player.PlayerCapabilities;
import legacyconnector.v1_7_10.net.minecraft.init.Items;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.decoration.Painting;

/**
 * A painting. Constructing this exact class makes a modern painting (Twilight Forest hangs them in its towers); its art is the
 * modern painting's variant (the 1.7.10 paintings are all still there, as {@code minecraft:<snake_case title>}).
 */
public class EntityPainting extends EntityHanging {
    /** Phantom: the picture (redirected to {@code mcc$get$art}). */
    @Deprecated
    public EnumArt art;
    private EnumArt mcc$local_art = EnumArt.Kebab;

    public EntityPainting(World world) {
        super(world);
    }

    /** A painting on the given side of the block, of a random art that fits there. */
    public EntityPainting(World world, int x, int y, int z, int direction) {
        super(world, x, y, z, direction);
        List<EnumArt> fitting = new ArrayList<>();
        for (EnumArt candidate : EnumArt.values()) {
            mcc$set$art(this, candidate);
            setDirection(direction);
            if (onValidSurface()) fitting.add(candidate);
        }
        if (!fitting.isEmpty()) mcc$set$art(this, fitting.get(rand.nextInt(fitting.size())));
        setDirection(direction);
    }

    /** Client: the painting with the art of this title. */
    public EntityPainting(World world, int x, int y, int z, int direction, String title) {
        this(world, x, y, z, direction);
        for (EnumArt candidate : EnumArt.values()) {
            if (candidate.title.equals(title)) {
                mcc$set$art(this, candidate);
                break;
            }
        }
        setDirection(direction);
    }

    /** View of a modern painting. */
    public EntityPainting(Painting modern) {
        super(modern);
    }

    private Painting mcc$painting() {
        return mcc$attached() instanceof Painting p ? p : null;
    }

    public static EnumArt mcc$get$art(EntityPainting e) {
        var p = e.mcc$painting();
        if (p != null) {
            var key = p.getVariant().unwrapKey();
            if (key.isPresent()) {
                EnumArt art = EnumArt.mcc$byId(key.get().location());
                if (art != null) return art;
            }
        }
        return e.mcc$local_art;
    }

    public static void mcc$set$art(EntityPainting e, EnumArt art) {
        e.mcc$local_art = art == null ? EnumArt.Kebab : art;
        var p = e.mcc$painting();
        if (p == null) return;
        var key = ResourceKey.create(Registries.PAINTING_VARIANT, e.mcc$local_art.mcc$id());
        p.registryAccess().registryOrThrow(Registries.PAINTING_VARIANT).getHolder(key).ifPresent(holder -> {
            p.setVariant(holder);
            // the box depends on the size of the picture
            org.kylepitcock.legacyconnector.v1_7_10.entity.ModernAccess.setHangingDirection(p, p.getDirection());
        });
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        tag.setString("Motive", mcc$get$art(this).title);
        super.writeEntityToNBT(tag);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        String motive = tag.getString("Motive");
        EnumArt found = EnumArt.Kebab;
        for (EnumArt candidate : EnumArt.values()) {
            if (candidate.title.equals(motive)) found = candidate;
        }
        mcc$set$art(this, found);
        super.readEntityFromNBT(tag);
    }

    @Override
    public int getWidthPixels() {
        return mcc$get$art(this).sizeX;
    }

    @Override
    public int getHeightPixels() {
        return mcc$get$art(this).sizeY;
    }

    /** Drops the painting item (not for a creative player). */
    @Override
    public void onBroken(Entity breaker) {
        if (breaker instanceof EntityPlayer p && PlayerCapabilities.mcc$get$isCreativeMode(p.capabilities)) return;
        entityDropItem(new ItemStack(Items.painting), 0.0F);
    }

    /** The 1.7.10 paintings: title, size and position on the painting texture (pixels). */
    public static enum EnumArt {
        Kebab("Kebab", 16, 16, 0, 0),
        Aztec("Aztec", 16, 16, 16, 0),
        Alban("Alban", 16, 16, 32, 0),
        Aztec2("Aztec2", 16, 16, 48, 0),
        Bomb("Bomb", 16, 16, 64, 0),
        Plant("Plant", 16, 16, 80, 0),
        Wasteland("Wasteland", 16, 16, 96, 0),
        Pool("Pool", 32, 16, 0, 32),
        Courbet("Courbet", 32, 16, 32, 32),
        Sea("Sea", 32, 16, 64, 32),
        Sunset("Sunset", 32, 16, 96, 32),
        Creebet("Creebet", 32, 16, 128, 32),
        Wanderer("Wanderer", 16, 32, 0, 64),
        Graham("Graham", 16, 32, 16, 64),
        Match("Match", 32, 32, 0, 128),
        Bust("Bust", 32, 32, 32, 128),
        Stage("Stage", 32, 32, 64, 128),
        Void("Void", 32, 32, 96, 128),
        SkullAndRoses("SkullAndRoses", 32, 32, 128, 128),
        Wither("Wither", 32, 32, 160, 128),
        Fighters("Fighters", 64, 32, 0, 96),
        Pointer("Pointer", 64, 64, 0, 192),
        Pigscene("Pigscene", 64, 64, 64, 192),
        BurningSkull("BurningSkull", 64, 64, 128, 192),
        Skeleton("Skeleton", 64, 48, 192, 64),
        DonkeyKong("DonkeyKong", 64, 48, 192, 112);

        public static final int maxArtTitleLength = "SkullAndRoses".length();
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

        /** The modern painting variant: {@code minecraft:} and the title in snake case ("SkullAndRoses" is skull_and_roses). */
        public ResourceLocation mcc$id() {
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < title.length(); i++) {
                char c = title.charAt(i);
                if (Character.isUpperCase(c) && i > 0) b.append('_');
                b.append(Character.toLowerCase(c));
            }
            return ResourceLocation.withDefaultNamespace(b.toString().toLowerCase(Locale.ROOT));
        }

        static EnumArt mcc$byId(ResourceLocation id) {
            for (EnumArt art : values()) if (art.mcc$id().equals(id)) return art;
            return null;
        }
    }
}

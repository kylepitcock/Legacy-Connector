package legacyconnector.v1_12_2.net.minecraft.tileentity;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.item.EnumDyeColor;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList;
import legacyconnector.v1_12_2.net.minecraft.util.text.ITextComponent;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentString;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextComponentTranslation;

/**
 * 1.12.2's banner: a base colour and a list of coloured patterns ({@code Patterns}: {@code Pattern} hash, {@code Color} dye damage),
 * and the texture cache key made from them. Banner items of the game carry their patterns as a component: the static helpers read those
 * too. A banner of the game is seen through a view reading the modern banner.
 */
public class TileEntityBanner extends TileEntity implements org.kylepitcock.legacyconnector.v1_12_2.bridge.TileViews1122.Refreshable {
    private String name;
    private EnumDyeColor baseColor = EnumDyeColor.BLACK;
    private NBTTagList patterns;
    private boolean patternDataSet;
    private List<BannerPattern> patternList;
    private List<EnumDyeColor> colorList;
    private String patternResourceLocation;
    private final net.minecraft.world.level.block.entity.BannerBlockEntity mcc$banner;

    public TileEntityBanner() {
        this.mcc$banner = null;
    }

    /** A view of a game banner. */
    public TileEntityBanner(net.minecraft.world.level.block.entity.BannerBlockEntity banner) {
        this.mcc$banner = banner;
        mcc$refresh();
    }

    @Override
    public void mcc$refresh() {
        if (mcc$banner == null) return;
        baseColor = EnumDyeColor.byMetadata(mcc$banner.getBaseColor().getId());
        patterns = mcc$patternsTag(mcc$banner.getPatterns());
        name = mcc$banner.getCustomName() == null ? null : mcc$banner.getCustomName().getString();
        patternList = null;
        colorList = null;
        patternResourceLocation = null;
        patternDataSet = true;
    }

    /** Modern banner layers as a 1.12 {@code Patterns} list. */
    private static NBTTagList mcc$patternsTag(net.minecraft.world.level.block.entity.BannerPatternLayers layers) {
        NBTTagList list = new NBTTagList();
        if (layers == null) return list;
        for (var layer : layers.layers()) {
            var key = layer.pattern().unwrapKey();
            if (key.isEmpty()) continue;
            BannerPattern pattern = null;
            for (BannerPattern p : BannerPattern.values()) if (p.getFileName().equals(key.get().location().getPath())) pattern = p;
            if (pattern == null) continue;
            NBTTagCompound c = new NBTTagCompound();
            c.setString("Pattern", pattern.getHashname());
            c.setInteger("Color", EnumDyeColor.byMetadata(layer.color().getId()).getDyeDamage());
            list.appendTag(c);
        }
        return list;
    }

    /** Sets the banner from a banner item (its patterns, base colour and name). */
    public void setItemValues(ItemStack stack, boolean p_175112_2_) {
        patterns = mcc$patterns(stack);
        baseColor = getColor(stack);
        patternList = null;
        colorList = null;
        patternResourceLocation = "";
        patternDataSet = true;
        name = stack.hasDisplayName() ? stack.getDisplayName() : null;
    }

    private static NBTTagList mcc$patterns(ItemStack stack) {
        var backing = stack.mcc$backing();
        if (backing != null && backing.has(net.minecraft.core.component.DataComponents.BANNER_PATTERNS)) {
            return mcc$patternsTag(backing.get(net.minecraft.core.component.DataComponents.BANNER_PATTERNS));
        }
        NBTTagCompound nbt = stack.getSubCompound("BlockEntityTag");
        return nbt != null && nbt.hasKey("Patterns", 9) ? nbt.getTagList("Patterns", 10).copy() : null;
    }

    /** The banner item of this banner: its base colour (the metadata), patterns and name. */
    public ItemStack getItem() {
        ItemStack stack = new ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Items.BANNER, 1, baseColor.getDyeDamage());
        if (patterns != null && !patterns.isEmpty()) stack.getOrCreateSubCompound("BlockEntityTag").setTag("Patterns", patterns.copy());
        if (hasCustomName()) stack.setStackDisplayName(name);
        return stack;
    }

    public String getName() {
        return hasCustomName() ? name : "banner";
    }

    public boolean hasCustomName() {
        return name != null && !name.isEmpty();
    }

    @Override
    public ITextComponent getDisplayName() {
        return hasCustomName() ? new TextComponentString(getName()) : new TextComponentTranslation(getName());
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setInteger("Base", baseColor.getDyeDamage());
        if (patterns != null) compound.setTag("Patterns", patterns);
        if (hasCustomName()) compound.setString("CustomName", name);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("CustomName", 8)) name = compound.getString("CustomName");
        baseColor = EnumDyeColor.byDyeDamage(compound.getInteger("Base"));
        patterns = compound.getTagList("Patterns", 10);
        patternList = null;
        colorList = null;
        patternResourceLocation = null;
        patternDataSet = true;
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    /** How many patterns a banner item has. */
    public static int getPatterns(ItemStack stack) {
        NBTTagList list = mcc$patterns(stack);
        return list == null ? 0 : list.tagCount();
    }

    public List<BannerPattern> getPatternList() {
        initializeBannerData();
        return patternList;
    }

    public List<EnumDyeColor> getColorList() {
        initializeBannerData();
        return colorList;
    }

    /** The texture cache key: "b" + base colour, then each pattern hash + colour. */
    public String getPatternResourceLocation() {
        initializeBannerData();
        return patternResourceLocation;
    }

    private void initializeBannerData() {
        if (patternList != null && colorList != null && patternResourceLocation != null) return;
        if (!patternDataSet) {
            patternResourceLocation = "";
            return;
        }
        patternList = new ArrayList<>();
        colorList = new ArrayList<>();
        patternList.add(BannerPattern.BASE);
        colorList.add(baseColor);
        StringBuilder key = new StringBuilder("b").append(baseColor.getDyeDamage());
        if (patterns != null) {
            for (int i = 0; i < patterns.tagCount(); ++i) {
                NBTTagCompound c = patterns.getCompoundTagAt(i);
                BannerPattern p = BannerPattern.byHash(c.getString("Pattern"));
                if (p != null) {
                    patternList.add(p);
                    int j = c.getInteger("Color");
                    colorList.add(EnumDyeColor.byDyeDamage(j));
                    key.append(p.getHashname()).append(j);
                }
            }
        }
        patternResourceLocation = key.toString();
    }

    /** Removes the last pattern of a banner item (a cauldron washing it). */
    public static void removeBannerData(ItemStack stack) {
        var backing = stack.mcc$backing();
        if (backing != null && backing.has(net.minecraft.core.component.DataComponents.BANNER_PATTERNS)) {
            var layers = backing.get(net.minecraft.core.component.DataComponents.BANNER_PATTERNS);
            if (layers != null && !layers.layers().isEmpty()) backing.set(net.minecraft.core.component.DataComponents.BANNER_PATTERNS, layers.removeLast());
            return;
        }
        NBTTagCompound nbt = stack.getSubCompound("BlockEntityTag");
        if (nbt == null || !nbt.hasKey("Patterns", 9)) return;
        NBTTagList list = nbt.getTagList("Patterns", 10);
        if (list.tagCount() == 0) return;
        list.removeTag(list.tagCount() - 1);
        if (list.tagCount() == 0) {
            stack.getTagCompound().removeTag("BlockEntityTag");
            if (stack.getTagCompound().hasNoTags()) stack.setTagCompound(null);
        }
    }

    /** The base colour of a banner item (the modern banner item's own colour, or {@code BlockEntityTag.Base}). */
    public static EnumDyeColor getColor(ItemStack stack) {
        var backing = stack.mcc$backing();
        if (backing == null && !stack.isEmpty()) backing = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.toModern(stack);
        if (backing != null && backing.getItem() instanceof net.minecraft.world.item.BannerItem b) return EnumDyeColor.byMetadata(b.getColor().getId());
        NBTTagCompound nbt = stack.getSubCompound("BlockEntityTag");
        return nbt != null && nbt.hasKey("Base") ? EnumDyeColor.byDyeDamage(nbt.getInteger("Base")) : EnumDyeColor.BLACK;
    }
}

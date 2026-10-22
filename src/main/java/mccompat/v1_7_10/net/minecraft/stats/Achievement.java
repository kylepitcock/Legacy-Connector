package mccompat.v1_7_10.net.minecraft.stats;

import mccompat.v1_7_10.net.minecraft.block.Block;
import mccompat.v1_7_10.net.minecraft.item.Item;
import mccompat.v1_7_10.net.minecraft.item.ItemStack;
import mccompat.v1_7_10.net.minecraft.util.ChatComponentTranslation;
import mccompat.v1_7_10.net.minecraft.util.StatCollector;

public class Achievement extends StatBase {
    public final int displayColumn;
    public final int displayRow;
    public final Achievement parentAchievement;
    private final String achievementDescription;
    private IStatStringFormat statStringFormatter;
    public final ItemStack theItemStack;
    private boolean isSpecial;

    public Achievement(String id, String name, int column, int row, Item icon, Achievement parent) {
        this(id, name, column, row, new ItemStack(icon), parent);
    }

    public Achievement(String id, String name, int column, int row, Block icon, Achievement parent) {
        this(id, name, column, row, new ItemStack(icon), parent);
    }

    public Achievement(String id, String name, int column, int row, ItemStack icon, Achievement parent) {
        super(id, new ChatComponentTranslation("achievement." + name));
        this.theItemStack = icon;
        this.achievementDescription = "achievement." + name + ".desc";
        this.displayColumn = column;
        this.displayRow = row;
        if (column < AchievementList.minDisplayColumn) AchievementList.minDisplayColumn = column;
        if (row < AchievementList.minDisplayRow) AchievementList.minDisplayRow = row;
        if (column > AchievementList.maxDisplayColumn) AchievementList.maxDisplayColumn = column;
        if (row > AchievementList.maxDisplayRow) AchievementList.maxDisplayRow = row;
        this.parentAchievement = parent;
    }

    @Override
    public Achievement initIndependentStat() {
        this.isIndependent = true;
        return this;
    }

    public Achievement setSpecial() {
        this.isSpecial = true;
        return this;
    }

    @Override
    public Achievement registerStat() {
        super.registerStat();
        AchievementList.achievementList.add(this);
        return this;
    }

    @Override
    public boolean isAchievement() {
        return true;
    }

    public String getDescription() {
        return statStringFormatter != null ? statStringFormatter.formatString(StatCollector.translateToLocal(achievementDescription))
                : StatCollector.translateToLocal(achievementDescription);
    }

    public Achievement setStatStringFormatter(IStatStringFormat formatter) {
        this.statStringFormatter = formatter;
        return this;
    }

    public boolean getSpecial() {
        return isSpecial;
    }

    @Override
    public Achievement func_150953_b(Class<?> cls) {
        super.func_150953_b(cls);
        return this;
    }
}

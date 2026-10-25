package legacyconnector.v1_12_2.net.minecraft.item;

import java.util.Set;
import legacyconnector.v1_12_2.net.minecraft.block.Block;

/** Base of pickaxes, axes and shovels: a tool material and the blocks it is effective on. */
public class ItemTool extends Item {
    protected final Set<Block> effectiveBlocks;
    protected float efficiency;
    protected float attackDamage;
    protected float attackSpeed;
    protected final ToolMaterial toolMaterial;

    protected ItemTool(ToolMaterial material, Set<Block> effectiveBlocks) {
        this(0.0F, 0.0F, material, effectiveBlocks);
    }

    protected ItemTool(float attackDamage, float attackSpeed, ToolMaterial material, Set<Block> effectiveBlocks) {
        this.toolMaterial = material;
        this.effectiveBlocks = effectiveBlocks;
        this.maxStackSize = 1;
        setMaxDamage(material.getMaxUses());
        this.efficiency = material.getEfficiency();
        this.attackDamage = attackDamage + material.getAttackDamage();
        this.attackSpeed = attackSpeed;
    }

    public ToolMaterial mcc$toolMaterial() {
        return toolMaterial;
    }

    public String getToolMaterialName() {
        return toolMaterial.toString();
    }

    public int getItemEnchantability() {
        return toolMaterial.getEnchantability();
    }
}

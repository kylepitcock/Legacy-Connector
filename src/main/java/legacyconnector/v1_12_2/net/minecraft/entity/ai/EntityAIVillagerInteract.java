package legacyconnector.v1_12_2.net.minecraft.entity.ai;

import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_12_2.net.minecraft.entity.item.EntityItem;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityVillager;
import legacyconnector.v1_12_2.net.minecraft.inventory.InventoryBasic;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;

/** Villagers look at each other; one with food to spare throws half of it (or bread made of its wheat) to a hungry one. */
public class EntityAIVillagerInteract extends EntityAIWatchClosest2 {
    private int interactionDelay;
    private final EntityVillager villager;

    public EntityAIVillagerInteract(EntityVillager villager) {
        super(villager, EntityVillager.class, 3.0F, 0.02F);
        this.villager = villager;
    }

    @Override
    public void startExecuting() {
        super.startExecuting();
        interactionDelay = villager.canAbondonItems() && closestEntity instanceof EntityVillager other && other.wantsMoreFood() ? 10 : 0;
    }

    @Override
    public void updateTask() {
        super.updateTask();
        if (interactionDelay <= 0 || --interactionDelay != 0) return;
        InventoryBasic inv = villager.getVillagerInventory();
        for (int i = 0; i < inv.getSizeInventory(); ++i) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            ItemStack thrown = ItemStack.EMPTY;
            Object item = stack.getItem().mcc$modern;
            if ((item == net.minecraft.world.item.Items.BREAD || item == net.minecraft.world.item.Items.POTATO || item == net.minecraft.world.item.Items.CARROT
                    || item == net.minecraft.world.item.Items.BEETROOT) && stack.getCount() > 3) {
                int half = stack.getCount() / 2;
                stack.shrink(half);
                thrown = new ItemStack(stack.getItem(), half, stack.getMetadata());
            } else if (item == net.minecraft.world.item.Items.WHEAT && stack.getCount() > 5) {
                int used = stack.getCount() / 2 / 3 * 3;
                stack.shrink(used);
                thrown = org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.fromModern(new net.minecraft.world.item.ItemStack(
                        net.minecraft.world.item.Items.BREAD, used / 3));
            }
            if (stack.isEmpty()) inv.setInventorySlotContents(i, ItemStack.EMPTY);
            if (!thrown.isEmpty()) {
                var world = Entity.mcc$get$world(villager);
                if (world == null) return;
                EntityItem drop = new EntityItem(world, villager.mcc$x(), villager.mcc$y() - 0.30000001192092896D + villager.getEyeHeight(), villager.mcc$z(), thrown);
                float yaw = EntityLivingBase.mcc$get$rotationYawHead(villager);
                float pitch = Entity.mcc$get$rotationPitch(villager);
                float rad = (float) (Math.PI / 180.0D);
                Entity.mcc$set$motionX(drop, -Math.sin(yaw * rad) * Math.cos(pitch * rad) * 0.3F);
                Entity.mcc$set$motionZ(drop, Math.cos(yaw * rad) * Math.cos(pitch * rad) * 0.3F);
                Entity.mcc$set$motionY(drop, -Math.sin(pitch * rad) * 0.3F + 0.1F);
                drop.setDefaultPickupDelay();
                world.spawnEntity(drop);
                break;
            }
        }
    }
}

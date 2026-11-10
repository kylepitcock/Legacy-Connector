package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.block.entity.BlockEntity;
import legacyconnector.vb1_7_3.net.minecraft.block.entity.ChestBlockEntity;
import legacyconnector.vb1_7_3.net.minecraft.block.entity.MobSpawnerBlockEntity;
import legacyconnector.vb1_7_3.net.minecraft.block.entity.SignBlockEntity;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;

class VanillaBlockEntityMappingB173Test {
    public static class RegisteredChest extends ChestBlockEntity {}
    public static class UnregisteredChest extends ChestBlockEntity {}
    @Test void vanillaChestStandInRoundtripPreservesOriginalIdCoordinatesAndLoot(){
        var chest=new ChestBlockEntity();chest.x=141;chest.y=31;chest.z=54;
        chest.inventory[3]=new ItemStack(264,4,0);
        var nbt=new NbtCompound();chest.writeNbt(nbt);assertEquals("Chest",nbt.getString("id"));
        var restored=assertInstanceOf(ChestBlockEntity.class,BlockEntity.createFromNbt(nbt));
        assertEquals(141,restored.x);assertEquals(31,restored.y);assertEquals(54,restored.z);
        assertEquals(264,restored.inventory[3].itemId);assertEquals(4,restored.inventory[3].count);
    }
    @Test void otherImplementedVanillaTypesHaveTheirOriginalSaveIds(){
        for(var pair:java.util.Map.of("Sign",SignBlockEntity.class,"MobSpawner",MobSpawnerBlockEntity.class).entrySet()){
            assertSame(pair.getValue(),BlockEntity.mcc$classOf(pair.getKey()));
            var nbt=new NbtCompound();nbt.putString("id",pair.getKey());
            var restored=BlockEntity.createFromNbt(nbt);assertNotNull(restored);assertEquals(pair.getValue(),restored.getClass());
            restored.writeNbt(nbt);assertEquals(pair.getKey(),nbt.getString("id"));
        }
    }
    @Test void modChestRegistrationRetainsItsOwnIdentityAndUnregisteredSubclassesDoNotBecomeVanilla(){
        BlockEntity.create(RegisteredChest.class,"portable_fixture_chest");
        var chest=new RegisteredChest();var nbt=new NbtCompound();chest.writeNbt(nbt);
        assertEquals("portable_fixture_chest",nbt.getString("id"));
        assertInstanceOf(RegisteredChest.class,BlockEntity.createFromNbt(nbt));
        assertThrows(RuntimeException.class,()->new UnregisteredChest().writeNbt(new NbtCompound()));
        assertSame(ChestBlockEntity.class,BlockEntity.mcc$classOf("Chest"));
    }
}

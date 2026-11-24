package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/** What a coremod's structural change to another mod's class needs from the coremod's own jar (Logistics Pipes into Thermal Dynamics' TravelingItem). */
class CoremodModTargetsTest {
    private static ClassNode travelingItem() {
        ClassNode c = new ClassNode();
        c.name = "cofh/thermaldynamics/duct/item/TravelingItem";
        c.methods.add(new MethodNode(Opcodes.ACC_PUBLIC, "toNBT", "(Lnet/minecraft/nbt/NBTTagCompound;)V", null, null));
        return c;
    }

    private static CoremodHookScan.Change change(ClassNode after) {
        return new CoremodHookScan.Change("set", "cofh/thermaldynamics/duct/item/TravelingItem", false, travelingItem(), after);
    }

    @Test
    void addedMembersNameTheCoremodJarsTypes() {
        ClassNode after = travelingItem();
        after.interfaces.add("logisticspipes/api/IRoutedItem");
        after.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "routing", "Llogisticspipes/routing/ItemRoutingInformation;", null, null));
        after.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "plain", "Ljava/lang/Object;", null, null));
        after.fields.add(new FieldNode(Opcodes.ACC_PUBLIC, "stack", "Lnet/minecraft/item/ItemStack;", null, null));
        after.methods.add(new MethodNode(Opcodes.ACC_PUBLIC, "route", "([Llogisticspipes/routing/Path;I)Llogisticspipes/routing/Router;", null, null));
        Set<String> refs = CoremodModTargets.addedReferences(change(after));
        assertEquals(Set.of("logisticspipes/api/IRoutedItem", "logisticspipes/routing/ItemRoutingInformation", "logisticspipes/routing/Path",
                "logisticspipes/routing/Router"), refs);
    }

    @Test
    void unchangedClassNeedsNothing() {
        assertTrue(CoremodModTargets.addedReferences(change(travelingItem())).isEmpty());
        assertEquals(List.of(), CoremodHookDiscovery.additions(travelingItem(), travelingItem()));
    }
}

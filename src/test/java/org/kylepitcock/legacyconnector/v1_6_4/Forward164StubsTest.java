package org.kylepitcock.legacyconnector.v1_6_4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import legacyconnector.v1_6_4.net.minecraft.logging.ILogAgent;
import legacyconnector.v1_6_4.net.minecraft.logging.LogAgent;
import legacyconnector.v1_7_10.net.minecraft.enchantment.Enchantment;
import legacyconnector.v1_7_10.net.minecraft.enchantment.EnchantmentDamage;
import legacyconnector.v1_7_10.net.minecraft.entity.EnumCreatureAttribute;
import org.junit.jupiter.api.Test;

/** Behaviour of the shim classes that replaced synthesized (empty) 1.6.4 API for The Lion King and Crafting Dead. */
class Forward164StubsTest {
    @Test
    void worldLogAgentWritesToTheLogger() {
        ILogAgent agent = LogAgent.world();
        assertSame(agent, LogAgent.world());
        java.util.List<LogRecord> seen = new java.util.ArrayList<>();
        Handler h = new Handler() {
            @Override public void publish(LogRecord r) { seen.add(r); }
            @Override public void flush() {}
            @Override public void close() {}
        };
        agent.func_120013_a().addHandler(h);
        try {
            agent.logSevere("boom");
            agent.logWarningException("careful", new IllegalStateException("x"));
        } finally {
            agent.func_120013_a().removeHandler(h);
        }
        assertEquals(2, seen.size());
        assertEquals(Level.SEVERE, seen.get(0).getLevel());
        assertEquals("boom", seen.get(0).getMessage());
        assertEquals(Level.WARNING, seen.get(1).getLevel());
        assertNotNull(seen.get(1).getThrown());
    }

    @Test
    void damageEnchantmentsScaleLikeSharpnessSmiteAndBane() {
        EnchantmentDamage sharp = new EnchantmentDamage(200, 10, 0);
        EnchantmentDamage smite = new EnchantmentDamage(201, 5, 1);
        EnchantmentDamage bane = new EnchantmentDamage(202, 5, 2);
        assertEquals(5, sharp.getMaxLevel());
        assertEquals(1.25F * 3, sharp.func_152376_a(3, EnumCreatureAttribute.UNDEFINED));
        assertEquals(2.5F * 2, smite.func_152376_a(2, EnumCreatureAttribute.UNDEAD));
        assertEquals(0.0F, smite.func_152376_a(2, EnumCreatureAttribute.ARTHROPOD));
        assertEquals(2.5F * 4, bane.func_152376_a(4, EnumCreatureAttribute.ARTHROPOD));
        assertEquals(1 + 2 * 11, sharp.getMinEnchantability(3));
        assertEquals(5, smite.getMinEnchantability(1));
        assertEquals("enchantment.damage.undead", smite.getName());
        assertFalse(sharp.canApplyTogether(smite));
        assertTrue(sharp.canApplyTogether(Enchantment.knockback));
        assertSame(sharp, Enchantment.enchantmentsList[200]);
    }
}

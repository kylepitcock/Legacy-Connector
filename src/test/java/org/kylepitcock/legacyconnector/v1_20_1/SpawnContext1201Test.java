/*
 * Copyright (c) 2026 kentvillamor08
 * Licensed under the MIT License.
 * See LICENSE in the project root.
 */
package org.kylepitcock.legacyconnector.v1_20_1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.v1_20_1.migrate.SpawnContext;
import org.kylepitcock.legacyconnector.v1_20_1.migrate.SpawnContext.Scope;

class SpawnContext1201Test {
    @Test
    void pendingTagFlowsThroughMatchingCreateAndBoundFactoryEntity() {
        Object entityType = new Object();
        Object mob = new Object();
        Object otherMob = new Object();
        CompoundTag tag = tag("source", "explicit");

        try (Scope pending = SpawnContext.pushPending(entityType, tag)) {
            assertNull(SpawnContext.currentTagFor(mob));

            try (Scope create = SpawnContext.enterCreate(entityType)) {
                assertNull(SpawnContext.currentTagFor(mob));

                try (Scope factory = SpawnContext.enterFactory(entityType)) {
                    assertNull(SpawnContext.currentTagFor(mob));
                    SpawnContext.bindCreatedEntity(entityType, mob);
                    SpawnContext.bindCreatedEntity(entityType, otherMob);
                }

                assertSame(tag, SpawnContext.currentTagFor(mob));
                assertNull(SpawnContext.currentTagFor(otherMob));
            }

            assertNull(SpawnContext.currentTagFor(mob));
        }

        assertNull(SpawnContext.currentTagFor(mob));
    }

    @Test
    void pendingTagIsNotConsumedByDifferentEntityType() {
        Object expectedType = new Object();
        Object otherType = new Object();
        Object mob = new Object();
        CompoundTag tag = tag("source", "expected");

        try (Scope pending = SpawnContext.pushPending(expectedType, tag)) {
            try (Scope unrelatedCreate = SpawnContext.enterCreate(otherType)) {
                try (Scope unrelatedFactory = SpawnContext.enterFactory(otherType)) {
                    SpawnContext.bindCreatedEntity(otherType, mob);
                }
                assertNull(SpawnContext.currentTagFor(mob));
            }

            try (Scope matchingCreate = SpawnContext.enterCreate(expectedType)) {
                try (Scope matchingFactory = SpawnContext.enterFactory(expectedType)) {
                    SpawnContext.bindCreatedEntity(expectedType, mob);
                }
                assertSame(tag, SpawnContext.currentTagFor(mob));
            }
        }
    }

    @Test
    void matchingPendingTagIsClaimedByOnlyFirstCreate() {
        Object entityType = new Object();
        Object firstMob = new Object();
        Object secondMob = new Object();
        CompoundTag tag = tag("source", "one-shot");

        try (Scope pending = SpawnContext.pushPending(entityType, tag)) {
            try (Scope firstCreate = SpawnContext.enterCreate(entityType)) {
                try (Scope firstFactory = SpawnContext.enterFactory(entityType)) {
                    SpawnContext.bindCreatedEntity(entityType, firstMob);
                }
                assertSame(tag, SpawnContext.currentTagFor(firstMob));
            }

            // The pending scope remains open until the outer spawn returns, but
            // the first matching create has claimed it. Later reentrant creates
            // must not receive the same legacy tag.
            try (Scope laterCreate = SpawnContext.enterCreate(entityType)) {
                try (Scope laterFactory = SpawnContext.enterFactory(entityType)) {
                    SpawnContext.bindCreatedEntity(entityType, secondMob);
                }
                assertNull(SpawnContext.currentTagFor(secondMob));
            }
        }

        assertNull(SpawnContext.currentTagFor(firstMob));
    }

    @Test
    void nestedNullTagShadowsAndThenRestoresOuterBoundEntityTag() {
        Object outerType = new Object();
        Object innerType = new Object();
        Object mob = new Object();
        CompoundTag outerTag = tag("source", "outer");

        try (Scope outerPending = SpawnContext.pushPending(outerType, outerTag);
             Scope outerCreate = SpawnContext.enterCreate(outerType)) {
            try (Scope outerFactory = SpawnContext.enterFactory(outerType)) {
                SpawnContext.bindCreatedEntity(outerType, mob);
            }
            assertSame(outerTag, SpawnContext.currentTagFor(mob));

            try (Scope innerPending = SpawnContext.pushPending(innerType, null);
                 Scope innerCreate = SpawnContext.enterCreate(innerType)) {
                try (Scope innerFactory = SpawnContext.enterFactory(innerType)) {
                    SpawnContext.bindCreatedEntity(innerType, mob);
                }
                // A present-but-null inner tag must not fall through to the outer Create frame.
                assertNull(SpawnContext.currentTagFor(mob));
            }

            assertSame(outerTag, SpawnContext.currentTagFor(mob));
        }

        assertNull(SpawnContext.currentTagFor(mob));
    }

    @Test
    void nestedCreateWithoutPendingTagDoesNotInheritParentTag() {
        Object entityType = new Object();
        Object outerMob = new Object();
        Object nestedMob = new Object();
        CompoundTag outerTag = tag("source", "outer");

        try (Scope pending = SpawnContext.pushPending(entityType, outerTag);
             Scope outerCreate = SpawnContext.enterCreate(entityType)) {
            try (Scope outerFactory = SpawnContext.enterFactory(entityType)) {
                SpawnContext.bindCreatedEntity(entityType, outerMob);
            }
            assertSame(outerTag, SpawnContext.currentTagFor(outerMob));

            try (Scope nestedCreate = SpawnContext.enterCreate(entityType)) {
                try (Scope nestedFactory = SpawnContext.enterFactory(entityType)) {
                    SpawnContext.bindCreatedEntity(entityType, nestedMob);
                }
                assertNull(SpawnContext.currentTagFor(nestedMob));
                assertNull(SpawnContext.currentTagFor(outerMob));
            }

            assertSame(outerTag, SpawnContext.currentTagFor(outerMob));
        }
    }

    @Test
    void nestedFactoryCannotBindToAncestorCreate() {
        Object entityType = new Object();
        Object outerMob = new Object();
        Object nestedMob = new Object();
        CompoundTag tag = tag("source", "outer");

        try (Scope pending = SpawnContext.pushPending(entityType, tag);
             Scope create = SpawnContext.enterCreate(entityType)) {
            try (Scope outerFactory = SpawnContext.enterFactory(entityType)) {
                try (Scope nestedFactory = SpawnContext.enterFactory(entityType)) {
                    SpawnContext.bindCreatedEntity(entityType, nestedMob);
                }

                SpawnContext.bindCreatedEntity(entityType, outerMob);
            }

            assertSame(tag, SpawnContext.currentTagFor(outerMob));
            assertNull(SpawnContext.currentTagFor(nestedMob));
        }
    }

    @Test
    void explicitActiveScopeUsesItsSuppliedTag() {
        Object mob = new Object();
        CompoundTag tag = tag("source", "direct-call");

        try (Scope active = SpawnContext.pushActive(mob, tag)) {
            assertSame(tag, SpawnContext.currentTagFor(mob));
        }

        assertNull(SpawnContext.currentTagFor(mob));
    }

    @Test
    void activeTagDoesNotCrossAnInnerCreateBoundary() {
        Object entityType = new Object();
        Object mob = new Object();
        CompoundTag activeTag = tag("source", "direct");

        try (Scope active = SpawnContext.pushActive(mob, activeTag)) {
            assertSame(activeTag, SpawnContext.currentTagFor(mob));
            try (Scope create = SpawnContext.enterCreate(entityType)) {
                assertNull(SpawnContext.currentTagFor(mob));
            }
            assertSame(activeTag, SpawnContext.currentTagFor(mob));
        }
    }

    @Test
    void scopesAreCleanedWhenNestedWorkThrows() {
        Object entityType = new Object();
        Object mob = new Object();
        CompoundTag tag = tag("source", "exception");

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
            try (Scope pending = SpawnContext.pushPending(entityType, tag);
                 Scope create = SpawnContext.enterCreate(entityType);
                 Scope factory = SpawnContext.enterFactory(entityType);
                 Scope active = SpawnContext.pushActive(mob, tag)) {
                assertSame(tag, SpawnContext.currentTagFor(mob));
                throw new IllegalStateException("test failure");
            }
        });

        assertEquals("test failure", thrown.getMessage());
        assertNull(SpawnContext.currentTagFor(mob));
    }

    @Test
    void scopesRequireLifoCloseAndCanBeClosedIdempotently() {
        Object entityType = new Object();
        Scope pending = SpawnContext.pushPending(entityType, null);
        Scope create = SpawnContext.enterCreate(entityType);

        assertThrows(IllegalStateException.class, pending::close);

        create.close();
        pending.close();
        pending.close();

        assertNull(SpawnContext.currentTagFor(new Object()));
    }

    private static CompoundTag tag(String key, String value) {
        CompoundTag tag = new CompoundTag();
        tag.putString(key, value);
        return tag;
    }
}

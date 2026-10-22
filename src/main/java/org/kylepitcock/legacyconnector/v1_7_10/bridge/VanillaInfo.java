package org.kylepitcock.legacyconnector.v1_7_10.bridge;

/**
 * Identity of a vanilla (or modern-only) block/item wrapper: its 1.7.10 registry name and numeric id
 * (-1 for things that did not exist in 1.7.10), plus the modern object it stands for, if fixed.
 */
public record VanillaInfo(String legacyName, int legacyId, Object modern) {
}

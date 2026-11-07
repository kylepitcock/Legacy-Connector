package org.kylepitcock.legacyconnector.v1_20_1.migrate;

/** 1.20.1's {@code PressurePlateBlock.Sensitivity}; 1.21.1 plates react to every entity (the argument is dropped). */
public enum LegacySensitivity {
    EVERYTHING,
    MOBS
}

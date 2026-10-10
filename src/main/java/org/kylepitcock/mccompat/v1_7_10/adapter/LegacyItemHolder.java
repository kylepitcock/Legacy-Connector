package org.kylepitcock.mccompat.v1_7_10.adapter;

/** A modern item that stands for one metadata variant of a legacy item. */
public interface LegacyItemHolder {
    Object legacyItem();

    int legacyMeta();
}

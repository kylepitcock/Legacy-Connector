package dev.latvian.mods.kubejs.block.entity;

/** Test fixture matching KubeJS 7's private entity field on its tick event. */
public final class BlockEntityTickKubeEvent {
    private final KubeBlockEntity entity;

    public BlockEntityTickKubeEvent(KubeBlockEntity entity) {
        this.entity = entity;
    }
}

package legacyconnector.v1_6_4.cpw.mods.fml.common;

import java.util.EnumSet;

/**
 * FML 1.6's tick kinds. What {@code tickData} carries: {@code WORLD} the world, {@code WORLDLOAD} the world (once, when it is first
 * ticked), {@code RENDER} the partial tick time as a Float, {@code PLAYER} the player; {@code CLIENT} and {@code SERVER} nothing.
 * {@code GAME} and {@code GLOBAL} only exist so code naming them links; nothing ticks them.
 */
public enum TickType {
    WORLD,
    RENDER,
    GLOBAL,
    CLIENT,
    PLAYER,
    SERVER,
    WORLDLOAD,
    GAME,
    /** 1.4.7 and 1.5.2: every client tick while a GUI screen is open, {@code tickData} is the screen (see {@code Ticks164}). */
    CLIENTGUI;

    private TickType[] partners = new TickType[0];

    static {
        // FML: the ticks that happen around the same time as this one
        WORLD.partners = new TickType[] {WORLDLOAD};
        WORLDLOAD.partners = new TickType[] {WORLD};
        CLIENT.partners = new TickType[] {RENDER};
        RENDER.partners = new TickType[] {CLIENT};
    }

    /** The tick types that may happen alongside this one. */
    public EnumSet<TickType> partnerTicks() {
        EnumSet<TickType> out = EnumSet.noneOf(TickType.class);
        for (TickType t : partners) out.add(t);
        return out;
    }
}

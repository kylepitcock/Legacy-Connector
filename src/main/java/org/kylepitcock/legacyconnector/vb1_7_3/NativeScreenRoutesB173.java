package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.*;

/** Only exact vanilla API identities bypass original screen callbacks. */
public final class NativeScreenRoutesB173 {
    private NativeScreenRoutesB173(){}
    public enum Route {ORIGINAL,ACHIEVEMENTS,STATS}
    public static Route route(Screen screen){
        if(screen!=null&&screen.getClass()==AchievementsScreen.class)return Route.ACHIEVEMENTS;
        if(screen!=null&&screen.getClass()==StatsScreen.class)return Route.STATS;
        return Route.ORIGINAL;
    }
}

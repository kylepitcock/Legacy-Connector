package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.*;
import legacyconnector.vb1_7_3.net.minecraft.stat.PlayerStats;
import org.junit.jupiter.api.Test;

class NativeScreenRoutesB173Test {
    @Test void originalConstructorIdentitiesSelectNativeViews(){
        var stats=new PlayerStats();var parent=new Screen();
        var achievements=new AchievementsScreen(stats);var statistics=new StatsScreen(parent,stats);
        assertSame(stats,achievements.stats);assertSame(stats,statistics.stats);assertSame(parent,statistics.parent);
        assertEquals(NativeScreenRoutesB173.Route.ACHIEVEMENTS,NativeScreenRoutesB173.route(achievements));
        assertEquals(NativeScreenRoutesB173.Route.STATS,NativeScreenRoutesB173.route(statistics));
    }
    @Test void customSubclassesKeepTheirOriginalCallbackPath(){
        assertEquals(NativeScreenRoutesB173.Route.ORIGINAL,NativeScreenRoutesB173.route(new AchievementsScreen(null){@Override public void init(){throw new AssertionError();}}));
        assertEquals(NativeScreenRoutesB173.Route.ORIGINAL,NativeScreenRoutesB173.route(new StatsScreen(null,null){@Override public void tick(){throw new AssertionError();}}));
        assertEquals(NativeScreenRoutesB173.Route.ORIGINAL,NativeScreenRoutesB173.route(new Screen()));
        assertEquals(NativeScreenRoutesB173.Route.ORIGINAL,NativeScreenRoutesB173.route(null));
    }
}

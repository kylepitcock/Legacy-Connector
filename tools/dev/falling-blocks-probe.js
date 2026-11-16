// Counts the falling-block entities of every level each 10 ticks (a runaway spawner makes one server tick take minutes: server watchdog crash).
// Run: ./gradlew runServerSubTest -PsubTestDir=<dir> -PserverTickScript=<abs path of this file> -PserverTickScriptFrom=100
if (tick % 10 === 0) {
    var it = server.getAllLevels().iterator();
    while (it.hasNext()) {
        var lvl = it.next();
        var n = 0, sample = '';
        var es = lvl.getAllEntities().iterator();
        while (es.hasNext()) {
            var e = es.next();
            if (String(e.getType().builtInRegistryHolder().key().location()) === 'minecraft:falling_block') {
                n++;
                if (n === 1) sample = e.blockPosition() + ' ' + e.getBlockState();
            }
        }
        if (n > 0) print('FALLING ' + lvl.dimension().location() + ' tick ' + tick + ': ' + n + ' first ' + sample);
    }
}

// 1.7.10 coremod places, round 6 (tools/dev/agent-tasks/coremod-sites-1710.md): checks that the 1.7.10 hook mixins applied, drives the places a server can
// reach (collision boxes, light probe), and times the hot paths (CoremodHotPathBench; -Dcm.bench=false to skip). Run:
//   ./gradlew runServerSubTest -PsubTestDir=run-cm6 -PserverTickScript=<abs path of this file> -PautostopTicks=1500
if (typeof cm6done === 'undefined') {
    cm6done = true;
    var out = [];
    function step(name, f) {
        try { out.push(name + ': ' + f()); } catch (e) { out.push(name + ': THREW ' + e); if (e.printStackTrace) e.printStackTrace(); }
    }
    var targets = ['net.minecraft.world.level.CollisionGetter', 'net.minecraft.world.entity.Entity', 'net.minecraft.world.entity.LivingEntity',
        'net.minecraft.world.level.Level', 'net.minecraft.world.entity.player.Player', 'net.minecraft.world.entity.ai.targeting.TargetingConditions'];
    for (var i = 0; i < targets.length; i++) {
        (function (n) { step('load ' + n.substring(n.lastIndexOf('.') + 1), function () { Java.type(n); return 'ok'; }); })(targets[i]);
    }
    step('mixin check', function () { return Java.type('org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodMixinCheck1710').report(false); });
    var level = server.overworld();
    level.setChunkForced(0, 0, true);
    var EntityType = Java.type('net.minecraft.world.entity.EntityType');
    var BlockPos = Java.type('net.minecraft.core.BlockPos');
    var Blocks = Java.type('net.minecraft.world.level.block.Blocks');
    var base = new BlockPos(4, 210, 4);
    level.setBlock(base.below(), Blocks.STONE.defaultBlockState(), 3);
    level.setBlock(base.north(), Blocks.STONE.defaultBlockState(), 3);
    var zombie = EntityType.ZOMBIE.create(level);
    zombie.setPos(4.5, 210, 4.5);
    step('block collisions with the hooks', function () {
        var n = 0, boxes = [];
        var it = level.getBlockCollisions(zombie, zombie.getBoundingBox().inflate(1.0)).iterator();
        while (it.hasNext()) { var s = it.next(); n++; boxes.push(s.bounds()); }
        return n + ' shape(s): ' + boxes.join(' ');
    });
    step('collision hook answers (null = no hook ran)', function () {
        var sites = Java.type('org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodSites');
        var shapes = sites.collisionShapes(level, zombie, zombie.getBoundingBox().inflate(1.0));
        return shapes === null ? 'null' : shapes.size() + ' shape(s)';
    });
    step('light probe', function () {
        return 'probe y=' + zombie.getLightProbePosition(1.0).y + ' eye y=' + zombie.getEyePosition(1.0).y;
    });
    step('Entity.move', function () {
        var MoverType = Java.type('net.minecraft.world.entity.MoverType'), Vec3 = Java.type('net.minecraft.world.phys.Vec3');
        zombie.move(MoverType.SELF, new Vec3(0, -1, 0));
        return 'y after a fall of 1 = ' + zombie.getY();
    });
    for (var j = 0; j < out.length; j++) log.info('CM6 ' + out[j]);
    if (Java.type('java.lang.System').getProperty('cm.bench') !== 'false') {
        var lines = Java.type('org.kylepitcock.legacyconnector.v1_7_10.bridge.CoremodHotPathBench').run(level, 1);
        for (var k = 0; k < lines.size(); k++) log.info('CM6 bench ' + lines.get(k));
    }
}

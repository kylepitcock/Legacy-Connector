// 1.7.10 coremod places, round 3 (tools/dev/agent-tasks/coremod-sites-1710.md): loads every mixin target (a mixin that does not apply throws here) and drives
// the places a server can reach without a player. Run: ./gradlew runServerSubTest -PsubTestDir=run-coremod -PserverTickScript=<abs path of this file>
if (typeof cm3done === 'undefined') {
    cm3done = true;
    var out = [];
    function step(name, f) {
        try { out.push(name + ': ' + f()); } catch (e) { out.push(name + ': THREW ' + e); if (e.printStackTrace) e.printStackTrace(); }
    }
    var targets = ['net.minecraft.world.level.block.BasePressurePlateBlock', 'net.minecraft.world.level.Level', 'net.minecraft.world.entity.player.Player',
        'net.minecraft.world.entity.ai.targeting.TargetingConditions', 'net.minecraft.server.level.ChunkMap$TrackedEntity'];
    for (var i = 0; i < targets.length; i++) {
        (function (n) { step('load ' + n.substring(n.lastIndexOf('.') + 1), function () { Java.type(n); return 'ok'; }); })(targets[i]);
    }
    var level = server.overworld();
    level.setChunkForced(0, 0, true);
    var EntityType = Java.type('net.minecraft.world.entity.EntityType');
    var BlockPos = Java.type('net.minecraft.core.BlockPos');
    var Blocks = Java.type('net.minecraft.world.level.block.Blocks');
    var pos = new BlockPos(4, 200, 4);
    step('pressure plate with a pig', function () {
        level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(pos, Blocks.STONE_PRESSURE_PLATE.defaultBlockState(), 3);
        var pig = EntityType.PIG.create(level);
        pig.setPos(4.5, 200, 4.5);
        level.addFreshEntity(pig);
        cm3pig = pig;
        return 'placed';
    });
    step('setBlock', function () {
        var p = pos.east(3);
        level.setBlock(p, Blocks.TORCH.defaultBlockState(), 3);
        level.setBlock(p.above(), Blocks.FIRE.defaultBlockState(), 3);
        return 'torch=' + level.getBlockState(p).getBlock() + ' above=' + level.getBlockState(p.above()).getBlock();
    });
    step('targeting', function () {
        var zombie = EntityType.ZOMBIE.create(level), pig = EntityType.PIG.create(level);
        zombie.setPos(4.5, 200, 8.5); pig.setPos(6.5, 200, 8.5);
        level.addFreshEntity(zombie); level.addFreshEntity(pig);
        var r = Java.type('net.minecraft.world.entity.ai.targeting.TargetingConditions').forCombat().test(zombie, pig);
        zombie.discard(); pig.discard();
        return 'zombie->pig=' + r;
    });
    cm3report = function () {
        var o = [];
        try { o.push('plate power=' + level.getBlockState(pos)); } catch (e) { o.push('plate THREW ' + e); }
        return o;
    };
    cm3at = tick;
    for (var j = 0; j < out.length; j++) log.info('CM3 ' + out[j]);
}
if (typeof cm3reported === 'undefined' && tick > cm3at + 100) {
    cm3reported = true;
    var rep = cm3report();
    for (var k = 0; k < rep.length; k++) log.info('CM3 ' + rep[k]);
}

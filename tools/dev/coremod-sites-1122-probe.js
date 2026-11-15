// Round 4 coremod places (tools/dev/agent-tasks/coremod-sites-1122.md): loads every mixin target (a mixin that does not apply throws here) and drives
// the places a server can reach without a player. Run: ./gradlew runServerSubTest -PsubTestDir=run-cm4 -PserverTickScript=tools/dev/coremod-sites-1122-probe.js
if (typeof cm4done === 'undefined') {
    cm4done = true;
    var out = [];
    function step(name, f) {
        try { out.push(name + ': ' + f()); } catch (e) { out.push(name + ': THREW ' + e); if (e.printStackTrace) e.printStackTrace(); }
    }
    var targets = ['net.minecraft.world.level.block.CakeBlock', 'net.minecraft.world.level.material.FlowingFluid',
        'net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity', 'net.minecraft.world.entity.LivingEntity',
        'net.minecraft.world.inventory.LoomMenu', 'net.minecraft.server.level.ServerPlayer', 'net.minecraft.server.level.ServerLevel',
        'net.minecraft.world.entity.ai.targeting.TargetingConditions', 'net.minecraft.world.entity.vehicle.VehicleEntity',
        'net.minecraft.world.entity.boss.wither.WitherBoss'];
    for (var i = 0; i < targets.length; i++) {
        (function (n) { step('load ' + n.substring(n.lastIndexOf('.') + 1), function () { Java.type(n); return 'ok'; }); })(targets[i]);
    }
    var level = server.overworld();
    level.setChunkForced(0, 0, true); // no player: without this neither the furnace nor the water ticks
    var EntityType = Java.type('net.minecraft.world.entity.EntityType');
    var BlockPos = Java.type('net.minecraft.core.BlockPos');
    var Blocks = Java.type('net.minecraft.world.level.block.Blocks');
    var pos = new BlockPos(4, 200, 4);
    step('boat destroyed', function () {
        var boat = EntityType.BOAT.create(level);
        boat.setPos(4.5, 200, 4.5);
        level.addFreshEntity(boat);
        var r = boat.hurt(level.damageSources().generic(), 100);
        return 'hurt=' + r + ' removed=' + boat.isRemoved();
    });
    step('minecart destroyed', function () {
        var cart = EntityType.MINECART.create(level);
        cart.setPos(6.5, 200, 4.5);
        level.addFreshEntity(cart);
        var r = cart.hurt(level.damageSources().generic(), 100);
        return 'hurt=' + r + ' removed=' + cart.isRemoved();
    });
    step('wither canDestroy', function () {
        var r = Java.type('net.minecraft.world.entity.boss.wither.WitherBoss').canDestroy(Blocks.STONE.defaultBlockState());
        return 'stone=' + r + ' bedrock=' + Java.type('net.minecraft.world.entity.boss.wither.WitherBoss').canDestroy(Blocks.BEDROCK.defaultBlockState());
    });
    step('targeting', function () {
        var zombie = EntityType.ZOMBIE.create(level), pig = EntityType.PIG.create(level);
        zombie.setPos(4.5, 200, 8.5); pig.setPos(6.5, 200, 8.5);
        level.addFreshEntity(zombie); level.addFreshEntity(pig);
        var r = Java.type('net.minecraft.world.entity.ai.targeting.TargetingConditions').forCombat().test(zombie, pig);
        zombie.discard(); pig.discard();
        return 'zombie->pig=' + r;
    });
    step('water flow', function () {
        level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(pos.above(), Blocks.WATER.defaultBlockState(), 3);
        level.setBlock(pos.east(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(pos.east().above(), Blocks.TORCH.defaultBlockState(), 3);
        return 'placed';
    });
    step('furnace', function () {
        var ItemStack = Java.type('net.minecraft.world.item.ItemStack'), Items = Java.type('net.minecraft.world.item.Items');
        var p = pos.south(3);
        level.setBlock(p, Blocks.FURNACE.defaultBlockState(), 3);
        var be = level.getBlockEntity(p);
        be.setItem(0, new ItemStack(Items.IRON_ORE, 2));
        be.setItem(1, new ItemStack(Items.COAL, 1));
        cm4furnace = be;
        return 'filled';
    });
    cm4report = function () {
        var o = [];
        try { o.push('furnace result=' + cm4furnace.getItem(2)); } catch (e) { o.push('furnace THREW ' + e); }
        try { o.push('torch beside water=' + level.getBlockState(pos.east().above())); } catch (e) { o.push('water THREW ' + e); }
        return o;
    };
    cm4at = tick;
    for (var j = 0; j < out.length; j++) log.info('CM4 ' + out[j]);
}
if (typeof cm4reported === 'undefined' && tick > cm4at + 300) {
    cm4reported = true;
    var rep = cm4report();
    for (var k = 0; k < rep.length; k++) log.info('CM4 ' + rep[k]);
}

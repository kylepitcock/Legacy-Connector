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

// Round 5 places (server side): RandomThings redstone power, slime chunk, slipperiness, inventory drop, tree ground, collision boxes.
if (typeof cm5done === 'undefined') {
    cm5done = true;
    var out5 = [];
    function step5(name, f) {
        try { out5.push(name + ': ' + f()); } catch (e) { out5.push(name + ': THREW ' + e); if (e.printStackTrace) e.printStackTrace(); }
    }
    var targets5 = ['net.minecraft.world.entity.monster.Slime', 'net.minecraft.world.entity.player.Inventory',
        'net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer', 'net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase',
        'net.minecraft.world.level.SignalGetter', 'net.minecraft.world.level.block.entity.BeaconBlockEntity', 'net.minecraft.world.item.ItemStack'];
    for (var i5 = 0; i5 < targets5.length; i5++) {
        (function (n) { step5('load ' + n.substring(n.lastIndexOf('.') + 1), function () { Java.type(n); return 'ok'; }); })(targets5[i5]);
    }
    var lvl5 = server.overworld();
    lvl5.setChunkForced(0, 0, true);
    lvl5.setChunkForced(1, 1, true);
    var BP5 = Java.type('net.minecraft.core.BlockPos');
    var Blocks5 = Java.type('net.minecraft.world.level.block.Blocks');
    var BuiltIn5 = Java.type('net.minecraft.core.registries.BuiltInRegistries');
    var RL5 = Java.type('net.minecraft.resources.ResourceLocation');
    var Direction5 = Java.type('net.minecraft.core.Direction');
    function blockOf(id) { return BuiltIn5.BLOCK.get(RL5.parse(id)); }
    function itemOf(id) { return BuiltIn5.ITEM.get(RL5.parse(id)); }
    function ItemStackOf(item) { return new (Java.type('net.minecraft.world.item.ItemStack'))(item, 1); }
    step5('redstone power', function () {
        var p = new BP5(6, 150, 6);
        return 'signal=' + lvl5.getSignal(p, Direction5.NORTH) + ' direct=' + lvl5.getDirectSignal(p, Direction5.NORTH);
    });
    step5('slime chunk', function () {
        var Slime = Java.type('net.minecraft.world.entity.monster.Slime');
        var ET = Java.type('net.minecraft.world.entity.EntityType');
        var MST = Java.type('net.minecraft.world.entity.MobSpawnType');
        var p = new BP5(8, 100, 8);
        function count() { var n = 0; for (var k = 0; k < 60; k++) if (Slime.checkSlimeSpawnRules(ET.SLIME, lvl5, MST.NATURAL, p, lvl5.random)) n++; return n; }
        var before = count();
        lvl5.setBlock(new BP5(5, 120, 5), blockOf('randomthings:slimecube').defaultBlockState(), 3);
        lvl5.getBlockEntity(new BP5(5, 120, 5)).legacy(); // the legacy tile registers itself in its constructor, which runs on first use
        var after = count();
        return 'spawn rule true of 60: before the slime cube=' + before + ' after=' + after;
    });
    step5('slipperiness', function () {
        var ET = Java.type('net.minecraft.world.entity.EntityType');
        var Vec3 = Java.type('net.minecraft.world.phys.Vec3');
        var res = [];
        var ids = ['minecraft:stone', 'randomthings:superlubricentstone'];
        for (var k = 0; k < 2; k++) {
            var bx = 20 + k * 20;
            for (var x = -3; x <= 3; x++) for (var z = -3; z <= 3; z++) {
                lvl5.setBlock(new BP5(bx + x, 199, 40 + z), BuiltIn5.BLOCK.get(RL5.parse(ids[k])).defaultBlockState(), 3);
                for (var y = 200; y <= 203; y++) lvl5.setBlock(new BP5(bx + x, y, 40 + z), Blocks5.AIR.defaultBlockState(), 3);
            }
            var z0 = ET.ZOMBIE.create(lvl5);
            z0.setPos(bx + 0.5, 200, 40.5);
            lvl5.addFreshEntity(z0);
            z0.setOnGround(true);
            z0.setDeltaMovement(0.5, 0, 0);
            z0.travel(Vec3.ZERO);
            res.push(ids[k] + ' dx after travel=' + z0.getDeltaMovement().x + ' onGround=' + z0.onGround());
            z0.discard();
        }
        return res.join('; ');
    });
    step5('inventory drop', function () {
        var FPF = Java.type('net.neoforged.neoforge.common.util.FakePlayerFactory');
        var fp = FPF.getMinecraft(lvl5);
        var inv = fp.getInventory();
        var DC = Java.type('net.minecraft.core.component.DataComponents');
        var CD = Java.type('net.minecraft.world.item.component.CustomData');
        var CT = Java.type('net.minecraft.nbt.CompoundTag');
        var Items = Java.type('net.minecraft.world.item.Items');
        var a = ItemStackOf(Items.STICK), b = ItemStackOf(Items.STICK);
        var t = new CT(); t.putBoolean('spectreAnchor', true);
        b.set(DC.CUSTOM_DATA, CD.of(t));
        b.setCount(1);
        inv.setItem(0, a);
        inv.setItem(1, b);
        inv.dropAll();
        return 'slot0 (plain) empty=' + inv.getItem(0).isEmpty() + ' slot1 (spectreAnchor) empty=' + inv.getItem(1).isEmpty();
    });
    step5('tree ground', function () {
        var Registries = Java.type('net.minecraft.core.registries.Registries');
        var TF = Java.type('net.minecraft.data.worldgen.features.TreeFeatures');
        var cf = lvl5.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(TF.OAK);
        var res = [];
        var grounds = ['randomthings:fertilizeddirt', 'minecraft:grass_block'];
        for (var k = 0; k < 2; k++) {
            var bx = 4 + k * 12;
            for (var x = -4; x <= 4; x++) for (var z = -4; z <= 4; z++) for (var y = 100; y <= 112; y++)
                lvl5.setBlock(new BP5(bx + x, y, 60 + z), y == 100 ? BuiltIn5.BLOCK.get(RL5.parse(grounds[k])).defaultBlockState() : Blocks5.AIR.defaultBlockState(), 2);
            var placed = cf.place(lvl5, lvl5.getChunkSource().getGenerator(), lvl5.random, new BP5(bx, 101, 60));
            res.push(grounds[k] + ' tree=' + placed + ' ground now=' + lvl5.getBlockState(new BP5(bx, 100, 60)).getBlock().getDescriptionId());
        }
        return res.join('; ');
    });
    step5('collision boxes', function () {
        var FPF = Java.type('net.neoforged.neoforge.common.util.FakePlayerFactory');
        var CC = Java.type('net.minecraft.world.phys.shapes.CollisionContext');
        var Slot = Java.type('net.minecraft.world.entity.EquipmentSlot');
        var fp = FPF.getMinecraft(lvl5);
        var p = new BP5(60, 100, 60);
        lvl5.setBlock(p, Blocks5.WATER.defaultBlockState(), 3);
        lvl5.setBlock(p.above(), Blocks5.AIR.defaultBlockState(), 3);
        fp.setPos(60.5, 101.5, 60.5);
        var bare = lvl5.getBlockState(p).getCollisionShape(lvl5, p, CC.of(fp)).isEmpty();
        fp.setItemSlot(Slot.FEET, ItemStackOf(itemOf('randomthings:waterwalkingboots')));
        var shod = lvl5.getBlockState(p).getCollisionShape(lvl5, p, CC.of(fp)).isEmpty();
        fp.setItemSlot(Slot.FEET, new (Java.type('net.minecraft.world.item.ItemStack'))(Java.type('net.minecraft.world.item.Items').AIR, 0));
        return 'water shape empty without boots=' + bare + ' with water walking boots=' + shod;
    });
    cm5at = tick;
    for (var j5 = 0; j5 < out5.length; j5++) log.info('CM5 ' + out5[j5]);
}

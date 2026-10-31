// Every Fisk block with a tile entity or a GUI: place it, right-click it with an empty hand (as a player would), report block entity, opened menu, result, and whether
// the block can be broken and drops its item (survival loop). Creative tab entries are counted too.
var ItemStack = Java.type("net.minecraft.world.item.ItemStack");
var InteractionHand = Java.type("net.minecraft.world.InteractionHand");
var BlockHitResult = Java.type("net.minecraft.world.phys.BlockHitResult");
var Direction = Java.type("net.minecraft.core.Direction");
var AABB = Java.type("net.minecraft.world.phys.AABB");
join("Machinist"); flat(1100, 1100, 100); tp(1100.5, 101, 1096.5);
var MP = P, lv = server.overworld();
var ids = []; var it = BuiltInRegistries.BLOCK.keySet().iterator();
while (it.hasNext()) { var k = it.next(); if (k.getNamespace() == "fiskheroes" || k.getNamespace() == "fisktag") ids.push(String(k)); }
var MR = {}; var steps = [];
ids.forEach(function (id) {
  steps.push(function () {
    var block = BuiltInRegistries.BLOCK.get(RL.parse(id));
    var pos = new BlockPos(1100, 101, 1100);
    lv.setBlock(pos, Blocks.AIR.defaultBlockState(), 3); lv.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
    lv.setBlock(pos, block.defaultBlockState(), 3);
    var r = { id: id, state: String(lv.getBlockState(pos)), be: String(lv.getBlockEntity(pos) == null ? "-" : lv.getBlockEntity(pos).getType().builtInRegistryHolder().key().location()) };
    MR[id] = r;
    MP.lookAt(Java.type("net.minecraft.commands.arguments.EntityAnchorArgument$Anchor").EYES, new Vec3(1100.5, 101.5, 1100.5));
    MP.closeContainer();
    try {
      var hit = new BlockHitResult(new Vec3(1100.5, 101.5, 1100.0), Direction.NORTH, pos, false);
      r.use = String(MP.gameMode.useItemOn(MP, lv, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit));
    } catch (e) { r.use = "EXC " + String(e).substring(0, 160); }
    r.menu = String(MP.containerMenu.getClass().getSimpleName());
    r.opened = MP.containerMenu !== MP.inventoryMenu;
    if (r.opened) MP.closeContainer();
    r.drops = ""; try { r.drops = String(Block.getDrops(lv.getBlockState(pos), lv, pos, lv.getBlockEntity(pos)).toString()); } catch (e) { r.drops = "EXC " + String(e).substring(0, 100); }
    return 0;
  });
});
var Block = Java.type("net.minecraft.world.level.block.Block");
steps.push(function () {
  var o = [];
  ids.forEach(function (id) { var r = MR[id]; if (r.be != "-" || r.opened || String(r.use).indexOf("EXC") >= 0) o.push(id.replace("fiskheroes:", "") + " be=" + r.be.replace("legacyconnector:", "") + " use=" + r.use + " menu=" + (r.opened ? r.menu : "-") + " drops=" + r.drops); });
  out("blocks " + ids.length + "; with tile/GUI/exceptions:\n" + o.join("\n"));
});
scenario(steps);
"machine sweep started: " + ids.length + " blocks"

// Right-click every Fisk item (air, then a block), swing it at a mob and let it tick in the hand; records the result of each use, new entities and
// exceptions; the server log shows anything a legacy handler threw ("Exception in legacy event handler", "threw").
var ItemStack = Java.type("net.minecraft.world.item.ItemStack");
var InteractionHand = Java.type("net.minecraft.world.InteractionHand");
var AABB = Java.type("net.minecraft.world.phys.AABB");
var Anchor = Java.type("net.minecraft.commands.arguments.EntityAnchorArgument$Anchor");
var BlockHitResult = Java.type("net.minecraft.world.phys.BlockHitResult");
var Direction = Java.type("net.minecraft.core.Direction");
join("ItemUser"); flat(900, 900, 100); tp(900.5, 101, 900.5);
var IU = P, dummy = spawn("minecraft:iron_golem", 900.5, 101, 904.5); dummy.setNoAi(true);
var ids = []; var it = BuiltInRegistries.ITEM.keySet().iterator();
while (it.hasNext()) { var k = it.next(); if (k.getNamespace() == "fiskheroes" || k.getNamespace() == "fisktag") ids.push(String(k)); }
var IR = {}; var steps = [];
function countEntities() { return server.overworld().getEntitiesOfClass(Java.type("net.minecraft.world.entity.Entity").class, new AABB(880, 90, 880, 920, 130, 920)).size(); }
ids.forEach(function (id) {
  steps.push(function () {
    var item = BuiltInRegistries.ITEM.get(RL.parse(id));
    var st = new ItemStack(item, 1);
    IU.setItemInHand(InteractionHand.MAIN_HAND, st);
    IU.lookAt(Anchor.EYES, dummy.position().add(0, 1.4, 0));
    var r = { id: id, before: countEntities(), res: [] };
    IR[id] = r;
    try { r.res.push("air:" + IU.gameMode.useItem(IU, server.overworld(), IU.getItemInHand(InteractionHand.MAIN_HAND), InteractionHand.MAIN_HAND)); } catch (e) { r.res.push("air:EXC " + String(e).substring(0, 120)); }
    try { r.res.push("entity:" + IU.interactOn(dummy, InteractionHand.MAIN_HAND)); } catch (e) { r.res.push("entity:EXC " + String(e).substring(0, 120)); }
    try { var hit = new BlockHitResult(new Vec3(900.5, 101, 900.5), Direction.UP, new BlockPos(900, 100, 900), false);
      r.res.push("block:" + IU.gameMode.useItemOn(IU, server.overworld(), IU.getItemInHand(InteractionHand.MAIN_HAND), InteractionHand.MAIN_HAND, hit)); } catch (e) { r.res.push("block:EXC " + String(e).substring(0, 120)); }
    try { IU.attack(dummy); } catch (e) { r.res.push("attack:EXC " + String(e).substring(0, 120)); }
    return 3;
  });
  steps.push(function () { var r = IR[id]; r.after = countEntities(); r.held = String(IU.getMainHandItem().getItem() == BuiltInRegistries.ITEM.get(RL.parse(id))); dummy.setHealth(100); dummy.invulnerableTime = 0;
    // discard what the use made (not the player or the dummy)
    var l = server.overworld().getEntitiesOfClass(Java.type("net.minecraft.world.entity.Entity").class, new AABB(880, 90, 880, 920, 130, 920));
    for (var i = 0; i < l.size(); i++) { var e = l.get(i); if (e !== IU && e !== dummy && !(e instanceof ServerPlayer)) e.discard(); }
    return 1; });
});
steps.push(function () {
  var bad = [], spawned = [], consumed = [];
  ids.forEach(function (id) { var r = IR[id]; var s = r.res.join(" "); if (s.indexOf("EXC") >= 0) bad.push(id + " " + s); if (r.after > r.before + 0) spawned.push(id.replace("fiskheroes:", "") + "+" + (r.after - r.before)); });
  out("items " + ids.length + "; exceptions: " + bad.length + "\n" + bad.join("\n") + "\nspawned entities: " + spawned.join(", "));
});
scenario(steps);
"item sweep started: " + ids.length + " items"

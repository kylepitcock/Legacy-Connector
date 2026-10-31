// Captain America: the shield throw (ItemCapsShield.receive, what MessageItemTrigger delivers) spawns EntityThrownShield, which hits the target and comes back to the hand.
var Anchor = Java.type("net.minecraft.commands.arguments.EntityAnchorArgument$Anchor");
var AABB = Java.type("net.minecraft.world.phys.AABB");
var Unpooled = Java.type("io.netty.buffer.Unpooled");
var ModItems2 = Java.type("com.fiskmods.heroes.common.item.ModItems");
var LItem2 = Java.type("legacyconnector.v1_7_10.net.minecraft.item.Item");
var InteractionHand2 = Java.type("net.minecraft.world.InteractionHand");
join("Rogers"); flat(2100, 2100, 100); tp(2100.5, 101, 2100.5); wear("captain_america");
var RG = P, shg = spawn("minecraft:iron_golem", 2100.5, 101, 2108.5); shg.setNoAi(true);
var shieldItem = ModItems2.captainAmericasShield;
function thrown() { var l = server.overworld().getEntitiesOfClass(Java.type("net.minecraft.world.entity.Entity").class, new AABB(2080, 90, 2080, 2120, 130, 2130)), o = []; for (var i = 0; i < l.size(); i++) { var e = l.get(i); if (String(e.getType().builtInRegistryHolder().key().location()).indexOf("shield") >= 0) o.push(e); } return o; }
function inv() { var n = 0; for (var i = 0; i < RG.getInventory().getContainerSize(); i++) { var s = RG.getInventory().getItem(i); if (!s.isEmpty() && String(s.getItem()).indexOf("shield") >= 0) n++; } return n; }
scenario([
  function () { return 5; },
  function () {
    RG.setItemInHand(InteractionHand2.MAIN_HAND, LItemStack.mcc$toModern(new LItemStack(shieldItem, 1, 0)));
    RG.lookAt(Anchor.EYES, shg.position().add(0, 1.2, 0));
    out("id roundtrip: " + (LItem2.getItemById(LItem2.getIdFromItem(shieldItem)) === shieldItem) + " permissions? " + hero("captain_america").getEnabledModifiers(L()).size() + " mods");
    var stack = L().getHeldItem();
    shieldItem.receive(L(), stack, Unpooled.buffer());
    out("thrown entities " + thrown().length + " hand now: " + RG.getMainHandItem() + " golem hp " + shg.getHealth()); return 10; },
  function () { var t = thrown(); out("t+10: thrown " + t.length + (t.length ? " at " + t[0].position() : "") + " golem hp " + shg.getHealth()); return 40; },
  function () { var t = thrown(); out("t+50: thrown " + t.length + (t.length ? " at " + t[0].position() : "") + " hand " + RG.getMainHandItem() + " shields in inventory " + inv() + " golem hp " + shg.getHealth()); return 40; },
  function () { var t = thrown(); out("t+90: thrown " + t.length + " hand " + RG.getMainHandItem() + " shields in inventory " + inv() + " item entities: " + server.overworld().getEntitiesOfClass(Java.type("net.minecraft.world.entity.item.ItemEntity").class, new AABB(2080, 90, 2080, 2120, 130, 2130)).size()); }
]);
"shield probe started"

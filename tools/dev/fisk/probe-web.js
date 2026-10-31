// Spider-Man web swinging: the swing key toggles WEB_SWINGING, a right click with an empty hand spawns an EntityWebRope that flies, sticks in a wall and pulls the player.
var Anchor = Java.type("net.minecraft.commands.arguments.EntityAnchorArgument$Anchor");
var AABB = Java.type("net.minecraft.world.phys.AABB");
var Interaction3 = Java.type("com.fiskmods.heroes.common.interaction.Interaction");
var InteractionType3 = Java.type("com.fiskmods.heroes.common.interaction.InteractionType");
var InteractionInfo3 = Java.type("com.fiskmods.heroes.common.interaction.InteractionInfo");
var LSide3 = Java.type("legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side");
join("Parker"); flat(1900, 1900, 100); tp(1900.5, 101, 1900.5); wear("spider_man");
var PK = P, lv3 = server.overworld();
for (var y = 101; y < 130; y++) for (var x = 1895; x < 1906; x++) lv3.setBlock(new BlockPos(x, y, 1912), Blocks.STONE.defaultBlockState(), 3);
function ropes() { var l = lv3.getEntitiesOfClass(Java.type("net.minecraft.world.entity.Entity").class, new AABB(1880, 90, 1880, 1920, 140, 1930)), o = []; for (var i = 0; i < l.size(); i++) { var e = l.get(i); if (String(e.getType().builtInRegistryHolder().key().location()).indexOf("web") >= 0) o.push(e); } return o; }
function fireWeb() { var reg = Interaction3.REGISTRY, hit = []; for (var i = 0; i < reg.size(); i++) { var it = reg.get(i); if (it.getClass().getSimpleName() == "InteractionWebSwing") hit.push(it.tryListen(L(), L(), new InteractionInfo3(InteractionType3.RIGHT_CLICK_AIR, 0, 0, 0, 0, 0, 0), LSide3.SERVER)); } return hit.join(","); }
var pos0;
scenario([
  function () { return 5; },
  function () { PK.lookAt(Anchor.EYES, new Vec3(1900.5, 112, 1912.5)); Vars.WEB_SWINGING.set(L(), true).sync(); out("worn " + worn() + " WEB_SWINGING " + Vars.WEB_SWINGING.get(L()) + " modifiers " + hero("spider_man").getEnabledModifiers(L()).size()); return 3; },
  function () { pos0 = PK.position(); out("fire web: tryListen=" + fireWeb() + " ropes now " + ropes().length + " WEB_ROPE_ID " + Vars.WEB_ROPE_ID.get(L())); return 8; },
  function () { var r = ropes(); out("after 8 ticks: ropes " + r.length + (r.length ? " pos " + r[0].position() + " motion " + r[0].getDeltaMovement() + " inGround " + (r[0].mcc$legacy ? r[0].mcc$legacy().field_70254_i : "?") : "") + " player motion " + PK.getDeltaMovement() + " pos " + PK.position() + " rope id var " + Vars.WEB_ROPE_ID.get(L()) + " swing timer " + Vars.WEB_SWINGING_TIMER.get(L())); return 25; },
  function () { var r = ropes(); out("after 33 ticks: ropes " + r.length + (r.length ? " pos " + r[0].position() : "") + " player moved " + PK.position().distanceTo(pos0).toFixed(2) + " motion " + PK.getDeltaMovement() + " sent " + sentSummary().substring(0, 300)); }
]);
"web probe started"

// Sonic wave trajectory: a Black Canary wave must fly away from the player along the look direction (EntityThrowable motion) and hurt a mob on the way.
var Anchor = Java.type("net.minecraft.commands.arguments.EntityAnchorArgument$Anchor");
var AABB = Java.type("net.minecraft.world.phys.AABB");
join("Canary"); flat(700, 700, 100); tp(700.5, 101, 700.5);
var CP = P, cg = spawn("minecraft:iron_golem", 700.5, 101, 706.5); cg.setNoAi(true);
function waves() { var l = server.overworld().getEntitiesOfClass(Java.type("net.minecraft.world.entity.Entity").class, new AABB(680, 90, 680, 720, 120, 720)); var o = []; for (var i = 0; i < l.size(); i++) { var e = l.get(i); if (String(e.getType().builtInRegistryHolder().key().location()) == "fiskheroes:canarycry") o.push(e); } return o; }
var steps = [function () { strip(); wear("black_canary_dinah"); return 4; }, function () { CP.lookAt(Anchor.EYES, cg.position().add(0, 1.4, 0)); Vars.SONIC_WAVES.set(L(), true).sync(); return 0; }];
for (var i = 0; i < 12; i++) steps.push(function () {
  var w = waves(); var s = "waves " + w.length;
  if (w.length) { var e = w[w.length - 1]; var d = e.getDeltaMovement(); s += " last pos " + e.position().x.toFixed(2) + "," + e.position().y.toFixed(2) + "," + e.position().z.toFixed(2) + " vel " + d.x.toFixed(2) + "," + d.y.toFixed(2) + "," + d.z.toFixed(2) + " age " + e.tickCount; }
  out(s + " golem hp " + cg.getHealth()); return 0;
});
steps.push(function () { Vars.SONIC_WAVES.set(L(), false).sync(); });
scenario(steps);
"sonic probe started"

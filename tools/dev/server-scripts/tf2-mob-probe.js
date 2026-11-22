// tf2 mercenary AI probe (#184): spawn a mercenary near a player and watch whether it targets, aims and fires its weapon.
// usage: LIBS=lib tools/dev/run-server-script.sh tools/dev/server-scripts/tf2-mob-probe.js 400 100   (TF2 Stuff Reloaded in run-subtest/mods/1.12.2)
var RL = T("net.minecraft.resources.ResourceLocation");
var BR = T("net.minecraft.core.registries.BuiltInRegistries");
var EB = T("org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122");
if (typeof TFS === "undefined") {
  TFS = { n: 0 };
  var names = [];
  BR.ENTITY_TYPE.keySet().forEach(function (k) { if (("" + k).indexOf("tf2") >= 0) names.push("" + k); });
  log.info("SERVERSCRIPT: tf2 entity types: " + names.join(","));
  var level = server.overworld();
  TFS.player = realPlayer("Tester");
  TFS.player.setPos(0.5, -60, 0.5);
  var Vec3 = T("net.minecraft.world.phys.Vec3");
  TFS.player.setHealth(20);
  TFS.player.setGameMode(T("net.minecraft.world.level.GameType").SURVIVAL);
  var before = {};
  level.getAllEntities().forEach(function (e) { before[e.getId()] = 1; });
  server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withLevel(level).withPosition(new Vec3(8.5, -60, 0.5)),
    "summon " + (typeof MOB === "undefined" ? "rafradek_tf2_weapons:soldier" : MOB) + " 8.5 -60 0.5 {Rotation:[90.0f,0.0f]}");
  level.getAllEntities().forEach(function (e) { if (!before[e.getId()] && ("" + e).indexOf("tf2") >= 0 || (!before[e.getId()] && !TFS.mob && !(e instanceof T("net.minecraft.server.level.ServerPlayer")))) TFS.mob = e; });
  "spawned " + TFS.mob + " hp=" + TFS.player.getHealth();
} else {
  TFS.n++;
  var out = "";
  if (TFS.n % 20 == 0) {
    var m = TFS.mob;
    var l = EB.wrap(m);
    out = "t=" + TFS.n + " mob=" + m + " target=" + m.getTarget() + " hp=" + TFS.player.getHealth() + " held=" + m.getMainHandItem()
      + " dist=" + m.distanceTo(TFS.player);
    var it = l.tasks.taskEntries.iterator(), total = 0, running = [];
    while (it.hasNext()) { var g = it.next(); total++; if (g.using) running.push("" + g.action.getClass().getSimpleName()); }
    out += " tasks=" + total + " running=" + running.join(",");
    var it2 = l.targetTasks.taskEntries.iterator(), tr = [];
    while (it2.hasNext()) { var g2 = it2.next(); if (g2.using) tr.push("" + g2.action.getClass().getSimpleName()); }
    var cnt = {}; server.overworld().getAllEntities().forEach(function (e) { var k = "" + e.getType().builtInRegistryHolder().key().location(); cnt[k] = (cnt[k] || 0) + 1; });
    var parts = []; for (var k in cnt) parts.push(k.replace("rafradek_tf2_weapons:", "") + "=" + cnt[k]);
    out += " ents[" + parts.join(",") + "] targetHp=" + (m.getTarget() ? m.getTarget().getHealth() : "-") + " clip=" + (m.getMainHandItem().getTag ? "" : "") + " targetRunning=" + tr.join(",");
  }
  if (TFS.n == 30) {
    try {
      var l2 = EB.wrap(TFS.mob);
      var P = T("legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer");
      var SMA = T("legacyconnector.v1_12_2.net.minecraft.entity.SharedMonsterAttributes");
      var w = l2.world;
      var bb = l2.getEntityBoundingBox().grow(32, 32, 32);
      var lst = w.getEntitiesWithinAABB(P.class, bb);
      out += " | players in box=" + lst.size() + " followRange=" + l2.getEntityAttribute(SMA.FOLLOW_RANGE).getAttributeValue()
        + " canSee=" + (lst.size() > 0 ? l2.getEntitySenses().canSee(lst.get(0)) : "-") + " team=" + l2.getTeam();
      var ents = w.getEntitiesWithinAABB(T("legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase").class, bb);
      out += " living in box=" + ents.size();
    } catch (e) { out += " DIAG EXC " + e; }
  }
  out;
}

var p = GC.player();
var o = "player " + p.level().dimension().location() + " y " + p.getY() + " riding " + p.isPassenger() + "\n";
o += "rocket alive " + ROCKET.isAlive() + " y " + ROCKET.getY() + " dim " + ROCKET.level().dimension().location() + " phase " + LROCKET.launchPhase + " fuel " + LROCKET.fuelTank.getFluidAmount() + "\n";
o;

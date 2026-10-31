# Create contraptions and Create Aeronautics/Sable ships

Back to [Home](Home.md).

Create contraptions (bearings, pistons, gantries, minecart contraptions, trains) and Create Aeronautics/Sable ships can carry
legacy blocks (1.7.10 and 1.12.2; issue #108; tested in game with a mechanical bearing and an Aeronautics airship carrying Iron Chest, Thaumcraft, RotaryCraft and ChromatiCraft blocks; minecart contraptions and 1.12.2 not yet). The hosted tile entity's NBT travels with the block and
is loaded at the new position with its coordinates corrected, and `breakBlock` is not run for a tile entity block that is carried
away (it would drop the inventory that also travels). With Create installed (optional), its movement checks follow the legacy
piston rules: mobility flag 2 and hardness -1 (also per position) refuse a block, mobility 1 blocks (torches, levers, wires) are
moved as brittle. On a moving contraption, tile entity renderers draw from the contraption's render world, but legacy tile
entities there do not tick; on a Sable ship the blocks stay in the real world and tick normally. Limitations: block orientation
kept in metadata is not rotated when a contraption is placed back turned; legacy inventories are not contraption storage (drills
and harvesters cannot fill them while moving); tile entities that store other blocks' absolute coordinates (multiblock masters)
keep pointing at the old place.

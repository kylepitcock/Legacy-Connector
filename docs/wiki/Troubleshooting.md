# Troubleshooting

## Clients disconnect on join with "length wider than 21-bit"

The client log shows `io.netty.handler.codec.CorruptedFrameException: length wider than 21-bit` (Minecraft's
`Varint21FrameDecoder`). Two causes, in order of likelihood:

1. **A corrupted or misaligned packet stream.** If the error appears right after login, at the start of the configuration phase, no
   large packet has been sent yet, so it is not a size problem. Mods that replace Minecraft's network pipeline are the first suspect,
   for example KryptonFNP (libdeflate compression, OpenSSL encryption, replaced frame decoder/prepender) or other
   packet-compression / Netty optimisers, particularly when the server and client run different NeoForge versions. Remove them
   from the server (and client) to confirm. Legacy Connector adds no packet handling of its own to the pipeline.
2. **A single frame over 2,097,151 bytes (after compression).** The recipe and tag sync packets grow with the number of mods.
   At server start Legacy Connector measures them and logs an error naming the packet if one is over the limit (a warning above 75 %),
   e.g. `the recipe sync packet is N KiB compressed, over the 2047 KiB network frame limit`. If you see it, disabling
   `blocks.flattenSubtypes` in the Legacy Connector config reduces item, block, tag and recipe counts (it changes which legacy content
   exists, so it is not a drop-in). A mod that lifts the packet limit would have to be installed on both sides; none was tested here.

## Measuring what a join sends

Start the server with `-Dlegacyconnector.joinSizeTest=true` (also enabled by `-Dlegacyconnector.entitySelfTest=true`). Once the server is up
it logs `JOIN SIZE TEST`: raw and zlib-compressed size of the recipe packet (also per namespace and recipe serializer), the tag packet
(per registry), every synced datapack registry, NeoForge's registry id sync, the command tree and the advancements, largest first.

Reference measurement (GTNH-style corpus of 1.7.10 mods, 23,081 recipes): recipes 467 KB compressed (3.98 MB raw), registry id sync of
`minecraft:item` 103 KB, tags 54 KB, everything else under 50 KB each; total about 760 KB, well under the limit.

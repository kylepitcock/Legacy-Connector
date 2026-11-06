#!/bin/sh
# Downloads legacy reference data used by tools/legacy_api.py and the shim audit into build/devtools/.
set -e
cd "$(dirname "$0")/.."
D=build/devtools/mcp
mkdir -p "$D"
M=https://maven.minecraftforge.net
curl -sfo "$D/mcp_stable-12-1.7.10.zip" $M/de/oceanlabs/mcp/mcp_stable/12-1.7.10/mcp_stable-12-1.7.10.zip
curl -sfo "$D/mcp_stable-39-1.12.zip" $M/de/oceanlabs/mcp/mcp_stable/39-1.12/mcp_stable-39-1.12.zip
curl -sfo "$D/forge1710-userdev.jar" $M/net/minecraftforge/forge/1.7.10-10.13.4.1614-1.7.10/forge-1.7.10-10.13.4.1614-1.7.10-userdev.jar
curl -sfo "$D/srg1710.zip" $M/de/oceanlabs/mcp/mcp/1.7.10/mcp-1.7.10-srg.zip
curl -sfo "$D/srg1122.zip" $M/de/oceanlabs/mcp/mcp_config/1.12.2/mcp_config-1.12.2.zip
# Beta 1.7.3 (tools/extract_beta_signatures.py): the game jars, Risugami's ModLoader, ModLoaderMP and Forge 1.0.7 (names and
# supertypes are read from them; nothing is shipped)
B=build/devtools/b1.7.3
mkdir -p "$B"
W=https://maven.wagyourtail.xyz/releases
curl -sfLo "$B/client.jar" https://launcher.mojang.com/v1/objects/43db9b498cb67058d2e12d394e6507722e71bb45/client.jar
curl -sfLo "$B/server.jar" https://files.betacraft.uk/server-archive/beta/b1.7.3.jar
curl -sfLo "$B/modloader.jar" $W/risugami/modloader/b1.7.3/modloader-b1.7.3.jar
curl -sfLo "$B/modloadermp-client.jar" $W/modloadermp/modloadermp/b1.7.3/modloadermp-b1.7.3-client.jar
curl -sfLo "$B/modloadermp-server.jar" $W/modloadermp/modloadermp/b1.7.3/modloadermp-b1.7.3-server.jar
curl -sfLo "$B/forge-client.zip" $W/net/minecraftforge/forge/b1.7.3-1.0.7/forge-b1.7.3-1.0.7-client.zip
curl -sfLo "$B/forge-server.zip" $W/net/minecraftforge/forge/b1.7.3-1.0.7/forge-b1.7.3-1.0.7-server.zip
echo "Reference data in $D and $B"

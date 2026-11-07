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
curl -sfo "$D/forge164-src.zip" $M/net/minecraftforge/forge/1.6.4-9.11.1.1345/forge-1.6.4-9.11.1.1345-src.zip
curl -sfo "$D/srg164.zip" $M/de/oceanlabs/mcp/mcp/1.6.4/mcp-1.6.4-srg.zip
curl -sfo "$D/forge152-src.zip" $M/net/minecraftforge/forge/1.5.2-7.8.1.738/forge-1.5.2-7.8.1.738-src.zip
curl -sfo "$D/forge147-src.zip" $M/net/minecraftforge/forge/1.4.7-6.6.2.534/forge-1.4.7-6.6.2.534-src.zip
mkdir -p build/devtools/mojang
curl -sfo build/devtools/mojang/client-1.5.2.jar https://launcher.mojang.com/v1/objects/465378c9dc2f779ae1d6e8046ebc46fb53a57968/client.jar
curl -sfo build/devtools/mojang/client-1.4.7.jar https://launcher.mojang.com/v1/objects/53ed4b9d5c358ecfff2d8b846b4427b888287028/client.jar
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

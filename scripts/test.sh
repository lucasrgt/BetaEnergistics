#!/bin/bash
set -e
BASE="$(cd "$(dirname "$0")/.." && pwd)"
MCP_ROOT="${BE_MCP_ROOT:-$BASE/mcp}"
cd "$BASE"

echo "=== Transpiling ==="
bash scripts/transpile.sh

echo "=== Building ==="
cd "$MCP_ROOT"
echo "build" | java -jar RetroMCP-Java-CLI.jar
cd "$BASE"

echo "=== Preparing test jar ==="
cp tests/data/minecraft_test.jar tests/data/minecraft_run.jar

# Inject mod classes from build zip into run jar
TMP="$BASE/temp_build"
rm -rf "$TMP" && mkdir -p "$TMP"
cd "$TMP"
jar xf "$MCP_ROOT/build/minecraft.zip"
jar uf "$BASE/tests/data/minecraft_run.jar" *.class
echo "Injected mod classes"

# Inject custom textures into run jar
if [ -d "$BASE/temp/merged" ]; then
    cd "$BASE/temp/merged"
    jar uf "$BASE/tests/data/minecraft_run.jar" .
    echo "Injected custom textures"
fi

cd "$BASE"
rm -rf "$TMP"

mkdir -p "$BASE/tests/data/tmp"
echo "=== Launching Minecraft ==="
LIBS="$MCP_ROOT/libraries"
java -Xms1024M -Xmx1024M \
  -Djava.library.path="$LIBS/natives" \
  -Djava.io.tmpdir="$BASE/tests/data/tmp" \
  -cp "temp/merged;tests/data/minecraft_run.jar;$LIBS/net/java/jinput/jinput/2.0.5/jinput-2.0.5.jar;$LIBS/net/java/jutils/jutils/1.0.0/jutils-1.0.0.jar;$LIBS/org/lwjgl/lwjgl/lwjgl/2.9.4-nightly-20150209/lwjgl-2.9.4-nightly-20150209.jar;$LIBS/org/lwjgl/lwjgl/lwjgl_util/2.9.4-nightly-20150209/lwjgl_util-2.9.4-nightly-20150209.jar;$LIBS/com/paulscode/codecjorbis/20230120/codecjorbis-20230120.jar;$LIBS/com/paulscode/codecwav/20101023/codecwav-20101023.jar;$LIBS/com/paulscode/libraryjavasound/20101123/libraryjavasound-20101123.jar;$LIBS/com/paulscode/librarylwjglopenal/20100824/librarylwjglopenal-20100824.jar;$LIBS/com/paulscode/soundsystem/20120107/soundsystem-20120107.jar" \
  net.minecraft.client.Minecraft

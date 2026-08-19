#!/bin/bash
# Transpiles organized mod and injected dependency sources into RetroMCP.
# Rewrites packages and removes internal imports so RetroMCP can compile
set -e
BASE="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$BASE/src/betaenergistics"
MCP_ROOT="${BE_MCP_ROOT:-$BASE/mcp}"
DEST="$MCP_ROOT/minecraft/src/net/minecraft/src"

if [ ! -d "$DEST" ]; then
    echo "Missing RetroMCP source tree: $DEST" >&2
    echo "Set BE_MCP_ROOT to an initialized external RetroMCP workspace." >&2
    exit 1
fi

# Remove old transpiled mod files (only BE_ prefixed + Aero_ prefixed + mod_BetaEnergistics)
find "$DEST" -maxdepth 1 -name "BE_*.java" -delete 2>/dev/null || true
find "$DEST" -maxdepth 1 -name "Aero_*.java" -delete 2>/dev/null || true
find "$DEST" -maxdepth 1 -name "mod_BetaEnergistics.java" -delete 2>/dev/null || true

# Transpile function: flatten packages and strip internal imports
transpile_file() {
    local file="$1"
    local filename
    filename=$(basename "$file")
    sed \
        -e 's/^package betaenergistics\(\.[a-z]*\)\?;/package net.minecraft.src;/' \
        -e 's/^package aero\.\([a-z_]*\);/package net.minecraft.src;/' \
        -e '/^import betaenergistics\./d' \
        -e '/^import static betaenergistics\./d' \
        -e '/^import aero\./d' \
        -e '/^import static aero\./d' \
        -e '/^import net\.minecraft\.src\.\*;/d' \
        "$file" > "$DEST/$filename"
}

# Transpile explicitly injected dependencies. Paths are semicolon-separated so
# Windows drive letters remain valid in MSYS shells.
LIB_COUNT=0
IFS=';' read -r -a DEPENDENCY_ROOTS <<< "${BE_DEPENDENCY_ROOTS:-}"
for dependency in "${DEPENDENCY_ROOTS[@]}"; do
    [ -n "$dependency" ] || continue
    if [ ! -d "$dependency" ]; then
        echo "Missing dependency source root: $dependency" >&2
        exit 1
    fi
    while IFS= read -r file; do
        transpile_file "$file"
        LIB_COUNT=$((LIB_COUNT + 1))
    done < <(find "$dependency" -name '*.java' -not -path '*/tools/*' -not -path '*/scripts/*' \
        -not -path '*/stationapi/*' -not -path '*/tests/*' | sort)
done

if [ "$AERO_RELEASE" = "1" ]; then
    find "$DEST" -maxdepth 1 -name "Aero_Dev*.java" -delete 2>/dev/null || true
    echo "[RELEASE] Excluded devtools from transpile"
fi

# Transpile mod source
find "$SRC" -name "*.java" | while read -r file; do
    transpile_file "$file"
done
SRC_COUNT=$(find "$SRC" -name '*.java' | wc -l)

echo "Transpiled $LIB_COUNT library + $SRC_COUNT mod files to $DEST"

# Copy assets (textures, models) to temp/merged for jar injection
ASSETS="$SRC/assets"
if [ -d "$ASSETS" ]; then
    mkdir -p "$BASE/temp/merged"
    cp -r "$ASSETS"/* "$BASE/temp/merged/"
    echo "Copied assets to temp/merged/"
fi

# BetaEnergistics Engineering Guide

All repository artifacts must be written in English.

## Source of truth

1. Edit original sources only under `src/betaenergistics/`.
2. Never commit transpiled RetroMCP output, Minecraft binaries, decompiled
   sources, generated classes, or game assets.
3. The standalone repository is canonical. The historical
   `minecraft-beta-modding` entry is only a Git pointer to this repository.
4. External libraries are dependencies, not vendored source and not required
   as Git submodules.

## Engineering rules

1. New product files must remain at or below 200 code lines.
2. Existing files above 200 lines are frozen by exact per-file allowances in
   `tools/harness/legacy-lines.properties`. An allowance may only decrease or
   be removed during refactoring.
3. Harness files must remain at or below 300 code lines and test files at or
   below 150 code lines.
4. Missing tools, stale legacy allowances, misplaced packages, generated
   artifacts, and failed tests fail closed.

## Canonical verification

Run from the repository root:

```text
java tools/harness/Verify.java
```

This zero-dependency gate enforces source ceilings, package layout, release
metadata, Java 8 compilation for the dependency-free core slice, and its test
suite. Full Minecraft runtime qualification remains a separate future gate.

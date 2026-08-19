# BetaEnergistics

Digital storage, automation, fluids, and crafting infrastructure for
Minecraft Beta 1.7.3.

This standalone repository is the canonical source. The entry under the old
`minecraft-beta-modding` workspace points to the exact same Git repository and
commit; it is not a separate implementation.

## Repository gate

Requirements: JDK 21 for the harness. Product contracts compiled by the host
gate target Java 8.

```text
java tools/harness/Verify.java
```

The mini harness verifies release metadata, package placement, source-file
ceilings, the absence of generated Minecraft artifacts, and a dependency-free
core contract suite. Current files over 200 code lines are recorded as a
strict ratchet: they cannot grow, and their allowance must be reduced whenever
they are refactored.

## External dependencies

Dependency revisions are recorded in `dependency-lock.properties`. They are
not Git submodules. Legacy RetroMCP transpilation accepts explicit checkout
paths through `BE_DEPENDENCY_ROOTS`, separated by semicolons, and an optional
`BE_MCP_ROOT`. This makes the repository independent of its former depth in a
monorepo.

Example in an MSYS-compatible shell:

```text
BE_MCP_ROOT=/c/work/retromcp \
BE_DEPENDENCY_ROOTS='/c/work/aero-machine-api;/c/work/aero-devtools' \
bash scripts/transpile.sh
```

The legacy scripts are retained for compatibility. A modern mapped runtime
build and the BetaVault integration are separate milestones.

## BetaVault integration gate

The integration module uses the pinned BetaVault revision without vendoring or
a Git submodule. Set `BETAVAULT_ROOT` to a checkout, or keep it as the sibling
`../betavault`, then run:

```text
java tools/harness/Verify.java --integration
```

This gate compiles the real BetaEnergistics item-key type with the real
BetaVault store and Minecraft reference adapter. It proves stable cell
identity across a physical copy, rejection of a stale duplicate extraction,
save/restart equivalence, and cross-world rejection. Carrying the canonical
reference through Beta 1.7.3 `ItemStack` save and network codecs remains a
Worldline-controlled runtime boundary.

## Layout

- `src/betaenergistics/`: original organized mod source
- `scripts/`: legacy RetroMCP transpile and launch workflows
- `tests/`: host contract tests
- `integrations/betavault/`: storage-cell codec and save-bound adapter
- `tools/harness/`: canonical repository gate
- `docs/STANDALONE_COMPARISON.md`: comparison with the old workspace pointer

No official Minecraft JAR, assets, or decompiled sources belong in this
repository.

<p align="center">
  <img src="src/betaenergistics/assets/blocks/be_controller.png" alt="BetaEnergistics Controller" width="128">
</p>

<h1 align="center">BetaEnergistics</h1>

<p align="center"><strong>Digital storage, logistics, and autocrafting for Minecraft Beta 1.7.3.</strong></p>

<p align="center">
  <a href="#getting-started">Getting Started</a> |
  <a href="#capabilities">Capabilities</a> |
  <a href="#architecture">Architecture</a> |
  <a href="#documentation">Documentation</a>
</p>

<p align="center">
  <a href="https://github.com/lucasrgt/BetaEnergistics/actions/workflows/verify.yml"><img src="https://github.com/lucasrgt/BetaEnergistics/actions/workflows/verify.yml/badge.svg?branch=main" alt="CI"></a>
  <img src="https://img.shields.io/badge/status-0.1.0--legacy%20baseline-8A6D3B?style=flat-square" alt="0.1.0 legacy baseline">
  <img src="https://img.shields.io/badge/Minecraft-Beta%201.7.3-62B47A?style=flat-square" alt="Minecraft Beta 1.7.3">
  <img src="https://img.shields.io/badge/runtime-ModLoader-5586A4?style=flat-square" alt="ModLoader runtime">
  <img src="https://img.shields.io/badge/product-Java%208-E76F00?style=flat-square" alt="Java 8 product">
  <img src="https://img.shields.io/badge/harness-JDK%2021-6B5B95?style=flat-square" alt="JDK 21 harness">
</p>

BetaEnergistics brings an ME-style storage network to Minecraft Beta 1.7.3.
Players connect controllers, cables, disk drives, terminals, buses, pattern
encoders, and crafting machines into one system for items, fluids, gases, and
automated production.

The implementation is inspired by the accessibility of Refined Storage and
the systems depth of Applied Energistics, while staying within Beta 1.7.3's
ModLoader-era runtime. The repository is now standalone, dependency revisions
are pinned, and host behavior is qualified with the public Worldline TestKit.

<table>
<tr><td><b>One storage network</b></td><td>Items, fluids, gases, external inventories, disks, and crafting providers share explicit network boundaries.</td></tr>
<tr><td><b>Terminal-driven workflow</b></td><td>Grid, crafting, request, fluid, gas, and mobile terminals expose storage and automation without physical slot scans.</td></tr>
<tr><td><b>Autocrafting</b></td><td>Encoded patterns, recursive planning, autocrafters, interfaces, coprocessors, and a crafting monitor form the production pipeline.</td></tr>
<tr><td><b>Incremental catalog</b></td><td>An opt-in mutation-driven index provides immutable views and zero-scan idle work; it remains a candidate until official-runtime qualification.</td></tr>
<tr><td><b>Fail-closed verification</b></td><td>Source ceilings, Java 8 contracts, BetaVault integration, and Worldline consumer specs reject missing or stale evidence.</td></tr>
</table>

---

## Runtime matrix

| Surface | Runtime | Toolchain | Current status |
| --- | --- | --- | --- |
| Host contracts | No Minecraft process | JDK 21 harness, product `--release 8` | Qualified on Linux and Windows |
| Worldline consumer | Host-only Worldline TestKit 0.3.0 | Gradle 8.14.4, JDK 21 | Qualified through the public Gradle plugin |
| BetaVault integration | Real pinned BetaVault sources | JDK 21 integration gate | Qualified for identity, restart, and catalog reconstruction |
| Legacy game adapter | Minecraft Beta 1.7.3 + ModLoader | External RetroMCP workspace | Source-compatible path retained; official runtime qualification pending |
| StationAPI | Minecraft Beta 1.7.3 + StationAPI | Not configured | Not currently supported |

The release manifest declares `0.1.0-legacy` with status `baseline`. This is an
engineering baseline, not a claim of a polished player release. Official
Minecraft JARs, decompiled sources, and generated game classes are never
stored in this repository.

---

## Getting started

### 1. Run the canonical repository gate

Requirements:

- JDK 21 for repository tooling;
- Git for dependency and source identity checks;
- no Minecraft installation for host-only verification.

```text
java tools/harness/Verify.java
```

The gate enforces package layout and per-file source ceilings, verifies release
metadata, compiles the dependency-free product slice as Java 8 with warnings as
errors, and runs the complete registered host suite. Derived output stays under
the ignored `.betaenergistics/build/` directory.

### 2. Run the published Worldline TestKit suite

The isolated project under `tests/worldline` resolves
`io.github.lucasrgt.worldline.test:0.3.0` from the Gradle Plugin Portal:

```text
tests\worldline\gradlew.bat -p tests/worldline worldlineDoctor worldlineTest
```

On Linux or macOS:

```text
./tests/worldline/gradlew -p tests/worldline worldlineDoctor worldlineTest
```

The current suite is host-only. `worldlineDoctor` therefore verifies that no
tracked official JAR is present and reports the runtime provider as disabled.

### 3. Qualify the BetaVault integration

Keep BetaVault as a sibling checkout at `../betavault`, or point
`BETAVAULT_ROOT` at a checkout of the revision pinned in
`dependency-lock.properties`:

```text
java tools/harness/Verify.java --integration
```

This proves stable storage-cell identity across physical copies, duplicate
rejection, world scoping, save/restart equivalence, exact catalog deltas, and
catalog reconstruction after restart. BetaVault remains an external dependency;
its source is not vendored here.

### 4. Prepare the legacy ModLoader source tree

The game-linked build requires an initialized external RetroMCP workspace and
explicit dependency source roots. In an MSYS-compatible shell:

```text
BE_MCP_ROOT=/c/work/retromcp \
BE_DEPENDENCY_ROOTS='/c/work/aero-machine-api;/c/work/aero-devtools' \
bash scripts/transpile.sh
```

`transpile.sh` flattens the organized source packages into the external
RetroMCP tree. Never edit that generated tree directly. Runtime build and launch
scripts are retained for legacy development, but they do not replace the
pending official Worldline runtime qualification.

---

## Capabilities

### Digital storage

| Capability | What it provides |
| --- | --- |
| Item disks | Six tiers from 1K through 1024K |
| Fluid disks | Four tiers from 8K through 512K mB |
| Gas disks | Four tiers from 8K through 512K |
| Disk drive | Persistent disk mounting with per-cell state |
| External storage | Item and fluid storage buses over existing inventories and handlers |
| Access policy | Insert-only, extract-only, and bidirectional storage behavior |
| BetaVault cells | Save-bound canonical references with duplicate and cross-world protection |

### Network and terminals

| Capability | What it provides |
| --- | --- |
| Controller and cable graph | Network membership, energy, and connected machine discovery |
| Grid terminal | Searchable, sortable virtual item catalog |
| Crafting terminal | Storage grid plus 3x3 crafting, network fill, and automatic refill |
| Request terminal | Craftable-item selection, plan preview, quantities, and confirmation |
| Fluid and gas terminals | Dedicated views for non-item resources |
| Mobile terminal | Portable access backed by the network registry |
| Multiplayer packets | Client/server actions and terminal, fluid, crafting, and controller updates |

### Automation and crafting

| Capability | What it provides |
| --- | --- |
| Import and export buses | Configured movement between networks and adjacent inventories |
| Recipe encoder | Blank and encoded patterns for crafting and processing recipes |
| Autocrafter | Pattern execution using network resources |
| Advanced interface | Processing-pattern bridge to external machines |
| Crafting coprocessor | Additional parallel crafting capacity |
| Crafting monitor | Visibility into active crafting work |
| Redstone emitters | Item and fluid thresholds exposed as redstone state |
| Facades | Per-face cable covers persisted through NBT |

Gas import and export IDs are reserved in the legacy registry, but those blocks
are not registered implementations and are not claimed as current capabilities.

---

## Hyperperformance model

The candidate catalog replaces query-time network scans with mutation-driven
indexing:

```text
provider mutation -> exact delta -> stable shard -> global catalog
                                              |-> immutable terminal view

extraction request <- provider directory <----+
```

The host contracts cover 100,000 providers, 10,000 idle ticks, deterministic
soak, immutable snapshots, generation invalidation, and zero full scans during
normal idle work. Structural counters are acceptance criteria; wall-clock time
is diagnostic only.

The optimized catalog is deliberately disabled by default. It requires either
the explicit constructor opt-in or:

```text
-Dbetaenergistics.storage.incrementalCatalog=true
```

The optimization records under `worldline/optimizations/catalog/` remain
`candidate`.
They will not be promoted until the real ModLoader adapter and official runtime
smokes cover the relevant mutation and lifecycle boundaries. See
[Hyperperformance architecture](docs/HYPERPERFORMANCE.md).

---

## Architecture

```text
ModLoader bootstrap
       |
       +--> blocks + tiles + containers + GUIs
       |                         |
       |                         +--> packet transport
       |
       +--> storage network -----+--> item/fluid/gas providers
                    |            |
                    |            +--> disks + external storage + BetaVault
                    |
                    +--> catalog snapshots --> terminals
                    |
                    +--> crafting planner --> patterns --> machines
```

| Boundary | Responsibility |
| --- | --- |
| `storage/` | Stable keys, storage interfaces, disks, registries, composites, and catalog indexes |
| `network/` | Graph membership, provider discovery, resource aggregation, and packet transport |
| `tile/` | Minecraft lifecycle, persistence, machine state, and neighboring-world interaction |
| `container/` | Server-authoritative inventory and terminal actions |
| `gui/` | Client rendering, search, scrolling, previews, and user input |
| `crafting/` | Recipe graphs, conservation, dependency planning, and execution requests |
| `integrations/betavault/` | External cell identity, codec, commit bridge, and restart reconstruction |
| `worldline/optimizations/` | Project-owned optimization metadata and evidence identities |
| `worldline/extensions/` | Reserved namespace for real Worldline extension manifests |
| `tests/worldline/` | Public TestKit consumer contracts, isolated from product runtime code |

Original sources live only under `src/betaenergistics/`. RetroMCP output is a
derived compatibility surface and must never become the source of truth.

---

## Documentation

| Document | Purpose |
| --- | --- |
| [Refactor plan](docs/REFACTOR_PLAN.md) | Ordered total-refactor sequence, ratchet, and qualification rules |
| [Hyperperformance](docs/HYPERPERFORMANCE.md) | Incremental catalog design, work counters, scale, and promotion boundary |
| [Worldline extension](docs/WORLDLINE_EXTENSION.md) | TestKit consumer and future runtime-provider contract |
| [BetaVault integration](docs/BETAVAULT_INTEGRATION.md) | Cell identity, save binding, conflict handling, and integration gate |
| [Standalone comparison](docs/STANDALONE_COMPARISON.md) | Relationship to the historical monorepo pointer |
| [Repository engineering guide](AGENTS.md) | Source ownership, file ceilings, and canonical verification |

---

## Compatibility and scope

- The maintained product target is Minecraft Beta 1.7.3 through ModLoader-era
  APIs and Java 8 bytecode.
- The host harness requires JDK 21; this does not raise the product bytecode
  target.
- External libraries are pinned dependencies, not Git submodules or copied
  source trees.
- StationAPI is not a supported BetaEnergistics runtime today.
- The official-JAR ModLoader provider, mapped lifecycle coverage, and
  multiplayer/runtime soak remain open qualification work.
- Player-ready packaging and installation instructions will be added only when
  a release artifact is reproducibly produced and qualified.

---

## Build and contribute

### Canonical checks

```text
java tools/harness/Verify.java
java tools/harness/Verify.java --integration
tests\worldline\gradlew.bat -p tests/worldline worldlineDoctor worldlineTest
```

Every new product source file must remain at or below 200 code lines. Existing
overages are exact allowances in `tools/harness/legacy-lines.properties`; an
allowance may only decrease or disappear during refactoring. Harness files are
limited to 300 lines and test files to 150 lines.

All repository artifacts are written in English. Changes must preserve the
standalone source layout, keep generated Minecraft output outside Git, and add
a reusable contract when they introduce or move behavior.

---

## Project transparency

| Claim | Evidence | Status |
| --- | --- | --- |
| Java 8 domain contracts | `java tools/harness/Verify.java` | Qualified |
| Incremental catalog invariants and scale | Host gate plus Worldline TestKit 0.3.0 | Qualified, optimization still opt-in |
| BetaVault cell and restart behavior | `Verify.java --integration` at the pinned revision | Qualified |
| Linux consumer resolution from the Gradle Plugin Portal | GitHub Actions `worldline-testkit` job | Qualified |
| ModLoader product compilation | External RetroMCP compatibility path | Not yet a canonical gate |
| Official Beta 1.7.3 runtime behavior | Future Worldline ModLoader provider | Pending |
| Player distribution | Reproducible release pipeline | Pending |

The standalone repository is canonical. The historical
`minecraft-beta-modding` entry is only a pointer to this Git history, not a
second implementation.

# BetaEnergistics refactor plan

The refactor starts from `08b4f1c` and keeps behavior changes separate from
structural changes. Every completed slice must pass the repository gate, the
BetaVault integration gate, and the published Worldline TestKit consumer suite.
Minecraft-facing slices additionally require the future ModLoader runtime
provider before their behavior can be called qualified.

## Sequence

1. Extract dependency-free terminal ordering, filtering, and paging contracts.
2. Separate packet value codecs from ModLoader packet transport and handlers.
3. Move crafting requests and storage mutations out of GUI containers.
4. Split shared terminal rendering and input state from concrete screens.
5. Isolate tile lifecycle, network membership, and persistence boundaries.
6. Split mod bootstrap, registration, recipes, and compatibility wiring.
7. Replace legacy polling only after mutation coverage and runtime evidence exist.

## Ratchet

The baseline has 14 legacy product files above 200 code lines. A slice may only
decrease an existing allowance; no new allowance is permitted. The target is
zero legacy allowances without moving product behavior into tests or tooling.

## Qualification

- `java tools/harness/Verify.java`
- `java tools/harness/Verify.java --integration`
- `tests/worldline/gradlew.bat worldlineDoctor worldlineTest --no-daemon`

The Gradle suite resolves `io.github.lucasrgt.worldline.test:0.3.0` from the
Plugin Portal. Official JARs, decompiled sources, and generated game classes
remain outside Git.

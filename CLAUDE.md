# Beta Energistics — Digital Storage for Minecraft Beta 1.7.3

Inspired by Refined Storage (simplicity) and Applied Energistics (depth).

## Workflow
- Edit mod source ONLY in `src/betaenergistics/` (organized packages)
- NEVER edit `mcp/minecraft/src/net/minecraft/src/BE_*.java` directly — those are transpiled output
- `bash scripts/test.sh` auto-transpiles → builds → injects → launches
- `bash scripts/test_unit.sh` auto-transpiles → recompiles → runs JUnit tests
- `bash scripts/transpile.sh` src/ → mcp/minecraft/src/ (flatten only)

## Build Commands
- Test (in-game): `bash scripts/test.sh`
- Unit tests: `bash scripts/test_unit.sh`
- Transpile only: `bash scripts/transpile.sh`

## Project Structure
- **Mod source (edit here):** `src/betaenergistics/`
  - `storage/` — ItemKey, FluidKey, DiskStorage, FluidDiskStorage, CompositeStorage, CompositeFluidStorage, ExternalStorage, ExternalFluidStorage, DiskRegistry, FluidDiskRegistry, PatternRegistry, MobileTerminalRegistry, IStorage, IFluidStorage
  - `network/` — StorageNetwork, NetworkNode, IStorageProvider, IFluidStorageProvider, PacketHandler, cable graph traversal
  - `block/` — Block subclasses (20 blocks)
  - `tile/` — TileEntity subclasses
  - `container/` — Container subclasses
  - `gui/` — GuiContainer subclasses (drawRect-based, no texture files)
  - `item/` — Items (StorageDisk, FluidDisk, Pattern, MobileTerminal, Facade)
  - `render/` — Block/Item renderers (cables, disk LEDs, facades)
  - `assets/` — textures (blocks/, gui/, item/)
  - `mod_BetaEnergistics.java` — main mod class (extends BaseModMp)
  - `BE_Recipes.java` — all crafting recipes
- **Transpiled output (don't edit):** `mcp/minecraft/src/net/minecraft/src/`
- **Dependencies:** revisions are pinned in `dependency-lock.properties`; legacy
  transpilation receives checkout paths through `BE_DEPENDENCY_ROOTS`.

## Block IDs
| ID | Block | Class |
|----|-------|-------|
| 240 | ME Controller | BE_BlockController |
| 241 | ME Cable | BE_BlockCable |
| 242 | ME Disk Drive | BE_BlockDiskDrive |
| 243 | ME Grid Terminal | BE_BlockGrid |
| 244 | ME Crafting Terminal | BE_BlockCraftingTerminal |
| 245 | ME Import Bus | BE_BlockImporter |
| 246 | ME Export Bus | BE_BlockExporter |
| 247 | ME Autocrafter | BE_BlockAutocrafter |
| 248 | ME Storage Bus | BE_BlockStorageBus |
| 249 | ME Energy Acceptor | BE_BlockEnergyAcceptor |
| 250 | ME Recipe Encoder | BE_BlockRecipeEncoder |
| 251 | ME Crafting Coprocessor | BE_BlockCoprocessor |
| 252 | ME Request Terminal | BE_BlockRequestTerminal |
| 253 | ME Redstone Emitter | BE_BlockRedstoneEmitter |
| 254 | ME Advanced Interface | BE_BlockAdvancedInterface |
| 255 | ME Fluid Terminal | BE_BlockFluidTerminal |
| 230 | ME Fluid Import Bus | BE_BlockFluidImporter |
| 231 | ME Fluid Export Bus | BE_BlockFluidExporter |
| 232 | ME Fluid Storage Bus | BE_BlockFluidStorageBus |
| 233 | ME Fluid Redstone Emitter | BE_BlockFluidRedstoneEmitter |

## Item IDs
| ID | Item | Class |
|----|------|-------|
| 700 | Storage Disk (6 tiers: 1K/4K/16K/64K/256K/1024K) | BE_ItemStorageDisk |
| 701 | Pattern (blank + encoded) | BE_ItemPattern |
| 702 | Fluid Disk (4 tiers: 8K/32K/128K/512K mB) | BE_ItemFluidDisk |
| 703 | Mobile Terminal | BE_ItemMobileTerminal |
| 704 | Facade (damage = block ID) | BE_ItemFacade |

## Naming Conventions
- All classes use `BE_` prefix (e.g., `BE_BlockController`, `BE_TileDiskDrive`)
- Block IDs: 240-255 (main range, fully used) + 230-233 (fluid buses/emitter)
- Item IDs: 700-704

## Architecture
- **StorageNetwork**: graph of NetworkNodes connected via cables
- **BE_IStorage / BE_IFluidStorage**: common interfaces for all storage types
- **DiskStorage / FluidDiskStorage**: Map-based storage backed by DiskRegistry / FluidDiskRegistry
- **ExternalStorage / ExternalFluidStorage**: wraps IInventory / Aero_IFluidHandler for Storage Bus
- **CompositeStorage / CompositeFluidStorage**: aggregates all storages sorted by priority
- **Controller**: heart of network, energy buffer, loads/saves all registries (Disk, FluidDisk, Pattern, MobileTerminal)
- **Grid Terminal**: GUI for viewing/inserting/extracting items from network
- **Crafting Terminal**: Grid + 3x3 crafting with auto-refill from network
- **Request Terminal**: shows craftable items, previews CraftingPlan, triggers auto-crafting
- **Fluid Terminal**: view/manage fluids in network
- **CraftingCalculator**: recursive dependency resolution for auto-crafting trees
- **Autocrafter**: accepts encoded patterns, executes crafts using network resources
- **Advanced Interface**: bridges processing patterns with external machines
- **Coprocessor**: adds parallel crafting capacity
- **PatternRegistry**: stores encoded recipe data (crafting + processing types)
- **PacketHandler**: 11 packet types for multiplayer sync (BaseModMp + Packet230ModLoader)
- **Facade**: decorative cable covers (per-face, NBT-persisted, rendered as thin slabs)

## Key Patterns
- New block checklist: Block class + Tile class + 7 registrations in mod constructor (ID const, static field, instantiate, RegisterBlock, RegisterTileEntity, AddName, texture) + recipe in BE_Recipes
- Registries (Disk, FluidDisk, Pattern, MobileTerminal) all follow same pattern: file-based persistence via func_28113_a(), Controller loads/saves
- openGui() instanceof ordering: check subclasses FIRST (CraftingTerminal before TileGrid)
- Ghost slots: override mouseClicked() to intercept before super, copy ItemKey without consuming
- Multiplayer: check mod_BetaEnergistics.isMultiplayer() — if true use PacketHandler, if false call tile directly
- Item.emerald does NOT exist in Beta 1.7.3 — use gold/diamond blocks for expensive recipes

## Language
- User communicates in Portuguese (BR). Respond in Portuguese.

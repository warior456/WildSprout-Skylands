# Implementation Plan - Floating Islands with Moss Bridges

This plan implements a modular generation feature for floating islands connected by mossy suspension bridges, utilizing irregular noise-based underside shapes, standard vegetation, and varying island sizes, based on the provided glassball structure example.

## User Review Required

> [!IMPORTANT]
> **Easily Disableable postProcess**
> We will introduce configuration options (or static toggle flags) at the top of the generator class to easily enable/disable different generation steps:
> - Toggling island block placement entirely.
> - Toggling top-surface decoration/vegetation generation.
> - Toggling bridge block placement.
> 
> **Dynamic Bridge Widths**
> Bridges will have randomized horizontal widths:
> - Some bridges will be 3 blocks wide (sturdy walkways).
> - Some will be 2 blocks wide.
> - Some will be 1 block wide (very thin, unstable, and difficult to walk on).

## Open Questions

All open questions have been resolved:
- **Underside**: We will proceed with the math-based 3D vertically-stretched noise + tapered cone density cutoff.
- **Vegetation**: We will generate vegetation procedurally on top of grass blocks.
- **Bridge Styling**: Moss walkways with random hanging vines, with randomized widths.

## Proposed Changes

We will implement the changes across three main files in `net.ugi.wildsprout_skylands`.

---

### World Gen Structure

#### [MODIFY] [FloatingIslandStructure.java](file:///c:/Users/matte/IdeaProjects/WildSprout-Skylands/src/main/java/net/ugi/wildsprout_skylands/world/gen/structure/FloatingIslandStructure.java)
- Replace the current sequential chain generation with a radial/cluster-based layout.
- Generate a random count of islands:
  - **Big**: 1-3 (diameter 25-40). The first is the center island.
  - **Medium**: 2-5 (diameter 12-20). Connected to the bridge connecting two big ones or directly to a big one.
  - **Small**: 3-10 (diameter 2-6). Randomly floating nearby, unconnected.
- Track coordinates of big and medium islands to spawn curved moss bridges between them.
- Add `FloatingIslandGenerator.Piece` for each island (storing its center, type, and shape seed) and `FloatingIslandGenerator.BridgePiece` for each connection.

#### [MODIFY] [FloatingIslandGenerator.java](file:///c:/Users/matte/IdeaProjects/WildSprout-Skylands/src/main/java/net/ugi/wildsprout_skylands/world/gen/structure/FloatingIslandGenerator.java)
- Update `Piece` class:
  - Add `IslandType` enum (or int codes) for `BIG`, `MEDIUM`, and `SMALL` to control size/height limits.
  - Implement NBT saving/loading for `IslandType` and diameter.
  - Update `postProcess` (with easy toggle flags/methods to disable placement/decoration):
    - Form the top terrain using a 2D noise layer offset by a tapering factor so edges slope down.
    - Form the bottom dripping underside using a 3D noise layer (stretched vertically) on top of a base tapered cone.
    - Set block states: grass on top, dirt immediately underneath, stone/andesite in the core.
    - Add a procedural vegetation step that generates grass, ferns, flowers, and a few small trees on top of grass blocks.
- Add/Update `BridgePiece` class:
  - Implement NBT serialization for start, end coordinates, and randomized width.
  - Implement `postProcess` (with easy toggle flags/methods to disable placement):
    - Draw a curved path between start and end blocks using a quadratic equation: $y(t) = (1-t)A_y + tB_y - sag \times 4t(1-t)$ where $sag$ is proportional to distance.
    - Place `Blocks.MOSS_BLOCK` along the path with the stored randomized width (1, 2, or 3 blocks).
    - Place hanging vines (`Blocks.VINES` / `Blocks.WEEPING_VINES`) under the moss walkway.

#### [MODIFY] [ModStructurePieceTypes.java](file:///c:/Users/matte/IdeaProjects/WildSprout-Skylands/src/main/java/net/ugi/wildsprout_skylands/world/gen/ModStructurePieceTypes.java)
- Register `FLOATING_ISLAND_BRIDGE` as a separate `StructurePieceType` mapped to `FloatingIslandGenerator.BridgePiece::new` to handle NBT load/save correctly.

---

## Verification Plan

### Automated Tests
- Build and compile using `.\gradlew compileJava` to ensure no syntax or registration issues.
- Run `.\gradlew runData` to regenerate worldgen json files if necessary.

### Manual Verification
- Launch the Minecraft development client using `.\gradlew runClient`.
- Create a new creative world with the WildSprout Skylands dimension/biomes.
- Use `/locate structure wildsprout_skylands:floating_island` and teleport to the generated coordinates to inspect:
  - Clustered structure layout (Big, Medium, Small islands).
  - Irregular, dripping undersides.
  - Non-flat but not overly round tops with vegetation/foliage.
  - Curved moss bridges hanging realistically, including narrow, 1-block-wide bridges.

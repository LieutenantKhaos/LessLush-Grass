# LessLush Grass 1.3.9 Changelog

## Added
- Added two additional custom grass textures: `shorter_grass.png` and `shortest_grass.png`.
- Grass tufts now randomly choose between the three custom grass textures for more visual variation.

## Changed
- Texture selection is deterministic per grass block, so variants stay stable when chunks rebuild.
- Existing fixed tuft height, natural density, developed-area suppression, and random rotation behavior are unchanged.

# LessLush Grass 1.3.8 Changelog

## Fixed
- Fixed a Java compilation error introduced by the fixed-height tuft change.
- Preserved the fixed tuft height while keeping the horizontal offset and random rotation behavior unchanged.

# LessLush Grass 1.3.7 Changelog

## Changed
- Grass tuft height is now fixed, so every tuft renders at the same vertical height.
- Random horizontal placement and rotation remain unchanged.


## 1.3.5
- Reworked the custom short grass model so the visible 8-pixel tuft starts exactly at the top surface of the grass block.
- Keeps the supplied 16x8 artwork intact inside a standard 16x16 transparent texture canvas for reliable atlas/model handling.

## 1.3.4
- Uses the supplied 16x8 short-grass artwork inside a 16x16 atlas texture, anchored to the block surface.
- Restores vanilla cross geometry so the custom artwork renders reliably without stretching.
- Removes renderer scaling; the texture itself controls the shorter visible height.

## 1.3.3
- Fixed the custom 16x8 short-grass texture not rendering by explicitly binding the tuft model to LessLush Grass's custom texture.
- Kept the 8-pixel-tall cross geometry and top-aligned placement from 1.3.2.

## 1.3.2\n- Use an explicit 16x8 cross model so the custom shorter-grass texture is mapped to its actual 8-pixel height.\n- Keep the custom texture directly on the grass-block top surface.\n\n## 1.3.1\n- Keeps the custom short grass firmly seated on the top surface of the grass block, while retaining horizontal variation.\n\n## 1.3.0
- Added the custom `short_grass.png` texture supplied for the shorter grass look.
- Updated the tuft model to use the LessLush Grass texture instead of Minecraft's vanilla texture.
- Kept the uniform 65% X/Y/Z geometry scaling from 1.2.9 so the vanilla proportions remain consistent.

## 1.2.9
- Changed the vanilla tuft transform to uniform 65% X/Y/Z scaling.
- Preserved the vanilla short-grass texture and UV mapping.
- Uniform scaling keeps the original blade proportions while making the tuft shorter and smaller.
- No custom grass texture is used.

## 1.2.8
- Kept the 1.1.9 vanilla short-grass texture and renderer.
- Reduced horizontal tuft geometry to 65% while retaining the 43.75% height scale.
- Tuned the geometry proportions to reduce the stretched appearance without changing the texture or UVs.

## 1.2.7
- Returned the tuft renderer to the 1.1.9 vanilla-grass rendering path.
- Kept the vanilla short-grass texture and UV mapping unchanged.
- Added a conservative 90% horizontal geometry scale while retaining the 43.75% vertical scale.
- No custom compact texture or custom tuft model is used in this experiment.

# 1.1.9
- Added stable random tuft orientation (0°–359°).
- Added a Mod Menu toggle for tuft rotation.

# Changelog

## [1.0.0] - 2026-07-27

- Added Fabric 26.2 support.

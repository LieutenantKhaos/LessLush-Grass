<p align="center">
  <img src="src/main/resources/icon.png?v=3" width="256">
</p>

# LessLush Grass

A version of [Lush Grass](https://www.curseforge.com/minecraft/mc-mods/lush-grass) that tries to make the extra grass feel a little more natural.

## Overview

I liked the look of Lush Grass because it reminded me of the grass from [Better Foliage](https://www.curseforge.com/minecraft/mc-mods/better-foliage). But I wasn't a big fan of the grass being everywhere, so I had this idea for the mod.

This mod tries to recognize blocks and areas that are actually being used, such as:

Villages, paths, farmland, workstations, chests, barrels, beds, bells, campfires, lanterns, beehives, doors and fences, and some other blocks that may indicate civilization.

The mod suppresses the grass from being rendered near those blocks, helping keep developed areas looking a little cleaner and more natural.

The mod also offers several configuration options and can be edited in the config folder. However, it's recommended to use [Mod Menu](https://modrinth.com/mod/modmenu).

## Features

- Natural-looking grass tufts
- Multiple grass tuft heights for visual variation
- Randomized tuft orientation
- Suppression around developed areas
- Adjustable developed-area detection radius
- Configurable through Mod Menu or the config file

## Recommendations

Works well with [Rainbow's Foliage w/ Polytone](https://modrinth.com/resourcepack/rainbows-foliage).

## Credits

Originally based on [Lush Grass](https://www.curseforge.com/minecraft/mc-mods/lush-grass).

## Fabric 26.3 beta (1.5.0-beta.1)

Voxy 0.2.18-beta can bake the selected grass-block sides. Changing Better Grass
Blocks refreshes Voxy's model atlas. Decorative tufts and developed-area
suppression remain within normal terrain render distance.

LessLush contains grass rendering and configuration. Distant beacon rendering
is provided separately by the optional Voxy Beacon Beams addon; it is not required
by LessLush.

This update fixes grass density selection, tuft overlap, Sodium/Iris integration,
breaking-overlay checks and numeric config validation. Existing grass settings
keep their names. See CHANGELOG.md for details.

Build with Java 25: `./gradlew clean build` (Windows: `gradlew.bat clean build`).


## Minecraft 26.3 release

This branch builds 1.5.0-beta.1 for Minecraft 26.3, Java 25, Fabric Loader
0.20.1+ and Fabric API 0.165.0+26.3. Mod Menu 21.0.0 is optional.
Use main / 1.4.6 for Minecraft 26.2.

The Voxy grass integration and rendering/configuration fixes from 1.4.6 are
preserved. The Voxy 0.2.18-beta statement above describes the previous 26.2
validation; Voxy, Sodium and Iris visuals need new 26.3 in-game testing.
The beacon addon remains separate and is not required.

Validation: clean Gradle build and both existing regression tests passed against
26.3. No client/world rendering session was run. This is a beta, not a claim of
verified support for every optional renderer version.

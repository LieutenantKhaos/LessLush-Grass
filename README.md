
A version of [Lush Grass](https://www.curseforge.com/minecraft/mc-mods/lush-grass) that tries to make the extra grass feel a little more natural.

Overview:

I liked the look of Lush Grass because it reminded me of the grass from [Better Foliage](https://www.curseforge.com/minecraft/mc-mods/better-foliage). But I wasn't a big fan of the grass being everywhere, so I had this idea for the mod.

This mod tries to recognize blocks and areas that are actually being used such, as:

🏘 Villages, 🛤️ Paths 🌾 Farmland, ⚒️ Workstations, 📦 Chests, 🛢️ Barrels, 🛏️ Beds 🔔 Bells 🔥 Campfires, 🏮 Lanterns, 🐝 Beehives, 🚪 Doors and fences, and some other blocks that may indicate civilization.

The mod eliminates the grass from being rendered near those blocks. The mod also offers several configuration options and it can be edited in the config folder. However, it's recommended to use [Mod Menu](https://modrinth.com/mod/modmenu).

### Custom grass texture
The decorative tuft uses a custom `short_grass.png` texture included with the mod. The texture is kept separate from Minecraft's vanilla asset so it can be adjusted independently.

## Requirements

- Minecraft 26.2
- Fabric Loader 0.19.3 or newer
- Fabric API 0.155.2+26.2 or newer

## Configuration

With Mod Menu installed, open **Mods > Lush Grass > Configure**, then select
**Visuals** to change the two rendering options in game.

The client configuration is stored in `config/lush_grass-client.json`.

| Option | Default | Description |
| --- | --- | --- |
| `full_grass_block_coverage` | `true` | Improves vanilla grass blocks with continuous grass coverage. |
| `render_grass_tufts` | `true` | Renders short grass on unobstructed vanilla grass blocks. |

Changing either option refreshes the affected chunks.

## License

- Lush Grass is licensed under the [MIT License](LICENSE).
- Third-party notices: [NOTICE](NOTICE)

Patchy wilderness tufts: decorative grass now uses deterministic noise + position hashing, so not every grass block gets a tuft.
(Release 1.3.6 custom short grass)

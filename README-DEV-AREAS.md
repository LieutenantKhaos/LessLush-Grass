# LushGrass developed areas

This version treats active/maintained areas as developed. It intentionally does not use generic structure materials (cobblestone, stone bricks, planks, logs), so abandoned structures such as jungle temples, desert pyramids, trail ruins, shipwrecks, ruined portals, ancient cities and similar ruins remain naturally overgrown. Villages are detected through paths, farms, hay, workstations, beds, bells and other active-use blocks.

Build with `gradlew.bat build`.


More-detection preset: adds extra player-use/infrastructure blocks. Torches remain ignored and abandoned structures are not classified by structure type. Armor stands require a separate safe entity-position cache and are not queried directly here.

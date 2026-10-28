# Minecraft 26.3 beta validation

- Based on GitHub commit 718a688, confirmed against the earlier "Add Voxy support to the mod" chat.
- Normal `gradlew.bat clean build` passed with Java 25 and Gradle 9.6.0.
- Both CompatibilityTest cases pass: Voxy static bake selection and numeric parsing. Neither is a live rendering test.
- Fabric metadata targets ~26.3 / Loader >=0.20.1 / API >=0.165.0 and remains client-only with mod ID lush_grass.
- Existing mod configuration and all 1.4.6 source fixes retained.
- No client/world rendering session was run. Vanilla visuals, Voxy 26.3 distant models, Sodium and Iris, configuration reload and shader appearance need in-game checks.

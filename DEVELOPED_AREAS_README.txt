# Lush Grass - Developed Areas v2

This fork suppresses lush grass tufts around developed-looking blocks, including village paths, farms, common building materials, workstations, bells, beehives, and bee nests.

The developed-area edge now uses deterministic multi-octave 2D value noise. This creates an irregular, patchy/noise-like transition instead of a smooth circular cutoff. The noise is coordinate-based and stable across chunk rebuilds.

Build with:

    gradlew.bat build

The resulting mod jar is in build/libs.


Noise v3: the original solid radius is restored close to developed blocks. Noise only affects the outer ~3 blocks and can move the boundary by about 1.5 blocks, with mixed broad/medium/detail noise for more variation. Bee hives and bee nests remain included.

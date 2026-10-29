# Third-party components

AFMR's own code and documentation are MIT licensed; see `LICENSE`. The components below retain their separate licenses.

This project uses the official NeoForge ModDevGradle development template. Its template license is preserved in `TEMPLATE_LICENSE.txt`. Minecraft and Forge are obtained by the development toolchain and are not included in the source distribution or mod JAR.

The libraries below are bundled under `META-INF/jarjar`. MP3SPI and VorbisSPI share one game-library JAR to avoid Forge's split-package restriction; their service registrations and notices are preserved. The remaining upstream JARs retain their original contents. Full upstream copyright notices are reproduced in `THIRD_PARTY_NOTICES.txt`; GNU Library/Lesser GPL texts are included as `LGPL-2.0.txt` and `LGPL-2.1.txt`. Their original sources and license information are available from these upstream projects and Maven Central coordinates:

- MP3SPI 1.9.5.4, JLayer 1.0.1.4, VorbisSPI 1.0.3.3, JOrbis 0.0.17.4, and Tritonus Share 0.3.7.4: `com.googlecode.soundlibs`, https://github.com/pdudits/soundlibs
- jFLAC codec 1.5.2: `org.jflac:jflac-codec`, https://github.com/nguillaumin/jflac/tree/1.5.2
- jaudiotagger 3.0.1: `net.jthink:jaudiotagger`, https://bitbucket.org/ijabz/jaudiotagger

The decoder implementations remain unmodified and separately licensed; the SPI packaging change is described below. LGPL libraries may be modified/replaced, including debugging those modifications; AFMR's MIT license does not restrict those rights. Release packages include exact Maven Central source JARs for the thirteen audio/image libraries plus both MixinExtras artifacts under `third-party-sources/`, with coordinates, download URLs and SHA-256 hashes. Keep those sources and notices with redistributed packages. Refer to the reproduced notices and original sources for exact terms. No music, album artwork, or other user-supplied assets are bundled in the runtime JAR. Test audio is a generated sine wave.

WebP import uses unmodified **TwelveMonkeys ImageIO 3.15.3**, distributed as six
nested JARs: `com.twelvemonkeys.imageio:imageio-webp`, `imageio-core`,
`imageio-metadata`, and `com.twelvemonkeys.common:common-lang`, `common-io`,
`common-image`. It is pure Java and uses the BSD 3-Clause license, reproduced in
`TWELVEMONKEYS_LICENSE.txt` in the source and runtime distributions.
Upstream source: https://github.com/haraldk/TwelveMonkeys/tree/twelvemonkeys-3.15.3.
GIF decoding uses Java's built-in ImageIO reader. Image fixtures are generated
geometric colors; no player images are bundled.

## Forge packaging additions

MixinExtras Forge/common 0.5.0 is MIT licensed by LlamaLad7; see
MIXINEXTRAS_LICENSE.txt and its original source archives. The two upstream
mp3spi/vorbisspi artifacts share an identical javazoom.spi.PropertiesContainer
class. Forge's Java module loader rejects their split package, so their classes
are packaged together in afmr-codec-spis.jar, with both service-provider lists
merged and their original license/notice files retained per artifact. No decoder
code is changed or relocated. The remaining libraries retain their own JARs.

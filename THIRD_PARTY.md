# Third-party components

This project uses the official NeoForge ModDevGradle development template. Its template license is preserved in `TEMPLATE_LICENSE.txt`. Minecraft and NeoForge are obtained by the development toolchain and are not included in the source distribution or mod JAR.

The following audio libraries are distributed as separate nested JARs under `META-INF/jarjar`, preserving their original contents and embedded notices. Their original sources and license information are available from these upstream projects and Maven Central coordinates:

- MP3SPI 1.9.5.4, JLayer 1.0.1.4, VorbisSPI 1.0.3.3, JOrbis 0.0.17.4, and Tritonus Share 0.3.7.4: `com.googlecode.soundlibs`, https://github.com/pdudits/soundlibs
- jFLAC codec 1.5.2: `org.jflac:jflac-codec`, https://github.com/jflac/jflac
- jaudiotagger 3.0.1: `net.jthink:jaudiotagger`, https://bitbucket.org/ijabz/jaudiotagger

These components are unmodified. Refer to the license notices in each nested JAR and the upstream source for exact terms. No music, album artwork, or other user-supplied assets are bundled in the runtime JAR. Test audio is a generated sine wave.

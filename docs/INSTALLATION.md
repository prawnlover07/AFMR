# Installing AFMR

AFMR runs on your Minecraft client. The server and other players do not need it.
Choose the package that matches your game and loader; the two JARs are separate
builds and cannot be used interchangeably.

| Package | Minecraft Java | Loader | Java runtime |
| --- | --- | --- | --- |
| NeoForge | 26.3 | NeoForge 26.3.0.90 or newer for 26.3 | 25 |
| Fabric | 1.20.1 | Fabric Loader 0.19.5 or newer, plus Fabric API for 1.20.1 | 17 or newer |

Close Minecraft, back up an existing AFMR setup if needed, and place the package's
AFMR `.jar` in that instance's `mods` folder. Remove older AFMR JARs so exactly one
remains. The audio decoders are bundled; do not extract the JAR or install them
separately. Keep the included license and third-party notices with shared copies.
Use separate game instances for the two targets; do not downgrade a newer world
to 1.20.1.

## Hear your first song

1. Launch the instance, join a world, and press **M**. Resolve any conflicting
   key assignment under Minecraft's Controls if necessary.
2. Select **Day outside** in the priority list, then **Add music**. Choose files
   or a folder through the system file chooser. Wait for the scan progress to finish.
3. Select an imported song and click **Preview** to hear it immediately. Check
   Minecraft's Master and Music volume if it is silent.
4. For automatic playback, keep the playlist enabled and meet its conditions.
   Fresh settings deliberately use Minecraft-like **10–20 minute gaps**. **Next**
   skips the current wait. Timing controls and optional per-playlist Silence let
   you change those gaps. A playlist override can play a playlist whose automatic
   selection is off; an individually disabled song stays excluded.

Files remain at their original locations and are read-only to AFMR. Moving music
after importing it requires **Library → Repair / missing sources**. For large
libraries, allow the scan to finish before assuming songs are unavailable.

## Backups, sharing and troubleshooting

Use **Import / export → Full configuration…** for a personal `.afmrb` backup.
Reviewing and applying a restore replaces AFMR's configuration on the next launch.
Backups contain private local paths and world/server links; music files stay separate.
Playlist `.afmrp` sharing can include settings and optional relative song references;
each recipient supplies their own music.

If playback seems wrong, open **Inspector** to see the winning playlist and reason.
Use **Copy diagnostic summary** and send that text with the steps to reproduce the
problem, game/loader version, and any relevant modpack details. The summary omits
song names, paths and saved location details. Do not send a personal full backup
or music library as a default troubleshooting step.

NeoForge supports Minecraft's newer music toast, Music Frequency setting and menu
blur. Fabric 1.20.1 uses AFMR's own now-playing card and has no menu blur or native
music-toast/frequency integration. Structure conditions and mob-target combat
retention require singleplayer; ordinary combat detection works in multiplayer.

See the [user guide](USER-GUIDE.md) for all controls and conditions. AFMR is an
independent project, not an official Minecraft product or approved by Mojang or Microsoft.

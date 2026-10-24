"""Generate disposable native test data; never open or modify personal audio files."""
from pathlib import Path
import argparse
import json
import math
import re
import shutil
import struct
import wave
import zlib
import copy

HERE = Path(__file__).resolve().parent
PROJECT = HERE.parents[1]


def tone(path: Path, seconds: float) -> None:
    if path.exists():
        return
    with wave.open(str(path), "wb") as output:
        output.setparams((2, 2, 22050, 0, "NONE", "not compressed"))
        data = bytearray()
        for index in range(int(seconds * 22050)):
            value = int(350 * math.sin(2 * math.pi * 220 * index / 22050))
            data.extend(struct.pack("<hh", value, value))
        output.writeframes(data)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--scan-files", type=int, default=0, help="Optional tiny WAV scan fixture count")
    parser.add_argument("--world", type=Path, help="Copy a disposable test world into the isolated profile")
    parser.add_argument("--multiplayer", action="store_true", help="Stage the separate vanilla-server test client")
    parser.add_argument("--workspace", action="store_true", help="Stage the separate workspace UI test client")
    parser.add_argument("--integration", action="store_true", help="Stage the separate release integration client")
    parser.add_argument("--performance", action="store_true", help="Stage the separate large-library performance client")
    parser.add_argument("--fresh", action="store_true", help="Stage a new client without an AFMR library/settings file")
    parser.add_argument("--library", type=Path, help="Read-only library metadata to stage for --performance")
    args = parser.parse_args()
    if not 0 <= args.scan_files <= 70000:
        parser.error("--scan-files must be between 0 and 70000")
    if args.multiplayer and args.world:
        parser.error("--world is only used by the singleplayer suite")
    if sum([args.multiplayer, args.workspace, args.integration, args.performance, args.fresh]) > 1:
        parser.error("Choose only one client suite")
    if args.library and not args.performance:
        parser.error("--library is only used with --performance")
    properties = (PROJECT / "gradle.properties").read_text(encoding="utf-8")
    version = re.search(r"^mod_version=(.+)$", properties, re.MULTILINE).group(1).strip()
    jar = PROJECT / "build/libs" / f"animals-final-music-replacer-{version}.jar"
    if not jar.is_file():
        parser.error("Build AFMR from the repository root before preparing native fixtures.")
    profile = HERE / ("run/fresh" if args.fresh else "run/performance" if args.performance else "run/integration" if args.integration else "run/workspace" if args.workspace else "run/multiplayer" if args.multiplayer else "run")
    world_dest = profile / "saves/New World"
    if args.world and (not args.world.is_dir() or world_dest.exists()):
        parser.error("The source world must exist and the destination New World must not already exist.")
    for path in [HERE / "fixtures", HERE / "results", profile / "mods", profile / "config/afmr"]:
        path.mkdir(parents=True, exist_ok=True)
    mods = profile / "mods"
    for previous in mods.glob("animals-final-music-replacer-*.jar"):
        if previous.name != jar.name:
            parser.error("Another AFMR version exists in the isolated mods folder; resolve it before continuing.")
    shutil.copy2(jar, mods / jar.name)
    tone(HERE / "fixtures/long.wav", 90)
    tone(HERE / "fixtures/short.wav", 1.5)
    cover_folder=HERE / "fixtures/with-cover"
    cover_folder.mkdir(exist_ok=True)
    shutil.copy2(HERE / "fixtures/long.wav",cover_folder / "long.wav")
    def chunk(kind, data):
        return struct.pack(">I",len(data))+kind+data+struct.pack(">I",zlib.crc32(kind+data))
    png=b"\x89PNG\r\n\x1a\n"+chunk(b"IHDR",struct.pack(">IIBBBBB",2,2,8,2,0,0,0))
    png+=chunk(b"IDAT",zlib.compress((b"\x00"+b"\xf0\x60\x30"*2)*2))+chunk(b"IEND",b"")
    (cover_folder / "cover.png").write_bytes(png)
    playlist = HERE / "fixtures/native-import.fplite"
    if not playlist.exists():
        long_file, short_file = HERE / "fixtures/long.wav", HERE / "fixtures/short.wav"
        playlist.write_text(f"file://{long_file}\nfile://{long_file}\n{short_file.as_uri()}\nmissing.wav\n", encoding="utf-8")
    config = profile / "config/afmr/library.json"
    if args.fresh and config.exists():
        parser.error("The fresh profile already has AFMR settings; choose a genuinely new test profile.")
    if not config.exists() and not args.fresh:
        underground={"elevationEnabled":True,"minY":-2147483648,"maxY":64,"cover":"YES","minCoverDepth":5}
        outside=copy.deepcopy(underground);outside["negated"]=True
        dark={"matchAny":True,"groups":[{"cover":"YES","minCoverDepth":0,"lightEnabled":True,"minLight":0,"maxLight":4,
              "groups":[{"dimensionEnabled":True,"dimension":d,"negated":True} for d in ["minecraft:the_nether","minecraft:the_end"]]}]+[
              {"label":n,"dimensionEnabled":True,"dimension":d,"enabled":False} for n,d in [("Nether","minecraft:the_nether"),("End","minecraft:the_end")]]}
        defaults=[("combat","Combat",{"combat":"YES"}),("home","Home",{"areaEnabled":True}),
                  ("dark_exploration","Dark exploration",dark),("underground","Underground",underground),
                  ("night_outside","Night outside",{"timeEnabled":True,"startTime":12000,"endTime":23999,"groups":[copy.deepcopy(outside)]}),
                  ("day_outside","Day outside",{"timeEnabled":True,"startTime":0,"endTime":11999,"groups":[copy.deepcopy(outside)]})]
        library = {"schemaVersion": 2, "settings": {"silenceMinSeconds": 1, "silenceMaxSeconds": 1,
                   "fadeInSeconds": 0.3, "fadeOutSeconds": 0.3, "bufferSeconds": 0.3, "rescanOnLaunch": False}, "tracks": [],
                   "playlists": [{"id": i, "name": n, "conditions": conditions, "includeVanilla": False,
                                  "tracks": {}} for i, n, conditions in defaults]}
        config.write_text(json.dumps(library, indent=2), encoding="utf-8")
    options = profile / "options.txt"
    if not options.exists():
        options.write_text("version:5023\npauseOnLostFocus:false\nrenderDistance:6\nsimulationDistance:6\n"
                           "onboardAccessibility:false\nskipMultiplayerWarning:true\n"
                           +("" if args.fresh else "key_key.afmr.next:key.keyboard.n\nkey_key.afmr.pause:key.keyboard.p\n"
                           "key_key.afmr.inspect:key.keyboard.i\nkey_key.afmr.override_1:key.keyboard.k\n"), encoding="utf-8")
    for index in range(args.scan_files):
        path = HERE / "scan-fixtures" / f"Album {index // 50:03d}" / f"Track {index:05d}.wav"
        if not path.exists():
            path.parent.mkdir(parents=True, exist_ok=True)
            with wave.open(str(path), "wb") as output:
                output.setparams((2, 2, 22050, 0, "NONE", "not compressed"))
                output.writeframes(struct.pack("<hh", 200, -200) * 100)
    if args.world:
        shutil.copytree(args.world, world_dest)
    if args.library:
        source=json.loads(args.library.read_text(encoding="utf-8"))
        # Deliberately omit personal playlists, locations, server links, preferences and history.
        metadata={"tracks":[t for t in source["tracks"] if not t.get("vanilla")],
                  "folders":[{"path":f["path"],"playlistId":"library","recursive":f.get("recursive",True)} for f in source.get("folders",[])]}
        (profile / "performance-library.json").write_text(json.dumps(metadata),encoding="utf-8")
        print(f"Staged read-only metadata for {len(metadata['tracks'])} local tracks")
    print(f"Prepared isolated native profile at {profile}")
    if not args.multiplayer and not world_dest.exists():
        print("Create a disposable singleplayer world named New World before running the suite.")


if __name__ == "__main__":
    main()

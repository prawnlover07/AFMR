"""Generate disposable native test data; never open or modify personal audio files."""
from pathlib import Path
import argparse
import json
import math
import re
import shutil
import struct
import wave

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
    args = parser.parse_args()
    if not 0 <= args.scan_files <= 70000:
        parser.error("--scan-files must be between 0 and 70000")
    properties = (PROJECT / "gradle.properties").read_text(encoding="utf-8")
    version = re.search(r"^mod_version=(.+)$", properties, re.MULTILINE).group(1).strip()
    jar = PROJECT / "build/libs" / f"animals-final-music-replacer-{version}.jar"
    if not jar.is_file():
        parser.error("Build AFMR from the repository root before preparing native fixtures.")
    world_dest = HERE / "run/saves/New World"
    if args.world and (not args.world.is_dir() or world_dest.exists()):
        parser.error("The source world must exist and the destination New World must not already exist.")
    for name in ["fixtures", "results", "run/mods", "run/config/afmr"]:
        (HERE / name).mkdir(parents=True, exist_ok=True)
    mods = HERE / "run/mods"
    for previous in mods.glob("animals-final-music-replacer-*.jar"):
        if previous.name != jar.name:
            parser.error("Another AFMR version exists in the isolated mods folder; resolve it before continuing.")
    shutil.copy2(jar, mods / jar.name)
    tone(HERE / "fixtures/long.wav", 90)
    tone(HERE / "fixtures/short.wav", 1.5)
    playlist = HERE / "fixtures/native-import.fplite"
    if not playlist.exists():
        long_file, short_file = HERE / "fixtures/long.wav", HERE / "fixtures/short.wav"
        playlist.write_text(f"file://{long_file}\nfile://{long_file}\n{short_file.as_uri()}\nmissing.wav\n", encoding="utf-8")
    config = HERE / "run/config/afmr/library.json"
    if not config.exists():
        defaults = [("combat", "Combat", "COMBAT"), ("home", "Home", "HOME"),
                    ("dark_exploration", "Dark exploration", "DARK_EXPLORATION"),
                    ("underground", "Underground", "UNDERGROUND"),
                    ("night_outside", "Night outside", "NIGHT_OUTSIDE"),
                    ("day_outside", "Day outside", "DAY_OUTSIDE")]
        library = {"schemaVersion": 2, "settings": {"silenceMinSeconds": 1, "silenceMaxSeconds": 1,
                   "fadeSeconds": 0.3, "bufferSeconds": 0.3, "rescanOnLaunch": False}, "tracks": [],
                   "playlists": [{"id": i, "name": n, "rule": rule, "includeVanilla": False,
                                  "tracks": {}} for i, n, rule in defaults]}
        config.write_text(json.dumps(library, indent=2), encoding="utf-8")
    options = HERE / "run/options.txt"
    if not options.exists():
        options.write_text("pauseOnLostFocus:false\nrenderDistance:6\nsimulationDistance:6\n"
                           "key_key.afmr.next:key.keyboard.n\nkey_key.afmr.pause:key.keyboard.p\n"
                           "key_key.afmr.inspect:key.keyboard.i\nkey_key.afmr.override_1:key.keyboard.k\n", encoding="utf-8")
    for index in range(args.scan_files):
        path = HERE / "scan-fixtures" / f"Album {index // 50:03d}" / f"Track {index:05d}.wav"
        if not path.exists():
            path.parent.mkdir(parents=True, exist_ok=True)
            with wave.open(str(path), "wb") as output:
                output.setparams((2, 2, 22050, 0, "NONE", "not compressed"))
                output.writeframes(struct.pack("<hh", 200, -200) * 100)
    if args.world:
        shutil.copytree(args.world, world_dest)
    print(f"Prepared isolated native profile at {HERE / 'run'}")
    if not world_dest.exists():
        print("Create a disposable singleplayer world named New World before running the suite.")


if __name__ == "__main__":
    main()

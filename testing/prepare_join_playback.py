"""Stage generated-only join policy fixtures in AFMR's disposable game profiles."""
import argparse
import json
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--fabric", action="store_true")
    args = parser.parse_args()
    project = Path(__file__).resolve().parent.parent
    profile = project / ("ports/fabric-1.20.1/run-smoke" if args.fabric else "testing/native/run/workspace")
    world = profile / "saves" / ("AFMR Smoke" if args.fabric else "New World")
    tone = project / ("ports/fabric-1.20.1/run-smoke/smoke-a.wav" if args.fabric else "testing/native/fixtures/long.wav")
    if not tone.is_file() or not world.is_dir():
        parser.error("Prepare the ordinary generated fixtures and disposable world first.")
    config = profile / "config/afmr"
    config.mkdir(parents=True, exist_ok=True)
    if (config / "pending-restore.afmrb").exists():
        parser.error("Finish the isolated profile's pending backup restore first.")
    settings = {"silenceMinSeconds": 600, "silenceMaxSeconds": 600, "bufferSeconds": 60,
                "fadeInSeconds": 2, "fadeOutSeconds": 2, "playImmediatelyOnJoin": False,
                "rescanOnLaunch": False, "replayGainEnabled": False}
    tracks = [{"id": f"file:join-{i}", "path": str(tone.resolve()), "title": f"Generated join {i}",
               "artist": "AFMR fixture", "vanilla": False, "durationSeconds": 90} for i in ["a", "b"]]

    def playlist(pid, name, song, dimension=None):
        return {"id": pid, "name": name, "includeVanilla": False, "volume": 0.1,
                "conditions": {} if dimension is None else {"dimensionEnabled": True, "dimension": dimension},
                "tracks": {"file:join-" + song: True}}

    playback = dict(settings)
    playback["playImmediatelyOnJoin"] = True
    saved = {"id": "join-test", "name": "Join test", "playback": playback, "folderDestinations": {},
             "playlists": [playlist("join-world", "Join world", "a", "minecraft:overworld"),
                           playlist("join-fallback", "Join fallback", "b")]}
    library = {"schemaVersion": 2, "settings": settings, "tracks": tracks, "folders": [], "legacyPlaylists": [],
               "playlists": [playlist("join-default", "Default should wait", "b")],
               "profiles": {"activeId": "default", "activeName": "Default", "inactive": [saved],
                            "bindings": {"singleplayer:" + str(world.resolve()): "join-test"}}}
    (config / "library.json").write_text(json.dumps(library, indent=2), encoding="utf-8")
    (config / "history.json").write_text("[]", encoding="utf-8")
    print(f"Staged join-only fixtures in {profile}")


if __name__ == "__main__":
    main()

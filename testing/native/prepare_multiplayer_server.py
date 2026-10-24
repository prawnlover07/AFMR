"""Stage a disposable vanilla 26.3 server bound only to loopback. Never launches it."""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import uuid

PROJECT = Path(__file__).resolve().parents[2]


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-jar", type=Path, required=True, help="Official Minecraft 26.3 server JAR")
    parser.add_argument("--accept-eula", action="store_true", help="Accept https://aka.ms/MinecraftEULA for this server")
    args = parser.parse_args()
    if not args.server_jar.is_file():
        parser.error("--server-jar must name an existing vanilla 26.3 server JAR")
    destination = PROJECT / "work/multiplayer/server"
    eula = destination / "eula.txt"
    if not args.accept_eula and (not eula.exists() or "eula=true" not in eula.read_text(encoding="utf-8")):
        parser.error("Read https://aka.ms/MinecraftEULA and pass --accept-eula only if you agree")
    settings = {
        "server-ip": "127.0.0.1", "server-port": "25575", "online-mode": "false",
        "enforce-secure-profile": "false", "level-name": "afmr-test", "level-type": "minecraft:flat",
        "generator-settings": json.dumps({"biome": "minecraft:plains", "layers": [
            {"block": "minecraft:bedrock", "height": 1}, {"block": "minecraft:stone", "height": 3}]}),
        "gamemode": "creative", "difficulty": "peaceful", "spawn-protection": "0",
        "view-distance": "4", "simulation-distance": "4", "max-players": "1",
        "enable-query": "false", "enable-rcon": "false", "motd": "Disposable AFMR vanilla multiplayer test",
    }
    properties = destination / "server.properties"
    if properties.exists():
        existing = dict(line.split("=", 1) for line in properties.read_text(encoding="utf-8").splitlines()
                        if "=" in line and not line.startswith("#"))
        if any(existing.get(key) != settings[key] for key in
               ("server-ip", "server-port", "online-mode", "enable-query", "enable-rcon", "level-name")):
            parser.error("Existing test server properties differ; inspect them before preparing this profile")
    destination.mkdir(parents=True, exist_ok=True)
    jar = destination / "server.jar"
    if args.server_jar.resolve() != jar.resolve():
        shutil.copy2(args.server_jar, jar)
    if not properties.exists():
        properties.write_text("".join(f"{key}={value}\n" for key, value in settings.items()), encoding="utf-8")
    if args.accept_eula:
        eula.write_text("eula=true\n", encoding="utf-8")
    # Java's offline player UUID uses raw MD5 name bytes, without a namespace prefix.
    player_id = uuid.UUID(bytes=hashlib.md5(b"OfflinePlayer:AFMRProbe").digest(), version=3)
    ops = destination / "ops.json"
    if not ops.exists():
        ops.write_text(json.dumps([{"uuid": str(player_id), "name": "AFMRProbe", "level": 4,
                                   "bypassesPlayerLimit": False}], indent=2), encoding="utf-8")
    print(f"Prepared vanilla server at {destination}")
    print("Run Java 25 here: java -Xmx1G -jar server.jar --nogui")
    print("Offline authentication is for this loopback-only test. Never expose this server publicly.")


if __name__ == "__main__":
    main()

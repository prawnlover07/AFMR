"""Verify numbered artifacts and assemble explicit redistributable packages."""
from pathlib import Path
import hashlib
import json
import shutil
import struct
import zipfile

ROOT = Path(__file__).resolve().parents[1]
NOTICES = ["LICENSE", "TEMPLATE_LICENSE.txt", "TWELVEMONKEYS_LICENSE.txt",
           "THIRD_PARTY.md", "THIRD_PARTY_NOTICES.txt", "LGPL-2.0.txt", "LGPL-2.1.txt"]


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main() -> None:
    version = next(line.split("=", 1)[1].strip() for line in
                   (ROOT / "gradle.properties").read_text().splitlines()
                   if line.startswith("mod_version="))
    output = ROOT / "outputs" / version
    sources = output / "third-party-sources"
    if len(list(sources.glob("*-sources.jar"))) != 13:
        raise ValueError("Prepare the exact thirteen dependency source archives first")
    packages = [
        (ROOT, "NeoForge-26.3", f"animals-final-music-replacer-{version}",
         "Minecraft Java 26.3, NeoForge 26.3.0.90+ for 26.3, Java 25."),
        (ROOT / "ports/fabric-1.20.1", "Fabric-1.20.1",
         f"animals-final-music-replacer-fabric-1.20.1-{version}",
         "Minecraft Java 1.20.1, Fabric Loader 0.19.5+, Fabric API for 1.20.1, Java 17+."),
    ]
    hashes = []
    for source_root, target, base, requirements in packages:
        jar = source_root / "build/libs" / f"{base}.jar"
        source_zip = source_root / "build/distributions" / f"{base}-source.zip"
        with zipfile.ZipFile(jar) as archive:
            names = archive.namelist()
            for notice in NOTICES:
                if archive.read(notice) != (source_root / notice).read_bytes():
                    raise ValueError(f"Packaged notice differs: {notice}")
            language = json.loads(archive.read("assets/afmr/lang/en_us.json"))
            if not language["afmr.copy.settings.play_immediately_on_join"]:
                raise ValueError("Reviewed author copy missing")
            if source_root == ROOT:
                metadata = archive.read("META-INF/neoforge.mods.toml").decode()
                assert f'version="{version}"' in metadata and 'license="MIT"' in metadata
                nested = [n for n in names if n.startswith("META-INF/jarjar/") and n.endswith(".jar")]
            else:
                metadata = json.loads(archive.read("fabric.mod.json"))
                assert metadata["version"] == version and metadata["license"] == "MIT"
                nested = [n for n in names if n.startswith("META-INF/jars/") and n.endswith(".jar")]
                for name in names:
                    if name.endswith(".class"):
                        assert struct.unpack(">H", archive.read(name)[6:8])[0] <= 61, name
            assert len(nested) == 13, nested
            assert not any("afmr/probe/" in n or "afmr/smoke/" in n for n in names)
        with zipfile.ZipFile(source_zip) as archive:
            for name in archive.namelist():
                parts = Path(name).parts
                assert not any(p in {".git", ".codex", ".gradle", "build", "work", "outputs", "__pycache__"}
                               or p.startswith("run-") or p == "run" for p in parts), name
                assert not name.endswith((".afmrb", ".fplite", ".log", ".pyc")), name
            for notice in NOTICES:
                assert archive.read(notice) == (source_root / notice).read_bytes(), notice
        folder = output / f"AFMR-{version}-{target}"
        folder.mkdir(parents=True, exist_ok=True)
        shutil.copy2(jar, folder / jar.name)
        for notice in NOTICES:
            shutil.copy2(source_root / notice, folder / notice)
        for relative in ["docs/INSTALLATION.md", "docs/USER-GUIDE.md", "CHANGELOG.md", "BACKLOG.md", "docs/PORTING.md"]:
            destination = folder / relative
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(ROOT / relative, destination)
        shutil.copytree(sources, folder / "third-party-sources", dirs_exist_ok=True)
        readme = (f"# AFMR {version} — {target}\n\n{requirements}\n\n"
                  "Close Minecraft and install only this AFMR JAR in the instance's mods folder.\n"
                  "Replace older AFMR JARs; do not mix the NeoForge and Fabric builds.\n"
                  "Join a world, press M, select Day outside, then Add music to import your own files.\n"
                  "Use the song's right-click/ellipsis menu for Preview. Normal music uses 10–20 minute gaps.\n\n"
                  "Optional server companions are separate packages for server owners. They add multiplayer\n"
                  "structures and mob-target combat retention; clients work without them. Never install\n"
                  "both the client and server companion JAR in one instance.\n\n"
                  "See [installation](docs/INSTALLATION.md) and the [user guide](docs/USER-GUIDE.md).\n"
                  "If something fails, use Playback status → Copy diagnostic summary and describe the steps.\n\n"
                  "AFMR is MIT licensed. Bundled libraries retain separate licenses; keep the included\n"
                  "notices and exact third-party sources with redistributed packages. Do not place\n"
                  "source JARs in Minecraft's mods folder. No music or personal configuration is included.\n\n"
                  "Independent project; not an official Minecraft product or approved by Mojang or Microsoft.\n")
        (folder / "README.md").write_text(readme, encoding="utf-8")
        allowed = {Path(name) for name in NOTICES + [jar.name, "README.md",
                   "docs/INSTALLATION.md", "docs/USER-GUIDE.md", "CHANGELOG.md", "BACKLOG.md", "docs/PORTING.md"]}
        allowed.update(Path("third-party-sources") / path.name for path in sources.iterdir()
                       if path.is_file() and (path.name.endswith("-sources.jar") or path.name in {"README.md", "SHA256SUMS.txt"}))
        actual = {path.relative_to(folder) for path in folder.rglob("*") if path.is_file()}
        if actual != allowed:
            raise ValueError(f"Unexpected release files: {actual ^ allowed}")
        packaged = Path(shutil.make_archive(str(folder), "zip", folder.parent, folder.name))
        shutil.copy2(jar, output / jar.name)
        shutil.copy2(source_zip, output / source_zip.name)
        for path in [jar, source_zip, packaged]:
            hashes.append(f"{digest(path)}  {path.name}")
        print(f"Verified {target}: {jar.name}; 13 nested libraries; notices/copy/source exclusions")

    for source_root, target, base, requirements in packages:
        neo = source_root == ROOT
        server_base = (f"animals-final-music-replacer-server-neoforge-26.3-{version}" if neo else
                       f"animals-final-music-replacer-server-fabric-1.20.1-{version}")
        jar = source_root / "build/libs" / f"{server_base}.jar"
        with zipfile.ZipFile(jar) as archive:
            names = archive.namelist()
            assert archive.read("LICENSE") == (ROOT / "LICENSE").read_bytes()
            assert not any(n.endswith(".jar") or n.startswith("assets/") for n in names)
            classes = [n for n in names if n.endswith(".class")]
            assert classes and all(n.startswith(("dev/animal/afmr/server/",
                "dev/animal/afmr/network/", "dev/animal/afmr/core/StructureContext")) for n in classes)
            assert all(struct.unpack(">H", archive.read(n)[6:8])[0] <= (69 if neo else 61) for n in classes)
            if neo:
                metadata = archive.read("META-INF/neoforge.mods.toml").decode()
                assert f'version="{version}"' in metadata and 'license="MIT"' in metadata
            else:
                metadata = json.loads(archive.read("fabric.mod.json"))
                assert metadata["version"] == version and metadata["license"] == "MIT"
                assert metadata["entrypoints"] == {"main": ["dev.animal.afmr.server.ServerAfmr"]}
                assert "mixins" not in metadata and "jars" not in metadata
        folder = output / f"AFMR-{version}-Server-{target}"
        folder.mkdir(parents=True, exist_ok=True)
        shutil.copy2(jar, folder / jar.name)
        shutil.copy2(ROOT / "LICENSE", folder / "LICENSE")
        (folder / "README.md").write_text(
            f"# AFMR {version} — optional {target} server companion\n\n{requirements}\n\n"
            "For server owners: stop the server, put this single JAR in its mods folder and restart.\n"
            "Players install the full client package on their own computers. Do not put both AFMR\n"
            "JARs in one instance: they use the same mod ID. Replace an older companion when upgrading.\n\n"
            "Provides loaded structure boundaries/catalogs and retains event-started combat while\n"
            "hostile or angered mobs target the player. Does not start combat from targeting alone,\n"
            "retain PvP combat, generate terrain, or share music/library/playlist data. Clients without\n"
            "the companion keep ordinary multiplayer playback and combat event timers.\n\n"
            "AFMR's code is MIT licensed; retain LICENSE. This companion bundles no audio libraries.\n"
            "Independent project; not an official Minecraft product or approved by Mojang or Microsoft.\n",
            encoding="utf-8")
        actual = {p.relative_to(folder) for p in folder.rglob("*") if p.is_file()}
        assert actual == {Path(jar.name), Path("LICENSE"), Path("README.md")}, actual
        packaged = Path(shutil.make_archive(str(folder), "zip", folder.parent, folder.name))
        shutil.copy2(jar, output / jar.name)
        hashes.extend(f"{digest(p)}  {p.name}" for p in (jar, packaged))
        print(f"Verified server {target}: {jar.name}; scoped classes; no client/audio assets")

    (output / "SHA256SUMS.txt").write_text("\n".join(hashes) + "\n", encoding="utf-8")
    print(f"Prepared packages under outputs/{version}")


if __name__ == "__main__":
    main()

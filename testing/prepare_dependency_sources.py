"""Fetch exact upstream sources for the nested libraries in a built NeoForge JAR.

Release-only tool: no game data, music, runtime dependency resolution or threads.
Downloaded source archives remain under ignored outputs/ alongside the release.
"""
from pathlib import Path
from urllib.request import urlopen
import hashlib
import io
import json
import re
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def download(url: str) -> bytes:
    with urlopen(url, timeout=30) as response:
        return response.read(32 * 1024 * 1024)


def main() -> None:
    version = next(line.split("=", 1)[1].strip() for line in
                   (ROOT / "gradle.properties").read_text().splitlines()
                   if line.startswith("mod_version="))
    destination = ROOT / "outputs" / version / "third-party-sources"
    destination.mkdir(parents=True, exist_ok=True)
    jar = ROOT / "build/libs" / f"animals-final-music-replacer-{version}.jar"
    with zipfile.ZipFile(jar) as archive:
        libraries = json.loads(archive.read("META-INF/jarjar/metadata.json"))["jars"]
    rows = ["# Exact bundled-library sources", "",
            "Original, unmodified Maven Central source artifacts. Preserve their notices.",
            "Sources match the coordinates embedded in AFMR's built NeoForge JAR.", ""]
    hashes = []
    notices = ["Upstream library notices", "",
               "Copyright/license comment blocks from the exact source artifacts listed below.",
               "Original sources accompany AFMR release packages; these libraries are unmodified.", ""]
    for library in sorted(libraries, key=lambda item: item["path"]):
        group = library["identifier"]["group"]
        artifact = library["identifier"]["artifact"]
        artifact_version = library["version"]["artifactVersion"]
        name = f"{artifact}-{artifact_version}-sources.jar"
        url = (f"https://repo.maven.apache.org/maven2/{group.replace('.', '/')}/"
               f"{artifact}/{artifact_version}/{name}")
        target = destination / name
        data = target.read_bytes() if target.exists() else download(url)
        with zipfile.ZipFile(io.BytesIO(data)) as source:
            if source.testzip() is not None or not any(n.endswith(".java") for n in source.namelist()):
                raise ValueError(f"Invalid source archive: {name}")
            blocks = set()
            notices.extend([f"=== {group}:{artifact}:{artifact_version} ===", url, ""])
            for path in sorted(source.namelist()):
                if not path.endswith(".java"):
                    continue
                raw = source.read(path)
                try:
                    text = raw.decode("utf-8")
                except UnicodeDecodeError:
                    text = raw.decode("latin-1")
                text = text.replace("\r\r\n", "\n").replace("\r\n", "\n")
                for block in re.findall(r"/\*.*?\*/", text, re.DOTALL):
                    if not re.search(r"copyright|redistribution|general public license|all rights reserved", block, re.IGNORECASE) or block in blocks:
                        continue
                    blocks.add(block)
                    notices.extend([path, block, ""])
        target.write_bytes(data)
        hashes.append(f"{hashlib.sha256(data).hexdigest()}  {name}")
        rows.append(f"- `{group}:{artifact}:{artifact_version}`: [{name}]({url})")
        print(f"Verified source: {name}")
    (destination / "README.md").write_text("\n".join(rows) + "\n", encoding="utf-8")
    (destination / "SHA256SUMS.txt").write_text("\n".join(hashes) + "\n", encoding="utf-8")
    (ROOT / "THIRD_PARTY_NOTICES.txt").write_text("\n".join(notices) + "\n", encoding="utf-8")
    for license_version in ["2.0", "2.1"]:
        target = ROOT / f"LGPL-{license_version}.txt"
        if not target.exists():
            data = download(f"https://www.gnu.org/licenses/old-licenses/lgpl-{license_version}.txt")
            if b"GNU" not in data or b"TERMS AND CONDITIONS" not in data:
                raise ValueError("Unexpected license response")
            target.write_bytes(data)
    print(f"Prepared {len(libraries)} exact source archives in outputs/{version}/third-party-sources")


if __name__ == "__main__":
    main()

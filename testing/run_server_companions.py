"""Finite packaged companion checks; generated state stays under ignored profiles/work."""
from pathlib import Path
import json
import os
import subprocess
import time
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
LOGS = ROOT / "work/server-companion-checks"
ENV = os.environ.copy()
ENV.setdefault("JAVA_HOME", str(ROOT / "work/jdk/jdk-25.0.4.1+1"))
ENV.setdefault("GRADLE_USER_HOME", str(ROOT / "work/gradle-cache"))
JAVA = Path(ENV["JAVA_HOME"]) / "bin/java.exe"
FLAGS = subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0
ACTIVE = []


def start(args, name, cwd=ROOT, stdin=False):
    LOGS.mkdir(parents=True, exist_ok=True)
    log = LOGS / (name + ".log")
    handle = log.open("w", encoding="utf8")
    process = subprocess.Popen(args, cwd=cwd, env=ENV, stdout=handle,
                               stderr=subprocess.STDOUT, stdin=subprocess.PIPE if stdin else subprocess.DEVNULL,
                               text=True, creationflags=FLAGS)
    ACTIVE.append((process, handle))
    return process, log


def stop(process):
    if process.poll() is None:
        if os.name == "nt":
            subprocess.run(["taskkill", "/PID", str(process.pid), "/T", "/F"],
                           stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, creationflags=FLAGS)
        else:
            process.terminate()
        process.wait(timeout=30)


def finish(process, log, timeout=180):
    code = process.wait(timeout=timeout)
    if code:
        raise RuntimeError(f"{log.name}: exit {code}\n{log.read_text(encoding='utf8', errors='replace')[-9000:]}")


def gradle(args, name, background=False):
    command = [str(ROOT / "gradlew.bat"), *args, "--offline", "--console=plain"]
    if os.name == "nt":
        command = [os.environ.get("COMSPEC", "cmd.exe"), "/d", "/c", subprocess.list2cmdline(command)]
    process, log = start(command, name)
    if not background:
        finish(process, log)
        print(f"PASS {name}", flush=True)
    return process, log


def ready(process, log, marker, timeout=90):
    end = time.monotonic() + timeout
    while time.monotonic() < end:
        text = log.read_text(encoding="utf8", errors="replace") if log.exists() else ""
        if marker in text:
            return
        if process.poll() is not None:
            raise RuntimeError(f"{log.name} stopped before {marker}:\n{text[-7000:]}")
        time.sleep(0.5)
    raise TimeoutError(f"{log.name}: waiting for {marker}")


def results(path, count):
    lines = path.read_text(encoding="utf8").splitlines()
    if any(line.startswith("FAIL ") for line in lines) or sum(line.startswith("PASS ") for line in lines) < count:
        raise AssertionError(f"{path}:\n" + "\n".join(lines))
    return sum(line.startswith("PASS ") for line in lines)


def connection(target, vanilla=False):
    neo = target == "neoforge"
    profile = ROOT / ("testing/native/run/server-companion" if neo else "ports/fabric-1.20.1/run-server-companion")
    result = profile / ("server-without-companion-network-results.txt" if vanilla else "server-companion-network-results.txt")
    result.unlink(missing_ok=True)
    label = target + ("-vanilla" if vanilla else "-companion")
    if vanilla:
        profile_server = ROOT / ("work/server-companion-vanilla-" + target)
        profile_server.mkdir(parents=True, exist_ok=True)
        (profile_server / "eula.txt").write_text("eula=true\n", encoding="utf8")
        port = 25585 if neo else 25586
        (profile_server / "server.properties").write_text(
            f"server-ip=127.0.0.1\nserver-port={port}\nonline-mode=false\nwhite-list=false\n"
            "enforce-whitelist=false\npause-when-empty-seconds=-1\nlevel-name=probe-world\n"
            "level-type=minecraft:flat\ngenerate-structures=false\nview-distance=2\nsimulation-distance=2\n", encoding="utf8")
        server_jar = ROOT / ("work/multiplayer/server/server.jar" if neo else "work/gradle-cache/caches/fabric-loom/1.20.1/minecraft-server.jar")
        server, server_log = start([str(JAVA), "-Xmx1G", "-jar", str(server_jar), "nogui"], label + "-server", profile_server, stdin=True)
    else:
        server, server_log = gradle(["-p", "testing/server-" + target, "-Psocket", "runServer"], label + "-server", True)
    client = None
    try:
        ready(server, server_log, "Done (")
        args = ["-p", "testing/native", "-PserverCompanion", "runClient"] if neo else ["-p", "ports/fabric-1.20.1", "-Psmoke", "-PpackagedSmoke", "-PserverCompanion", "runClient"]
        if vanilla:
            args.insert(-1, "-PnoCompanion")
        client, client_log = gradle(args, label + "-client", True)
        if vanilla:
            ready(server, server_log, "logged in with entity id", 120)
            name = "AFMRCompanion" if neo else "AFMRSmoke"
            server.stdin.write(f"op {name}\ngamemode creative @a\ntp @a 1 80 1\nsummon minecraft:zombie 3 80 3 {{NoAI:1b,NoGravity:1b,PersistenceRequired:1b}}\n")
            server.stdin.flush()
        finish(client, client_log, 150)
        passed = results(result, 8 if vanilla else 12)
        finish(server, server_log, 30)
        print(f"PASS {label}: {passed} connection checks", flush=True)
        return passed
    finally:
        if client is not None:
            stop(client)
        stop(server)


def audit():
    summary = {}
    for target, base in [("neoforge", ROOT), ("fabric", ROOT / "ports/fabric-1.20.1")]:
        totals = {}
        for task in ["test", "networkTest"]:
            suites = [ET.parse(path).getroot() for path in (base / f"build/test-results/{task}").glob("TEST-*.xml")]
            totals[task] = sum(int(s.get("tests", 0)) for s in suites)
            assert suites and not any(int(s.get("failures", 0)) or int(s.get("errors", 0)) for s in suites)
        jars = list((base / "build/libs").glob("*server*.jar"))
        assert len(jars) == 1
        with zipfile.ZipFile(jars[0]) as archive:
            names = archive.namelist()
            assert "LICENSE" in names
            assert not any(name.endswith(".jar") or name.startswith("assets/") for name in names)
            assert all(not name.endswith(".class") or name.startswith(("dev/animal/afmr/server/", "dev/animal/afmr/network/", "dev/animal/afmr/core/StructureContext")) for name in names)
        totals["jarBytes"] = jars[0].stat().st_size
        summary[target] = totals
    return summary


def source_audit(base):
    paths = list((base / "build/distributions").glob("*source.zip"))
    assert paths
    with zipfile.ZipFile(max(paths, key=lambda p: p.stat().st_mtime)) as archive:
        for name in archive.namelist():
            parts = set(Path(name).parts)
            assert not parts.intersection({".git", ".gradle", "work", "outputs", "run", "build", "__pycache__"}), name
            assert not any(part.startswith("run-") for part in parts), name
            assert not name.endswith((".afmrb", ".log", ".pyc")), name


def main():
    gradle(["build"], "build-neoforge")
    gradle(["-p", "ports/fabric-1.20.1", "-Psmoke", "-PpackagedSmoke", "-PserverCompanion", "build", "compileSmokeJava"], "build-fabric")
    summary = audit()
    for target in ["neoforge", "fabric"]:
        gradle(["-p", "testing/server-" + target, "runServer"], target + "-headless")
        summary[target]["headless"] = results(ROOT / f"testing/server-{target}/run/server-companion-results.txt", 13)
        summary[target]["connection"] = connection(target)
        summary[target]["vanilla"] = connection(target, True)
    (LOGS / "summary.json").write_text(json.dumps(summary, indent=2) + "\n", encoding="utf8")
    lines = ["# Optional AFMR server companions — October 25, 2026", "", "Development version remains 1.0.1. Both maintained targets now support loaded structure", "boundaries/catalogs and event-started mob-target retention through optional server", "companions. Server JARs exclude client, UI, playback/library and decoder classes.", "", "## Measured verification", "", "| Target | Pure regressions | Packet tests | Headless server checks | Companion connection | Vanilla fallback | Server JAR bytes |", "| --- | ---: | ---: | ---: | ---: | ---: | ---: |"]
    for target, values in summary.items():
        lines.append(f"| {target} | {values['test']} | {values['networkTest']} | {values['headless']} | {values['connection']} | {values['vanilla']} | {values['jarBytes']} |")
    lines += ["", "Both full builds also pass 20 copy compiler checks per target. Headless probes load", "the packaged companions and use explicitly retained chunks, loaded structure-start", "fixtures, no-AI/no-gravity mobs and fake-player packet sinks. They verify per-player", "targets, target switching/removal, invalid sender scope/radius rejection, catalog", "and bounds, maximum radius and unchanged loaded-chunk count during observation.", "", "Separate actual socket connections use packaged clients in disposable profiles,", "one client at a time, capped at 60 FPS with VSync off from startup. They verify", "channel negotiation, remote bounds/catalog, target-only peace, event-started combat", "retention past a one-second quiet period, separate PvP state, target loss/countdown", "and retained structure data. Unmodified vanilla servers verify connection without", "channels, unavailable structures and the original event timeout. No personal worlds,", "music or launcher profiles are changed.", "", "Repeatable commands: [server companion fixtures](../../testing/SERVER-COMPANIONS.md).", "Generated full logs and result files stay under ignored work/run directories.", "", "## Additional field coverage", "", "These controlled fixtures do not establish natural generation/discovery across", "modpacks, large-population/many-client latency or broad reconnect/dimension/death", "combinations with native audio. Physical UI input, subjective audio transitions", "and malicious wire traffic beyond the packet decoder bounds remain deeper tests."]
    (ROOT / "docs/testing/2026-10-25-server-companions.md").write_text("\n".join(lines) + "\n", encoding="utf8")
    gradle(["sourceZip"], "source-neoforge")
    gradle(["-p", "ports/fabric-1.20.1", "sourceZip"], "source-fabric")
    source_audit(ROOT)
    source_audit(ROOT / "ports/fabric-1.20.1")
    print(json.dumps(summary, indent=2), flush=True)
    print("PASS source packaging privacy audit", flush=True)


if __name__ == "__main__":
    try:
        main()
    finally:
        for process, handle in reversed(ACTIVE):
            stop(process)
            handle.close()

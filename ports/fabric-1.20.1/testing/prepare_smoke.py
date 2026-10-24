"""Generate quiet test tones and copy only the repository's synthetic decoder fixtures."""
from pathlib import Path
import math
import shutil
import struct
import wave
import zlib

project = Path(__file__).resolve().parents[1]
profile = project / 'run-smoke'
profile.mkdir(exist_ok=True)
for name, frequency in [('smoke-a.wav', 220), ('smoke-b.wav', 330)]:
    with wave.open(str(profile / name), 'wb') as output:
        output.setparams((1, 2, 22050, 0, 'NONE', 'not compressed'))
        block = b''.join(struct.pack('<h', int(300 * math.sin(2 * math.pi * frequency * i / 22050)))
                         for i in range(22050))
        for _ in range(120):
            output.writeframesraw(block)
shutil.copy2(profile / 'smoke-a.wav', profile / 'import-smoke.wav')
codecs = profile / 'codecs'
codecs.mkdir(exist_ok=True)
for fixture in (project / 'src/test/resources/audio').iterdir():
    if fixture.is_file():
        shutil.copy2(fixture, codecs / fixture.name)

def chunk(kind, data):
    return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data))

png = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 2, 2, 8, 2, 0, 0, 0))
png += chunk(b'IDAT', zlib.compress((b'\x00' + b'\xf0\x60\x30' * 2) * 2)) + chunk(b'IEND', b'')
(profile / 'cover.png').write_bytes(png)
print('Prepared only generated audio/artwork in', profile)

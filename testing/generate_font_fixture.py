"""Original four-glyph TrueType fixture; stdlib only, no redistributed font data."""
from pathlib import Path
import struct


def u16(*values):
    return struct.pack('>' + 'H' * len(values), *values)


def i16(*values):
    return struct.pack('>' + 'h' * len(values), *values)


def u32(*values):
    return struct.pack('>' + 'I' * len(values), *values)


def padded(data):
    return data + b'\0' * (-len(data) % 4)


def checksum(data):
    data = padded(data)
    return sum(struct.unpack('>' + 'I' * (len(data) // 4), data)) & 0xffffffff


def rectangle(right):
    return padded(i16(1, 60, 0, right, 700) + u16(3, 0) + b'\x01' * 4
                  + i16(60, right - 60, 0, 60 - right) + i16(0, 0, 700, 0))


def generate():
    glyphs = [rectangle(650), b'', rectangle(200), rectangle(650)]
    offsets, offset = [], 0
    for glyph in glyphs:
        offsets.append(offset // 2)
        offset += len(glyph)
    offsets.append(offset // 2)
    names = {0: 'Original generated AFMR test fixture', 1: 'AFMR Fixture', 2: 'Regular',
             3: 'AFMRGeneratedFixture', 4: 'AFMR Fixture Regular', 5: 'Version 1.0', 6: 'AFMRFixture-Regular'}
    records, strings = b'', b''
    for key, value in sorted(names.items()):
        encoded = value.encode('utf-16-be')
        records += u16(3, 1, 0x409, key, len(encoded), len(strings))
        strings += encoded
    indices = [1 if c == 32 else 2 if chr(c) in 'ilI.,:' else 3 for c in range(32, 127)]
    cmap4 = u16(4, 32 + 2 * len(indices), 0, 4, 4, 1, 0, 126, 65535, 0, 32, 65535, 0, 1, 4, 0, *indices)
    tables = {
        b'head': u32(0x10000, 0x10000, 0, 0x5f0f3cf5) + u16(3, 1000) + b'\0' * 16 + i16(0, 0, 650, 700) + u16(0, 8) + i16(2, 0, 0),
        b'hhea': u32(0x10000) + i16(800, -200, 0) + u16(750) + i16(0, 0, 650, 1, 0, 0, 0, 0, 0, 0, 0) + u16(4),
        b'maxp': u32(0x10000) + u16(4, 4, 1, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0),
        b'hmtx': b''.join(u16(advance) + i16(0) for advance in [750, 250, 300, 750]),
        b'loca': u16(*offsets), b'glyf': b''.join(glyphs),
        b'cmap': u16(0, 1, 3, 1) + u32(12) + cmap4,
        b'name': u16(0, len(names), 6 + 12 * len(names)) + records + strings,
        b'OS/2': u16(0) + i16(500) + u16(400, 5, 0) + i16(650, 600, 0, 75, 650, 600, 0, 350, 50, 300, 0)
                 + b'\0' * 10 + u32(1, 0, 0, 0) + b'AFMR' + u16(0x40, 32, 126) + i16(800, -200, 0) + u16(800, 200),
        b'post': u32(0x30000, 0) + i16(-100, 50) + u32(0, 0, 0, 0, 0),
    }
    result = u32(0x10000) + u16(10, 128, 3, 32)
    directory, data, head = b'', b'', 0
    for tag, table in sorted(tables.items()):
        offset = 12 + 16 * len(tables) + len(data)
        if tag == b'head':
            head = offset
        directory += tag + u32(checksum(table), offset, len(table))
        data += padded(table)
    result = bytearray(result + directory + data)
    struct.pack_into('>I', result, head + 8, (0xb1b0afba - checksum(result)) & 0xffffffff)
    return result


if __name__ == '__main__':
    root = Path(__file__).resolve().parents[1]
    for target in [root / 'src/test/resources/fonts', root / 'ports/fabric-1.20.1/src/test/resources/fonts']:
        target.mkdir(parents=True, exist_ok=True)
        (target / 'afmr-fixture.ttf').write_bytes(generate())
    print('Generated original AFMR TrueType fixture')

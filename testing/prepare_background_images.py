"""Generate owned image decoder fixtures; never read player images or music."""
from pathlib import Path
from PIL import Image, ImageDraw
import shutil
import sys

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'src/test/resources/backgrounds'
DEST.mkdir(parents=True, exist_ok=True)

image = Image.new('RGBA', (96, 64), (255, 0, 0, 128))
ImageDraw.Draw(image).rectangle((48, 0, 95, 63), fill=(0, 120, 255, 255))
image.putpixel((0, 0), (0, 0, 0, 0))
image.save(DEST / 'lossless.webp', lossless=True, exact=True)
Image.new('RGB', (96, 64), (30, 140, 220)).save(DEST / 'lossy.webp', quality=90)
image.resize((4096, 1024)).save(DEST / 'wide.webp', lossless=True)
image.save(DEST / 'animated.webp', save_all=True,
           append_images=[Image.new('RGBA', image.size, 'green')], duration=[100, 200], loop=0, lossless=True)

palette = [0, 0, 0, 255, 0, 0, 0, 255, 0, 0, 0, 255, 255, 255, 0] + [0] * (768-15)
frames = []
for color, box in [(1, (0, 0, 15, 19)), (2, (16, 0, 31, 19)),
                   (3, (32, 20, 47, 39)), (4, (48, 20, 63, 39))]:
    frame = Image.new('P', (64, 40), 0)
    frame.putpalette(palette)
    ImageDraw.Draw(frame).rectangle(box, fill=color)
    frames.append(frame)
frames[0].save(DEST / 'disposal.gif', save_all=True, append_images=frames[1:],
               duration=[100, 200, 300, 400], disposal=[1, 2, 3, 1], transparency=0, loop=0, optimize=False)
with Image.open(DEST / 'disposal.gif') as gif:
    for i in range(gif.n_frames):
        gif.seek(i)
        gif.convert('RGBA').save(DEST / f'disposal-{i}.png')

def sequence(name, count, size):
    frames = [Image.new('RGB', size, ('red', 'blue')[i % 2]) for i in range(count)]
    frames[0].save(DEST / name, save_all=True, append_images=frames[1:], duration=100, loop=0)

sequence('bounded.gif', 20, (1024, 1024))
sequence('too-many.gif', 513, (4, 4))
Image.new('RGB', (12, 8), 'blue').save(DEST / 'still.gif')

port = ROOT / 'ports/fabric-1.20.1/src/test/resources/backgrounds'
port.mkdir(parents=True, exist_ok=True)
for path in DEST.iterdir():
    shutil.copyfile(path, port / path.name)
print('Generated image fixtures:', ', '.join(p.name for p in DEST.iterdir()))
if '--stage' in sys.argv:
    for profile in [ROOT / 'testing/native/run/workspace', ROOT / 'ports/fabric-1.20.1/run-smoke']:
        if not profile.is_dir():
            raise RuntimeError(f'Prepare the disposable profile first: {profile}')
        target = profile / 'background-fixtures'
        target.mkdir(exist_ok=True)
        for name in ['lossless.webp', 'disposal.gif']:
            shutil.copyfile(DEST / name, target / name)
    print('Staged generated images in both disposable test profiles.')

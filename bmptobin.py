import struct
import sys

# what
def die(msg):
    print(f"fck: {msg}")
    sys.exit(1)
if len(sys.argv) != 3:
    die("usage: bmp2blob.py input.bmp output.bin")
with open(sys.argv[1], "rb") as f:
    data = f.read()
if data[0:2] != b"BM":
    die("not a BMP")
bfOffBits = struct.unpack_from("<I", data, 0x0A)[0]
dib_size = struct.unpack_from("<I", data, 0x0E)[0]
if dib_size < 40:
    die("unsupported DIB header")
width  = struct.unpack_from("<i", data, 0x12)[0]
height = struct.unpack_from("<i", data, 0x16)[0]
planes = struct.unpack_from("<H", data, 0x1A)[0]
bpp    = struct.unpack_from("<H", data, 0x1C)[0]
comp   = struct.unpack_from("<I", data, 0x1E)[0]
if planes != 1:
    die("invalid planes")
if bpp != 24:
    die("only 24-bit BMP supported")
if comp != 0:
    die("compressed BMP not supported")
flip_y = True
if height < 0:
    height = -height
    flip_y = False

row_size = ((width * 3 + 3) // 4) * 4
out = bytearray()
for y in range(height):
    src_y = (height - 1 - y) if flip_y else y
    row_start = bfOffBits + src_y * row_size 
    for x in range(width):
        b = data[row_start + x*3 + 0] # vram bs
        g = data[row_start + x*3 + 1]
        r = data[row_start + x*3 + 2]
        out.extend((0, r, g, b))
with open(sys.argv[2], "wb") as f:
    f.write(out)
print(f"ok: {width}x{height}, wrote {len(out)} bytes")

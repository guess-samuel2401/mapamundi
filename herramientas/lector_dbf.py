"""Lector mínimo de tablas Visual FoxPro (.DBF con memos .FPT)."""
import os
import struct

ENC = 'cp1252'


def read_dbf(path):
    """Devuelve (campos, filas). Cada fila es un dict con '_deleted' y los campos ya decodificados."""
    b = open(path, 'rb').read()
    n, hl, rl = struct.unpack('<IHH', b[4:12])
    fields = []
    i = 32
    while b[i] != 0x0D:
        name = b[i:i + 11].split(b'\0')[0].decode()
        fields.append((name, chr(b[i + 11]), b[i + 16], struct.unpack('<I', b[i + 12:i + 16])[0]))
        i += 32

    fpt = None
    base = os.path.splitext(path)[0]
    for ext in ('.FPT', '.fpt', '.SCT', '.sct'):
        if os.path.exists(base + ext):
            fpt = open(base + ext, 'rb').read()
            break

    def memo(ptr):
        if not fpt or ptr == 0:
            return ''
        block = struct.unpack('>H', fpt[6:8])[0]
        off = ptr * block
        length = struct.unpack('>I', fpt[off + 4:off + 8])[0]
        return fpt[off + 8:off + 8 + length].decode(ENC, 'replace')

    rows = []
    for r in range(n):
        rec = b[hl + r * rl: hl + (r + 1) * rl]
        row = {'_deleted': rec[0:1] == b'*'}
        for name, kind, length, disp in fields:
            raw = rec[disp:disp + length]
            if kind == 'M':
                row[name] = memo(struct.unpack('<I', raw)[0]) if length == 4 else ''
            elif kind == 'L':
                row[name] = raw in (b'T', b'Y')
            else:
                row[name] = raw.decode(ENC, 'replace').strip()
        rows.append(row)
    return fields, rows

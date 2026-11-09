#!/usr/bin/env python3
"""Minimal zipalign: stored (uncompressed) entries start on a 4-byte boundary (.so on 4096).
Usage: zipalign.py in.apk out.apk"""
import sys, zipfile

def main(src, dst):
    zin = zipfile.ZipFile(src)
    with zipfile.ZipFile(dst, 'w') as zout:
        for info in zin.infolist():
            data = zin.read(info.filename)
            ni = zipfile.ZipInfo(info.filename, date_time=info.date_time)
            ni.compress_type = info.compress_type
            ni.external_attr = info.external_attr
            ni.create_system = info.create_system
            extra = b''
            if ni.compress_type == zipfile.ZIP_STORED:
                align = 4096 if info.filename.endswith('.so') else 4
                off = zout.fp.tell() + 30 + len(info.filename.encode('utf-8'))
                pad = (align - off % align) % align
                extra = b'\0' * pad
            ni.extra = extra
            zout.writestr(ni, data)

if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])

#!/usr/bin/env python3
"""Build the bundled city catalog from GeoNames (CC-BY 4.0).

Downloads cities15000.zip once, keeps cities with population >= 80k plus all
capitals, and packs them into app/src/main/assets/cities.bin:

  magic   4 bytes 'CCIT'
  version u8      1
  count   u16
  per city (sorted by iso2, then population desc):
    iso2  2 bytes ascii
    lat   i16  degrees * 100
    lon   i16  degrees * 100
    name  u8 length + UTF-8 bytes

Stdlib only. Usage:  py tools/generate_cities.py
"""
import csv
import io
import os
import struct
import urllib.request
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CACHE = os.path.join(ROOT, "tools", "cache")
CSV_PATH = os.path.join(ROOT, "tools", "data", "countries.csv")
OUT = os.path.join(ROOT, "app", "src", "main", "assets", "cities.bin")
URL = "https://download.geonames.org/export/dump/cities15000.zip"
MIN_POPULATION = 80_000


def main():
    os.makedirs(CACHE, exist_ok=True)
    zip_path = os.path.join(CACHE, "cities15000.zip")
    if not os.path.exists(zip_path):
        print(f"downloading {URL}")
        urllib.request.urlretrieve(URL, zip_path)

    with io.open(CSV_PATH, encoding="utf-8-sig") as f:
        iso_codes = {row["iso2"].strip() for row in csv.DictReader(f)}

    cities = []
    with zipfile.ZipFile(zip_path) as zf:
        with zf.open("cities15000.txt") as f:
            for line in io.TextIOWrapper(f, encoding="utf-8"):
                cols = line.rstrip("\n").split("\t")
                if len(cols) < 15:
                    continue
                name, lat, lon = cols[1], float(cols[4]), float(cols[5])
                feature_code, iso2 = cols[7], cols[8]
                population = int(cols[14] or 0)
                if iso2 not in iso_codes:
                    continue
                if population < MIN_POPULATION and feature_code != "PPLC":
                    continue
                if len(name.encode("utf-8")) > 255:
                    continue
                cities.append((iso2, name, lat, lon, population))

    cities.sort(key=lambda c: (c[0], -c[4]))
    if len(cities) > 65_535:
        raise SystemExit("too many cities for u16 count — raise MIN_POPULATION")

    buf = bytearray()
    buf += b"CCIT"
    buf += struct.pack("<B", 1)
    buf += struct.pack("<H", len(cities))
    for iso2, name, lat, lon, _pop in cities:
        buf += iso2.encode("ascii")
        buf += struct.pack("<hh", round(lat * 100), round(lon * 100))
        encoded = name.encode("utf-8")
        buf += struct.pack("<B", len(encoded))
        buf += encoded
    with open(OUT, "wb") as f:
        f.write(buf)
    per_country = {}
    for c in cities:
        per_country[c[0]] = per_country.get(c[0], 0) + 1
    print(f"wrote {OUT}: {len(cities)} cities, {len(buf)} bytes, "
          f"{len(per_country)} countries covered")


if __name__ == "__main__":
    main()

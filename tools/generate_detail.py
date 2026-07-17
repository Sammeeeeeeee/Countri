#!/usr/bin/env python3
"""Generate Countri's high-resolution per-country shape asset.

Reads Natural Earth 10m admin-0 country polygons (downloaded and cached on
first run) plus the curated catalog in tools/data/countries.csv, and writes:

  app/src/main/assets/detailmap.bin  -- per-country rings, bbox-normalised

The world map keeps using the light 110m worldmap.bin; this asset exists so
the country detail hero can draw a genuinely precise silhouette (fjords,
archipelagos, real coastlines) and is only decoded one country at a time.

Binary format (little-endian):
  magic   4 bytes  'CDTL'
  version u8       1
  count   u16      number of countries present
  per country:
    iso2    2 ASCII bytes
    bbox    4 * f32   minLon, minLat, maxLon, maxLat
    rings   u16
    per ring:
      n     u16     vertex count
      n * (x u16, y u16)   normalised 0..65535 inside the bbox

Stdlib only. Usage:  py tools/generate_detail.py [--budget 2600]
"""
import argparse
import csv
import io
import json
import math
import os
import struct
import sys
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CACHE = os.path.join(ROOT, "tools", "cache")
CSV_PATH = os.path.join(ROOT, "tools", "data", "countries.csv")
BIN_OUT = os.path.join(ROOT, "app", "src", "main", "assets", "detailmap.bin")

GEOJSON_URLS = [
    "https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/ne_10m_admin_0_countries.geojson",
    "https://raw.githubusercontent.com/martynafford/natural-earth-geojson/master/10m/cultural/ne_10m_admin_0_countries.json",
]

# Natural Earth quirks: ISO_A2 == "-99" for these; map from ADM0_A3 instead.
ADM0_TO_ISO2 = {
    "FRA": "FR", "NOR": "NO", "KOS": "XK", "PSX": "PS", "SDS": "SS",
    "SOL": "SO", "KAS": "-99",  # Siachen glacier -- leave untagged
}

CHIP_SEGMENTS = 24
CHIP_RADIUS_DEG = 0.05

MAX_RINGS_PER_COUNTRY = 96


def fetch_geojson():
    os.makedirs(CACHE, exist_ok=True)
    path = os.path.join(CACHE, "ne_10m_admin_0.geojson")
    if not os.path.exists(path):
        last_err = None
        for url in GEOJSON_URLS:
            try:
                print(f"downloading {url} (this one is ~25MB)")
                with urllib.request.urlopen(url, timeout=600) as r:
                    data = r.read()
                with open(path, "wb") as f:
                    f.write(data)
                break
            except Exception as e:  # try the mirror
                last_err = e
        else:
            raise SystemExit(f"could not download Natural Earth 10m data: {last_err}")
    with io.open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def load_catalog():
    rows = []
    with io.open(CSV_PATH, "r", encoding="utf-8-sig") as f:
        for row in csv.DictReader(f):
            rows.append({
                "iso2": row["iso2"].strip(),
                "name": row["name"].strip(),
                "lat": float(row["lat"]),
                "lon": float(row["lon"]),
            })
    return rows


def feature_iso2(props):
    for key in ("ISO_A2_EH", "ISO_A2"):
        v = props.get(key)
        if v and v != "-99":
            return v
    return ADM0_TO_ISO2.get(props.get("ADM0_A3", ""), None)


# --- Douglas-Peucker on a closed ring (same scheme as generate_map.py) -----

def _dp(points, first, last, tol, keep):
    stack = [(first, last)]
    while stack:
        a, b = stack.pop()
        ax, ay = points[a]
        bx, by = points[b]
        dx, dy = bx - ax, by - ay
        seg2 = dx * dx + dy * dy
        max_d2, max_i = -1.0, -1
        for i in range(a + 1, b):
            px, py = points[i]
            if seg2 == 0.0:
                d2 = (px - ax) ** 2 + (py - ay) ** 2
            else:
                t = ((px - ax) * dx + (py - ay) * dy) / seg2
                t = max(0.0, min(1.0, t))
                cx, cy = ax + t * dx, ay + t * dy
                d2 = (px - cx) ** 2 + (py - cy) ** 2
            if d2 > max_d2:
                max_d2, max_i = d2, i
        if max_i != -1 and max_d2 > tol * tol:
            keep[max_i] = True
            stack.append((a, max_i))
            stack.append((max_i, b))


def simplify_ring(ring, tol):
    pts = ring[:-1] if ring[0] == ring[-1] else ring[:]
    n = len(pts)
    if n <= 4 or tol <= 0:
        return pts
    ax, ay = pts[0]
    far = max(range(1, n), key=lambda i: (pts[i][0] - ax) ** 2 + (pts[i][1] - ay) ** 2)
    keep = [False] * n
    keep[0] = keep[far] = True
    _dp(pts, 0, far, tol, keep)
    wrapped = pts[far:] + pts[:1]
    wkeep = [False] * len(wrapped)
    wkeep[0] = wkeep[-1] = True
    _dp(wrapped, 0, len(wrapped) - 1, tol, wkeep)
    for j in range(1, len(wrapped) - 1):
        if wkeep[j]:
            keep[far + j] = True
    out = [p for i, p in enumerate(pts) if keep[i]]
    return out if len(out) >= 4 else pts


def ring_area(ring):
    a = 0.0
    n = len(ring)
    for i in range(n):
        x1, y1 = ring[i]
        x2, y2 = ring[(i + 1) % n]
        a += x1 * y2 - x2 * y1
    return abs(a) / 2.0


def make_chip(lon, lat):
    r = CHIP_RADIUS_DEG
    rl = r / max(0.2, math.cos(math.radians(lat)))
    return [(lon + rl * math.cos(2 * math.pi * i / CHIP_SEGMENTS),
             lat + r * math.sin(2 * math.pi * i / CHIP_SEGMENTS))
            for i in range(CHIP_SEGMENTS)]


def build_country(raw_rings, budget):
    """Adaptive simplification: finest tolerance that fits the budget."""
    # Largest ring drives the scale; drop dust far below it.
    areas = [ring_area(r) for r in raw_rings]
    biggest = max(areas) if areas else 0.0
    keep_rings = [r for r, a in zip(raw_rings, areas)
                  if a >= biggest * 1e-5 or a >= 0.0004]
    keep_rings.sort(key=ring_area, reverse=True)
    keep_rings = keep_rings[:MAX_RINGS_PER_COUNTRY]

    span = 0.0
    for r in keep_rings:
        lons = [p[0] for p in r]
        lats = [p[1] for p in r]
        span = max(span, max(lons) - min(lons), max(lats) - min(lats))
    tol = max(span / 2600.0, 0.0008)
    for _ in range(12):
        rings = [simplify_ring(r, tol) for r in keep_rings]
        rings = [r for r in rings if len(r) >= 4]
        if sum(len(r) for r in rings) <= budget:
            return rings
        tol *= 1.45
    return rings  # last attempt, slightly over budget — acceptable


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--budget", type=int, default=2600,
                    help="max vertices per country")
    args = ap.parse_args()

    catalog = load_catalog()
    gj = fetch_geojson()

    by_iso = {}
    for feat in gj["features"]:
        props = feat.get("properties", {})
        if props.get("ADM0_A3") == "ATA":
            continue
        iso2 = feature_iso2(props)
        if iso2 is None:
            continue
        geom = feat.get("geometry") or {}
        polys = []
        if geom.get("type") == "Polygon":
            polys = [geom["coordinates"]]
        elif geom.get("type") == "MultiPolygon":
            polys = geom["coordinates"]
        rings = by_iso.setdefault(iso2, [])
        for poly in polys:
            for ring in poly:
                rings.append([(p[0], p[1]) for p in ring])

    out = []
    total = 0
    for c in catalog:
        iso2 = c["iso2"]
        raw = by_iso.get(iso2)
        rings = build_country(raw, args.budget) if raw else [make_chip(c["lon"], c["lat"])]
        if not raw:
            print(f"  chip for {iso2} (absent from 10m)")
        n = sum(len(r) for r in rings)
        total += n
        out.append((iso2, rings))

    print(f"countries: {len(out)}, total vertices: {total}")

    buf = bytearray()
    buf += b"CDTL"
    buf += struct.pack("<B", 1)
    buf += struct.pack("<H", len(out))
    for iso2, rings in out:
        min_lon = min(p[0] for r in rings for p in r)
        max_lon = max(p[0] for r in rings for p in r)
        min_lat = min(p[1] for r in rings for p in r)
        max_lat = max(p[1] for r in rings for p in r)
        # Guard zero-span boxes (a chip on a meridian).
        if max_lon - min_lon < 1e-6:
            max_lon = min_lon + 1e-6
        if max_lat - min_lat < 1e-6:
            max_lat = min_lat + 1e-6
        buf += iso2.encode("ascii")
        buf += struct.pack("<ffff", min_lon, min_lat, max_lon, max_lat)
        buf += struct.pack("<H", len(rings))
        sx = 65535.0 / (max_lon - min_lon)
        sy = 65535.0 / (max_lat - min_lat)
        for r in rings:
            buf += struct.pack("<H", len(r))
            for lon, lat in r:
                x = int(round((lon - min_lon) * sx))
                y = int(round((lat - min_lat) * sy))
                buf += struct.pack("<HH", max(0, min(65535, x)), max(0, min(65535, y)))

    os.makedirs(os.path.dirname(BIN_OUT), exist_ok=True)
    with open(BIN_OUT, "wb") as f:
        f.write(buf)
    print(f"wrote {BIN_OUT} ({len(buf)} bytes)")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Generate Countri's world map asset and country catalog.

Reads Natural Earth 110m admin-0 country polygons (downloaded and cached on
first run) plus the curated catalog in tools/data/countries.csv, and writes:

  app/src/main/assets/worldmap.bin   -- packed polygon rings tagged by country
  app/src/main/java/dev/sam/countri/data/catalog/CountryCatalog.kt

Binary format (little-endian):
  magic   4 bytes  'CMAP'
  version u8       1
  rings   u16      ring count
  per ring:
    country u8     0 = land outside the catalog (not tappable), else 1-based
                   index into the catalog (CSV order)
    n       u16    vertex count
    n * (lon i16, lat i16)   degrees * 100

Rings of one feature share the feature's country tag; holes (e.g. Lesotho
inside South Africa) rely on even-odd fill in the renderer. Catalog countries
absent from 110m geometry (microstates) get a small circular chip at their
centroid so every country is visible and tappable.

Stdlib only. Usage:  py tools/generate_map.py [--tolerance 0.22]
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
BIN_OUT = os.path.join(ROOT, "app", "src", "main", "assets", "worldmap.bin")
KT_OUT = os.path.join(
    ROOT, "app", "src", "main", "java", "dev", "sam", "countri",
    "data", "catalog", "CountryCatalog.kt")

GEOJSON_URLS = [
    "https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/ne_110m_admin_0_countries.geojson",
    "https://raw.githubusercontent.com/martynafford/natural-earth-geojson/master/110m/cultural/ne_110m_admin_0_countries.json",
]

# Natural Earth quirks: ISO_A2 == "-99" for these; map from ADM0_A3 instead.
ADM0_TO_ISO2 = {
    "FRA": "FR", "NOR": "NO", "KOS": "XK", "PSX": "PS", "SDS": "SS",
    "SOL": "SO", "KAS": "-99",  # Siachen glacier — leave untagged
}

CHIP_SEGMENTS = 10
CHIP_RADIUS_DEG = 0.55  # small but visibly tappable at world zoom


def fetch_geojson():
    os.makedirs(CACHE, exist_ok=True)
    path = os.path.join(CACHE, "ne_110m_admin_0.geojson")
    if not os.path.exists(path):
        last_err = None
        for url in GEOJSON_URLS:
            try:
                print(f"downloading {url}")
                with urllib.request.urlopen(url, timeout=120) as r:
                    data = r.read()
                with open(path, "wb") as f:
                    f.write(data)
                break
            except Exception as e:  # try the mirror
                last_err = e
        else:
            raise SystemExit(f"could not download Natural Earth data: {last_err}")
    with io.open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def load_catalog():
    rows = []
    with io.open(CSV_PATH, "r", encoding="utf-8-sig") as f:
        for row in csv.DictReader(f):
            rows.append({
                "iso2": row["iso2"].strip(),
                "name": row["name"].strip(),
                "continent": row["continent"].strip(),
                "lat": float(row["lat"]),
                "lon": float(row["lon"]),
            })
    isos = [r["iso2"] for r in rows]
    assert len(rows) == 195, f"catalog must have 195 rows, got {len(rows)}"
    assert len(set(isos)) == 195, "duplicate iso2 in catalog"
    return rows


def feature_iso2(props):
    for key in ("ISO_A2_EH", "ISO_A2"):
        v = props.get(key)
        if v and v != "-99":
            return v
    return ADM0_TO_ISO2.get(props.get("ADM0_A3", ""), None)


# --- Douglas-Peucker on a closed ring -------------------------------------

def _dp(points, first, last, tol, keep):
    """Mark indices to keep between anchors first/last (exclusive)."""
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
    """ring: list of (lon, lat), closed (first == last). Returns open ring."""
    pts = ring[:-1] if ring[0] == ring[-1] else ring[:]
    n = len(pts)
    if n <= 4 or tol <= 0:
        return pts
    # anchor at 0 and the point farthest from 0, then DP each arc
    ax, ay = pts[0]
    far = max(range(1, n), key=lambda i: (pts[i][0] - ax) ** 2 + (pts[i][1] - ay) ** 2)
    keep = [False] * n
    keep[0] = keep[far] = True
    _dp(pts, 0, far, tol, keep)
    # wrap-around arc: far .. n-1 .. 0  (treat as linear list ending at index 0)
    wrapped = pts[far:] + pts[:1]
    wkeep = [False] * len(wrapped)
    wkeep[0] = wkeep[-1] = True
    _dp(wrapped, 0, len(wrapped) - 1, tol, wkeep)
    for j in range(1, len(wrapped) - 1):
        if wkeep[j]:
            keep[far + j] = True
    out = [p for i, p in enumerate(pts) if keep[i]]
    return out if len(out) >= 4 else pts


def make_chip(lon, lat):
    r = CHIP_RADIUS_DEG
    rl = r / max(0.2, math.cos(math.radians(lat)))
    return [(lon + rl * math.cos(2 * math.pi * i / CHIP_SEGMENTS),
             lat + r * math.sin(2 * math.pi * i / CHIP_SEGMENTS))
            for i in range(CHIP_SEGMENTS)]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--tolerance", type=float, default=0.22,
                    help="Douglas-Peucker tolerance in degrees")
    args = ap.parse_args()

    catalog = load_catalog()
    index_of = {c["iso2"]: i + 1 for i, c in enumerate(catalog)}  # 1-based
    gj = fetch_geojson()

    rings = []          # (country_idx, [(lon, lat), ...])
    seen_iso = set()
    for feat in gj["features"]:
        props = feat.get("properties", {})
        if props.get("ADM0_A3") == "ATA":  # Antarctica
            continue
        iso2 = feature_iso2(props)
        idx = index_of.get(iso2, 0)
        if idx:
            seen_iso.add(iso2)
        geom = feat.get("geometry") or {}
        polys = []
        if geom.get("type") == "Polygon":
            polys = [geom["coordinates"]]
        elif geom.get("type") == "MultiPolygon":
            polys = geom["coordinates"]
        for poly in polys:
            for ring in poly:  # exterior first, then holes; same tag, even-odd fill
                simplified = simplify_ring([(p[0], p[1]) for p in ring], args.tolerance)
                if len(simplified) >= 4:
                    rings.append((idx, simplified))

    missing = [c for c in catalog if c["iso2"] not in seen_iso]
    for c in missing:
        rings.append((index_of[c["iso2"]], make_chip(c["lon"], c["lat"])))
    print(f"chips injected for {len(missing)} countries: "
          + " ".join(c["iso2"] for c in missing))

    total_vertices = sum(len(r) for _, r in rings)
    print(f"rings: {len(rings)}, vertices: {total_vertices}")
    if total_vertices > 9500:
        print("WARNING: vertex budget exceeded — raise --tolerance", file=sys.stderr)

    buf = bytearray()
    buf += b"CMAP"
    buf += struct.pack("<B", 1)
    buf += struct.pack("<H", len(rings))
    for idx, ring in rings:
        buf += struct.pack("<BH", idx, len(ring))
        for lon, lat in ring:
            lon = max(-180.0, min(180.0, lon))
            lat = max(-90.0, min(90.0, lat))
            buf += struct.pack("<hh", round(lon * 100), round(lat * 100))
    os.makedirs(os.path.dirname(BIN_OUT), exist_ok=True)
    with open(BIN_OUT, "wb") as f:
        f.write(buf)
    print(f"wrote {BIN_OUT} ({len(buf)} bytes)")

    lines = [
        "// GENERATED by tools/generate_map.py — do not edit by hand.",
        "package dev.sam.countri.data.catalog",
        "",
        "data class Country(",
        "    val iso2: String,",
        "    val name: String,",
        "    val continent: Continent,",
        "    val lat: Float,",
        "    val lon: Float,",
        ")",
        "",
        "object CountryCatalog {",
        "    val all: List<Country> = listOf(",
    ]
    for c in catalog:
        name = c["name"].replace("\\", "\\\\").replace("\"", "\\\"")
        lines.append(
            f"        Country(\"{c['iso2']}\", \"{name}\", "
            f"Continent.{c['continent']}, {c['lat']}f, {c['lon']}f),")
    lines += [
        "    )",
        "    val byIso2: Map<String, Country> = all.associateBy { it.iso2 }",
        "    /** 1-based index used by the worldmap.bin asset; 0 means no country. */",
        "    fun indexOf(iso2: String): Int = all.indexOfFirst { it.iso2 == iso2 } + 1",
        "}",
        "",
    ]
    os.makedirs(os.path.dirname(KT_OUT), exist_ok=True)
    with io.open(KT_OUT, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines))
    print(f"wrote {KT_OUT}")


if __name__ == "__main__":
    main()

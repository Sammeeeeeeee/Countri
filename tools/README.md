# Countri asset tools

## generate_map.py

Regenerates the two generated inputs the app ships with:

- `app/src/main/assets/worldmap.bin` — packed country polygons
  (Natural Earth 110m, Douglas-Peucker simplified, microstate chips injected)
- `app/src/main/java/dev/sam/countri/data/catalog/CountryCatalog.kt` —
  the 195-country catalog compiled from `tools/data/countries.csv`

Run (Python 3, stdlib only):

```
py tools/generate_map.py              # default tolerance 0.22°
py tools/generate_map.py --tolerance 0.06   # what the shipped asset uses
```

The Natural Earth GeoJSON downloads once into `tools/cache/` (git-ignored).
Both outputs are checked in; regeneration is manual, not part of the build.
After regenerating, run `gradlew testDebugUnitTest` — CatalogTest and
WorldMapAssetTest lock the invariants (195 countries, continent totals,
every country tappable).

## data/countries.csv

Hand-curated: 193 UN members + Vatican City + Palestine. Continent
assignment follows the UN buckets and must keep the totals
Africa 54 / Asia 48 / Europe 44 / N. America 23 / S. America 12 / Oceania 14
(Türkiye counts as Asia, Russia as Europe) — the Stats screen's
per-continent denominators depend on it.

# Usage

This guide covers how to generate your own Sourdough vector tiles and deploy them.

You will need:

- Java 21 or later
- Maven for building
- A machine with sufficient RAM and disk space

For smaller regions (a small country or U.S. state), you can likely build tiles on your own computer in a matter of minutes. For creating tiles of the entire planet, you'll need a more powerful machine. Planetiler has [excellent docs](https://github.com/onthegomap/planetiler/blob/main/PLANET.md) on how to build tiles for the whole planet, including detailed system requirements and advice on performance tuning.

## Compiling the Sourdough builder

```bash
mvn package
```

This creates `target/sourdough-builder-HEAD-with-deps.jar`, a standalone Java archive that includes Planetiler and all other dependencies.

## Generating tiles

You can run the compiled Sourdough builder like this:

```bash
java -jar target/sourdough-builder-HEAD-with-deps.jar \
  --download \
  --area <area-name> \
  --output sourdough.pmtiles
```

### Common Planetiler arguments

- `--area <name>` - Download and process a named area (e.g., `washington`, `iceland`)
- `--bounds <west>,<south>,<east>,<north>` - Only build tiles for a given bounds; if not specified, the bounds will be determined from the input OSM PBF file
- `--maxzoom <int>` - Build tiles up to (and including) this zoom level. Sourdough's default maxzoom is 15, but you can omit higher zoom tiles if you don't need them. Note that certain features only show up at high zooms (for example `amenity=bench`, `natural=tree`, and `highway=street_lamp` only appear at zoom 15), so if you set a lower maxzoom, then your map won't be able to display these features.
- `--output <path>` - Output file path
- `--download` - Automatically download the required input data if missing
- `--force` - Overwrite existing output file if it exists

### Sourdough-specific arguments

- `--language <code>` - Change the preferred language for `name` attributes on tile features. When not set, a feature's `name` will be equal to the value of the `name` tag on the corresponding OSM element. But if you set `--language fr`, then the `name` tag will be equal to the value of the `name:fr` tag if one is present, and fall back to the `name` tag if it isn't.

   Basically, if you want your map labels to be in the local language (matching OSM's convention for what goes in the `name` tag), then don't use this option. But if you want your map labels to be in a specific language whenever possible (no matter where in the world you're looking at), then use the `--language` option to specify your desired language, and names in that language will be preferred when they are available in OSM.

## Deploying and serving tiles

Once you've generated a `.pmtiles` file, you need to host it somewhere so that a client (like a web browser or mobile app) can fetch the tiles it needs to display your map.

PMTiles is a single-file archive format that can be served directly from cloud storage without a tile server. It uses HTTP Range Requests to fetch only the tiles needed for the current map view. You can upload a multi-gigabyte PMTiles archive to Amazon S3, Cloudflare R2, or any other storage provider that supports Range requests, and then [use MapLibre and the PMTiles JavaScript library](https://github.com/protomaps/PMTiles/blob/main/js/README.md) to fetch tiles on demand.

However, for production maps, it's recommended to deploy your PMTiles tileset behind a cloud function (AWS Lambda, Cloudflare Worker, etc). The cloud function can recieve normal requests for tiles by `/z/x/y` URL path, fetch the appropriate range of data from the PMTiles archive behind the scenes, and return it to the client. This has several advantages:
- It's backwards-compatible with any map rendering library that supports `/z/x/y` tiles.
- Caching is easier, and you can benefit from an edge cache (which stores data nearer to where the client is requesting it from), resulting in faster load times for users
- The cloud function costs nothing when nobody is using it (unlike a traditional VM where you'll usually pay a fixed monthly fee). Of course, this is also a _disadvantage_ in that if lots of people start using it, you may get a large bill.

See [docs.protomaps.com/deploy](https://docs.protomaps.com/deploy/) for more details on how to serve PMTiles data.

## Troubleshooting

### Out of memory

If you see `java.lang.OutOfMemoryError` or reports that Java has exceeded the allowed heap space, it means Planetiler needed more RAM than was available to it. You may be able to fix this by increasing the JVM heap size setting with `-Xmx`:

```bash
java -Xmx16g -jar target/sourdough-builder-HEAD-with-deps.jar ...
```

But if that doesn't help, you'll need to build your tileset on a machine with more RAM.

### No space left on device

This means you've run out of disk space. Planetiler writes large temporary files while it is building the tileset (much larger than the final tileset size); you'll probably need about 10x as much free disk space as the size of the OSM PBF file you're processing.

## Further reading

- [Planetiler documentation](https://github.com/onthegomap/planetiler)
- [Planetiler planet build guide](https://github.com/onthegomap/planetiler/blob/main/PLANET.md)
- [PMTiles specification](https://github.com/protomaps/PMTiles)
- [Protomaps deployment guides](https://docs.protomaps.com/deploy)

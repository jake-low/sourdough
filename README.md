# Sourdough vector tiles

Sourdough is a free vector tile schema for [OpenStreetMap] data. This repository contains the Sourdough [schema specification](./SCHEMA.md) and the Planetiler [reference implementation](./src/main/java/fyi/osm/sourdough/).

[![screenshot of a visualization of Sourdough tiles, showing the Boston area and the many kinds of data available in the tile schema](https://sourdough.osm.fyi/assets/banner.png)](https://sourdough.osm.fyi/#11.8/42.365/-71.025)

You can see a demo of Sourdough at [sourdough.osm.fyi](https://sourdough.osm.fyi)

## Design goals

- **Simple to understand:** Sourdough divides OpenStreetMap features into tile layers that roughly correspond to OSM [top-level tags]: there are layers for `buildings`, `amenities`, `highways`, `landuse`, etc. Features within each layer have attributes that match the names of the OSM tags they are derived from. So if you're already familiar with OpenStreetMap's tags, Sourdough will feel easy to pick up and use.
- **Support cartographic experimentation:** Sourdough includes a wide range of different feature types, and many different OSM tags on each feature. The hope is that map makers can use Sourdough tiles to create diverse and specialized maps that highlight all the different types of data in OpenStreetMap.
- **Showcase OSM's taxonomy, and help improve it:** Sourdough presents OSM data more or less as it is. This highlights both the strengths and quirks of OSM's tagging conventions. By doing so, Sourdough hopes to inform ongoing community efforts to make OSM data more consistent and useful for data consumers.
- **Hackable starting point for custom tiles:** Sourdough's reference implementation is written for the [Planetiler] tile generator, which is fast to run on modest hardware and does not require an external database. The Sourdough code is written in a straightforward style that is easy to modify and extend. Sourdough aims to be a great starting point for creating custom vector tilesets for advanced use cases.

These goals come with some trade-offs:
- Because Sourdough tiles are meant to be flexible for many different types of maps, they contain a lot of data, and may be larger (and make your map load more slowly for your users) than tiles from other schemas.
- Since Sourdough aims to be a simple and direct representation of OSM data, it leaves it to map-makers to handle [synonymous tags], [troll tags], and other idiosyncracies in OSM's data model.
- Some popular cartographic effects (like placing curved text labels along lakes, or clustering nearby points of interest) require making opinionated decisions about how to transform the raw input data during tile generation. Sourdough aims to be a direct and naïve transformation of OSM data into vector tiles, so it avoids implementing these features, which means some effects are not possible (unless you modify the code yourself to add them).

## License and attribution requirements

The **Sourdough tile schema** and its **Planetiler reference implementation** contained in this repository are dedicated to the public domain via the [CC0] license. You may use them however you want, and do not need to give credit to the Sourdough project or its authors. See the [LICENSE](./LICENSE) file for details.

**OpenStreetMap itself** is available under the [ODbL] license, which requires that you attribute (credit) OpenStreetMap when you use the data to make maps. If you make a map using Sourdough tiles (or any other map that uses OpenStreetMap data), you should display a message that tells users that the map data is from OpenStreetMap. See OpenStreetMap's [attribution guidelines] for details.

[OpenStreetMap]: https://openstreetmap.org/about
[top-level tags]: https://wiki.openstreetmap.org/wiki/Top-level_tag
[synonymous tags]: https://wiki.openstreetmap.org/wiki/Synonymous_tags
[troll tags]: https://wiki.openstreetmap.org/wiki/Trolltag
[Planetiler]: https://github.com/onthegomap/planetiler?tab=readme-ov-file#planetiler
[CC0]: https://creativecommons.org/public-domain/cc0/
[ODbL]: https://opendatacommons.org/licenses/odbl/
[attribution guidelines]: https://osmfoundation.org/wiki/Licence/Attribution_Guidelines#Attribution_text

# Sourdough Vector Tile Schema

Sourdough organizes OpenStreetMap data into vector tile layers that closely follow OSM's own tag structure. Each layer corresponds to a top-level OSM tag (like `highway`, `building`, `amenity`), and the attributes on each feature are taken directly from the value of an OSM tag with the same name.

## Geometries

Most feature types in OpenStreetMap can be mapped using multiple geometry types. For example, a parking lot can be mapped as an `amenity=parking` node, or as a closed way representing an area. A `barrier=kerb` can be a node (representing a point where a footpath or other `highway` crosses a kerb), or it can be an way representing a length of kerb (like that separating a sidewalk from a roadway).

Sourdough layers, consequently, may in most cases contain any kind of geometry: `Point`, `LineString`, or `Polygon` (or their `Multi-` equivalents). For example, the `aeroways` layer contains points (gates, towers, windsocks), lines (runways, taxiways, parking positions), and polygons (terminals, aprons, and the airport area as a whole).

OSM Nodes are always interpreted as Points; unclosed OSM Ways are always interpreted as LineStrings, and OSM `multipolygon` Relations are always interpreted as Polygons. However, closed Ways in OSM are ambiguous, and may represent either linear features (like a roundabout) or areas (like a building). Sourdough chooses how to interpret these closed ways based on their tags. When `area=yes` or `area=no` is given explicitly, closed ways will be interpreted as Polygons or LineStrings respectively. When no such tag is present (which is quite common), Sourdough chooses how to interpret the way based on its top-level tags. Each layer defines its own behavior in this case: for example, the `waterways` layer treats ways as lines by default, but treats ways tagged `waterway=dam/dock/boatyard/fuel` as polygons.

## Zoom levels

Any vector tile schema must make decisions about which features to omit at lower zoom levels; otherwise the 0/0/0 tile would contain all of the data in OpenStreetMap and be many gigabytes.

In order to support a wide range of cartographic use cases, Sourdough attempts to include lots of features at each zoom level. Any given map may not actually _display_ all of this data, but it offers flexibility for map makers to highlight different kinds of features.

For point and line features, Sourdough relies on the feature's tags to decide what zooms to include it at. For example, `highway` lines appear depending on their highway class, and `amenity` points appear depending on how "important" they are (libraries and community centers at lower zooms than benches and wastebaskets).

For polygon features, Sourdough includes any feature that is larger than a certain visual size. This means that very large lakes, landuse areas, protected area boundaries, etc will be shown at low zooms. In the event that an area feature is small but has tags which imply that it's important (e.g. a very small library), it will appear at a lower zoom---the same zoom that it would have appeared at if it were mapped as a node.

## Primary and Detail tags

At the lowest zoom level that a feature appears at, only a small subset of tags are included on it, which Sourdough calls the _primary tags_. Generally, primary tags include the feature's type, but not its name, access restrictions, physical details, etc. Omitting these details at lower zooms helps reduce tile sizes.

At some higher zoom level, additional tags are added to the feature. These are its _detail tags_. Sourdough includes lots of details about a feature at higher zooms, so that map makers have lots of inputs to use when styling the feature.

Much like the zoom level that a feature first appears at, the zoom level at which detail tags become available may depend on the feature's kind, its visual size on screen, or both.

## Physical ordering tags

At low zooms, most maps render features in a fixed order based on importance; for example: rendering motorways on top of minor roads, and roads above waterways. But at high zooms, many maps switch to rendering features in a way that reflects their real-world vertical order (so that bridges, tunnels, overpasses, and underpasses are depicted accurately).

To facilitate this, Sourdough defines a set of _physical order tags_ (`layer`, `bridge`, `tunnel`, `location`, and `covered`). These tags are included on _all_ features in several tile layers (`highways`, `railways`, `waterways`) beginning at a fixed zoom level (zoom 12), regardless of the feature's type.

This allows map stylesheet authors to render these features in several passes, in order to show accurate vertical relationships.

## Common tags

A small set of detail tags are included on features in every layer. These are: `name`, `ref`, `alt_name`, `short_name`, `official_name`, `wikidata`, `wikipedia`.

## Attribute types

Sourdough processes OSM's string-valued tags into appropriate data types for vector tiles. Any given OSM tag key is handled identically across all layers that tag is included in. Any given tag (like `access` or `height`) is treated consistenty across all layers that it appears in.

The following tags are parsed to **integers**: `admin_level`, `building:levels`, `capacity`, `capacity:disabled`, `layer`, `level`, `population`.

The following tags are parsed as numeric values with units, and **converted to a floating point value in meters**: `ele`, `height`.

All other tags that are included in Sourdough are kept as **strings**.

Sidenote: _Why no booleans?_

Many OSM tags commonly take values `"yes"` and `"no"`. Sourdough keeps these tag values as strings, rather than converting them to `true` and `false` booleans. This is because many of the tags that look like booleans at first glance may actually take more than two values. Examples include `seasonal=yes/no/summer/winter`, `takeaway=yes/no/only`, and  `wheelchair=yes/no/limited`. Other tags like `fee=yes/no` or `intermittent=yes/no` do not have any other widely accepted values today, but additional values might be created in the future.

Using the strings `"yes"` and `"no"` for these boolean-ish attributes does not significantly change the size of the output tiles, because each string in a given layer is only encoded once (even if it is used by multiple features). And in most renderers, it will not significantly affect memory footprint or frame time.


## Label points

For area features, Sourdough also includes Points at the centroids of those areas. These label points have exactly the same ID and attributes as the area they correspond to. They are meant to help map makers place labels (text, icons, etc) at the center of the area if they wish. Note that map rendering engines such as MapLibre can't do this client-side, because an area may be split across several tiles and may not all be in view at once, so the client can't necessarily know where its centroid is.

Point labels for polygon features appear at the same zoom level as the detail tags for that feature.

## Layers

### Advertising

The `advertising` layer contains features from OSM which are tagged `advertising=*`, including billboards, posters, and other advertising structures.

**Attributes**:
- **Primary**: `advertising`
- **Detail**: `name`, `ref`, `alt_name`, `short_name`, `official_name`, `wikidata`, `wikipedia`, `support`, `lit`, `operator`, `visibility`, `message`, `sides`, `animated`, `luminous`, `direction`

### Aerialways

The `aerialways` layer contains features from OSM which are tagged `aerialway=*`, including cable cars, gondolas, and chairlifts.

**Attributes**:
- **Primary**: `aerialway`
- **Detail**: `name`, `ref`, `alt_name`, `short_name`, `official_name`, `wikidata`, `wikipedia`, `access`, `aerialway:access`, `aerialway:capacity`, `aerialway:duration`, `aerialway:occupancy`, `ele`, `fee`, `oneway`, `operator`, `wheelchair`,

### Aeroways

The `aeroways` layer contains features from OSM which are tagged `aeroway=*`, including airports, runways, taxiways, helipads, and other aviation facilities.

**Attributes**:
- **Primary**: `aeroway`
- **Detail**: `name`, `ref`, `alt_name`, `short_name`, `official_name`, `wikidata`, `wikipedia`, `iata`, `icao`, `faa`, `aerodrome:type`, `military`, `operator`, `surface`, `access`, `ele`, `width`, `direction`, `navigationaid`, `holding_position:type`

### Amenities

The `amenities` layer contains features from OSM which are tagged `amenity=*`. This tag covers a broad range of businesses, institutions, and facilities.

**Attributes**:
- **Primary**: `amenity`
- **Detail**: `name`, `ref`, `alt_name`, `short_name`, `official_name`, `wikidata`, `wikipedia`, `access`, `air_conditioning`, `atm`, `bicycle_parking`, `bicycle_rental`, `booth`, `branch`, `brand`, `capacity`, `capacity:disabled`, `changing_table`, `community_centre`, `covered`, `cuisine`, `denomination`, `diet:gluten_free`, `diet:halal`, `diet:kosher`, `diet:vegan`, `diet:vegetarian`, `dispensing`, `drive_through`, `fair_trade`, `fee`, `female`, `fountain`, `gender_segregated`, `indoor`, `indoor_seating`, `internet_access`, `layer`, `level`, `location`, `male`, `organic`, `outdoor_seating`, `parking`, `parking_space`, `recycling:cans`, `recycling:clothes`, `recycling:glass`, `recycling:glass_bottles`, `recycling:paper`, `recycling:plastic`, `recycling:plastic_bottles`, `recycling:plastic_packaging`, `recycling_type`, `religion`, `reservation`, `self_service`, `smoking`, `social_facility`, `social_facility:for`, `surface`, `takeaway`, `toilets`, `toilets:disposal`, `townhall:type`, `unisex`, `vending`, `waste`, `wheelchair`

### Barriers

The `barriers` layer contains features from OSM which are tagged `barrier=*`, including fences, walls, gates, and kerbs.

**Attributes**:
- **Primary**: `barrier`, `fence_type`, `wall`, `kerb`
- **Detail**: `name`, `ref`, `alt_name`, `short_name`, `official_name`, `wikidata`, `wikipedia`, `material`, `height`, `access`, `wheelchair`

### Boundaries

The `boundaries` layer contains administrative boundaries from OSM relations tagged `boundary=administrative`, as well as protected areas and other boundary features tagged `boundary=*`.

**Attributes**:
- **Primary**: `boundary`, `admin_level`, `maritime`
- **Detail**: `name`, `ref`, `alt_name`, `short_name`, `official_name`, `wikidata`, `wikipedia`, `protected_area`, `leisure`, `protection_title`, `protect_class`, `operator`, `ownership`, `claimed_by`, `disputed_by`, `recognized_by`

### Buildings

The `buildings` layer contains features from OSM which are tagged `building=*`, which represent permanent enclosed structures.

This layer also contains features tagged `building:part=*` (representing parts of buildings), and features tagged `entrance=*` (building entrances).

**Attributes**:
- **Primary**: `building`, `building:part`, `entrance`
- **Detail**: `name`, `ref`, `alt_name`, `short_name`, `official_name`, `wikidata`, `wikipedia`, `height`, `building:levels`, `building:material`, `roof:material`, `layer`, `level`

Notes
- Heights are quantized at lower zooms to reduce tile sizes

### Clubs

The `clubs` layer contains features from OSM which are tagged `club=*`, which is used for places where organized clubs meet.

**Attributes**:
- **Primary**: `club`, `sport`
- **Detail**: `name`, `ref`, `alt_name`, `short_name`, `official_name`, `wikidata`, `wikipedia`, `access`, `website`, `phone`, `operator`, `wheelchair`

### Craft

The `craft` layer contains features from OSM which are tagged `craft=*`, including workshops, artisan businesses, and manufacturing facilities.

**Attributes**:
- **Primary**: `craft`
- **Detail**: `name`, `ref`, `alt_name`, `short_name`, `official_name`, `wikidata`, `wikipedia`, `website`, `phone`, `product`, `operator`, `level`, `wheelchair`

### Education

The `education` layer contains features from OSM which are tagged `education=*`, including schools, colleges, and universities.

**Attributes**:
- **Primary**: `education`
- **Detail**: `name`, `ref`, `alt_name`, `short_name`, `official_name`, `wikidata`, `wikipedia`, `operator`, `website`, `phone`, `wheelchair`, `isced:level`, `education_profile:general`, `education_level:primary`, `education_level:secondary`, `education_level:preschool`

### Emergency

The `emergency` layer contains features from OSM which are tagged `emergency=*`, including fire hydrants, defibrilator stations, emergency phones, etc.

**Attributes**:
- **Primary**: `emergency`
- **Detail**: `name`, `ref`, `alt_name`, `short_name`, `official_name`, `wikidata`, `wikipedia`, `operator`, `access`, `fire_hydrant:type`, `fire_hydrant:position`, `fire_hydrant:diameter`, `fire_hydrant:pressure`, `water_source`, `couplings`, `colour`, `description`

### Geological

The `geological` layer contains features from OSM which are tagged `geological=*`, which includes various kinds of geological formations.

**Attributes**:
- **Primary**: `geological`
- **Detail**: `name`, `ref`, `alt_name`, `short_name`, `official_name`, `wikidata`, `wikipedia`, `website`, `height`, `ele`, `rock`, `outcrop:type`, `start_date`

### Healthcare

The `healthcare` layer contains features from OSM which are tagged `healthcare=*`, including hospitals, clinics, pharmacies, and other medical facilities.

**Attributes**:
- **Primary**: `healthcare`
- **Detail**: `name`, `ref`, `alt_name`, `short_name`, `official_name`, `wikidata`, `wikipedia`, `healthcare:speciality`, `operator`, `phone`, `website`, `wheelchair`, `emergency`

### Highways

The `highways` layer contains features from OSM which are tagged `highway=*`, including roads, pedestrian paths, and other transportation routes.

This layer also contains features tagged `junction=*`, which is used to map certain kinds of intersections.

This layer **excludes** features tagged `highway=construction` or `highway=proposed`.

**Attributes**:
- **Primary**: `highway`, `expressway`, `junction`
- **Detail**: `name`, `ref`, `surface`, `service`, `junction`, `dual_carriageway`, `motorroad`, `oneway`, `informal`, `operator`, `website`, `access`, `motor_vehicle`, `bicycle`, `foot`, `wheelchair`, `dog`, `supervised`, `lit`, `smoothness`, `sac_scale`, `trail_visibility`, `mtb`, `mtb:scale`, `mtb:scale:imba`, `layer`, `bridge`, `tunnel`, `location`, `covered`, `indoor`

Notes
- Connected highway segments with the same attributes are merged
- Surface tags are categorized into `paved` and `unpaved`

### Historic

The `historic` layer contains features from OSM which are tagged `historic=*`, including archaeological sites, monuments, castles, ruins, and other historically significant features.

**Attributes**:
- **Primary**: `historic`
- **Detail**: `name`, `ref`, `alt_name`, `short_name`, `official_name`, `wikidata`, `wikipedia`, `access`, `aircraft:type`, `amenity`, `archaeological_site`, `building`, `heritage`, `inscription`, `material`, `memorial:type`, `memorial`, `model`, `operator`, `religion`, `ruins`, `start_date`, `tourism`, `website`

### Landcover

The `landcover` layer contains features from OSM which are tagged `landcover=*`, representing ground cover and vegetation areas.

**Attributes**:
- **Primary**: `landcover`
- **Detail**: `name`, `ref`, `alt_name`, `short_name`, `official_name`, `wikidata`, `wikipedia`, `leaf_type`, `leaf_cycle`, `surface`, `height`, `wetland`

### Landuse

The `landuse` layer contains features from OSM which are tagged `landuse=*`, representing how land is used for residential, commercial, industrial, agricultural, and other purposes.

**Attributes**:
- **Primary**: `landuse`
- **Detail**: `name`, `ref`, `alt_name`, `short_name`, `official_name`, `wikidata`, `wikipedia`, `operator`, `website`, `access`, `residential`, `industrial`, `crop`, `produce`, `farmland`, `religion`, `denomination`

### Leisure

The `leisure` layer contains features from OSM which are tagged `leisure=*`, including parks, sports facilities, recreation areas, and entertainment venues.

This layer also contains features tagged `playground=*`, which is used to map parts of playgrounds, and `golf=*`, which is used to map parts of golf courses.

**Attributes**:
- **Primary**: `leisure`, `playground`, `golf`
- **Detail**: `name`, `ref`, `alt_name`, `short_name`, `official_name`, `wikidata`, `wikipedia`, `sport`, `access`, `surface`, `operator`, `website`, `wheelchair`, `lit`, `covered`, `garden:type`, `swimming_pool`

### Man Made

The `man_made` layer contains features from OSM which are tagged `man_made=*`, including towers, bridges, storage tanks, and other artificial structures.

**Attributes**:
- **Primary**: `man_made`
- **Detail**: `name`, `ref`, `alt_name`, `short_name`, `official_name`, `wikidata`, `wikipedia`, `operator`, `tower:type`, `tower:construction`, `surveillance:type`, `camera:type`, `height`, `content`, `substance`, `material`, `utility`, `location`, `usage`, `diameter`

### Military

The `military` layer contains features from OSM which are tagged `military=*`, including military bases, barracks, bunkers, etc.

**Attributes**:
- **Primary**: `military`
- **Detail**: `name`, `ref`, `alt_name`, `short_name`, `official_name`, `wikidata`, `wikipedia`, `building`, `bunker_type`, `landuse`, `historic`, `barrier`, `ruins`, `disused`, `access`, `operator`, `military_service`, `website`, `location`

### Natural

The `natural` layer contains features from OSM which are tagged `natural=*`, including forests, peaks, cliffs, beaches, etc.

This layer **excludes** `natural=water` areas and `natural=coastline` ways, which are instead handled by the [Water](#water) layer.

**Attributes**:
- **Primary**: `natural`
- **Detail**: `name`, `ref`, `alt_name`, `short_name`, `official_name`, `wikidata`, `wikipedia`, `leaf_type`, `leaf_cycle`, `wetland`, `species`, `genus`, `ele`, `height`, `circumference`, `diameter`, `denotation`, `protected_area`, `intermittent`, `seasonal`, `tidal`, `salt`, `surface`, `access`, `operator`

### Offices

The `offices` layer contains features from OSM which are tagged `office=*`, including corporate offices, government offices, NGOs, and other business facilities.

**Attributes**:
- **Primary**: `office`
- **Detail**: `name`, `ref`, `alt_name`, `short_name`, `official_name`, `wikidata`, `wikipedia`, `operator`, `brand`, `phone`, `website`, `email`, `wheelchair`, `internet_access`, `government`

### Pistes

The `pistes` layer contains features from OSM which are tagged `piste:type=*`, including ski runs, cross-country tracks, and other winter sports routes.

**Attributes**:
- **Primary**: `piste:type`
- **Detail**: `name`, `ref`, `alt_name`, `short_name`, `official_name`, `wikidata`, `wikipedia`, `piste:grooming`, `piste:name`, `operator`, `access`, `informal`, `website`

### Places

The `places` layer contains features from OSM which are tagged `place=*`, including countries, states, cities, towns, villages, neighborhoods, and other settlement labels.

**Attributes**:
- **Primary**: `place`, `name`
- **Detail**: `name`, `ref`, `alt_name`, `short_name`, `official_name`, `wikidata`, `wikipedia`, `admin_level`, `ISO3166-1`, `capital`, `population`, `ele`

Notes
- Labels are gridded to ensure a reasonably uniform distribution of labels (preventing too many labels from appearing very close together).
- Labels are also sorted in order of importance, so that client-side label placement can favor more significant places when label text would overlap

### Power

The `power` layer contains features from OSM which are tagged `power=*`, including power lines, substations, and other electrical infrastructure.

**Attributes**:
- **Primary**: `power`, `voltage`
- **Detail**: `name`, `ref`, `alt_name`, `short_name`, `official_name`, `wikidata`, `wikipedia`, `operator`, `frequency`, `cables`, `circuits`, `material`, `design`, `structure`, `location`, `generator:source`, `generator:method`, `generator:type`, `generator:output:electricity`

### Public Transport

The `public_transport` layer contains features from OSM which are tagged `public_transport=*`, including stations, platforms, and other public transportation infrastructure.

**Attributes**:
- **Primary**: `public_transport`
- **Detail**: `name`, `ref`, `alt_name`, `short_name`, `official_name`, `wikidata`, `wikipedia`, `bus`, `train`, `tram`, `trolleybus`, `light_rail`, `subway`, `station`, `highway`, `railway`, `network`, `operator`, `shelter`, `bench`, `bin`, `covered`, `lit`, `tactile_paving`, `wheelchair`, `departures_board`, `passenger_information_display`, `surface`, `network:wikidata`

### Railways

The `railways` layer contains features from OSM which are tagged `railway=*`, including train, tram, and subway tracks, and other rail infrastructure.

**Attributes**:
- **Primary**: `railway`, `service`, `usage`
- **Detail**: `name`, `ref`, `alt_name`, `short_name`, `official_name`, `wikidata`, `wikipedia`, `operator`, `gauge`, `electrified`, `frequency`, `voltage`, `maxspeed`, `railway:track_ref`, `public_transport`, `train`, `subway`, `tram`, `bridge`, `tunnel`, `layer`

### Routes

The `routes` layer contains route relations from OSM, which are used to represent named regular lines of travel. This includes highways (i.e. long distance major roads), train, subway, bus, and ferry routes, and named hiking, biking, and ski routes.

**Attributes**:
- **Primary**: `route`, `type`
- **Detail**: `name`, `ref`, `alt_name`, `short_name`, `official_name`, `wikidata`, `wikipedia`, `network`, `operator`, `colour`, `distance`, `ascent`, `descent`, `roundtrip`, `symbol`, `osmc:symbol`, `from`, `to`, `via`

### Shops

The `shops` layer contains features from OSM which are tagged `shop=*`, representing places that sell retail goods.

**Attributes**:
- **Primary**: `shop`
- **Detail**: `name`, `ref`, `alt_name`, `short_name`, `official_name`, `wikidata`, `wikipedia`, `access`, `wheelchair`, `toilets`, `brand`, `operator`, `beauty`, `clothes`, `male`, `female`, `unisex`, `level`, `indoor`, `cuisine`, `diet:vegetarian`, `diet:vegan`, `diet:halal`, `diet:kosher`, `diet:gluten_free`, `fair_trade`, `organic`, `second_hand`, `self_service`, `service:bicycle:pump`, `service:bicycle:repair`, `service:bicycle:retail`, `service:vehicle:car_repair`, `service:vehicle:tyres`, `trade`, `sport`

### Tourism

The `tourism` layer contains features from OSM which are tagged `tourism=*`, including hotels, museums, theme parks, campgrounds, viewpoints, and other tourist-oriented facilities.

This layer also contains features tagged `attraction=*`, which is used to map attractions at theme parks, water parks, and zoos.

**Attributes**:
- **Primary**: `tourism`, `attraction`
- **Detail**: `name`, `ref`, `alt_name`, `short_name`, `official_name`, `wikidata`, `wikipedia`, `attraction`, `museum`, `artwork_type`, `artist_name`, `information`, `board_type`, `board:title`, `access`, `wheelchair`, `level`, `toilets`, `fee`, `brand`, `operator`, `website`

### Water

The `water` layer contains areas in OSM tagged `natural=water`, as well as ocean polygons derived from preprocessed coastline data.

In OSM, the oceans are not modeled as polygons, because they would be too huge for editing software to work with. Instead, the coastlines are mapped short segments tagged `natural=coastline`. The [OSMCoastline](https://osmcode.org/osmcoastline/) tool can be used to postprocess OSM data and reconstruct the oceans by joining all of these coastlines together.

In Sourdough, this reconstructed ocean polygon is represented in the tiles as if it were a huge multipolygon in OSM tagged `natural=water` + `water=ocean` + `salt=yes`.

**Attributes**:
- **Primary**: `water`, `intermittent`, `seasonal`
- **Detail**: `name`, `ref`, `alt_name`, `short_name`, `official_name`, `wikidata`, `wikipedia`, `layer`, `salt`, `tidal`, `basin`, `boat`, `motorboat`, `canoe`, `swimming`, `fishing`

### Waterways

The `waterways` layer contains features from OSM which are tagged `waterway=*`, including rivers, streams, and canals, as well as water-related infrastructure like dams and locks.

**Attributes**:
- **Primary**: `waterway`
- **Detail**: `name`, `ref`, `alt_name`, `short_name`, `official_name`, `wikidata`, `wikipedia`, `usage`, `layer`, `intermittent`

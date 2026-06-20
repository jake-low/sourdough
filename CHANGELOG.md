# Changelog

This file documents notable changes to the Sourdough schema and its reference implementation.

## v0.3.0

Headline items:
- Add road routes and simplify route minzoom code
- Add lifecycle prefixed features to amenities/buildings/highways/railways/shops
- Add GitHub Action to build and publish a container image
- Add `--additional-languages` option to add alternate language names as separate attributes
- Fix handling of circular railways (default to lines, not areas)

Added additional detail tags:
- amenities: added `private` tag
- barriers: added `tactile_paving` tag
- buildings: added `buiding:colour`, `roof:shape`, and `roof:colour` tags
- highways: added `footway`, `crossing`, `crossing:markings`, `crossing:signals`, `crossing:signed`, `crossing:island`, `tactile_paving`, `button_operated`, `segregated`, `sidewalk`, `sidewalk:left`, `sidewalk:right`
- pistes: added `piste:difficulty` tag
- railways: added `station` tag
- shops: added `pet` tag
- tourism: added `zoo` tag
- waterways: added `bridge`, `tunnel`, `location`, `seasonal`, `tidal`, `material`, `height`, `boat`, `motorboat`, `canoe`, `swimming`, `fishing` tags

Zoom level tweaks:
- Include `aeroway=terminal` at zoom 13
- Include `tourism=information` nodes at lower zooms

Other housekeeping
- Bump planetiler to 0.10.1
- Fix minor issues in schema to match reference implementation

## v0.2.0

- Include `highway=services` elements as areas instead of lines
- Add `building:levels:underground` attribute to the buildings layer
- Upgrade `informal` to a primary attribute on highways
- Add `colour` attribute to public transit routes
- Modify power plant minzooms based on the plant's output (from `plant:output:electricity` tag)
- Tweak minzooms for `aeroway=aerodrome` labels to show major airports at lower zooms
- Add label points for named `place=archipelago`, `place=island`, and `place=islet` areas to the places layer

## v0.1.0

The initial public release of Sourdough.

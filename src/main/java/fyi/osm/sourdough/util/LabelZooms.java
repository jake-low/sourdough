package fyi.osm.sourdough.util;

/**
 * Zoom levels controlling when a point or label point feature appears in the tiles
 * (min, emitted as _minzoom) and when Sourdough recommends general-purpose maps
 * begin displaying its label (rec, emitted as _reczoom).
 *
 * Both values describe a typical small feature of the kind. Node features use them
 * as-is. For area features, both values are lowered together based on the feature's
 * physical size, preserving the spread between them, so that physically large
 * features appear (and are recommended) earlier; see Utils.createLabelPoint. The
 * spread acts as headroom: stylesheets that shift _reczoom earlier by up to
 * (rec - min) zoom levels will find the data present in the tiles.
 */
public record LabelZooms(int min, int rec) {}

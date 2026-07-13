package fyi.osm.sourdough.util;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.geo.GeometryException;
import com.onthegomap.planetiler.reader.SourceFeature;
import fyi.osm.sourdough.Configuration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Utils {

  // Upper bound for _reczoom values and for size-based zoom calculations. Recommended
  // zooms may exceed the tileset maxzoom (stylesheets can use them when overzooming).
  public static final int MAX_LABEL_ZOOM = 20;

  private static final double LOG2 = Math.log(2);

  /**
   * Returns the union of two or more sets as an immutable set.
   */
  @SafeVarargs
  public static <T> Set<T> union(Set<T>... sets) {
    var result = new HashSet<T>();
    for (var set : sets) {
      result.addAll(set);
    }
    return Set.copyOf(result);
  }

  /** Returns a list containing the key followed by each prefix applied to it. */
  public static List<String> withPrefixes(String key, Set<String> prefixes) {
    var result = new ArrayList<String>(1 + prefixes.size());
    result.add(key);
    for (var prefix : prefixes) {
      result.add(prefix + ":" + key);
    }
    return List.copyOf(result);
  }

  /** Returns the value of the first matching tag, or null if none match. */
  public static String getFirstTag(SourceFeature sf, Collection<String> keys) {
    for (var key : keys) {
      var value = sf.getString(key);
      if (value != null) return value;
    }
    return null;
  }

  /** Returns true if the source feature has any of the given tags. */
  public static boolean hasAnyTag(SourceFeature sf, Collection<String> keys) {
    for (var key : keys) {
      if (sf.hasTag(key)) return true;
    }
    return false;
  }

  /**
   * Returns the zoom level at which the feature's geometry reaches the given size in
   * pixels. Unlike GeoUtils.minZoomForPixelSize, the result is not clamped to the max
   * tile zoom, so it can express zooms beyond the end of the tileset.
   */
  public static int minZoomForPixelSize(SourceFeature sf, double pixelSize) {
    try {
      var worldPixels = sf.size() * 256;
      return Math.clamp((int) Math.ceil(Math.log(pixelSize / worldPixels) / LOG2), 0, MAX_LABEL_ZOOM);
    } catch (GeometryException e) {
      return MAX_LABEL_ZOOM;
    }
  }

  /**
   * Creates a label point for an area (or long line) feature, for map makers to place
   * text or icon labels at the feature's center. Adds extra `_minzoom` and `_reczoom`
   * attributes to help stylesheet authors decide when to begin displaying the label.
   */
  public static void createLabelPoint(
    SourceFeature sf,
    FeatureCollector fc,
    String layerName,
    LabelZooms zooms,
    Set<String> primaryTags,
    Set<String> detailTags,
    Configuration config
  ) {
    var sizeZoom = minZoomForPixelSize(sf, 32);
    var recZoom = Math.min(zooms.rec(), sizeZoom);
    var minZoom = Math.max(0, recZoom - (zooms.rec() - zooms.min()));

    var label = fc.pointOnSurface(layerName);
    label.setMinZoom(minZoom);
    label.setBufferPixels(32);

    label.setAttr("_minzoom", minZoom);
    label.setAttr("_reczoom", recZoom);

    AttributeProcessor.setAttributes(sf, label, primaryTags, config);
    AttributeProcessor.setAttributes(sf, label, detailTags, config);
  }

  /**
   * Creates a point feature for a node-mapped feature. The point appears in the tiles
   * at zooms.min() and carries the same _minzoom and _reczoom attributes as area label
   * points (with no size adjustment, since nodes have no area).
   */
  public static void createPoint(
    SourceFeature sf,
    FeatureCollector fc,
    String layerName,
    LabelZooms zooms,
    Set<String> primaryTags,
    Set<String> detailTags,
    Configuration config
  ) {
    var point = fc.point(layerName);
    point.setMinZoom(zooms.min());
    point.setBufferPixels(32);

    point.setAttr("_minzoom", zooms.min());
    point.setAttr("_reczoom", zooms.rec());

    AttributeProcessor.setAttributes(sf, point, primaryTags, config);
    AttributeProcessor.setAttributes(sf, point, detailTags, config);
  }
}

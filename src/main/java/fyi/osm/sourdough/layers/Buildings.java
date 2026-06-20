package fyi.osm.sourdough.layers;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.FeatureMerge;
import com.onthegomap.planetiler.ForwardingProfile.FeatureProcessor;
import com.onthegomap.planetiler.ForwardingProfile.LayerPostProcessor;
import com.onthegomap.planetiler.VectorTile;
import com.onthegomap.planetiler.expression.Expression;
import com.onthegomap.planetiler.geo.GeometryException;
import com.onthegomap.planetiler.reader.SourceFeature;
import fyi.osm.sourdough.Configuration;
import fyi.osm.sourdough.Constants;
import fyi.osm.sourdough.util.AttributeProcessor;
import fyi.osm.sourdough.util.Utils;
import java.util.List;
import java.util.Set;

public class Buildings implements FeatureProcessor, LayerPostProcessor {

  private final Configuration config;

  public Buildings(Configuration config) {
    this.config = config;
  }

  public static final String LAYER_NAME = "buildings";

  public String name() {
    return LAYER_NAME;
  }

  private static final List<String> BUILDING_KEYS = Utils.withPrefixes(
    "building",
    Constants.LIFECYCLE_PREFIXES
  );
  private static final List<String> BUILDING_PART_KEYS = Utils.withPrefixes(
    "building:part",
    Constants.LIFECYCLE_PREFIXES
  );
  private static final List<String> ENTRANCE_KEYS = Utils.withPrefixes(
    "entrance",
    Constants.LIFECYCLE_PREFIXES
  );

  public static final Set<String> TOP_LEVEL_TAGS = Utils.union(
    Set.copyOf(BUILDING_KEYS),
    Set.copyOf(BUILDING_PART_KEYS),
    Set.copyOf(ENTRANCE_KEYS)
  );

  public static final Set<String> PRIMARY_TAGS = Utils.union(TOP_LEVEL_TAGS, Set.of("indoor"));

  public static final Set<String> DETAIL_TAGS = Utils.union(
    Constants.COMMON_DETAIL_TAGS,
    Set.of(
      "height",
      "building:levels",
      "building:levels:underground",
      "building:material",
      "building:colour",
      "roof:shape",
      "roof:material",
      "roof:colour",
      "layer",
      "level",
      "location"
    )
  );

  @Override
  public Expression filter() {
    return Expression.or(
      TOP_LEVEL_TAGS.stream().map(Expression::matchField).toArray(Expression[]::new)
    );
  }

  @Override
  public void processFeature(SourceFeature sf, FeatureCollector fc) {
    if (Utils.hasAnyTag(sf, BUILDING_KEYS)) {
      if (sf.canBePolygon()) {
        processBuildingArea(sf, fc);
      } else if (sf.isPoint()) {
        processBuildingPoint(sf, fc);
      }
      return;
    }

    if (Utils.hasAnyTag(sf, BUILDING_PART_KEYS)) {
      if (sf.canBePolygon()) {
        processBuildingPartArea(sf, fc);
      }
      return;
    }

    if (Utils.hasAnyTag(sf, ENTRANCE_KEYS)) {
      if (sf.isPoint()) {
        processEntrancePoint(sf, fc);
      }
      return;
    }

    // Handle indoor=* features (excluding indoor=yes/no, dependent on buildings)
    // if (sf.hasTag("indoor") && !sf.hasTag("indoor", "yes", "no")) {
    //   if (sf.canBePolygon() && !sf.hasTag("area", "no")) {
    //     processIndoorArea(sf, fc);
    //   } else if (sf.canBeLine()) {
    //     processIndoorLine(sf, fc);
    //   } else if (sf.isPoint()) {
    //     processIndoorPoint(sf, fc);
    //   }
    // }
  }

  private void processBuildingArea(SourceFeature sf, FeatureCollector fc) {
    var polygon = fc.polygon(this.name());
    polygon.setZoomRange(11, 15);
    polygon.setMinPixelSize(1.0);

    AttributeProcessor.setAttributes(sf, polygon, PRIMARY_TAGS, config);

    var detailMinZoom = Math.min(14, polygon.getMinZoomForPixelSize(16));
    AttributeProcessor.setAttributesWithMinzoom(sf, polygon, DETAIL_TAGS, detailMinZoom, config);
  }

  private void processBuildingPoint(SourceFeature sf, FeatureCollector fc) {
    var point = fc.point(this.name());
    point.setMinZoom(14);
    point.setBufferPixels(32);

    AttributeProcessor.setAttributes(sf, point, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, point, DETAIL_TAGS, config);
  }

  private void processBuildingPartArea(SourceFeature sf, FeatureCollector fc) {
    var polygon = fc.polygon(this.name());
    polygon.setZoomRange(13, 15);
    polygon.setMinPixelSize(0.5);

    AttributeProcessor.setAttributes(sf, polygon, PRIMARY_TAGS, config);

    var detailMinZoom = Math.min(14, polygon.getMinZoomForPixelSize(8));
    AttributeProcessor.setAttributesWithMinzoom(sf, polygon, DETAIL_TAGS, detailMinZoom, config);
  }

  private void processEntrancePoint(SourceFeature sf, FeatureCollector fc) {
    var point = fc.point(this.name());
    point.setMinZoom(15);
    point.setBufferPixels(16);

    AttributeProcessor.setAttributes(sf, point, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, point, DETAIL_TAGS, config);
  }

  /*
  private void processIndoorArea(SourceFeature sf, FeatureCollector fc) {
    var polygon = fc.polygon(this.name());
    polygon.setMinZoom(15);
    polygon.setMinPixelSize(0.25);

    AttributeProcessor.setAttributes(sf, polygon, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, polygon, DETAIL_TAGS, config);
  }

  private void processIndoorLine(SourceFeature sf, FeatureCollector fc) {
    var line = fc.line(this.name());
    line.setMinZoom(15);
    line.setMinPixelSize(0.25);

    AttributeProcessor.setAttributes(sf, line, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, line, DETAIL_TAGS, config);
  }

  private void processIndoorPoint(SourceFeature sf, FeatureCollector fc) {
    var point = fc.point(this.name());
    point.setMinZoom(15);
    point.setBufferPixels(8);

    AttributeProcessor.setAttributes(sf, point, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, point, DETAIL_TAGS, config);
  }
  */

  @Override
  public List<VectorTile.Feature> postProcess(int zoom, List<VectorTile.Feature> items)
    throws GeometryException {
    // Quantize height values (reduces tile sizes and facilitates better merging)
    for (var item : items) {
      var height = item.tags().get("height");
      if (height instanceof Number num && num.doubleValue() > 0) {
        item.tags().put("height", quantizeHeight(num.doubleValue(), zoom));
      }
    }

    items = FeatureMerge.mergeMultiPoint(items);
    if (zoom < 15) {
      items = FeatureMerge.mergeNearbyPolygons(items, 3.0, 3.0, 0.5, 0.5);
    } else {
      items = FeatureMerge.mergeMultiPolygon(items);
    }

    return items;
  }

  private static int quantizeHeight(double height, int zoom) {
    int step = switch (zoom) {
      case 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 -> 20;
      case 13 -> 10;
      case 14 -> 5;
      default -> 1;
    };
    return height < step ? step : (int) Math.round(height / step) * step;
  }
}

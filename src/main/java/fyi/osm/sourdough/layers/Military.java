package fyi.osm.sourdough.layers;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.ForwardingProfile.FeatureProcessor;
import com.onthegomap.planetiler.ForwardingProfile.LayerPostProcessor;
import com.onthegomap.planetiler.expression.Expression;
import com.onthegomap.planetiler.reader.SourceFeature;
import fyi.osm.sourdough.Configuration;
import fyi.osm.sourdough.Constants;
import fyi.osm.sourdough.util.AttributeProcessor;
import fyi.osm.sourdough.util.Utils;
import java.util.Set;

public class Military implements FeatureProcessor {

  private final Configuration config;

  public Military(Configuration config) {
    this.config = config;
  }

  public static final String LAYER_NAME = "military";

  public String name() {
    return LAYER_NAME;
  }

  public static final Set<String> PRIMARY_TAGS = Set.of("military");

  public static final Set<String> DETAIL_TAGS = Utils.union(
    Constants.COMMON_DETAIL_TAGS,
    Set.of(
      "building",
      "bunker_type",
      "landuse",
      "historic",
      "barrier",
      "ruins",
      "disused",
      "access",
      "operator",
      "military_service",
      "ref",
      "website",
      "location"
    )
  );

  @Override
  public Expression filter() {
    return Expression.matchField("military");
  }

  @Override
  public void processFeature(SourceFeature sf, FeatureCollector fc) {
    if (sf.hasTag("military", "yes", "no")) {
      return;
    }

    if (sf.canBePolygon()) {
      processMilitaryArea(sf, fc);
    } else if (sf.isPoint()) {
      processMilitaryPoint(sf, fc);
    }
  }

  private void processMilitaryArea(SourceFeature sf, FeatureCollector fc) {
    var polygon = fc.polygon(this.name());
    polygon.setZoomRange(2, 15);
    polygon.setMinPixelSize(4.0);

    AttributeProcessor.setAttributes(sf, polygon, PRIMARY_TAGS, config);

    var detailMinZoom = Math.min(getLabelMinZoom(sf), polygon.getMinZoomForPixelSize(64));
    AttributeProcessor.setAttributesWithMinzoom(sf, polygon, DETAIL_TAGS, detailMinZoom, config);

    if (sf.hasTag("name")) {
      var label = fc.pointOnSurface(this.name());
      label.setMinZoom(detailMinZoom);
      label.setBufferPixels(32);

      AttributeProcessor.setAttributes(sf, label, PRIMARY_TAGS, config);
      AttributeProcessor.setAttributes(sf, label, DETAIL_TAGS, config);
    }
  }

  private void processMilitaryPoint(SourceFeature sf, FeatureCollector fc) {
    var point = fc.point(this.name());
    point.setMinZoom(getLabelMinZoom(sf));
    point.setBufferPixels(32);

    AttributeProcessor.setAttributes(sf, point, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, point, DETAIL_TAGS, config);
  }

  private int getLabelMinZoom(SourceFeature sf) {
    return switch (sf.getString("military")) {
      case "base", "naval_base", "airfield" -> 9;
      case "barracks", "training_area", "range", "danger_area" -> 11;
      case "checkpoint", "office", "depot", "nuclear_explosion_site" -> 12;
      case "bunker", "trench", "obstacle_course", "guard_house" -> 14;
      default -> 13;
    };
  }
}

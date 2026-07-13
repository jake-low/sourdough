package fyi.osm.sourdough.layers;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.ForwardingProfile.FeatureProcessor;
import com.onthegomap.planetiler.ForwardingProfile.LayerPostProcessor;
import com.onthegomap.planetiler.expression.Expression;
import com.onthegomap.planetiler.reader.SourceFeature;
import fyi.osm.sourdough.Configuration;
import fyi.osm.sourdough.Constants;
import fyi.osm.sourdough.util.AttributeProcessor;
import fyi.osm.sourdough.util.LabelZooms;
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

    var detailMinZoom = Math.min(getLabelZooms(sf).min(), polygon.getMinZoomForPixelSize(64));
    AttributeProcessor.setAttributesWithMinzoom(sf, polygon, DETAIL_TAGS, detailMinZoom, config);

    if (sf.hasTag("name")) {
      Utils.createLabelPoint(sf, fc, this.name(), getLabelZooms(sf), PRIMARY_TAGS, DETAIL_TAGS, config);
    }
  }

  private void processMilitaryPoint(SourceFeature sf, FeatureCollector fc) {
    Utils.createPoint(sf, fc, this.name(), getLabelZooms(sf), PRIMARY_TAGS, DETAIL_TAGS, config);
  }

  private LabelZooms getLabelZooms(SourceFeature sf) {
    return switch (sf.getString("military")) {
      case "base", "naval_base", "airfield" -> new LabelZooms(9, 12);
      case "barracks", "training_area", "range", "danger_area" -> new LabelZooms(11, 13);
      case "checkpoint", "office", "depot", "nuclear_explosion_site" -> new LabelZooms(12, 14);
      case "bunker", "trench", "obstacle_course", "guard_house" -> new LabelZooms(14, 16);
      default -> new LabelZooms(13, 15);
    };
  }
}

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

public class Emergency implements FeatureProcessor {

  private final Configuration config;

  public Emergency(Configuration config) {
    this.config = config;
  }

  public static final String LAYER_NAME = "emergency";

  public String name() {
    return LAYER_NAME;
  }

  public static final Set<String> PRIMARY_TAGS = Set.of("emergency");

  public static final Set<String> DETAIL_TAGS = Utils.union(
    Constants.COMMON_DETAIL_TAGS,
    Set.of(
      "operator",
      "ref",
      "access",
      "fire_hydrant:type",
      "fire_hydrant:position",
      "fire_hydrant:diameter",
      "fire_hydrant:pressure",
      "water_source",
      "couplings",
      "colour",
      "description"
    )
  );

  @Override
  public Expression filter() {
    return Expression.matchField("emergency");
  }

  @Override
  public void processFeature(SourceFeature sf, FeatureCollector fc) {
    if (sf.hasTag("emergency", "yes", "no")) {
      return;
    }

    if (sf.canBePolygon()) {
      processEmergencyArea(sf, fc);
    } else if (sf.canBeLine()) {
      processEmergencyLine(sf, fc);
    } else if (sf.isPoint()) {
      processEmergencyPoint(sf, fc);
    }
  }

  private void processEmergencyArea(SourceFeature sf, FeatureCollector fc) {
    var polygon = fc.polygon(this.name());
    polygon.setZoomRange(2, 15);
    polygon.setMinPixelSize(2.0);

    AttributeProcessor.setAttributes(sf, polygon, PRIMARY_TAGS, config);

    var detailMinZoom = Math.min(getLabelZooms(sf).min(), polygon.getMinZoomForPixelSize(32));
    AttributeProcessor.setAttributesWithMinzoom(sf, polygon, DETAIL_TAGS, detailMinZoom, config);

    Utils.createLabelPoint(sf, fc, this.name(), getLabelZooms(sf), PRIMARY_TAGS, DETAIL_TAGS, config);
  }

  private void processEmergencyLine(SourceFeature sf, FeatureCollector fc) {
    var line = fc.line(this.name());
    line.setZoomRange(2, 15);
    line.setMinPixelSize(1.0);

    AttributeProcessor.setAttributes(sf, line, PRIMARY_TAGS, config);

    var detailMinZoom = line.getMinZoomForPixelSize(32);
    AttributeProcessor.setAttributesWithMinzoom(sf, line, DETAIL_TAGS, detailMinZoom, config);
  }

  private void processEmergencyPoint(SourceFeature sf, FeatureCollector fc) {
    Utils.createPoint(sf, fc, this.name(), getLabelZooms(sf), PRIMARY_TAGS, DETAIL_TAGS, config);
  }

  private LabelZooms getLabelZooms(SourceFeature sf) {
    return switch (sf.getString("emergency")) {
      case
        "ambulance_station",
        "disaster_response",
        "fire_lookout",
        "mountain_rescue",
        "water_rescue" -> new LabelZooms(13, 14);
      case "lifeguard", "assembly_point" -> new LabelZooms(14, 15);
      default -> new LabelZooms(15, 17);
    };
  }
}

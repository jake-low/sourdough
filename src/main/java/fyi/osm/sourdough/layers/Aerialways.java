package fyi.osm.sourdough.layers;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.ForwardingProfile.FeatureProcessor;
import com.onthegomap.planetiler.expression.Expression;
import com.onthegomap.planetiler.reader.SourceFeature;
import fyi.osm.sourdough.Configuration;
import fyi.osm.sourdough.Constants;
import fyi.osm.sourdough.util.AttributeProcessor;
import fyi.osm.sourdough.util.LabelZooms;
import fyi.osm.sourdough.util.Utils;
import java.util.Set;

public class Aerialways implements FeatureProcessor {

  private final Configuration config;

  public Aerialways(Configuration config) {
    this.config = config;
  }

  public static final String LAYER_NAME = "aerialways";

  public String name() {
    return LAYER_NAME;
  }

  public static final Set<String> PRIMARY_TAGS = Set.of("aerialway");

  public static final Set<String> DETAIL_TAGS = Utils.union(
    Constants.COMMON_DETAIL_TAGS,
    Set.of(
      "access",
      "aerialway:access",
      "aerialway:capacity",
      "aerialway:duration",
      "aerialway:occupancy",
      "ele",
      "fee",
      "oneway",
      "operator",
      "wheelchair"
    )
  );

  @Override
  public Expression filter() {
    return Expression.matchField("aerialway");
  }

  @Override
  public void processFeature(SourceFeature sf, FeatureCollector fc) {
    var isArea = sf.hasTag("area", "yes") || sf.hasTag("aerialway", "station");

    if (sf.canBePolygon() && isArea) {
      processAerialwayArea(sf, fc);
    } else if (sf.canBeLine()) {
      processAerialwayLine(sf, fc);
    } else if (sf.isPoint()) {
      processAerialwayPoint(sf, fc);
    }
  }

  private void processAerialwayArea(SourceFeature sf, FeatureCollector fc) {
    var polygon = fc.polygon(this.name());
    polygon.setZoomRange(2, 15);
    polygon.setMinPixelSize(2.0);

    AttributeProcessor.setAttributes(sf, polygon, PRIMARY_TAGS, config);

    var detailMinZoom = Math.min(14, polygon.getMinZoomForPixelSize(32));
    AttributeProcessor.setAttributesWithMinzoom(sf, polygon, DETAIL_TAGS, detailMinZoom, config);

    Utils.createLabelPoint(sf, fc, this.name(), getLabelZooms(sf), PRIMARY_TAGS, DETAIL_TAGS, config);
  }

  private void processAerialwayLine(SourceFeature sf, FeatureCollector fc) {
    var line = fc.line(this.name());
    line.setZoomRange(2, 15);
    line.setMinPixelSize(16.0);

    AttributeProcessor.setAttributes(sf, line, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, line, DETAIL_TAGS, config);
  }

  private void processAerialwayPoint(SourceFeature sf, FeatureCollector fc) {
    Utils.createPoint(sf, fc, this.name(), getLabelZooms(sf), PRIMARY_TAGS, DETAIL_TAGS, config);
  }

  private LabelZooms getLabelZooms(SourceFeature sf) {
    return sf.hasTag("aerialway", "station") ? new LabelZooms(14, 15) : new LabelZooms(14, 16);
  }
}

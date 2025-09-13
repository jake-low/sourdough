package fyi.osm.sourdough.layers;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.ForwardingProfile.FeatureProcessor;
import com.onthegomap.planetiler.expression.Expression;
import com.onthegomap.planetiler.reader.SourceFeature;
import fyi.osm.sourdough.Configuration;
import fyi.osm.sourdough.Constants;
import fyi.osm.sourdough.util.AttributeProcessor;
import fyi.osm.sourdough.util.Utils;
import java.util.Set;

public class Clubs implements FeatureProcessor {

  private final Configuration config;

  public Clubs(Configuration config) {
    this.config = config;
  }

  public static final String LAYER_NAME = "clubs";

  public String name() {
    return LAYER_NAME;
  }

  public static final Set<String> PRIMARY_TAGS = Set.of("club", "sport");

  public static final Set<String> DETAIL_TAGS = Utils.union(
    Constants.COMMON_DETAIL_TAGS,
    Set.of("access", "website", "phone", "operator", "wheelchair")
  );

  @Override
  public Expression filter() {
    return Expression.matchField("club");
  }

  @Override
  public void processFeature(SourceFeature sf, FeatureCollector fc) {
    if (sf.canBePolygon()) {
      processClubArea(sf, fc);
    } else if (sf.isPoint()) {
      processClubPoint(sf, fc);
    }
  }

  private void processClubArea(SourceFeature sf, FeatureCollector fc) {
    var polygon = fc.polygon(this.name());
    polygon.setZoomRange(8, 15);
    polygon.setMinPixelSize(4.0);

    AttributeProcessor.setAttributes(sf, polygon, PRIMARY_TAGS, config);

    var detailMinZoom = Math.min(getLabelMinZoom(sf), polygon.getMinZoomForPixelSize(32));
    AttributeProcessor.setAttributesWithMinzoom(sf, polygon, DETAIL_TAGS, detailMinZoom, config);

    var point = fc.pointOnSurface(this.name());
    point.setMinZoom(detailMinZoom);
    point.setBufferPixels(32);

    AttributeProcessor.setAttributes(sf, point, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, point, DETAIL_TAGS, config);
  }

  private void processClubPoint(SourceFeature sf, FeatureCollector fc) {
    var point = fc.point(this.name());
    point.setMinZoom(getLabelMinZoom(sf));
    point.setBufferPixels(32);

    AttributeProcessor.setAttributes(sf, point, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, point, DETAIL_TAGS, config);
  }

  private int getLabelMinZoom(SourceFeature sf) {
    return switch (sf.getString("club")) {
      case "sport" -> sf.hasTag("sport", "golf") ? 11 : 12;
      case "scout", "youth" -> 12;
      case "social", "culture", "veterans" -> 13;
      case "music", "sailing", "automobile", "motorcycle" -> 13;
      case "freemasonry" -> 14;
      default -> 13;
    };
  }
}

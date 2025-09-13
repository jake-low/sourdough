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

public class Historic implements FeatureProcessor {

  private final Configuration config;

  public Historic(Configuration config) {
    this.config = config;
  }

  public static final String LAYER_NAME = "historic";

  public String name() {
    return LAYER_NAME;
  }

  public static final Set<String> PRIMARY_TAGS = Set.of("historic");

  public static final Set<String> DETAIL_TAGS = Utils.union(
    Constants.COMMON_DETAIL_TAGS,
    Set.of(
      "access",
      "aircraft:type",
      "amenity",
      "archaeological_site",
      "building",
      "heritage",
      "inscription",
      "material",
      "memorial:type",
      "memorial",
      "model",
      "operator",
      "religion",
      "ruins",
      "start_date",
      "tourism",
      "website"
    )
  );

  @Override
  public Expression filter() {
    return Expression.matchField("historic");
  }

  @Override
  public void processFeature(SourceFeature sf, FeatureCollector fc) {
    if (sf.hasTag("historic", "yes", "no")) {
      return;
    }

    if (sf.canBePolygon()) {
      processHistoricArea(sf, fc);
    } else if (sf.canBeLine()) {
      processHistoricLine(sf, fc);
    } else if (sf.isPoint()) {
      processHistoricPoint(sf, fc);
    }
  }

  private void processHistoricArea(SourceFeature sf, FeatureCollector fc) {
    var polygon = fc.polygon(this.name());
    polygon.setZoomRange(2, 15);
    polygon.setMinPixelSize(4.0);

    AttributeProcessor.setAttributes(sf, polygon, PRIMARY_TAGS, config);

    var detailMinZoom = Math.min(getLabelMinZoom(sf), polygon.getMinZoomForPixelSize(32));
    AttributeProcessor.setAttributesWithMinzoom(sf, polygon, DETAIL_TAGS, detailMinZoom, config);

    if (sf.hasTag("name")) {
      var label = fc.pointOnSurface(this.name());
      label.setMinZoom(detailMinZoom);
      label.setBufferPixels(32);

      AttributeProcessor.setAttributes(sf, label, PRIMARY_TAGS, config);
      AttributeProcessor.setAttributes(sf, label, DETAIL_TAGS, config);
    }
  }

  private void processHistoricLine(SourceFeature sf, FeatureCollector fc) {
    var line = fc.line(this.name());
    line.setMinZoom(12);
    line.setMinPixelSize(1.0);
    line.setBufferPixels(4);

    AttributeProcessor.setAttributes(sf, line, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, line, DETAIL_TAGS, config);
  }

  private void processHistoricPoint(SourceFeature sf, FeatureCollector fc) {
    var point = fc.point(this.name());
    point.setMinZoom(getLabelMinZoom(sf));
    point.setBufferPixels(32);

    AttributeProcessor.setAttributes(sf, point, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, point, DETAIL_TAGS, config);
  }

  private int getLabelMinZoom(SourceFeature sf) {
    return switch (sf.getString("historic")) {
      case "castle", "fort", "archaeological_site" -> 11;
      case "monument", "battlefield", "palace" -> 12;
      case "ruins", "tomb", "manor", "building", "church", "mine", "tower", "windmill" -> 13;
      default -> 14;
    };
  }
}

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

public class Shops implements FeatureProcessor {

  private final Configuration config;

  public Shops(Configuration config) {
    this.config = config;
  }

  public static final String LAYER_NAME = "shops";

  public String name() {
    return LAYER_NAME;
  }

  public static final Set<String> PRIMARY_TAGS = Set.of("shop");

  public static final Set<String> DETAIL_TAGS = Utils.union(
    Constants.COMMON_DETAIL_TAGS,
    Set.of(
      "access",
      "beauty",
      "brand",
      "clothes",
      "cuisine",
      "diet:gluten_free",
      "diet:halal",
      "diet:kosher",
      "diet:vegan",
      "diet:vegetarian",
      "fair_trade",
      "female",
      "indoor",
      "level",
      "male",
      "operator",
      "organic",
      "second_hand",
      "self_service",
      "service:bicycle:pump",
      "service:bicycle:repair",
      "service:bicycle:retail",
      "service:vehicle:car_repair",
      "service:vehicle:tyres",
      "sport",
      "toilets",
      "trade",
      "unisex",
      "wheelchair"
    )
  );

  @Override
  public Expression filter() {
    return Expression.matchField("shop");
  }

  @Override
  public void processFeature(SourceFeature sf, FeatureCollector fc) {
    if (sf.canBePolygon()) {
      processShopArea(sf, fc);
    } else if (sf.isPoint()) {
      processShopPoint(sf, fc);
    }
  }

  private void processShopArea(SourceFeature sf, FeatureCollector fc) {
    var polygon = fc.polygon(this.name());
    polygon.setZoomRange(2, 15);
    polygon.setMinPixelSize(2.0);

    AttributeProcessor.setAttributes(sf, polygon, PRIMARY_TAGS, config);

    var detailMinZoom = Math.min(getLabelMinZoom(sf), polygon.getMinZoomForPixelSize(32));
    AttributeProcessor.setAttributesWithMinzoom(sf, polygon, DETAIL_TAGS, detailMinZoom, config);

    var label = fc.pointOnSurface(this.name());
    label.setMinZoom(detailMinZoom);
    label.setBufferPixels(32);

    AttributeProcessor.setAttributes(sf, label, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, label, DETAIL_TAGS, config);
  }

  private void processShopPoint(SourceFeature sf, FeatureCollector fc) {
    var point = fc.point(this.name());
    point.setMinZoom(getLabelMinZoom(sf));
    point.setBufferPixels(32);

    AttributeProcessor.setAttributes(sf, point, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, point, DETAIL_TAGS, config);
  }

  private int getLabelMinZoom(SourceFeature sf) {
    return switch (sf.getString("shop")) {
      case "supermarket", "department_store" -> 13;
      default -> 14;
    };
  }
}

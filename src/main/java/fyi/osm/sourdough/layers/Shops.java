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
import java.util.List;
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

  private static final List<String> SHOP_KEYS = Utils.withPrefixes(
    "shop",
    Constants.LIFECYCLE_PREFIXES
  );

  public static final Set<String> TOP_LEVEL_TAGS = Set.copyOf(SHOP_KEYS);

  public static final Set<String> PRIMARY_TAGS = TOP_LEVEL_TAGS;

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
      "pet",
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
    return Expression.or(
      TOP_LEVEL_TAGS.stream().map(Expression::matchField).toArray(Expression[]::new)
    );
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

    var detailMinZoom = Math.min(getLabelZooms(sf).min(), polygon.getMinZoomForPixelSize(32));
    AttributeProcessor.setAttributesWithMinzoom(sf, polygon, DETAIL_TAGS, detailMinZoom, config);

    Utils.createLabelPoint(sf, fc, this.name(), getLabelZooms(sf), PRIMARY_TAGS, DETAIL_TAGS, config);
  }

  private void processShopPoint(SourceFeature sf, FeatureCollector fc) {
    Utils.createPoint(sf, fc, this.name(), getLabelZooms(sf), PRIMARY_TAGS, DETAIL_TAGS, config);
  }

  private LabelZooms getLabelZooms(SourceFeature sf) {
    return switch (Utils.getFirstTag(sf, SHOP_KEYS)) {
      case "supermarket", "department_store" -> new LabelZooms(13, 14);
      default -> new LabelZooms(14, 15);
    };
  }
}

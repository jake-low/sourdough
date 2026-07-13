package fyi.osm.sourdough.layers;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.FeatureMerge;
import com.onthegomap.planetiler.ForwardingProfile.FeatureProcessor;
import com.onthegomap.planetiler.ForwardingProfile.LayerPostProcessor;
import com.onthegomap.planetiler.Profile;
import com.onthegomap.planetiler.VectorTile;
import com.onthegomap.planetiler.expression.Expression;
import com.onthegomap.planetiler.geo.GeometryException;
import com.onthegomap.planetiler.reader.SourceFeature;
import fyi.osm.sourdough.Configuration;
import fyi.osm.sourdough.Constants;
import fyi.osm.sourdough.util.AttributeProcessor;
import fyi.osm.sourdough.util.LabelZooms;
import fyi.osm.sourdough.util.Utils;
import java.util.List;
import java.util.Set;

public class Natural implements FeatureProcessor, LayerPostProcessor {

  private final Configuration config;

  public Natural(Configuration config) {
    this.config = config;
  }

  public static final String LAYER_NAME = "natural";

  public String name() {
    return LAYER_NAME;
  }

  public static final Set<String> PRIMARY_TAGS = Set.of("natural");

  public static final Set<String> DETAIL_TAGS = Utils.union(
    Constants.COMMON_DETAIL_TAGS,
    Set.of(
      "leaf_type",
      "leaf_cycle",
      "wetland",
      "species",
      "genus",
      "ele",
      "height",
      "circumference",
      "diameter",
      "denotation",
      "protected_area",
      "intermittent",
      "seasonal",
      "tidal",
      "salt",
      "surface",
      "access",
      "operator"
    )
  );

  @Override
  public Expression filter() {
    return Expression.and(
      Expression.matchField("natural"),
      // Exclude natural=water and natural=coastline features (handled by Water.java layer)
      Expression.not(Expression.matchAny("natural", "water", "coastline"))
    );
  }

  @Override
  public void processFeature(SourceFeature sf, FeatureCollector fc) {
    if (sf.canBePolygon()) {
      processNaturalArea(sf, fc);
    } else if (sf.canBeLine()) {
      processNaturalLine(sf, fc);
    } else if (sf.isPoint()) {
      processNaturalPoint(sf, fc);
    }
  }

  private void processNaturalArea(SourceFeature sf, FeatureCollector fc) {
    var polygon = fc.polygon(this.name());
    polygon.setZoomRange(2, 15);
    polygon.setPixelTolerance(0.5);

    AttributeProcessor.setAttributes(sf, polygon, PRIMARY_TAGS, config);

    var detailMinZoom = Math.min(getLabelZooms(sf).min(), polygon.getMinZoomForPixelSize(32));
    AttributeProcessor.setAttributesWithMinzoom(sf, polygon, DETAIL_TAGS, detailMinZoom, config);

    if (sf.hasTag("name")) {
      Utils.createLabelPoint(sf, fc, this.name(), getLabelZooms(sf), PRIMARY_TAGS, DETAIL_TAGS, config);
    }
  }

  private void processNaturalLine(SourceFeature sf, FeatureCollector fc) {
    var line = fc.line(this.name());
    line.setMinZoom(getLineMinZoom(sf));
    line.setBufferPixels(8);

    AttributeProcessor.setAttributes(sf, line, PRIMARY_TAGS, config);

    var detailMinZoom = line.getMinZoomForPixelSize(64);
    AttributeProcessor.setAttributesWithMinzoom(sf, line, DETAIL_TAGS, detailMinZoom, config);
  }

  private void processNaturalPoint(SourceFeature sf, FeatureCollector fc) {
    Utils.createPoint(sf, fc, this.name(), getLabelZooms(sf), PRIMARY_TAGS, DETAIL_TAGS, config);
  }

  private int getLineMinZoom(SourceFeature sf) {
    return 10; // TODO
  }

  private LabelZooms getLabelZooms(SourceFeature sf) {
    return switch (sf.getString("natural")) {
      case "peak", "volcano" -> sf.hasTag("name")
        ? new LabelZooms(10, 11)
        : new LabelZooms(11, 12);
      case "bay", "spring", "strait" -> new LabelZooms(12, 13);
      case "cave_entrance", "saddle" -> new LabelZooms(13, 14);
      case "tree", "shrub", "stone" -> sf.hasTag("name")
        ? new LabelZooms(14, 16)
        : new LabelZooms(15, 17);
      case
        "wood",
        "scrub",
        "heath",
        "wetland",
        "beach",
        "glacier",
        "sand",
        "grassland",
        "bare_rock",
        "scree",
        "shingle",
        "fell",
        "tundra" -> new LabelZooms(14, 17);
      default -> new LabelZooms(14, 15);
    };
  }

  @Override
  public List<VectorTile.Feature> postProcess(int zoom, List<VectorTile.Feature> items)
    throws GeometryException {
    items = FeatureMerge.mergeMultiPoint(items);
    items = FeatureMerge.mergeNearbyPolygons(items, 3.0, 3.0, 0.5, 0.5);
    items = FeatureMerge.mergeLineStrings(items, 5.0, 0.25, 8);
    return items;
  }
}

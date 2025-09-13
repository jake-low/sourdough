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

public class Barriers implements FeatureProcessor, LayerPostProcessor {

  private final Configuration config;

  public Barriers(Configuration config) {
    this.config = config;
  }

  public static final String LAYER_NAME = "barriers";

  public String name() {
    return LAYER_NAME;
  }

  public static final Set<String> PRIMARY_TAGS = Set.of("barrier", "fence_type", "wall", "kerb");

  public static final Set<String> DETAIL_TAGS = Utils.union(
    Constants.COMMON_DETAIL_TAGS,
    Set.of("material", "height", "access", "wheelchair")
  );

  @Override
  public Expression filter() {
    return Expression.matchField("barrier");
  }

  @Override
  public void processFeature(SourceFeature sf, FeatureCollector fc) {
    if (sf.canBeLine()) {
      processBarrierLine(sf, fc);
    } else if (sf.isPoint()) {
      processBarrierPoint(sf, fc);
    }
  }

  private void processBarrierLine(SourceFeature sf, FeatureCollector fc) {
    var line = fc.line(this.name());
    line.setMinZoom(13);
    line.setMinPixelSize(0.5);
    line.setBufferPixels(4);

    AttributeProcessor.setAttributes(sf, line, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, line, DETAIL_TAGS, config);
  }

  private void processBarrierPoint(SourceFeature sf, FeatureCollector fc) {
    var point = fc.point(this.name());
    point.setMinZoom(getLabelMinZoom(sf));
    point.setBufferPixels(32);

    AttributeProcessor.setAttributes(sf, point, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, point, DETAIL_TAGS, config);
  }

  private int getLabelMinZoom(SourceFeature sf) {
    return switch (sf.getString("barrier")) {
      case "border_control", "toll_booth" -> 11;
      case "gate", "lift_gate", "swing_gate" -> 13;
      case "block", "motorcycle_barrier" -> 14;
      default -> 15;
    };
  }

  @Override
  public List<VectorTile.Feature> postProcess(int zoom, List<VectorTile.Feature> items)
    throws GeometryException {
    items = FeatureMerge.mergeMultiPoint(items);
    items = FeatureMerge.mergeLineStrings(items, 5.0, 0.25, 8);
    items = FeatureMerge.mergeNearbyPolygons(items, 3.0, 3.0, 0.5, 0.5);
    return items;
  }
}

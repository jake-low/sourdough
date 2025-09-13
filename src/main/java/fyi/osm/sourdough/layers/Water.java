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

public class Water implements FeatureProcessor, LayerPostProcessor {

  private final Configuration config;

  public Water(Configuration config) {
    this.config = config;
  }

  public static final String LAYER_NAME = "water";

  public String name() {
    return LAYER_NAME;
  }

  public static final Set<String> PRIMARY_TAGS = Set.of("water", "intermittent", "seasonal");

  public static final Set<String> DETAIL_TAGS = Utils.union(
    Constants.COMMON_DETAIL_TAGS,
    Set.of(
      "ref",
      "layer",
      "salt",
      "tidal",
      "basin",
      "boat",
      "motorboat",
      "canoe",
      "swimming",
      "fishing"
    )
  );

  @Override
  public Expression filter() {
    return Expression.matchAny("natural", "water");
  }

  @Override
  public void processFeature(SourceFeature sf, FeatureCollector fc) {
    if (!sf.canBePolygon()) {
      return;
    }

    this.processWaterArea(sf, fc);
  }

  private void processWaterArea(SourceFeature sf, FeatureCollector fc) {
    var polygon = fc.polygon(this.name());
    polygon.setMinPixelSize(1.0);
    polygon.setBufferPixels(8);

    AttributeProcessor.setAttributes(sf, polygon, PRIMARY_TAGS, config);

    var detailMinZoom = Math.min(15, polygon.getMinZoomForPixelSize(32));
    AttributeProcessor.setAttributesWithMinzoom(sf, polygon, DETAIL_TAGS, detailMinZoom, config);

    if (sf.hasTag("name") || sf.hasTag("ref")) {
      var label = fc.pointOnSurface(this.name());
      label.setMinZoom(detailMinZoom);
      label.setBufferPixels(32);

      AttributeProcessor.setAttributes(sf, label, PRIMARY_TAGS, config);
      AttributeProcessor.setAttributes(sf, label, DETAIL_TAGS, config);
    }
  }

  public void processPreparedOsm(SourceFeature _sf, FeatureCollector fc) {
    fc
      .polygon(this.name())
      .setId(-1)
      .setAttr("water", "ocean")
      .setAttr("salt", "yes")
      .setZoomRange(0, 15)
      .setBufferPixels(8);
  }

  @Override
  public List<VectorTile.Feature> postProcess(int zoom, List<VectorTile.Feature> items)
    throws GeometryException {
    return FeatureMerge.mergeOverlappingPolygons(items, 1);
  }
}

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

public class Landcover implements FeatureProcessor, LayerPostProcessor {

  private final Configuration config;

  public Landcover(Configuration config) {
    this.config = config;
  }

  public static final String LAYER_NAME = "landcover";

  public String name() {
    return LAYER_NAME;
  }

  public static final Set<String> PRIMARY_TAGS = Set.of("landcover");

  public static final Set<String> DETAIL_TAGS = Utils.union(
    Constants.COMMON_DETAIL_TAGS,
    Set.of("leaf_type", "leaf_cycle", "surface", "height", "wetland")
  );

  @Override
  public Expression filter() {
    return Expression.matchField("landcover");
  }

  @Override
  public void processFeature(SourceFeature sf, FeatureCollector fc) {
    if (!sf.canBePolygon()) {
      return;
    }

    this.processLandcoverArea(sf, fc);
  }

  private void processLandcoverArea(SourceFeature sf, FeatureCollector fc) {
    var polygon = fc.polygon(this.name());
    polygon.setZoomRange(6, 15);
    polygon.setMinPixelSize(8.0);

    AttributeProcessor.setAttributes(sf, polygon, PRIMARY_TAGS, config);

    var detailMinZoom = polygon.getMinZoomForPixelSize(64);
    AttributeProcessor.setAttributesWithMinzoom(sf, polygon, DETAIL_TAGS, detailMinZoom, config);

    if (sf.hasTag("name")) {
      var label = fc.pointOnSurface(this.name());
      label.setMinZoom(Math.min(14, detailMinZoom));
      label.setBufferPixels(32);

      AttributeProcessor.setAttributes(sf, label, PRIMARY_TAGS, config);
      AttributeProcessor.setAttributes(sf, label, DETAIL_TAGS, config);
    }
  }

  @Override
  public List<VectorTile.Feature> postProcess(int zoom, List<VectorTile.Feature> items)
    throws GeometryException {
    return FeatureMerge.mergeNearbyPolygons(items, 3.0, 3.0, 0.5, 0.5);
  }
}

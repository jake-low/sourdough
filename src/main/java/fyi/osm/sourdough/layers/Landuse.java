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
import fyi.osm.sourdough.util.LabelZooms;
import fyi.osm.sourdough.util.Utils;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class Landuse implements FeatureProcessor, LayerPostProcessor {

  private final Configuration config;

  public Landuse(Configuration config) {
    this.config = config;
  }

  public static final String LAYER_NAME = "landuse";

  public String name() {
    return LAYER_NAME;
  }

  public static final Set<String> PRIMARY_TAGS = Set.of("landuse");

  public static final Set<String> DETAIL_TAGS = Utils.union(
    Constants.COMMON_DETAIL_TAGS,
    Set.of(
      "operator",
      "website",
      "access",
      "residential",
      "industrial",
      "crop",
      "produce",
      "farmland",
      "religion",
      "denomination"
    )
  );

  @Override
  public Expression filter() {
    return Expression.matchField("landuse");
  }

  @Override
  public void processFeature(SourceFeature sf, FeatureCollector fc) {
    if (sf.canBePolygon()) {
      this.processLanduseArea(sf, fc);
    }
  }

  private void processLanduseArea(SourceFeature sf, FeatureCollector fc) {
    var polygon = fc.polygon(this.name());
    polygon.setPixelTolerance(0.25);

    AttributeProcessor.setAttributes(sf, polygon, PRIMARY_TAGS, config);
    var detailMinZoom = polygon.getMinZoomForPixelSize(32);
    AttributeProcessor.setAttributesWithMinzoom(sf, polygon, DETAIL_TAGS, detailMinZoom, config);

    if (sf.hasTag("name")) {
      Utils.createLabelPoint(sf, fc, this.name(), new LabelZooms(14, 17), PRIMARY_TAGS, DETAIL_TAGS, config);
    }
  }

  @Override
  public List<VectorTile.Feature> postProcess(int zoom, List<VectorTile.Feature> items)
    throws GeometryException {
    return FeatureMerge.mergeNearbyPolygons(items, 3.0, 3.0, 0.5, 0.5);
  }
}

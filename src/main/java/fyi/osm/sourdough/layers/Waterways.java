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

public class Waterways implements FeatureProcessor, LayerPostProcessor {

  private final Configuration config;

  public Waterways(Configuration config) {
    this.config = config;
  }

  public static final String LAYER_NAME = "waterways";

  public String name() {
    return LAYER_NAME;
  }

  public static final Set<String> PRIMARY_TAGS = Set.of("waterway");

  // TODO: LAYER_TAGS should appear at fixed zoom (z12)

  public static final Set<String> DETAIL_TAGS = Utils.union(
    Constants.COMMON_DETAIL_TAGS,
    Set.of("usage", "layer", "intermittent")
  );

  @Override
  public Expression filter() {
    return Expression.matchField("waterway");
  }

  @Override
  public void processFeature(SourceFeature sf, FeatureCollector fc) {
    var isArea =
      sf.hasTag("waterway", "dam", "dock", "boatyard", "fuel") || sf.hasTag("area", "yes");

    if (sf.canBePolygon() && isArea) {
      processWaterwayArea(sf, fc);
    } else if (sf.canBeLine()) {
      processWaterwayLine(sf, fc);
    } else if (sf.isPoint()) {
      processWaterwayPoint(sf, fc);
    }
  }

  private void processWaterwayArea(SourceFeature sf, FeatureCollector fc) {
    var polygon = fc.polygon(this.name());
    polygon.setZoomRange(2, 15);
    polygon.setMinPixelSize(2.0);

    AttributeProcessor.setAttributes(sf, polygon, PRIMARY_TAGS, config);

    var detailMinZoom = Math.min(14, polygon.getMinZoomForPixelSize(32));
    AttributeProcessor.setAttributesWithMinzoom(sf, polygon, DETAIL_TAGS, detailMinZoom, config);

    if (sf.hasTag("name")) {
      var label = fc.pointOnSurface(this.name());
      label.setMinZoom(detailMinZoom);
      label.setBufferPixels(32);

      AttributeProcessor.setAttributes(sf, label, PRIMARY_TAGS, config);
      AttributeProcessor.setAttributes(sf, label, DETAIL_TAGS, config);
    }
  }

  private void processWaterwayLine(SourceFeature sf, FeatureCollector fc) {
    var line = fc.line(this.name());
    line.setMinZoom(getWaterwayLineMinZoom(sf));
    line.setMinPixelSize(1.0);
    line.setBufferPixels(8);

    AttributeProcessor.setAttributes(sf, line, PRIMARY_TAGS, config);

    var detailMinZoom = Math.min(getWaterwayLineMinZoom(sf) + 3, 14);
    AttributeProcessor.setAttributesWithMinzoom(sf, line, DETAIL_TAGS, detailMinZoom, config);
  }

  private void processWaterwayPoint(SourceFeature sf, FeatureCollector fc) {
    var point = fc.point(this.name());
    point.setMinZoom(getLabelMinZoom(sf));
    point.setBufferPixels(32);

    AttributeProcessor.setAttributes(sf, point, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, point, DETAIL_TAGS, config);
  }

  private int getWaterwayLineMinZoom(SourceFeature sf) {
    return switch (sf.getString("waterway")) {
      case "river" -> 8;
      default -> 12;
    };
  }

  private int getLabelMinZoom(SourceFeature sf) {
    return switch (sf.getString("waterway")) {
      case "waterfall", "dam" -> 11;
      case "lock_gate", "weir", "sluice_gate" -> 12;
      case "rapids", "waterhole" -> 13;
      default -> 14;
    };
  }

  @Override
  public List<VectorTile.Feature> postProcess(int zoom, List<VectorTile.Feature> items)
    throws GeometryException {
    items = FeatureMerge.mergeOverlappingPolygons(items, 1);

    double tolerance = zoom < 15 ? 0.4 : 0.125;
    items = FeatureMerge.mergeLineStrings(items, 0, tolerance, 8);

    return items;
  }
}

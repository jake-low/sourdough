package fyi.osm.sourdough.layers;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.FeatureMerge;
import com.onthegomap.planetiler.ForwardingProfile.FeatureProcessor;
import com.onthegomap.planetiler.ForwardingProfile.LayerPostProcessor;
import com.onthegomap.planetiler.VectorTile;
import com.onthegomap.planetiler.expression.Expression;
import com.onthegomap.planetiler.geo.GeometryException;
import com.onthegomap.planetiler.reader.SourceFeature;
import com.onthegomap.planetiler.util.Parse;
import fyi.osm.sourdough.Configuration;
import fyi.osm.sourdough.Constants;
import fyi.osm.sourdough.util.AttributeProcessor;
import fyi.osm.sourdough.util.Utils;
import java.util.List;
import java.util.Set;

public class ManMade implements FeatureProcessor, LayerPostProcessor {

  private final Configuration config;

  public ManMade(Configuration config) {
    this.config = config;
  }

  public static final String LAYER_NAME = "man_made";

  public String name() {
    return LAYER_NAME;
  }

  public static final Set<String> PRIMARY_TAGS = Set.of("man_made");

  public static final Set<String> DETAIL_TAGS = Utils.union(
    Constants.COMMON_DETAIL_TAGS,
    Set.of(
      "ref",
      "operator",
      "tower:type",
      "tower:construction",
      "surveillance:type",
      "camera:type",
      "height",
      "content",
      "substance",
      "material",
      "utility",
      "location",
      "usage",
      "diameter"
    )
  );

  @Override
  public Expression filter() {
    return Expression.matchField("man_made");
  }

  @Override
  public void processFeature(SourceFeature sf, FeatureCollector fc) {
    if (sf.canBePolygon()) {
      processManmadeArea(sf, fc);
    } else if (sf.canBeLine()) {
      processManmadeLine(sf, fc);
    } else if (sf.isPoint()) {
      processManmadePoint(sf, fc);
    }
  }

  private void processManmadeArea(SourceFeature sf, FeatureCollector fc) {
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

  private void processManmadeLine(SourceFeature sf, FeatureCollector fc) {
    var minZoom = getLineMinZoom(sf);

    var line = fc.line(this.name());
    line.setZoomRange(minZoom, 15);
    line.setMinPixelSize(1.0);

    AttributeProcessor.setAttributes(sf, line, PRIMARY_TAGS, config);

    var detailMinZoom = Math.min(minZoom + 3, line.getMinZoomForPixelSize(32));
    AttributeProcessor.setAttributesWithMinzoom(sf, line, DETAIL_TAGS, detailMinZoom, config);
  }

  private void processManmadePoint(SourceFeature sf, FeatureCollector fc) {
    var point = fc.point(this.name());
    point.setMinZoom(getLabelMinZoom(sf));
    point.setBufferPixels(32);

    AttributeProcessor.setAttributes(sf, point, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, point, DETAIL_TAGS, config);
  }

  private int getLineMinZoom(SourceFeature sf) {
    return switch (sf.getString("man_made")) {
      case "pipeline" -> getPipelineMinZoom(sf);
      case "pier", "breakwater", "groyne" -> 8;
      case "embankment", "cutline" -> 10;
      case "dyke", "levee" -> 11;
      default -> 12;
    };
  }

  private int getPipelineMinZoom(SourceFeature sf) {
    String usage = sf.getString("usage");

    String diameterStr = sf.getString("diameter");
    if (diameterStr != null) {
      Double diameterMeters = Parse.meters(diameterStr);
      if (diameterMeters != null) {
        // Large diameter pipelines appear at lower zooms
        if (diameterMeters >= 2.0) return 8;
        if (diameterMeters >= 1.0) return 9;
        if (diameterMeters >= 0.5) return 10;
        if (diameterMeters >= 0.3) return 11;
        return 12;
      }
    }

    // Fall back to usage if diameter isn't specified
    return switch (usage) {
      case "transmission" -> 6;
      case "distribution" -> 10;
      case "gathering" -> 11;
      case null -> 12;
      default -> 12;
    };
  }

  private int getLabelMinZoom(SourceFeature sf) {
    return switch (sf.getString("man_made")) {
      case "tower", "communications_tower", "telescope" -> 12;
      case "mast" -> sf.hasTag("tower:type", "lighting") ? 14 : 12;
      case "lighthouse", "water_tower", "windmill", "silo", "storage_tank" -> 13;
      case "chimney", "crane", "cross", "obelisk", "monitoring_station" -> 14;
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

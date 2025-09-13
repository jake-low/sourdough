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

public class Railways implements FeatureProcessor, LayerPostProcessor {

  private final Configuration config;

  public Railways(Configuration config) {
    this.config = config;
  }

  public static final String LAYER_NAME = "railways";

  public String name() {
    return LAYER_NAME;
  }

  public static final Set<String> PRIMARY_TAGS = Set.of("railway", "service", "usage");

  // TODO: LAYER_TAGS should appear at fixed zoom (z12)

  public static final Set<String> DETAIL_TAGS = Utils.union(
    Constants.COMMON_DETAIL_TAGS,
    Set.of(
      "operator",
      "gauge",
      "electrified",
      "frequency",
      "voltage",
      "maxspeed",
      "ref",
      "railway:track_ref",
      "public_transport",
      "train",
      "subway",
      "tram",
      "bridge",
      "tunnel",
      "layer"
    )
  );

  @Override
  public Expression filter() {
    return Expression.matchField("railway");
  }

  @Override
  public void processFeature(SourceFeature sf, FeatureCollector fc) {
    if (sf.hasTag("railway", "construction", "proposed")) {
      return;
    }

    if (sf.canBePolygon()) {
      processRailwayArea(sf, fc);
    } else if (sf.canBeLine()) {
      processRailwayLine(sf, fc);
    } else if (sf.isPoint()) {
      processRailwayPoint(sf, fc);
    }
  }

  private void processRailwayArea(SourceFeature sf, FeatureCollector fc) {
    var polygon = fc.polygon(this.name());
    polygon.setZoomRange(2, 15);
    polygon.setMinPixelSize(2.0);

    AttributeProcessor.setAttributes(sf, polygon, PRIMARY_TAGS, config);

    var detailMinZoom = Math.min(getLabelMinZoom(sf), polygon.getMinZoomForPixelSize(32));
    AttributeProcessor.setAttributesWithMinzoom(sf, polygon, DETAIL_TAGS, detailMinZoom, config);

    var point = fc.pointOnSurface(this.name());
    point.setMinZoom(detailMinZoom);
    point.setBufferPixels(32);

    AttributeProcessor.setAttributes(sf, point, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, point, DETAIL_TAGS, config);
  }

  private void processRailwayLine(SourceFeature sf, FeatureCollector fc) {
    var line = fc.line(this.name());
    line.setMinZoom(getRailwayLineMinZoom(sf));
    line.setMinPixelSize(0.0);

    AttributeProcessor.setAttributes(sf, line, PRIMARY_TAGS, config);

    var detailMinZoom = Math.min(getRailwayLineMinZoom(sf) + 3, 15);
    AttributeProcessor.setAttributesWithMinzoom(sf, line, DETAIL_TAGS, detailMinZoom, config);
  }

  private void processRailwayPoint(SourceFeature sf, FeatureCollector fc) {
    var point = fc.point(this.name());
    point.setMinZoom(getLabelMinZoom(sf));
    point.setBufferPixels(32);

    AttributeProcessor.setAttributes(sf, point, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, point, DETAIL_TAGS, config);
  }

  private int getRailwayLineMinZoom(SourceFeature sf) {
    String railway = sf.getString("railway");
    String service = sf.getString("service");
    String usage = sf.getString("usage");

    // Base zoom by railway type
    int baseZoom = switch (railway) {
      case "rail" -> 6;
      case "narrow_gauge" -> 8;
      case "subway", "light_rail" -> 10;
      case "tram", "monorail", "funicular" -> 11;
      case "disused", "abandoned" -> 12;
      case "razed" -> 14;
      default -> 12;
    };

    // Adjust zoom based on usage tag (main vs branch lines)
    if (usage != null) {
      int adjust = switch (usage) {
        case "main" -> -1;
        case "branch" -> 0;
        case "industrial" -> 1;
        case "military" -> 2;
        case "test" -> 3;
        default -> 0;
      };
      baseZoom += adjust;
    }

    // Adjust zoom based on service tag (minor tracks at railway yards/stations)
    if (service != null) {
      int adjust = switch (service) {
        case "spur" -> 4;
        case "yard" -> 5;
        case "siding" -> 6;
        case "crossover" -> 5;
        default -> 4;
      };
      baseZoom += adjust;
    }

    return Math.min(baseZoom, 13);
  }

  private int getLabelMinZoom(SourceFeature sf) {
    return switch (sf.getString("railway")) {
      case "station" -> 10;
      case "halt" -> 11;
      case "stop", "tram_stop" -> 12;
      default -> 15;
    };
  }

  @Override
  public List<VectorTile.Feature> postProcess(int zoom, List<VectorTile.Feature> items)
    throws GeometryException {
    double tolerance = zoom < 14 ? 0.5 : 0.25;
    items = FeatureMerge.mergeLineStrings(items, 1, tolerance, 8);
    items = FeatureMerge.mergeMultiPoint(items);

    return items;
  }
}

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

public class Power implements FeatureProcessor, LayerPostProcessor {

  private final Configuration config;

  public Power(Configuration config) {
    this.config = config;
  }

  public static final String LAYER_NAME = "power";

  public String name() {
    return LAYER_NAME;
  }

  public static final Set<String> PRIMARY_TAGS = Set.of("power", "voltage");

  public static final Set<String> DETAIL_TAGS = Utils.union(
    Constants.COMMON_DETAIL_TAGS,
    Set.of(
      "operator",
      "frequency",
      "cables",
      "circuits",
      "material",
      "design",
      "structure",
      "location",
      "ref",
      "generator:source",
      "generator:method",
      "generator:type",
      "generator:output:electricity"
    )
  );

  @Override
  public Expression filter() {
    return Expression.matchField("power");
  }

  @Override
  public void processFeature(SourceFeature sf, FeatureCollector fc) {
    if (sf.canBePolygon()) {
      processPowerArea(sf, fc);
    } else if (sf.canBeLine()) {
      processPowerLine(sf, fc);
    } else if (sf.isPoint()) {
      processPowerPoint(sf, fc);
    }
  }

  private void processPowerArea(SourceFeature sf, FeatureCollector fc) {
    var polygon = fc.polygon(this.name());
    polygon.setZoomRange(8, 15);
    polygon.setMinPixelSize(2.0);

    AttributeProcessor.setAttributes(sf, polygon, PRIMARY_TAGS, config);

    var detailMinZoom = Math.min(getLabelMinZoom(sf), polygon.getMinZoomForPixelSize(32));
    AttributeProcessor.setAttributesWithMinzoom(sf, polygon, DETAIL_TAGS, detailMinZoom, config);

    if (sf.hasTag("name") || sf.hasTag("ref")) {
      var point = fc.pointOnSurface(this.name());
      point.setMinZoom(detailMinZoom);
      point.setBufferPixels(32);

      AttributeProcessor.setAttributes(sf, point, PRIMARY_TAGS, config);
      AttributeProcessor.setAttributes(sf, point, DETAIL_TAGS, config);
    }
  }

  private void processPowerLine(SourceFeature sf, FeatureCollector fc) {
    var line = fc.line(this.name());
    line.setZoomRange(getPowerLineMinZoom(sf), 15);
    line.setMinPixelSize(1.0);

    AttributeProcessor.setAttributes(sf, line, PRIMARY_TAGS, config);

    var detailMinZoom = Math.min(getPowerLineMinZoom(sf) + 3, 15);
    AttributeProcessor.setAttributesWithMinzoom(sf, line, DETAIL_TAGS, detailMinZoom, config);
  }

  private void processPowerPoint(SourceFeature sf, FeatureCollector fc) {
    var point = fc.point(this.name());
    point.setMinZoom(getLabelMinZoom(sf));
    point.setBufferPixels(32);

    AttributeProcessor.setAttributes(sf, point, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, point, DETAIL_TAGS, config);
  }

  private int getPowerLineMinZoom(SourceFeature sf) {
    return switch (sf.getString("power")) {
      case "line", "cable" -> getVoltageBasedMinZoom(sf);
      case "minor_line" -> 13;
      default -> 13;
    };
  }

  private int getVoltageBasedMinZoom(SourceFeature sf) {
    String voltageStr = sf.getString("voltage");
    if (voltageStr == null) {
      return 10; // Default for lines without voltage information
    }

    // Parse voltage, handling cases like "20000;400" (take the highest)
    int voltage = parseMaxVoltage(voltageStr);

    if (voltage >= 345000) {
      return 6;
    } else if (voltage >= 220000) {
      return 7;
    } else if (voltage >= 110000) {
      return 8;
    } else if (voltage >= 33000) {
      return 9;
    } else if (voltage >= 10000) {
      return 10;
    } else if (voltage >= 1000) {
      return 11;
    } else {
      return 12;
    }
  }

  private int parseMaxVoltage(String voltageStr) {
    int maxVoltage = 0;

    // Handle multiple voltages separated by semicolon (e.g., "20000;400")
    String[] voltages = voltageStr.split(";");

    for (String voltage : voltages) {
      try {
        int v = Integer.parseInt(voltage.trim());
        maxVoltage = Math.max(maxVoltage, v);
      } catch (NumberFormatException e) {
        // Ignore invalid voltage values
      }
    }

    return maxVoltage;
  }

  private int getLabelMinZoom(SourceFeature sf) {
    return switch (sf.getString("power")) {
      case "plant" -> 11;
      case "tower", "substation" -> 12;
      case "generator" -> switch (sf.getString("generator:source")) {
        case "wind" -> 13;
        case null -> 15;
        default -> 15;
      };
      case "pole", "transformer" -> 14;
      default -> 14;
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

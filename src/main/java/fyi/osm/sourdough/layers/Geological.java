package fyi.osm.sourdough.layers;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.ForwardingProfile.FeatureProcessor;
import com.onthegomap.planetiler.expression.Expression;
import com.onthegomap.planetiler.reader.SourceFeature;
import fyi.osm.sourdough.Configuration;
import fyi.osm.sourdough.Constants;
import fyi.osm.sourdough.util.AttributeProcessor;
import fyi.osm.sourdough.util.Utils;
import java.util.Set;

public class Geological implements FeatureProcessor {

  private final Configuration config;

  public Geological(Configuration config) {
    this.config = config;
  }

  public static final String LAYER_NAME = "geological";

  public String name() {
    return LAYER_NAME;
  }

  public static final Set<String> PRIMARY_TAGS = Set.of("geological");

  public static final Set<String> DETAIL_TAGS = Utils.union(
    Constants.COMMON_DETAIL_TAGS,
    Set.of("website", "height", "ele", "rock", "outcrop:type", "start_date")
  );

  @Override
  public Expression filter() {
    return Expression.matchField("geological");
  }

  @Override
  public void processFeature(SourceFeature sf, FeatureCollector fc) {
    if (sf.canBePolygon()) {
      processGeologicalArea(sf, fc);
    } else if (sf.canBeLine()) {
      processGeologicalLine(sf, fc);
    } else if (sf.isPoint()) {
      processGeologicalPoint(sf, fc);
    }
  }

  private void processGeologicalArea(SourceFeature sf, FeatureCollector fc) {
    var polygon = fc.polygon(this.name());
    polygon.setZoomRange(6, 15);
    polygon.setMinPixelSize(8.0);

    AttributeProcessor.setAttributes(sf, polygon, PRIMARY_TAGS, config);

    var detailMinZoom = Math.min(getLabelMinZoom(sf), polygon.getMinZoomForPixelSize(64));
    AttributeProcessor.setAttributesWithMinzoom(sf, polygon, DETAIL_TAGS, detailMinZoom, config);

    if (sf.hasTag("name") || sf.hasTag("tourism")) {
      var point = fc.pointOnSurface(this.name());
      point.setMinZoom(detailMinZoom);
      point.setBufferPixels(32);

      AttributeProcessor.setAttributes(sf, point, PRIMARY_TAGS, config);
      AttributeProcessor.setAttributes(sf, point, DETAIL_TAGS, config);
    }
  }

  private void processGeologicalLine(SourceFeature sf, FeatureCollector fc) {
    var line = fc.line(this.name());
    line.setMinZoom(getGeologicalLineMinZoom(sf));
    line.setMinPixelSize(2.0);
    line.setBufferPixels(8);

    AttributeProcessor.setAttributes(sf, line, PRIMARY_TAGS, config);

    var detailMinZoom = Math.min(getGeologicalLineMinZoom(sf) + 2, 14);
    AttributeProcessor.setAttributesWithMinzoom(sf, line, DETAIL_TAGS, detailMinZoom, config);
  }

  private void processGeologicalPoint(SourceFeature sf, FeatureCollector fc) {
    var point = fc.point(this.name());
    point.setMinZoom(getLabelMinZoom(sf));
    point.setBufferPixels(32);

    AttributeProcessor.setAttributes(sf, point, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, point, DETAIL_TAGS, config);
  }

  private int getGeologicalLineMinZoom(SourceFeature sf) {
    return switch (sf.getString("geological")) {
      case "fault" -> 8;
      case "volcanic_caldera_rim" -> 10;
      default -> 12;
    };
  }

  private int getLabelMinZoom(SourceFeature sf) {
    return switch (sf.getString("geological")) {
      case "volcanic_lava_field", "volcanic_caldera_rim" -> 8;
      case "moraine", "volcanic_lava_flow" -> 9;
      case "outcrop", "palaeontological_site" -> 10;
      case "fault", "volcanic_vent" -> 11;
      case "glacial_erratic", "rock_glacier", "meteor_crater" -> 12;
      case "nunatak", "landslide", "giants_kettle", "limestone_pavement" -> 13;
      case "karst", "geyser", "hot_spring", "sinkhole", "cave_entrance" -> 13;
      default -> 12;
    };
  }
}

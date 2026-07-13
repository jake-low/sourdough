package fyi.osm.sourdough.layers;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.ForwardingProfile.FeatureProcessor;
import com.onthegomap.planetiler.expression.Expression;
import com.onthegomap.planetiler.reader.SourceFeature;
import fyi.osm.sourdough.Configuration;
import fyi.osm.sourdough.Constants;
import fyi.osm.sourdough.util.AttributeProcessor;
import fyi.osm.sourdough.util.LabelZooms;
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

    var detailMinZoom = Math.min(getLabelZooms(sf).min(), polygon.getMinZoomForPixelSize(64));
    AttributeProcessor.setAttributesWithMinzoom(sf, polygon, DETAIL_TAGS, detailMinZoom, config);

    if (sf.hasTag("name") || sf.hasTag("tourism")) {
      Utils.createLabelPoint(sf, fc, this.name(), getLabelZooms(sf), PRIMARY_TAGS, DETAIL_TAGS, config);
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
    Utils.createPoint(sf, fc, this.name(), getLabelZooms(sf), PRIMARY_TAGS, DETAIL_TAGS, config);
  }

  private int getGeologicalLineMinZoom(SourceFeature sf) {
    return switch (sf.getString("geological")) {
      case "fault" -> 8;
      case "volcanic_caldera_rim" -> 10;
      default -> 12;
    };
  }

  private LabelZooms getLabelZooms(SourceFeature sf) {
    return switch (sf.getString("geological")) {
      case "volcanic_lava_field", "volcanic_caldera_rim" -> new LabelZooms(8, 10);
      case "moraine", "volcanic_lava_flow" -> new LabelZooms(9, 11);
      case "outcrop", "palaeontological_site" -> new LabelZooms(10, 12);
      case "fault", "volcanic_vent" -> new LabelZooms(11, 13);
      case "glacial_erratic", "rock_glacier", "meteor_crater" -> new LabelZooms(12, 13);
      case "nunatak", "landslide", "giants_kettle", "limestone_pavement" -> new LabelZooms(13, 14);
      case "karst", "geyser", "hot_spring", "sinkhole", "cave_entrance" -> new LabelZooms(13, 14);
      default -> new LabelZooms(12, 13);
    };
  }
}

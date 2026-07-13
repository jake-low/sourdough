package fyi.osm.sourdough.layers;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.ForwardingProfile.FeatureProcessor;
import com.onthegomap.planetiler.ForwardingProfile.LayerPostProcessor;
import com.onthegomap.planetiler.expression.Expression;
import com.onthegomap.planetiler.reader.SourceFeature;
import fyi.osm.sourdough.Configuration;
import fyi.osm.sourdough.Constants;
import fyi.osm.sourdough.util.AttributeProcessor;
import fyi.osm.sourdough.util.LabelZooms;
import fyi.osm.sourdough.util.Utils;
import java.util.Set;

public class Tourism implements FeatureProcessor {

  private final Configuration config;

  public Tourism(Configuration config) {
    this.config = config;
  }

  public static final String LAYER_NAME = "tourism";

  public String name() {
    return LAYER_NAME;
  }

  public static final Set<String> PRIMARY_TAGS = Set.of("tourism", "attraction");

  public static final Set<String> DETAIL_TAGS = Utils.union(
    Constants.COMMON_DETAIL_TAGS,
    Set.of(
      "access",
      "artist_name",
      "artwork_type",
      "attraction",
      "board:title",
      "board_type",
      "brand",
      "fee",
      "information",
      "level",
      "museum",
      "operator",
      "toilets",
      "website",
      "wheelchair",
      "zoo"
    )
  );

  @Override
  public Expression filter() {
    return Expression.or(Expression.matchField("tourism"), Expression.matchField("attraction"));
  }

  @Override
  public void processFeature(SourceFeature sf, FeatureCollector fc) {
    if (sf.hasTag("tourism")) {
      if (sf.canBePolygon()) {
        processTourismArea(sf, fc);
      } else if (sf.isPoint()) {
        processTourismPoint(sf, fc);
      }
      return;
    }

    if (sf.hasTag("attraction")) {
      if (sf.canBePolygon()) {
        processAttractionArea(sf, fc);
      } else if (sf.canBeLine()) {
        processAttractionLine(sf, fc);
      } else if (sf.isPoint()) {
        processAttractionPoint(sf, fc);
      }
      return;
    }
  }

  private void processTourismArea(SourceFeature sf, FeatureCollector fc) {
    var polygon = fc.polygon(this.name());
    polygon.setZoomRange(2, 15);
    polygon.setMinPixelSize(2.0);

    AttributeProcessor.setAttributes(sf, polygon, PRIMARY_TAGS, config);

    var detailMinZoom = Math.min(getLabelZooms(sf).min(), polygon.getMinZoomForPixelSize(32));
    AttributeProcessor.setAttributesWithMinzoom(sf, polygon, DETAIL_TAGS, detailMinZoom, config);

    Utils.createLabelPoint(sf, fc, this.name(), getLabelZooms(sf), PRIMARY_TAGS, DETAIL_TAGS, config);
  }

  private void processTourismPoint(SourceFeature sf, FeatureCollector fc) {
    Utils.createPoint(sf, fc, this.name(), getLabelZooms(sf), PRIMARY_TAGS, DETAIL_TAGS, config);
  }

  private void processAttractionArea(SourceFeature sf, FeatureCollector fc) {
    var polygon = fc.polygon(this.name());
    polygon.setZoomRange(13, 15);
    polygon.setMinPixelSize(1.0);

    AttributeProcessor.setAttributes(sf, polygon, PRIMARY_TAGS, config);

    var detailMinZoom = Math.min(14, polygon.getMinZoomForPixelSize(16));
    AttributeProcessor.setAttributesWithMinzoom(sf, polygon, DETAIL_TAGS, detailMinZoom, config);

    if (sf.hasTag("name") || sf.hasTag("ref")) {
      Utils.createLabelPoint(sf, fc, this.name(), new LabelZooms(14, 17), PRIMARY_TAGS, DETAIL_TAGS, config);
    }
  }

  private void processAttractionLine(SourceFeature sf, FeatureCollector fc) {
    var line = fc.line(this.name());
    line.setZoomRange(14, 15);
    line.setMinPixelSize(8.0);

    AttributeProcessor.setAttributes(sf, line, PRIMARY_TAGS, config);

    var detailMinZoom = line.getMinZoomForPixelSize(16);
    AttributeProcessor.setAttributesWithMinzoom(sf, line, DETAIL_TAGS, detailMinZoom, config);
  }

  private void processAttractionPoint(SourceFeature sf, FeatureCollector fc) {
    Utils.createPoint(sf, fc, this.name(), new LabelZooms(15, 16), PRIMARY_TAGS, DETAIL_TAGS, config);
  }

  private LabelZooms getLabelZooms(SourceFeature sf) {
    return switch (sf.getString("tourism")) {
      case "alpine_hut", "wilderness_hut" -> new LabelZooms(11, 12);
      case "camp_site", "caravan_site", "museum", "theme_park", "aquarium", "zoo" ->
        new LabelZooms(12, 13);
      case "picnic_site", "viewpoint" -> new LabelZooms(13, 14);
      case "hotel", "guest_house", "hostel", "motel", "chalet", "information" ->
        new LabelZooms(14, 15);
      default -> new LabelZooms(15, 16);
    };
  }
}

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

public class Offices implements FeatureProcessor {

  private final Configuration config;

  public Offices(Configuration config) {
    this.config = config;
  }

  public static final String LAYER_NAME = "offices";

  public String name() {
    return LAYER_NAME;
  }

  public static final Set<String> PRIMARY_TAGS = Set.of("office");

  public static final Set<String> DETAIL_TAGS = Utils.union(
    Constants.COMMON_DETAIL_TAGS,
    Set.of(
      "operator",
      "brand",
      "phone",
      "website",
      "email",
      "wheelchair",
      "internet_access",
      "government",
      "ref"
    )
  );

  @Override
  public Expression filter() {
    return Expression.matchField("office");
  }

  @Override
  public void processFeature(SourceFeature sf, FeatureCollector fc) {
    if (sf.canBePolygon()) {
      processOfficeArea(sf, fc);
    } else if (sf.isPoint()) {
      processOfficePoint(sf, fc);
    }
  }

  private void processOfficeArea(SourceFeature sf, FeatureCollector fc) {
    var polygon = fc.polygon(this.name());
    polygon.setZoomRange(2, 15);
    polygon.setMinPixelSize(2.0);

    AttributeProcessor.setAttributes(sf, polygon, PRIMARY_TAGS, config);

    var detailMinZoom = Math.min(getLabelZooms(sf).min(), polygon.getMinZoomForPixelSize(32));
    AttributeProcessor.setAttributesWithMinzoom(sf, polygon, DETAIL_TAGS, detailMinZoom, config);

    Utils.createLabelPoint(sf, fc, this.name(), getLabelZooms(sf), PRIMARY_TAGS, DETAIL_TAGS, config);
  }

  private void processOfficePoint(SourceFeature sf, FeatureCollector fc) {
    Utils.createPoint(sf, fc, this.name(), getLabelZooms(sf), PRIMARY_TAGS, DETAIL_TAGS, config);
  }

  private LabelZooms getLabelZooms(SourceFeature sf) {
    return switch (sf.getString("office")) {
      case "government", "diplomatic" -> new LabelZooms(13, 15);
      default -> new LabelZooms(14, 16);
    };
  }
}

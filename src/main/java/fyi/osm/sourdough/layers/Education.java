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

public class Education implements FeatureProcessor {

  private final Configuration config;

  public Education(Configuration config) {
    this.config = config;
  }

  public static final String LAYER_NAME = "education";

  public String name() {
    return LAYER_NAME;
  }

  public static final Set<String> PRIMARY_TAGS = Set.of("education");

  public static final Set<String> DETAIL_TAGS = Utils.union(
    Constants.COMMON_DETAIL_TAGS,
    Set.of(
      "operator",
      "website",
      "phone",
      "wheelchair",
      "isced:level",
      "education_profile:general",
      "education_level:primary",
      "education_level:secondary",
      "education_level:preschool"
    )
  );

  @Override
  public Expression filter() {
    return Expression.matchField("education");
  }

  @Override
  public void processFeature(SourceFeature sf, FeatureCollector fc) {
    if (sf.canBePolygon()) {
      processEducationArea(sf, fc);
    } else if (sf.isPoint()) {
      processEducationPoint(sf, fc);
    }
  }

  private void processEducationArea(SourceFeature sf, FeatureCollector fc) {
    var polygon = fc.polygon(this.name());
    polygon.setZoomRange(8, 15);
    polygon.setMinPixelSize(4.0);

    AttributeProcessor.setAttributes(sf, polygon, PRIMARY_TAGS, config);

    var detailMinZoom = Math.min(getLabelMinZoom(sf), polygon.getMinZoomForPixelSize(32));
    AttributeProcessor.setAttributesWithMinzoom(sf, polygon, DETAIL_TAGS, detailMinZoom, config);

    if (sf.hasTag("name")) {
      var point = fc.pointOnSurface(this.name());
      point.setMinZoom(detailMinZoom);
      point.setBufferPixels(32);

      AttributeProcessor.setAttributes(sf, point, PRIMARY_TAGS, config);
      AttributeProcessor.setAttributes(sf, point, DETAIL_TAGS, config);
    }
  }

  private void processEducationPoint(SourceFeature sf, FeatureCollector fc) {
    var point = fc.point(this.name());
    point.setMinZoom(getLabelMinZoom(sf));
    point.setBufferPixels(32);

    AttributeProcessor.setAttributes(sf, point, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, point, DETAIL_TAGS, config);
  }

  private int getLabelMinZoom(SourceFeature sf) {
    return switch (sf.getString("education")) {
      case "university", "college" -> 10;
      case "school" -> 11;
      case "kindergarten" -> 12;
      case
        "facultative_school",
        "centre",
        "courses",
        "driving_school",
        "music",
        "language_school" -> 13;
      default -> 12;
    };
  }
}

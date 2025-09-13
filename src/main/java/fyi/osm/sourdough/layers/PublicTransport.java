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

public class PublicTransport implements FeatureProcessor {

  private final Configuration config;

  public PublicTransport(Configuration config) {
    this.config = config;
  }

  public static final String LAYER_NAME = "public_transport";

  public String name() {
    return LAYER_NAME;
  }

  public static final Set<String> PRIMARY_TAGS = Set.of("public_transport");

  public static final Set<String> DETAIL_TAGS = Utils.union(
    Constants.COMMON_DETAIL_TAGS,
    Set.of(
      "bus",
      "train",
      "tram",
      "trolleybus",
      "light_rail",
      "subway",
      "station",
      "highway",
      "railway",
      "network",
      "operator",
      "ref",
      "shelter",
      "bench",
      "bin",
      "covered",
      "lit",
      "tactile_paving",
      "wheelchair",
      "departures_board",
      "passenger_information_display",
      "surface",
      "network:wikidata"
    )
  );

  @Override
  public Expression filter() {
    return Expression.matchField("public_transport");
  }

  @Override
  public void processFeature(SourceFeature sf, FeatureCollector fc) {
    if (sf.hasTag("public_transport", "no")) {
      return;
    }

    if (sf.canBePolygon()) {
      processPublicTransportArea(sf, fc);
    } else if (sf.canBeLine()) {
      processPublicTransportLine(sf, fc);
    } else if (sf.isPoint()) {
      processPublicTransportPoint(sf, fc);
    }
  }

  private void processPublicTransportArea(SourceFeature sf, FeatureCollector fc) {
    var polygon = fc.polygon(this.name());
    polygon.setZoomRange(8, 15);
    polygon.setMinPixelSize(4.0);

    AttributeProcessor.setAttributes(sf, polygon, PRIMARY_TAGS, config);

    var detailMinZoom = Math.min(getLabelMinZoom(sf), polygon.getMinZoomForPixelSize(32));
    AttributeProcessor.setAttributesWithMinzoom(sf, polygon, DETAIL_TAGS, detailMinZoom, config);

    var point = fc.pointOnSurface(this.name());
    point.setMinZoom(detailMinZoom);
    point.setBufferPixels(32);

    AttributeProcessor.setAttributes(sf, point, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, point, DETAIL_TAGS, config);
  }

  private void processPublicTransportLine(SourceFeature sf, FeatureCollector fc) {
    var line = fc.line(this.name());
    line.setMinZoom(getLabelMinZoom(sf));
    line.setMinPixelSize(2.0);
    line.setBufferPixels(8);

    AttributeProcessor.setAttributes(sf, line, PRIMARY_TAGS, config);

    var detailMinZoom = Math.min(getLabelMinZoom(sf) + 2, 14);
    AttributeProcessor.setAttributesWithMinzoom(sf, line, DETAIL_TAGS, detailMinZoom, config);
  }

  private void processPublicTransportPoint(SourceFeature sf, FeatureCollector fc) {
    var point = fc.point(this.name());
    point.setMinZoom(getLabelMinZoom(sf));
    point.setBufferPixels(32);

    AttributeProcessor.setAttributes(sf, point, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, point, DETAIL_TAGS, config);
  }

  private int getLabelMinZoom(SourceFeature sf) {
    return switch (sf.getString("public_transport")) {
      case "station" -> 10;
      case "platform" -> sf.hasTag("train", "yes") || sf.hasTag("railway") ? 11 : 13;
      case "stop_position" -> sf.hasTag("train", "yes") || sf.hasTag("railway") ? 14 : 15;
      default -> 13;
    };
  }
}

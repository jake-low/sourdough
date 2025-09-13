package fyi.osm.sourdough.layers;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.ForwardingProfile.FeatureProcessor;
import com.onthegomap.planetiler.ForwardingProfile.LayerPostProcessor;
import com.onthegomap.planetiler.expression.Expression;
import com.onthegomap.planetiler.reader.SourceFeature;
import fyi.osm.sourdough.Configuration;
import fyi.osm.sourdough.Constants;
import fyi.osm.sourdough.util.AttributeProcessor;
import fyi.osm.sourdough.util.Utils;
import java.util.Set;

public class Pistes implements FeatureProcessor {

  private final Configuration config;

  public Pistes(Configuration config) {
    this.config = config;
  }

  public static final String LAYER_NAME = "pistes";

  public String name() {
    return LAYER_NAME;
  }

  public static final Set<String> PRIMARY_TAGS = Set.of("piste:type");

  public static final Set<String> DETAIL_TAGS = Utils.union(
    Constants.COMMON_DETAIL_TAGS,
    Set.of("piste:grooming", "piste:name", "operator", "access", "informal", "website")
  );

  @Override
  public Expression filter() {
    return Expression.matchField("piste:type");
  }

  @Override
  public void processFeature(SourceFeature sf, FeatureCollector fc) {
    if (sf.canBeLine()) {
      this.processPisteLine(sf, fc);
    } else if (sf.canBePolygon()) {
      this.processPisteArea(sf, fc);
    } else if (sf.isPoint()) {
      this.processPistePoint(sf, fc);
    }
  }

  private void processPisteLine(SourceFeature sf, FeatureCollector fc) {
    var line = fc.line(this.name());
    line.setMinPixelSize(0);
    line.setPixelTolerance(0);
    line.setMinZoom(12);

    AttributeProcessor.setAttributes(sf, line, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, line, DETAIL_TAGS, config);
  }

  private void processPisteArea(SourceFeature sf, FeatureCollector fc) {
    var polygon = fc.polygon(this.name());
    polygon.setMinPixelSize(32);

    AttributeProcessor.setAttributes(sf, polygon, PRIMARY_TAGS, config);

    var detailMinZoom = Math.min(14, polygon.getMinZoomForPixelSize(64));
    AttributeProcessor.setAttributesWithMinzoom(sf, polygon, DETAIL_TAGS, detailMinZoom, config);

    if (sf.hasTag("name") || sf.hasTag("piste:name")) {
      var label = fc.pointOnSurface(this.name());
      label.setMinZoom(detailMinZoom);
      label.setBufferPixels(32);

      AttributeProcessor.setAttributes(sf, label, PRIMARY_TAGS, config);
      AttributeProcessor.setAttributes(sf, label, DETAIL_TAGS, config);
    }
  }

  private void processPistePoint(SourceFeature sf, FeatureCollector fc) {
    var point = fc.point(this.name());
    point.setMinZoom(12);

    AttributeProcessor.setAttributes(sf, point, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, point, DETAIL_TAGS, config);
  }
}

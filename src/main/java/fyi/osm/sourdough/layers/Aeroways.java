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

public class Aeroways implements FeatureProcessor, LayerPostProcessor {

  private final Configuration config;

  public Aeroways(Configuration config) {
    this.config = config;
  }

  public static final String LAYER_NAME = "aeroways";

  public String name() {
    return LAYER_NAME;
  }

  public static final Set<String> PRIMARY_TAGS = Set.of("aeroway");

  public static final Set<String> DETAIL_TAGS = Utils.union(
    Constants.COMMON_DETAIL_TAGS,
    Set.of(
      "ref",
      "iata",
      "icao",
      "faa",
      "aerodrome:type",
      "military",
      "operator",
      "surface",
      "access",
      "ele",
      "width",
      "direction",
      "navigationaid",
      "holding_position:type"
    )
  );

  @Override
  public Expression filter() {
    return Expression.matchField("aeroway");
  }

  @Override
  public void processFeature(SourceFeature sf, FeatureCollector fc) {
    if (sf.canBePolygon()) {
      this.processAerowayArea(sf, fc);
    } else if (sf.canBeLine()) {
      this.processAerowayLine(sf, fc);
    } else if (sf.isPoint()) {
      this.processAerowayPoint(sf, fc);
    }
  }

  private void processAerowayArea(SourceFeature sf, FeatureCollector fc) {
    var polygon = fc.polygon(this.name());
    polygon.setMinPixelSize(2.0);
    polygon.setZoomRange(2, 15);

    AttributeProcessor.setAttributes(sf, polygon, PRIMARY_TAGS, config);

    var detailMinZoom = Math.min(14, polygon.getMinZoomForPixelSize(32));
    AttributeProcessor.setAttributesWithMinzoom(sf, polygon, DETAIL_TAGS, detailMinZoom, config);

    if (sf.hasTag("name")) {
      var labelMinZoom = Math.min(getLabelMinZoom(sf), detailMinZoom);

      var label = fc.pointOnSurface(this.name());
      label.setMinZoom(labelMinZoom);
      label.setBufferPixels(32);

      AttributeProcessor.setAttributes(sf, label, PRIMARY_TAGS, config);
      AttributeProcessor.setAttributes(sf, label, DETAIL_TAGS, config);
    }
  }

  private void processAerowayLine(SourceFeature sf, FeatureCollector fc) {
    var line = fc.line(this.name());
    line.setMinPixelSize(16.0);
    line.setZoomRange(2, 15);

    AttributeProcessor.setAttributes(sf, line, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, line, DETAIL_TAGS, config);

    if (sf.hasTag("name")) {
      var labelMinZoom = Math.min(this.getLabelMinZoom(sf), line.getMinZoomForPixelSize(32));

      var label = fc.pointOnSurface(this.name());
      label.setMinZoom(labelMinZoom);
      label.setBufferPixels(32);

      AttributeProcessor.setAttributes(sf, label, PRIMARY_TAGS, config);
      AttributeProcessor.setAttributes(sf, label, DETAIL_TAGS, config);
    }
  }

  private void processAerowayPoint(SourceFeature sf, FeatureCollector fc) {
    var point = fc.point(this.name());
    point.setMinZoom(this.getLabelMinZoom(sf));

    AttributeProcessor.setAttributes(sf, point, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, point, DETAIL_TAGS, config);
  }

  private int getLabelMinZoom(SourceFeature sf) {
    if (sf.hasTag("aeroway", "aerodrome")) {
      if (sf.hasTag("aerodrome:type", "public", "international")) {
        return 8;
      } else if (sf.hasTag("iata") || sf.hasTag("icao")) {
        return 10;
      } else {
        return 12;
      }
    }
    return 14;
  }

  @Override
  public List<VectorTile.Feature> postProcess(int zoom, List<VectorTile.Feature> items)
    throws GeometryException {
    if (zoom < 15) {
      items = FeatureMerge.mergeMultiPoint(items);
      items = FeatureMerge.mergeLineStrings(items, 5.0, 0.25, 8);
      items = FeatureMerge.mergeNearbyPolygons(items, 3.0, 3.0, 0.5, 0.5);
    }

    return items;
  }
}

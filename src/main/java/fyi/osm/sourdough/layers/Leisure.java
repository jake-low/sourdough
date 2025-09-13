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

public class Leisure implements FeatureProcessor, LayerPostProcessor {

  private final Configuration config;

  public Leisure(Configuration config) {
    this.config = config;
  }

  public static final String LAYER_NAME = "leisure";

  public String name() {
    return LAYER_NAME;
  }

  public static final Set<String> PRIMARY_TAGS = Set.of("leisure", "playground", "golf");

  public static final Set<String> DETAIL_TAGS = Utils.union(
    Constants.COMMON_DETAIL_TAGS,
    Set.of(
      "sport",
      "access",
      "surface",
      "operator",
      "website",
      "wheelchair",
      "lit",
      "covered",
      "garden:type",
      "swimming_pool",
      "boundary"
    )
  );

  @Override
  public Expression filter() {
    return Expression.or(
      Expression.matchField("leisure"),
      Expression.matchField("playground"),
      Expression.matchField("golf")
    );
  }

  @Override
  public void processFeature(SourceFeature sf, FeatureCollector fc) {
    if (sf.hasTag("leisure")) {
      if (sf.canBePolygon()) {
        processLeisureArea(sf, fc);
      } else if (sf.canBeLine()) {
        processLeisureLine(sf, fc);
      } else if (sf.isPoint()) {
        processLeisurePoint(sf, fc);
      }
      return;
    }

    if (sf.hasTag("playground")) {
      if (sf.canBePolygon()) {
        processPlaygroundArea(sf, fc);
      } else if (sf.canBeLine()) {
        processPlaygroundLine(sf, fc);
      } else if (sf.isPoint()) {
        processPlaygroundPoint(sf, fc);
      }
      return;
    }

    if (sf.hasTag("golf")) {
      if (sf.canBePolygon()) {
        processGolfArea(sf, fc);
      } else if (sf.canBeLine()) {
        processGolfLine(sf, fc);
      } else if (sf.isPoint()) {
        processGolfPoint(sf, fc);
      }
      return;
    }
  }

  private void processLeisureArea(SourceFeature sf, FeatureCollector fc) {
    var polygon = fc.polygon(this.name());
    polygon.setZoomRange(2, 15);
    polygon.setMinPixelSize(2.0);

    AttributeProcessor.setAttributes(sf, polygon, PRIMARY_TAGS, config);

    var detailMinZoom = polygon.getMinZoomForPixelSize(32);
    AttributeProcessor.setAttributesWithMinzoom(sf, polygon, DETAIL_TAGS, detailMinZoom, config);

    if (sf.hasTag("name") || sf.hasTag("ref")) {
      var label = fc.pointOnSurface(this.name());
      label.setMinZoom(Math.min(getLabelMinZoom(sf), detailMinZoom));
      label.setBufferPixels(32);

      AttributeProcessor.setAttributes(sf, label, PRIMARY_TAGS, config);
      AttributeProcessor.setAttributes(sf, label, DETAIL_TAGS, config);
    }
  }

  private void processLeisureLine(SourceFeature sf, FeatureCollector fc) {
    var line = fc.line(this.name());
    line.setZoomRange(2, 15);
    line.setMinPixelSize(16.0);

    AttributeProcessor.setAttributes(sf, line, PRIMARY_TAGS, config);

    var detailMinZoom = line.getMinZoomForPixelSize(32);
    AttributeProcessor.setAttributesWithMinzoom(sf, line, DETAIL_TAGS, detailMinZoom, config);
  }

  private void processLeisurePoint(SourceFeature sf, FeatureCollector fc) {
    var point = fc.point(this.name());
    point.setMinZoom(getLabelMinZoom(sf));
    point.setBufferPixels(32);

    AttributeProcessor.setAttributes(sf, point, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, point, DETAIL_TAGS, config);
  }

  private void processPlaygroundArea(SourceFeature sf, FeatureCollector fc) {
    var polygon = fc.polygon(this.name());
    polygon.setZoomRange(13, 15);
    polygon.setMinPixelSize(1.0);

    AttributeProcessor.setAttributes(sf, polygon, PRIMARY_TAGS, config);

    var detailMinZoom = Math.min(14, polygon.getMinZoomForPixelSize(16));
    AttributeProcessor.setAttributesWithMinzoom(sf, polygon, DETAIL_TAGS, detailMinZoom, config);

    if (sf.hasTag("name") || sf.hasTag("ref")) {
      var label = fc.pointOnSurface(this.name());
      label.setMinZoom(Math.min(15, detailMinZoom));
      label.setBufferPixels(32);

      AttributeProcessor.setAttributes(sf, label, PRIMARY_TAGS, config);
      AttributeProcessor.setAttributes(sf, label, DETAIL_TAGS, config);
    }
  }

  private void processPlaygroundLine(SourceFeature sf, FeatureCollector fc) {
    var line = fc.line(this.name());
    line.setZoomRange(14, 15);
    line.setMinPixelSize(8.0);

    AttributeProcessor.setAttributes(sf, line, PRIMARY_TAGS, config);

    var detailMinZoom = line.getMinZoomForPixelSize(16);
    AttributeProcessor.setAttributesWithMinzoom(sf, line, DETAIL_TAGS, detailMinZoom, config);
  }

  private void processPlaygroundPoint(SourceFeature sf, FeatureCollector fc) {
    var point = fc.point(this.name());
    point.setMinZoom(15);
    point.setBufferPixels(32);

    AttributeProcessor.setAttributes(sf, point, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, point, DETAIL_TAGS, config);
  }

  private void processGolfArea(SourceFeature sf, FeatureCollector fc) {
    var polygon = fc.polygon(this.name());
    polygon.setZoomRange(13, 15);
    polygon.setMinPixelSize(1.0);

    AttributeProcessor.setAttributes(sf, polygon, PRIMARY_TAGS, config);

    var detailMinZoom = Math.min(14, polygon.getMinZoomForPixelSize(16));
    AttributeProcessor.setAttributesWithMinzoom(sf, polygon, DETAIL_TAGS, detailMinZoom, config);

    if (sf.hasTag("name") || sf.hasTag("ref")) {
      var label = fc.pointOnSurface(this.name());
      label.setMinZoom(Math.min(15, detailMinZoom));
      label.setBufferPixels(32);

      AttributeProcessor.setAttributes(sf, label, PRIMARY_TAGS, config);
      AttributeProcessor.setAttributes(sf, label, DETAIL_TAGS, config);
    }
  }

  private void processGolfLine(SourceFeature sf, FeatureCollector fc) {
    var line = fc.line(this.name());
    line.setZoomRange(14, 15);
    line.setMinPixelSize(8.0);

    AttributeProcessor.setAttributes(sf, line, PRIMARY_TAGS, config);

    var detailMinZoom = line.getMinZoomForPixelSize(16);
    AttributeProcessor.setAttributesWithMinzoom(sf, line, DETAIL_TAGS, detailMinZoom, config);
  }

  private void processGolfPoint(SourceFeature sf, FeatureCollector fc) {
    var point = fc.point(this.name());
    point.setMinZoom(15);
    point.setBufferPixels(32);

    AttributeProcessor.setAttributes(sf, point, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, point, DETAIL_TAGS, config);
  }

  private int getLabelMinZoom(SourceFeature sf) {
    return switch (sf.getString("leisure")) {
      case "nature_reserve", "park", "garden", "common" -> 13;
      case "stadium", "marina", "golf_course", "beach_resort" -> 13;
      case "playground", "track", "slipway" -> 14;
      case
        "fitness_center",
        "resort",
        "water_park",
        "miniature_golf",
        "ice_rink",
        "amusement_arcade",
        "sports_hall",
        "dog_park",
        "horse_riding" -> 14;
      default -> 15;
    };
  }

  @Override
  public List<VectorTile.Feature> postProcess(int zoom, List<VectorTile.Feature> items)
    throws GeometryException {
    //if (zoom < 15) {
    items = FeatureMerge.mergeMultiPoint(items);
    items = FeatureMerge.mergeLineStrings(items, 5.0, 0.25, 8);
    items = FeatureMerge.mergeNearbyPolygons(items, 3.0, 3.0, 0.5, 0.5);
    //}

    return items;
  }
}

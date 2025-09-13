package fyi.osm.sourdough.layers;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.FeatureCollector.Feature;
import com.onthegomap.planetiler.FeatureMerge;
import com.onthegomap.planetiler.ForwardingProfile.FeatureProcessor;
import com.onthegomap.planetiler.ForwardingProfile.LayerPostProcessor;
import com.onthegomap.planetiler.ForwardingProfile.OsmRelationPreprocessor;
import com.onthegomap.planetiler.VectorTile;
import com.onthegomap.planetiler.expression.Expression;
import com.onthegomap.planetiler.geo.GeometryException;
import com.onthegomap.planetiler.reader.SourceFeature;
import com.onthegomap.planetiler.reader.osm.OsmElement;
import com.onthegomap.planetiler.reader.osm.OsmRelationInfo;
import fyi.osm.sourdough.Configuration;
import fyi.osm.sourdough.util.AttributeProcessor;
import java.util.*;

public class Highways implements FeatureProcessor, LayerPostProcessor {

  private final Configuration config;

  public Highways(Configuration config) {
    this.config = config;
  }

  public static final String LAYER_NAME = "highways";

  public String name() {
    return LAYER_NAME;
  }

  public static final Set<String> PRIMARY_TAGS = Set.of("highway", "expressway", "junction");

  public static final Set<String> LABEL_TAGS = Set.of("name", "ref", "surface");

  public static final Set<String> LAYER_TAGS = Set.of(
    "layer",
    "bridge",
    "tunnel",
    "location",
    "covered",
    "indoor"
  );

  public static final Set<String> DETAIL_TAGS = Set.of(
    "service",
    "junction",
    "dual_carriageway",
    "motorroad",
    "oneway",
    "informal",
    "operator",
    "website",
    "access",
    "motor_vehicle",
    "bicycle",
    "foot",
    "wheelchair",
    "dog",
    "supervised",
    "lit",
    "smoothness",
    "sac_scale",
    "trail_visibility",
    "mtb",
    "mtb:scale",
    "mtb:scale:imba"
  );

  @Override
  public Expression filter() {
    return Expression.or(Expression.matchField("highway"), Expression.matchField("junction"));
  }

  @Override
  public void processFeature(SourceFeature sf, FeatureCollector fc) {
    if (sf.hasTag("highway")) {
      if (sf.hasTag("highway", "proposed", "construction")) {
        return;
      }

      var isArea = sf.hasTag("area", "yes") || sf.hasTag("highway", "rest_area");

      if (sf.canBeLine() && !isArea) {
        this.processHighwayLine(sf, fc);
      } else if (sf.canBePolygon()) {
        this.processHighwayArea(sf, fc);
      } else if (sf.isPoint()) {
        this.processHighwayPoint(sf, fc);
      }
    } else if (sf.hasTag("junction")) {
      if (sf.isPoint()) {
        this.processJunctionPoint(sf, fc);
      }
    }
  }

  private void processHighwayLine(SourceFeature sf, FeatureCollector fc) {
    var minZoom = getHighwayLineMinZoom(sf);

    var line = fc.line(this.name());
    line.setMinPixelSize(0);
    line.setPixelTolerance(0);
    line.setMinZoom(minZoom);

    AttributeProcessor.setAttributes(sf, line, PRIMARY_TAGS, config);

    var labelMinZoom = Math.min(minZoom + 3, 14);
    AttributeProcessor.setAttributesWithMinzoom(sf, line, LABEL_TAGS, labelMinZoom, config);
    AttributeProcessor.setAttributesWithMinzoom(sf, line, DETAIL_TAGS, labelMinZoom, config);

    AttributeProcessor.setAttributesWithMinzoom(sf, line, LAYER_TAGS, 12, config);

    // Special processing for surface tag (distill down to paved/unpaved)
    var surfaceCategory = surfaceCategory(sf);
    if (surfaceCategory != null) {
      line.setAttrWithMinzoom("surface", surfaceCategory, 11);
    }
  }

  private void processHighwayArea(SourceFeature sf, FeatureCollector fc) {
    var polygon = fc.polygon(this.name());
    polygon.setMinPixelSize(32);

    AttributeProcessor.setAttributes(sf, polygon, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, polygon, LABEL_TAGS, config);
    AttributeProcessor.setAttributes(sf, polygon, LAYER_TAGS, config);
    AttributeProcessor.setAttributes(sf, polygon, DETAIL_TAGS, config);

    // Special processing for surface tag (see above)
    var surfaceCategory = surfaceCategory(sf);
    if (surfaceCategory != null) {
      polygon.setAttr("surface", surfaceCategory);
    }
  }

  private void processHighwayPoint(SourceFeature sf, FeatureCollector fc) {
    var point = fc.point(this.name());
    point.setMinZoom(getLabelMinZoom(sf));

    AttributeProcessor.setAttributes(sf, point, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, point, LABEL_TAGS, config);
    AttributeProcessor.setAttributes(sf, point, LAYER_TAGS, config);
    AttributeProcessor.setAttributes(sf, point, DETAIL_TAGS, config);

    var surfaceCategory = surfaceCategory(sf);
    if (surfaceCategory != null) {
      point.setAttr("surface", surfaceCategory);
    }
  }

  private void processJunctionPoint(SourceFeature sf, FeatureCollector fc) {
    var point = fc.point(this.name());
    point.setMinZoom(14);

    AttributeProcessor.setAttributes(sf, point, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, point, LABEL_TAGS, config);
    AttributeProcessor.setAttributes(sf, point, LAYER_TAGS, config);
    AttributeProcessor.setAttributes(sf, point, DETAIL_TAGS, config);

    var surfaceCategory = surfaceCategory(sf);
    if (surfaceCategory != null) {
      point.setAttr("surface", surfaceCategory);
    }
  }

  private int getHighwayLineMinZoom(SourceFeature sf) {
    if (sf.hasTag("footway", "sidewalk", "crossing")) return 14;
    if (sf.hasTag("service", "driveway", "parking_aisle")) return 14;
    if (sf.hasTag("indoor", "yes")) return 14;

    return switch (sf.getString("highway")) {
      case "motorway", "motorway_link" -> 3;
      case "trunk", "trunk_link" -> sf.hasTag("expressway", "yes") ? 3 : 4;
      case "primary", "primary_link" -> 7;
      case "secondary", "secondary_link" -> 9;
      case "tertiary", "tertiary_link" -> 10;
      case "residential", "unclassified" -> 11;
      case "trailhead" -> 11;
      case "track", "path", "footway", "cycleway", "bridleway" -> sf.hasTag("name") ? 11 : 12;
      case "service", "busway", "pedestrian", "living_street" -> 12;
      default -> 12;
    };
  }

  private int getLabelMinZoom(SourceFeature sf) {
    return switch (sf.getString("highway")) {
      case "trailhead" -> 11;
      case "turning_circle", "turning_loop" -> 14;
      default -> 15;
    };
  }

  private static final Set<String> PAVED_SURFACES = Set.of(
    "paved",
    "asphalt",
    "concrete",
    "chipseal",
    "concrete:plates",
    "concrete:lanes",
    "paving_stones",
    "sett",
    "cobblestone",
    "unhewn_cobblestone",
    "brick",
    "bricks",
    "wood",
    "metal"
  );

  private static final Set<String> UNPAVED_SURFACES = Set.of(
    "unpaved",
    "gravel",
    "fine_gravel",
    "dirt",
    "ground",
    "earth",
    "sand",
    "woodchips"
  );

  private String surfaceCategory(SourceFeature sf) {
    var surface = sf.getString("surface");
    if (surface == null) return null;

    if (PAVED_SURFACES.contains(surface)) return "paved";
    if (UNPAVED_SURFACES.contains(surface)) return "unpaved";
    return null;
  }

  @Override
  public List<VectorTile.Feature> postProcess(int zoom, List<VectorTile.Feature> items)
    throws GeometryException {
    // At zoom 12+, don't remove short segments to prevent gaps in the network
    // when physical detail tags create segment breaks
    var minLength = zoom >= 12 ? 0.0 : 1.0;

    items = FeatureMerge.mergeMultiPoint(items);
    items = FeatureMerge.mergeLineStrings(items, minLength, 0.125, 8);
    items = FeatureMerge.mergeNearbyPolygons(items, 3.0, 3.0, 0.5, 0.5);

    return items;
  }
}

package fyi.osm.sourdough.layers;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.FeatureMerge;
import com.onthegomap.planetiler.ForwardingProfile.FeatureProcessor;
import com.onthegomap.planetiler.ForwardingProfile.LayerPostProcessor;
import com.onthegomap.planetiler.ForwardingProfile.OsmRelationPreprocessor;
import com.onthegomap.planetiler.VectorTile;
import com.onthegomap.planetiler.expression.Expression;
import com.onthegomap.planetiler.geo.GeometryException;
import com.onthegomap.planetiler.reader.SourceFeature;
import com.onthegomap.planetiler.reader.osm.OsmElement;
import com.onthegomap.planetiler.reader.osm.OsmReader;
import com.onthegomap.planetiler.reader.osm.OsmRelationInfo;
import fyi.osm.sourdough.Configuration;
import fyi.osm.sourdough.Constants;
import fyi.osm.sourdough.util.AttributeProcessor;
import fyi.osm.sourdough.util.Utils;
import java.util.List;
import java.util.Set;

public class Routes implements FeatureProcessor, LayerPostProcessor, OsmRelationPreprocessor {

  private final Configuration config;

  public Routes(Configuration config) {
    this.config = config;
  }

  public static final String LAYER_NAME = "routes";

  public String name() {
    return LAYER_NAME;
  }

  public static final Set<String> PRIMARY_TAGS = Set.of("route");

  // FIXME: most of these are unused, since RouteRecord does not store them
  public static final Set<String> DETAIL_TAGS = Utils.union(
    Constants.COMMON_DETAIL_TAGS,
    Set.of(
      "network",
      "operator",
      "from",
      "to",
      "via",
      "osmc:symbol",
      "colour",
      "network:wikidata",
      "distance",
      "symbol",
      "website",
      "roundtrip",
      "duration",
      "fee",
      "cycle_network"
    )
  );

  private record RouteRecord(
    long id,
    String route,
    String network,
    String ref,
    String name,
    String operator
    // TODO: add other DETAIL_TAGS fields from relations
  ) implements OsmRelationInfo {}

  @Override
  public List<OsmRelationInfo> preprocessOsmRelation(OsmElement.Relation relation) {
    if (!relation.hasTag("type", "route") || !relation.hasTag("route")) {
      return null;
    }

    if (relation.hasTag("route", "proposed")) {
      return null;
    }

    // FIXME: right? we want these?
    if (relation.hasTag("route", "road")) {
      return null;
    }

    return List.of(
      new RouteRecord(
        relation.id(),
        relation.getString("route"),
        relation.getString("network"),
        relation.getString("ref"),
        relation.getString("name"),
        relation.getString("operator")
      )
    );
  }

  @Override
  public void processFeature(SourceFeature sf, FeatureCollector fc) {
    // Handle route relations that have been preprocessed and applied to ways
    List<OsmReader.RelationMember<RouteRecord>> routes = sf.relationInfo(RouteRecord.class);
    if (sf.canBeLine() && !routes.isEmpty()) {
      processRouteFromRelation(sf, fc, routes);
      return;
    }

    // Handle ways with direct route=* tags (e.g., ferry routes)
    if (sf.canBeLine() && sf.hasTag("route")) {
      processRouteWay(sf, fc);
      return;
    }
  }

  private void processRouteFromRelation(
    SourceFeature sf,
    FeatureCollector fc,
    List<OsmReader.RelationMember<RouteRecord>> routes
  ) {
    // Process each route relation that this way is part of
    for (var routeMember : routes) {
      var route = routeMember.relation();
      var minZoom = getRouteMinZoom(route.route);
      var detailMinZoom = Math.min(minZoom + 2, 14);

      var line = fc.line(this.name());
      line.setId(route.id * 10 + 3);
      line.setMinZoom(minZoom);
      line.setMinPixelSize(0);
      line.setBufferPixels(8);

      line.setAttr("route", route.route);
      // line.setAttr("name", route.name);
      // line.setAttr("ref", route.ref);
      // line.setAttr("network", route.network);
      // line.setAttr("operator", route.operator);

      line.setAttrWithMinzoom("name", route.name, detailMinZoom);
      line.setAttrWithMinzoom("ref", route.ref, detailMinZoom);
      line.setAttrWithMinzoom("network", route.network, detailMinZoom);
      line.setAttrWithMinzoom("operator", route.operator, detailMinZoom);

      // Add detail attributes from the way itself at higher zoom levels
      // var detailMinZoom = Math.min(minZoom + 3, 15);
      // AttributeProcessor.setAttributesWithMinzoom(sf, line, DETAIL_TAGS, detailMinZoom);
    }
  }

  private void processRouteWay(SourceFeature sf, FeatureCollector fc) {
    var minZoom = getLabelMinZoom(sf);
    var detailMinZoom = Math.min(minZoom + 2, 14);

    var line = fc.line(this.name());
    line.setMinZoom(minZoom);
    line.setMinPixelSize(0);
    line.setBufferPixels(8);

    AttributeProcessor.setAttributes(sf, line, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributesWithMinzoom(sf, line, DETAIL_TAGS, detailMinZoom, config);
  }

  private int getLabelMinZoom(SourceFeature sf) {
    return getRouteMinZoom(sf.getString("route"));
  }

  private int getRouteMinZoom(String routeType) {
    if (routeType == null) return 12;

    return switch (routeType) {
      case "road", "train", "waterway" -> 6;
      case "ferry", "subway", "light_rail", "railway" -> 8;
      case "bus", "trolleybus", "tram", "monorail", "funicular", "share_taxi" -> 9;
      case "hiking", "bicycle", "foot", "mtb" -> 10;
      case "horse", "canoe", "snowmobile", "running", "fitness_trail", "ski", "piste" -> 12;
      default -> 13;
    };
  }

  @Override
  public List<VectorTile.Feature> postProcess(int zoom, List<VectorTile.Feature> items)
    throws GeometryException {
    return FeatureMerge.mergeLineStrings(items, 1.0, 0.125, 8);
  }
}

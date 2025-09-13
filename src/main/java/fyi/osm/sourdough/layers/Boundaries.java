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
import com.onthegomap.planetiler.util.Parse;
import fyi.osm.sourdough.Configuration;
import fyi.osm.sourdough.Constants;
import fyi.osm.sourdough.util.AttributeProcessor;
import fyi.osm.sourdough.util.Utils;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

public class Boundaries implements FeatureProcessor, LayerPostProcessor, OsmRelationPreprocessor {

  private final Configuration config;

  public Boundaries(Configuration config) {
    this.config = config;
  }

  public static final String LAYER_NAME = "boundaries";

  public String name() {
    return LAYER_NAME;
  }

  public static final Set<String> PRIMARY_TAGS = Set.of("boundary", "admin_level", "maritime");

  public static final Set<String> DETAIL_TAGS = Utils.union(
    Constants.COMMON_DETAIL_TAGS,
    Set.of(
      "protected_area",
      "access",
      "leisure",
      "protection_title",
      "protect_class",
      "operator",
      "ownership",
      "claimed_by",
      "disputed_by",
      "recognized_by"
    )
  );

  @Override
  public void processFeature(SourceFeature sf, FeatureCollector fc) {
    // Handle administrative boundary lines from relations
    List<OsmReader.RelationMember<BoundaryRecord>> recs = sf.relationInfo(BoundaryRecord.class);

    if (sf.canBeLine() && !recs.isEmpty()) {
      processAdministrativeBoundaryLine(sf, fc, recs);
      return;
    }

    // Handle boundary polygons
    if (sf.canBePolygon() && sf.hasTag("boundary")) {
      processBoundaryArea(sf, fc);
    }
  }

  private void processAdministrativeBoundaryLine(
    SourceFeature sf,
    FeatureCollector fc,
    List<OsmReader.RelationMember<BoundaryRecord>> recs
  ) {
    int minAdminLevel = recs
      .stream()
      .mapToInt(r -> r.relation().adminLevel())
      .min()
      .orElse(99);

    if (minAdminLevel > 8) {
      return;
    }

    boolean disputed = recs.stream().anyMatch(r -> r.relation().disputed());

    var line = fc.line(this.name());
    line.setMinPixelSize(0);
    line.setMinZoom(getAdminBoundaryMinZoom(minAdminLevel));

    line.setAttr("boundary", "administrative");
    line.setAttr("admin_level", minAdminLevel);
    if (
      disputed ||
      sf.hasTag("boundary", "disputed", "claim") ||
      sf.hasTag("disputed", "yes") ||
      sf.hasTag("disputed_by") ||
      sf.hasTag("claimed_by")
    ) {
      line.setAttr("disputed", true);
    }
    if (sf.hasTag("maritime", "yes")) {
      line.setAttr("maritime", true);
    }
  }

  private void processBoundaryArea(SourceFeature sf, FeatureCollector fc) {
    if (sf.hasTag("boundary", "national_park", "protected_area", "aboriginal_lands")) {
      var polygon = fc.polygon(this.name());
      polygon.setZoomRange(2, 15);
      polygon.setMinPixelSize(2.0);

      AttributeProcessor.setAttributes(sf, polygon, PRIMARY_TAGS, config);

      var detailMinZoom = polygon.getMinZoomForPixelSize(32);
      AttributeProcessor.setAttributesWithMinzoom(sf, polygon, DETAIL_TAGS, detailMinZoom, config);

      if (sf.hasTag("name")) {
        var label = fc.pointOnSurface(this.name());
        label.setMinZoom(detailMinZoom);
        label.setBufferPixels(32);

        AttributeProcessor.setAttributes(sf, label, PRIMARY_TAGS, config);
        AttributeProcessor.setAttributes(sf, label, DETAIL_TAGS, config);
      }
    }
  }

  private int getAdminBoundaryMinZoom(int adminLevel) {
    return switch (adminLevel) {
      case 1, 2 -> 0;
      case 3, 4 -> 3;
      case 5, 6 -> 7;
      default -> 10;
    };
  }

  @Override
  public List<OsmRelationInfo> preprocessOsmRelation(OsmElement.Relation relation) {
    if (
      relation.hasTag("type", "boundary") &&
      relation.hasTag("boundary", "administrative", "disputed", "claim")
    ) {
      Integer adminLevel = Parse.parseIntOrNull(relation.getString("admin_level"));
      boolean disputed =
        relation.hasTag("boundary", "disputed", "claim") ||
        relation.hasTag("disputed", "yes") ||
        relation.hasTag("disputed_by") ||
        relation.hasTag("claimed_by");

      if (adminLevel == null || adminLevel > 8) return null;
      return List.of(
        new BoundaryRecord(
          relation.id(),
          relation.getString("boundary"),
          (int) adminLevel,
          disputed
        )
      );
    }
    return null;
  }

  @Override
  public List<VectorTile.Feature> postProcess(int zoom, List<VectorTile.Feature> items)
    throws GeometryException {
    items = FeatureMerge.mergeLineStrings(items, 0, 0.25, 4, true);
    items = FeatureMerge.mergeOverlappingPolygons(items, 2.0);
    return items;
  }

  private record BoundaryRecord(long id, String kind, int adminLevel, boolean disputed) implements
    OsmRelationInfo {}
}

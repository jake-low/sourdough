package fyi.osm.sourdough.layers;

import static com.onthegomap.planetiler.util.Parse.parseIntOrNull;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.ForwardingProfile.FeatureProcessor;
import com.onthegomap.planetiler.expression.Expression;
import com.onthegomap.planetiler.reader.SourceFeature;
import com.onthegomap.planetiler.util.SortKey;
import com.onthegomap.planetiler.util.ZoomFunction;
import fyi.osm.sourdough.Configuration;
import fyi.osm.sourdough.Constants;
import fyi.osm.sourdough.util.AttributeProcessor;
import fyi.osm.sourdough.util.Utils;
import java.util.Map;
import java.util.Set;

public class Places implements FeatureProcessor {

  private final Configuration config;

  public Places(Configuration config) {
    this.config = config;
  }

  public static final String LAYER_NAME = "places";

  public String name() {
    return LAYER_NAME;
  }

  public static final Set<String> PRIMARY_TAGS = Set.of("place", "name");

  public static final Set<String> DETAIL_TAGS = Utils.union(
    Constants.COMMON_DETAIL_TAGS,
    Set.of("ref", "admin_level", "ISO3166-1", "capital", "population", "ele")
  );

  private static final ZoomFunction<Number> LOCALITY_GRID_SIZE_ZOOM_FUNCTION =
    ZoomFunction.fromMaxZoomThresholds(Map.of(2, 16, 4, 32, 20, 64));

  private static final ZoomFunction<Number> LOCALITY_GRID_LIMIT_ZOOM_FUNCTION =
    ZoomFunction.fromMaxZoomThresholds(Map.of(20, 3));

  private record ZoomRange(double minZoom, double maxZoom) {}

  @Override
  public Expression filter() {
    return Expression.and(
      Expression.matchAny(
        "place",
        "country",
        "state",
        "province",
        "city",
        "borough",
        "suburb",
        "quarter",
        "neighbourhood",
        "town",
        "village",
        "hamlet",
        "isolated_dwelling",
        "farm",
        "allotments"
      ),
      Expression.matchField("name")
    );
  }

  @Override
  public void processFeature(SourceFeature sf, FeatureCollector fc) {
    if (sf.isPoint()) {
      processPlacePoint(sf, fc);
    }
  }

  private void processPlacePoint(SourceFeature sf, FeatureCollector fc) {
    var point = fc.point(this.name());
    point.setMinZoom(getLabelMinZoom(sf));

    // TODO: since we only emit point features for places, there isn't a
    // difference between primary and detail tags (both are always included on
    // any feature)
    AttributeProcessor.setAttributes(sf, point, PRIMARY_TAGS, config);
    AttributeProcessor.setAttributes(sf, point, DETAIL_TAGS, config);

    point.setBufferPixels(64);
    point.setSortKey(getSortKey(sf).get());

    if (!sf.hasTag("place", "country", "state", "province")) {
      point.setPointLabelGridPixelSize(LOCALITY_GRID_SIZE_ZOOM_FUNCTION);
      point.setPointLabelGridLimit(LOCALITY_GRID_LIMIT_ZOOM_FUNCTION);
    }
  }

  private int getLabelMinZoom(SourceFeature sf) {
    int minZoom = switch (sf.getString("place")) {
      case "country" -> 2;
      case "state", "province" -> 3;
      case "city" -> 3;
      case "town" -> 5;
      case "village" -> 9;
      case "borough" -> 9;
      default -> 11;
    };

    if (sf.hasTag("place", "city", "town")) {
      int population = getPopulation(sf);

      if (population > 1_000_000) {
        minZoom = 1;
      } else if (population > 500_000) {
        minZoom = 2;
      } else if (population > 250_000) {
        minZoom = 3;
      } else if (population > 100_000) {
        minZoom = 4;
      } else if (population > 50_000) {
        minZoom = 5;
      } else if (population > 10_000) {
        minZoom = 6;
      } else if (population > 1_000) {
        minZoom = 7;
      } else {
        minZoom = 8;
      }

      if (sf.hasTag("capital", "yes", "2", "3") && minZoom >= 3) {
        minZoom -= 1;
      } else if (sf.hasTag("capital", "4") && minZoom >= 5) {
        minZoom -= 1;
      }
    }

    return Math.max(minZoom, 0);
  }

  private SortKey getSortKey(SourceFeature sf) {
    int rank = getRank(sf);
    int population = getPopulation(sf);

    return SortKey
      // Ascending (rank 0 = countries, 1 = states, 2 = cities, etc)
      .orderByInt(rank, 0, 7)
      // Descending (higher population wins)
      // CAREFUL: setting the minimum endpoint to 0 (rather than 1) will quietly
      // break sorting, causing all features to be sorted into the same bucket
      .thenByLog(population, 25_000_000, 1, 100);
  }

  private int getRank(SourceFeature sf) {
    return switch (sf.getString("place")) {
      case "country" -> 0;
      case "state", "province" -> 1;
      case "city" -> sf.hasTag("capital", "yes", "2", "3") ? 2 : 3;
      case "town" -> 4;
      case "village" -> 5;
      case "borough" -> 6;
      default -> 7;
    };
  }

  private int getPopulation(SourceFeature sf) {
    if (sf.hasTag("population")) {
      Integer parsed = parseIntOrNull(sf.getString("population"));
      if (parsed != null) {
        return parsed;
      }
    }

    return 0;
  }
}

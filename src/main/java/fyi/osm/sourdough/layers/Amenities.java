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
import fyi.osm.sourdough.util.LabelZooms;
import fyi.osm.sourdough.util.Utils;
import java.util.List;
import java.util.Set;

public class Amenities implements FeatureProcessor, LayerPostProcessor {

  private final Configuration config;

  public Amenities(Configuration config) {
    this.config = config;
  }

  public static final String LAYER_NAME = "amenities";

  public String name() {
    return LAYER_NAME;
  }

  private static final List<String> AMENITY_KEYS = Utils.withPrefixes(
    "amenity",
    Constants.LIFECYCLE_PREFIXES
  );

  public static final Set<String> TOP_LEVEL_TAGS = Set.copyOf(AMENITY_KEYS);

  public static final Set<String> PRIMARY_TAGS = TOP_LEVEL_TAGS;

  public static final Set<String> DETAIL_TAGS = Utils.union(
    Constants.COMMON_DETAIL_TAGS,
    Set.of(
      "access",
      "air_conditioning",
      "atm",
      "bicycle_parking",
      "bicycle_rental",
      "booth",
      "branch",
      "brand",
      "capacity",
      "capacity:disabled",
      "changing_table",
      "community_centre",
      "covered",
      "cuisine",
      "denomination",
      "diet:gluten_free",
      "diet:halal",
      "diet:kosher",
      "diet:vegan",
      "diet:vegetarian",
      "dispensing",
      "drive_through",
      "fair_trade",
      "fee",
      "female",
      "fountain",
      "gender_segregated",
      "indoor",
      "indoor_seating",
      "internet_access",
      "layer",
      "level",
      "location",
      "male",
      "organic",
      "outdoor_seating",
      "parking",
      "parking_space",
      "private",
      "recycling:cans",
      "recycling:clothes",
      "recycling:glass",
      "recycling:glass_bottles",
      "recycling:paper",
      "recycling:plastic",
      "recycling:plastic_bottles",
      "recycling:plastic_packaging",
      "recycling_type",
      "ref",
      "religion",
      "reservation",
      "self_service",
      "smoking",
      "social_facility",
      "social_facility:for",
      "surface",
      "takeaway",
      "toilets",
      "toilets:disposal",
      "townhall:type",
      "unisex",
      "vending",
      "waste",
      "wheelchair"
    )
  );

  @Override
  public Expression filter() {
    return Expression.or(
      TOP_LEVEL_TAGS.stream().map(Expression::matchField).toArray(Expression[]::new)
    );
  }

  @Override
  public void processFeature(SourceFeature sf, FeatureCollector fc) {
    if (sf.canBePolygon()) {
      processAmenityArea(sf, fc);
    } else if (sf.isPoint()) {
      processAmenityPoint(sf, fc);
    }
  }

  private void processAmenityArea(SourceFeature sf, FeatureCollector fc) {
    var polygon = fc.polygon(this.name());
    polygon.setZoomRange(2, 15);
    polygon.setMinPixelSize(2.0);

    AttributeProcessor.setAttributes(sf, polygon, PRIMARY_TAGS, config);

    var detailMinZoom = polygon.getMinZoomForPixelSize(32);
    AttributeProcessor.setAttributesWithMinzoom(sf, polygon, DETAIL_TAGS, detailMinZoom, config);

    if (sf.hasTag("name") || sf.hasTag("ref")) {
      Utils.createLabelPoint(sf, fc, this.name(), getLabelZooms(sf), PRIMARY_TAGS, DETAIL_TAGS, config);
    }
  }

  private void processAmenityPoint(SourceFeature sf, FeatureCollector fc) {
    Utils.createPoint(sf, fc, this.name(), getLabelZooms(sf), PRIMARY_TAGS, DETAIL_TAGS, config);
  }

  private LabelZooms getLabelZooms(SourceFeature sf) {
    return switch (Utils.getFirstTag(sf, AMENITY_KEYS)) {
      case "hospital", "university" -> new LabelZooms(11, 14);
      case
        "place_of_worship",
        "school",
        "college",
        "post_office",
        "police",
        "clinic",
        "townhall",
        "community_centre",
        "library" -> new LabelZooms(12, 15);
      case
        "pharmacy",
        "bank",
        "fire_station",
        "courthouse",
        "kindergarten",
        "social_facility",
        "veterinary",
        "conference_center" -> new LabelZooms(13, 15);
      // case
      //   "restaurant",
      //   "cafe",
      //   "bar",
      //   "pub",
      //   "biergarten",
      //   "fast_food",
      //   "food_court",
      //   "ice_cream",
      //   "nightclub" -> new LabelZooms(14, 16);
      case
        "bench",
        "lounger",
        "bbq",
        "vending_machine",
        "public_bookcase",
        "give_box",
        "parcel_locker",
        "clock",
        "fountain",
        "waste_basket",
        "waste_disposal",
        "recycling",
        "telephone",
        "drinking_water",
        "water_point",
        "watering_place",
        "payment_terminal",
        "device_charging_station",
        "parking_space",
        "bicycle_parking",
        "stadium_seating" -> new LabelZooms(15, 17);
      default -> new LabelZooms(14, 16);
    };
  }

  @Override
  public List<VectorTile.Feature> postProcess(int zoom, List<VectorTile.Feature> items)
    throws GeometryException {
    items = FeatureMerge.mergeMultiPoint(items);
    items = FeatureMerge.mergeLineStrings(items, 5.0, 0.25, 8);
    items = FeatureMerge.mergeNearbyPolygons(items, 3.0, 3.0, 0.5, 0.5);

    return items;
  }
}

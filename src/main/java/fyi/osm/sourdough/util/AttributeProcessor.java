package fyi.osm.sourdough.util;

import com.onthegomap.planetiler.FeatureCollector;
import com.onthegomap.planetiler.reader.SourceFeature;
import com.onthegomap.planetiler.util.Parse;
import fyi.osm.sourdough.Configuration;
import fyi.osm.sourdough.Constants;
import java.util.Map;
import java.util.Set;

public class AttributeProcessor {

  public enum AttributeType {
    STRING,
    BOOLEAN,
    INTEGER,
    DOUBLE,
    LENGTH_METERS,
  }

  private static final Map<String, AttributeType> GLOBAL_TYPE_MAP = Map.ofEntries(
    // Numeric types
    Map.entry("admin_level", AttributeType.INTEGER),
    Map.entry("building:levels", AttributeType.INTEGER),
    Map.entry("building:levels:underground", AttributeType.INTEGER),
    Map.entry("capacity", AttributeType.INTEGER),
    Map.entry("capacity:disabled", AttributeType.INTEGER),
    Map.entry("layer", AttributeType.INTEGER),
    Map.entry("level", AttributeType.INTEGER),
    Map.entry("population", AttributeType.INTEGER),
    // Special types
    Map.entry("ele", AttributeType.LENGTH_METERS),
    Map.entry("height", AttributeType.LENGTH_METERS)
    // ... all other tags are treated as strings
  );

  public static AttributeType getType(String key) {
    return GLOBAL_TYPE_MAP.getOrDefault(key, AttributeType.STRING);
  }

  public static Object parse(String value, AttributeType type) {
    return switch (type) {
      case STRING -> value;
      case BOOLEAN -> parseBoolOrNull(value);
      case INTEGER -> Parse.parseIntOrNull(value);
      case DOUBLE -> Parse.parseDoubleOrNull(value);
      case LENGTH_METERS -> Parse.meters(value);
    };
  }

  public static void setAttributes(
    SourceFeature sf,
    FeatureCollector.Feature feature,
    Set<String> keys,
    Configuration config
  ) {
    for (var key : keys) {
      var value = getValue(sf, key, config);
      if (value != null) {
        var type = getType(key);
        var parsed = parse(value, type);
        feature.setAttr(key, parsed);
      }
      setAdditionalLanguageAttrs(sf, feature, key, null, config);
    }
  }

  public static void setAttributesWithMinzoom(
    SourceFeature sf,
    FeatureCollector.Feature feature,
    Set<String> keys,
    int minZoom,
    Configuration config
  ) {
    for (var key : keys) {
      var value = getValue(sf, key, config);
      if (value != null) {
        var type = getType(key);
        var parsed = parse(value, type);
        feature.setAttrWithMinzoom(key, parsed, minZoom);
      }
      setAdditionalLanguageAttrs(sf, feature, key, minZoom, config);
    }
  }

  private static String getValue(SourceFeature sf, String key, Configuration config) {
    if (config.hasLanguage() && Constants.LOCALIZABLE_NAME_KEYS.contains(key)) {
      String localizedName = sf.getString(key + ":" + config.language());
      if (localizedName != null) {
        return localizedName;
      }
    }
    return sf.getString(key);
  }

  // Emit language-suffixed variants of the given tag key for each language listed
  // in --additional-languages (if the feature has no name tag in a given language,
  // no attribute is emitted).
  private static void setAdditionalLanguageAttrs(
    SourceFeature sf,
    FeatureCollector.Feature feature,
    String key,
    Integer minZoom,
    Configuration config
  ) {
    if (!config.hasAdditionalLanguages() || !Constants.LOCALIZABLE_NAME_KEYS.contains(key)) {
      return;
    }
    for (var language : config.additionalLanguages()) {
      var suffixedKey = key + ":" + language;
      var value = sf.getString(suffixedKey);
      if (value != null) {
        if (minZoom == null) {
          feature.setAttr(suffixedKey, value);
        } else {
          feature.setAttrWithMinzoom(suffixedKey, value, minZoom);
        }
      }
    }
  }

  private static Object parseBoolOrNull(String value) {
    return switch (value) {
      case "yes" -> true;
      case "no" -> false;
      default -> null;
    };
  }
}

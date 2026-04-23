package fyi.osm.sourdough.util;

import com.onthegomap.planetiler.reader.SourceFeature;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Utils {

  /**
   * Returns the union of two or more sets as an immutable set.
   */
  @SafeVarargs
  public static <T> Set<T> union(Set<T>... sets) {
    var result = new HashSet<T>();
    for (var set : sets) {
      result.addAll(set);
    }
    return Set.copyOf(result);
  }

  /** Returns a list containing the key followed by each prefix applied to it. */
  public static List<String> withPrefixes(String key, Set<String> prefixes) {
    var result = new ArrayList<String>(1 + prefixes.size());
    result.add(key);
    for (var prefix : prefixes) {
      result.add(prefix + ":" + key);
    }
    return List.copyOf(result);
  }

  /** Returns the value of the first matching tag, or null if none match. */
  public static String getFirstTag(SourceFeature sf, Collection<String> keys) {
    for (var key : keys) {
      var value = sf.getString(key);
      if (value != null) return value;
    }
    return null;
  }

  /** Returns true if the source feature has any of the given tags. */
  public static boolean hasAnyTag(SourceFeature sf, Collection<String> keys) {
    for (var key : keys) {
      if (sf.hasTag(key)) return true;
    }
    return false;
  }
}

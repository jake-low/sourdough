package fyi.osm.sourdough.util;

import java.util.HashSet;
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
}

package fyi.osm.sourdough;

import java.util.Set;

public class Constants {

  public static final Set<String> COMMON_DETAIL_TAGS = Set.of(
    "name",
    "ref",
    "alt_name",
    "short_name",
    "official_name",
    "wikidata",
    "wikipedia"
  );

  public static final Set<String> LIFECYCLE_PREFIXES = Set.of(
    // future
    "proposed",
    "planned",
    "construction",
    // past
    "disused",
    "abandoned",
    "ruins",
    "demolished",
    "removed",
    "razed",
    "destroyed",
    "was"
  );
}

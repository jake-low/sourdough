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

  // Name-like tags that can have language-suffixed variants in OSM (e.g. name:fr).
  // These are the keys affected by the --language and --additional-languages options.
  public static final Set<String> LOCALIZABLE_NAME_KEYS = Set.of(
    "name",
    "alt_name",
    "short_name",
    "official_name"
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

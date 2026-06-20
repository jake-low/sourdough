package fyi.osm.sourdough;

import java.util.List;

public record Configuration(String language, List<String> additionalLanguages) {
  public static Configuration defaults() {
    return new Configuration(null, List.of());
  }

  public boolean hasLanguage() {
    return language != null;
  }

  public boolean hasAdditionalLanguages() {
    return additionalLanguages != null && !additionalLanguages.isEmpty();
  }
}

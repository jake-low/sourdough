package fyi.osm.sourdough;

public record Configuration(String language) {
  
  public static Configuration defaults() {
    return new Configuration(null);
  }
  
  public boolean hasLanguage() {
    return language != null;
  }
}
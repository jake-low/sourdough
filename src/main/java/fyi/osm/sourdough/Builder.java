package fyi.osm.sourdough;

import com.onthegomap.planetiler.ForwardingProfile;
import com.onthegomap.planetiler.Planetiler;
import com.onthegomap.planetiler.config.Arguments;
import fyi.osm.sourdough.layers.Advertising;
import fyi.osm.sourdough.layers.Aerialways;
import fyi.osm.sourdough.layers.Aeroways;
import fyi.osm.sourdough.layers.Amenities;
import fyi.osm.sourdough.layers.Barriers;
import fyi.osm.sourdough.layers.Boundaries;
import fyi.osm.sourdough.layers.Buildings;
import fyi.osm.sourdough.layers.Clubs;
import fyi.osm.sourdough.layers.Craft;
import fyi.osm.sourdough.layers.Education;
import fyi.osm.sourdough.layers.Emergency;
import fyi.osm.sourdough.layers.Geological;
import fyi.osm.sourdough.layers.Healthcare;
import fyi.osm.sourdough.layers.Highways;
import fyi.osm.sourdough.layers.Historic;
import fyi.osm.sourdough.layers.Landcover;
import fyi.osm.sourdough.layers.Landuse;
import fyi.osm.sourdough.layers.Leisure;
import fyi.osm.sourdough.layers.ManMade;
import fyi.osm.sourdough.layers.Military;
import fyi.osm.sourdough.layers.Natural;
import fyi.osm.sourdough.layers.Offices;
import fyi.osm.sourdough.layers.Pistes;
import fyi.osm.sourdough.layers.Places;
import fyi.osm.sourdough.layers.Power;
import fyi.osm.sourdough.layers.PublicTransport;
import fyi.osm.sourdough.layers.Railways;
import fyi.osm.sourdough.layers.Routes;
import fyi.osm.sourdough.layers.Shops;
import fyi.osm.sourdough.layers.Tourism;
import fyi.osm.sourdough.layers.Water;
import fyi.osm.sourdough.layers.Waterways;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class Builder extends ForwardingProfile {

  private final Configuration config;

  public Builder(Configuration config) {
    this.config = config;
    var layers = List.of(
      new Advertising(config),
      new Aerialways(config),
      new Aeroways(config),
      new Amenities(config),
      new Barriers(config),
      new Boundaries(config),
      new Buildings(config),
      new Clubs(config),
      new Craft(config),
      new Education(config),
      new Emergency(config),
      new Geological(config),
      new Healthcare(config),
      new Highways(config),
      new Historic(config),
      new Landcover(config),
      new Landuse(config),
      new Leisure(config),
      new ManMade(config),
      new Military(config),
      new Natural(config),
      new Offices(config),
      new Pistes(config),
      new Places(config),
      new Power(config),
      new PublicTransport(config),
      new Railways(config),
      new Routes(config),
      new Shops(config),
      new Tourism(config),
      new Water(config),
      new Waterways(config)
    );

    for (var layer : layers) {
      registerHandler(layer);

      // Water layer has special requirement for preprocessed ocean data
      if (layer instanceof Water) {
        registerSourceHandler("osm_water", ((Water) layer)::processPreparedOsm);
      }
    }
  }

  @Override
  public String name() {
    return "Sourdough Tiles";
  }

  @Override
  public String description() {
    return "Vector tiles derived from OpenStreetMap data. See https://sourdough.osm.fyi/";
  }

  @Override
  public String version() {
    return "0.1.0";
  }

  @Override
  public boolean isOverlay() {
    return false;
  }

  @Override
  public String attribution() {
    return "Map data from <a href='https://www.openstreetmap.org/copyright' target='_blank'>OpenStreetMap</a>";
  }

  public static void main(String[] args) throws IOException {
    run(Arguments.fromArgsOrConfigFile(args));
  }

  static void run(Arguments args) throws IOException {
    args = args.orElse(Arguments.of("maxzoom", 15));
    String area = args.getString("area", "geofabrik area to download", "monaco");
    String language = args.getString(
      "language",
      "language code for name substitution (e.g. 'es' for Spanish)",
      null
    );

    var planetiler = Planetiler.create(args)
      .addOsmSource("osm", Path.of("data", "sources", area + ".osm.pbf"), "geofabrik:" + area)
      .addShapefileSource(
        "osm_water",
        Path.of("data", "sources", "water-polygons-split-3857.zip"),
        "https://osmdata.openstreetmap.de/download/water-polygons-split-3857.zip"
      );

    var config = new Configuration(language);
    planetiler.setProfile(new Builder(config)).setOutput("data/sourdough.pmtiles").run();
  }
}

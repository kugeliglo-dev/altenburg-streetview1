package de.altenburg.streetview;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Einfache JSON-Config unter config/altenburgstreetview.json.
 * Das Mapillary-Token kann alternativ ueber die Umgebungsvariable MAPILLARY_TOKEN gesetzt werden.
 */
public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public String mapillaryToken = "";
    public double originLat = GeoMath.ALTENBURG_LAT;
    public double originLon = GeoMath.ALTENBURG_LON;
    /** Halbe Kantenlaenge der Suchbox in Grad (Mapillary erlaubt max. 0.01 Grad Boxgroesse). */
    public double searchRadiusDeg = 0.003;

    public String effectiveToken() {
        String env = System.getenv("MAPILLARY_TOKEN");
        if (env != null && !env.isBlank()) return env.trim();
        return mapillaryToken == null ? "" : mapillaryToken.trim();
    }

    public static Config load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("altenburgstreetview.json");
        try {
            if (Files.exists(path)) {
                Config c = GSON.fromJson(Files.readString(path), Config.class);
                if (c != null) return c;
            }
            Config c = new Config();
            Files.writeString(path, GSON.toJson(c));
            return c;
        } catch (IOException | RuntimeException e) {
            AltenburgStreetViewClient.LOGGER.warn("Config konnte nicht geladen werden, nutze Standardwerte", e);
            return new Config();
        }
    }
}

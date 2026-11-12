package de.altenburg.streetview;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Duenner Client fuer die Mapillary Graph API v4 (offene Strassenfotos, CC-BY-SA). */
public final class MapillaryClient {
    private static final String FIELDS = "id,computed_geometry,compass_angle,is_pano,thumb_2048_url";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final String token;

    public MapillaryClient(String token) {
        this.token = token;
    }

    /** Fotos in einer kleinen Box um den Punkt, sortiert nach Entfernung. Blockierend, nicht auf dem Render-Thread aufrufen. */
    public List<StreetImage> fetchNearby(double lat, double lon, double radiusDeg) throws IOException, InterruptedException {
        if (token == null || token.isBlank()) {
            throw new IOException("Kein Mapillary-Token gesetzt (config/altenburgstreetview.json oder MAPILLARY_TOKEN)");
        }
        // Mapillary: Boxen muessen kleiner als 0.01 Grad sein.
        double r = Math.min(radiusDeg, 0.0049);
        String bbox = (lon - r) + "," + (lat - r) + "," + (lon + r) + "," + (lat + r);
        String url = "https://graph.mapillary.com/images"
                + "?access_token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
                + "&fields=" + FIELDS
                + "&bbox=" + bbox
                + "&limit=200";

        HttpResponse<String> res = http.send(
                HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
            throw new IOException("Mapillary antwortete mit HTTP " + res.statusCode());
        }

        List<StreetImage> out = new ArrayList<>();
        JsonArray data = JsonParser.parseString(res.body()).getAsJsonObject().getAsJsonArray("data");
        if (data == null) return out;
        for (JsonElement el : data) {
            JsonObject o = el.getAsJsonObject();
            if (!o.has("computed_geometry") || !o.has("thumb_2048_url")) continue;
            JsonArray coords = o.getAsJsonObject("computed_geometry").getAsJsonArray("coordinates");
            out.add(new StreetImage(
                    o.get("id").getAsString(),
                    coords.get(1).getAsDouble(),
                    coords.get(0).getAsDouble(),
                    o.has("compass_angle") ? o.get("compass_angle").getAsDouble() : 0.0,
                    o.has("is_pano") && o.get("is_pano").getAsBoolean(),
                    o.get("thumb_2048_url").getAsString()));
        }
        out.sort(Comparator.comparingDouble(i -> GeoMath.distanceMeters(lat, lon, i.lat(), i.lon())));
        return out;
    }

    public byte[] fetchImageBytes(String imageUrl) throws IOException, InterruptedException {
        HttpResponse<byte[]> res = http.send(
                HttpRequest.newBuilder(URI.create(imageUrl)).timeout(Duration.ofSeconds(30)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        if (res.statusCode() != 200) {
            throw new IOException("Bild-Download fehlgeschlagen: HTTP " + res.statusCode());
        }
        return res.body();
    }
}

package de.altenburg.streetview;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Kleines Fenster mit dem Strassenfoto.
 *
 * Bedienung:
 *   Pfeil links/rechts  vorheriges/naechstes Foto (nach Entfernung sortiert)
 *   M                   aktuellen Fotostandort als Messpunkt A setzen
 *   C                   Messpunkt loeschen
 *   Esc / Keybind       schliessen
 *
 * HINWEIS: Fuer Minecraft 1.21.11 geschrieben, aber noch nicht kompiliert. Falls der
 * Compiler bei GuiGraphics, KeyEvent oder DynamicTexture meckert, schick mir die Fehler.
 */
public class StreetViewScreen extends Screen {
    private static final Identifier TEXTURE_ID = Identifier.fromNamespaceAndPath(AltenburgStreetViewClient.MOD_ID, "current_photo");

    private final Config config;
    private final MapillaryClient client;

    private volatile List<StreetImage> images = List.of();
    private volatile String status = "Lade Fotos ...";
    private int index = 0;
    private DynamicTexture texture;
    private int texW, texH;
    private StreetImage measureA;
    private boolean started;

    public StreetViewScreen(Config config) {
        super(Component.literal("Altenburg Street View"));
        this.config = config;
        this.client = new MapillaryClient(config.effectiveToken());
    }

    @Override
    protected void init() {
        if (started) return;
        started = true;
        // Start im Zentrum von Altenburg (konfigurierbar)
        search(config.originLat, config.originLon);
    }

    /** Laedt die Fotos rund um einen Punkt und zeigt das naechstgelegene. */
    private void search(double lat, double lon) {
        status = "Lade Fotos ...";
        CompletableFuture.runAsync(() -> {
            try {
                List<StreetImage> found = client.fetchNearby(lat, lon, config.searchRadiusDeg);
                this.images = found;
                if (found.isEmpty()) {
                    status = "Keine Fotos in der Naehe gefunden";
                } else {
                    loadImage(0);
                }
            } catch (Exception e) {
                AltenburgStreetViewClient.LOGGER.warn("Laden fehlgeschlagen", e);
                status = "Fehler: " + e.getMessage();
            }
        });
    }

    private void loadImage(int newIndex) {
        List<StreetImage> list = images;
        if (list.isEmpty()) return;
        int i = Math.floorMod(newIndex, list.size());
        StreetImage img = list.get(i);
        status = "Lade Foto " + (i + 1) + "/" + list.size() + " ...";
        CompletableFuture.runAsync(() -> {
            try {
                byte[] jpg = client.fetchImageBytes(img.imageUrl());
                // NativeImage kann nur PNG lesen -> JPEG erst umwandeln
                BufferedImage bi = ImageIO.read(new ByteArrayInputStream(jpg));
                ByteArrayOutputStream png = new ByteArrayOutputStream();
                ImageIO.write(bi, "png", png);
                NativeImage ni = NativeImage.read(new ByteArrayInputStream(png.toByteArray()));
                Minecraft.getInstance().execute(() -> {
                    if (texture != null) texture.close();
                    texture = new DynamicTexture(() -> "altenburg_photo", ni);
                    Minecraft.getInstance().getTextureManager().register(TEXTURE_ID, texture);
                    texW = ni.getWidth();
                    texH = ni.getHeight();
                    index = i;
                    status = "";
                });
            } catch (Exception e) {
                AltenburgStreetViewClient.LOGGER.warn("Bild konnte nicht geladen werden", e);
                status = "Fehler: " + e.getMessage();
            }
        });
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        // Fenster: 70 % der Breite, 4:3-aehnlich, mittig
        int w = (int) (this.width * 0.7);
        int h = (int) (this.height * 0.7);
        int x = (this.width - w) / 2;
        int y = (this.height - h) / 2;

        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF000000);

        if (texture != null && texW > 0) {
            g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE_ID, x, y, 0f, 0f, w, h, texW, texH, texW, texH);
        } else {
            g.fill(x, y, x + w, y + h, 0xFF202020);
        }
        if (!status.isEmpty()) {
            g.drawCenteredString(this.font, status, this.width / 2, y + h / 2, 0xFFFFFFFF);
        }

        // Infozeilen unter dem Fenster
        List<StreetImage> list = images;
        int ty = y + h + 6;
        if (!list.isEmpty()) {
            StreetImage cur = list.get(Math.floorMod(index, list.size()));
            if (measureA != null) {
                g.drawString(this.font, String.format("Entfernung zu Punkt A: %.1f m", cur.distanceTo(measureA.lat(), measureA.lon())), x, ty, 0xFF7FFF7F);
                ty += 11;
            }
            g.drawString(this.font, String.format("Foto %d/%d  |  %.5f, %.5f", index + 1, list.size(), cur.lat(), cur.lon()), x, ty, 0xFFAAAAAA);
            ty += 11;
        }
        g.drawString(this.font, "<- ->: Foto wechseln   R: hier neu suchen   M: Punkt A   C: loeschen   Mapillary, CC-BY-SA", x, ty, 0xFF888888);

        super.render(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        List<StreetImage> list = images;
        if (!list.isEmpty()) {
            if (keyCode == GLFW.GLFW_KEY_RIGHT) { loadImage(index + 1); return true; }
            if (keyCode == GLFW.GLFW_KEY_LEFT) { loadImage(index - 1); return true; }
            if (keyCode == GLFW.GLFW_KEY_M) { measureA = list.get(Math.floorMod(index, list.size())); return true; }
            if (keyCode == GLFW.GLFW_KEY_C) { measureA = null; return true; }
            if (keyCode == GLFW.GLFW_KEY_R) {
                StreetImage cur = list.get(Math.floorMod(index, list.size()));
                search(cur.lat(), cur.lon());
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        if (texture != null) {
            Minecraft.getInstance().getTextureManager().release(TEXTURE_ID);
            texture = null;
        }
    }
}

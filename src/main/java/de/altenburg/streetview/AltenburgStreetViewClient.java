package de.altenburg.streetview;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Einstiegspunkt. Der Keybind wird ganz normal als KeyMapping registriert und ist deshalb
 * im Spiel unter Optionen > Steuerung > "Altenburg Street View" frei einstellbar.
 *
 * HINWEIS: Fuer Minecraft 1.21.11 geschrieben, aber noch nicht kompiliert. Namen wie
 * KeyMappingHelper, KeyMapping.Category und Identifier haengen vom Mapping-Stand ab -
 * bei Compilerfehlern bitte die Meldung schicken, dann passe ich es an.
 */
public class AltenburgStreetViewClient implements ClientModInitializer {
    public static final String MOD_ID = "altenburgstreetview";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyMapping openKey;
    public static Config config;

    @Override
    public void onInitializeClient() {
        config = Config.load();

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.altenburgstreetview.open",   // Name in der Steuerungs-Liste (siehe lang-Datei)
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,                   // nur der Standardwert, jederzeit aenderbar
                category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.consumeClick()) {
                toggleWindow(client);
            }
        });
    }

    private static void toggleWindow(Minecraft client) {
        if (client.screen instanceof StreetViewScreen) {
            client.setScreen(null);
        } else if (client.screen == null && client.player != null) {
            client.setScreen(new StreetViewScreen(config));
        }
    }
}

package org.pythonchik.drawler;


import net.fabricmc.api.ModInitializer;
import org.pythonchik.drawler.client.DrawlerSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class Drawler implements ModInitializer {
    public static final String MOD_ID = "drawler";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    @Override
    public void onInitialize() {
        DrawlerConfig.loadConfig();
        DrawlerSettings.loadSettings();
        LOGGER.info("\n   /\\_/\\  \n" +
                    "  ( o.o ) \n" +
                    "  > ^ <\n" +
                    "/       \\\n" +
                    "   meow!");
    }
}

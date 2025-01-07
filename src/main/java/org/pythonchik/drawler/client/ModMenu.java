package org.pythonchik.drawler.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.MinecraftClient;

public class ModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        //return parent -> AutoConfig.getConfigScreen(ModConfig.class, parent).get();
        return parent -> DrawlerSettings.create(MinecraftClient.getInstance().currentScreen);
        //MinecraftClient.getInstance().setScreen(DrawlerSettings.create(MinecraftClient.getInstance().currentScreen));

    }
}


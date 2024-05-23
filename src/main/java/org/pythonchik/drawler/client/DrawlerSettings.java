package org.pythonchik.drawler.client;


import me.shedaniel.cloth.clothconfig.shadowed.org.yaml.snakeyaml.DumperOptions;
import me.shedaniel.cloth.clothconfig.shadowed.org.yaml.snakeyaml.Yaml;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.pythonchik.drawler.Drawler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.*;
import java.util.function.Supplier;


public class DrawlerSettings {

    private static final File settings_file = new File("config/saved.yml");
    public static void saveSettings() {
        Map<String,Object> data = new HashMap<>();
        data.put("scale",DrawlerClient.scale);
        data.put("delay",DrawlerClient.delay);
        data.put("oneback",DrawlerClient.oneback);
        data.put("mode34",DrawlerClient.mode34);
        data.put("needtocorrect",DrawlerClient.needtocorrect);
        data.put("correction_mode",DrawlerClient.correction_mode);
        //add line here to save value

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        try (FileWriter writer = new FileWriter(settings_file)) {
            yaml.dump(data,writer);
        } catch (Exception ignored) {}
    }
    public static void loadSettings(){
        if (!settings_file.exists()) {
            return;
        }

        Yaml yaml = new Yaml();
        try (InputStream inputStream = new FileInputStream(settings_file)) {
            Map<String, Object> data = yaml.load(inputStream);

            DrawlerClient.scale = ((Number) data.getOrDefault("scale", 1)).floatValue();
            DrawlerClient.delay = (Integer) data.getOrDefault("delay", 200);
            DrawlerClient.oneback = (Integer) data.getOrDefault("oneback", 3);
            DrawlerClient.mode34 = (Boolean) data.getOrDefault("mode34", true);
            DrawlerClient.needtocorrect = (Boolean) data.getOrDefault("needtocorrect", true);
            DrawlerClient.correction_mode = (Integer) data.getOrDefault("correction_mode",0);
            //add line here to load value

        } catch (Exception ignored) {}
    }
    static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setDefaultBackgroundTexture(new Identifier("minecraft:textures/block/honeycomb_block.png"))
                .setTitle(Text.translatableWithFallback("settings.title.main","settings"));

        builder.setSavingRunnable(() -> {
            saveSettings();
            Drawler.LOGGER.info("This message is mostly for testing, BUTt after this version this message will contain a hidden message, so keep an eye on this one!");
        });


        ConfigCategory general = builder.getOrCreateCategory(Text.translatableWithFallback("settings.title.general","general"));
        ConfigCategory drawing = builder.getOrCreateCategory(Text.translatableWithFallback("settings.title.drawing","drawing"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        //rendering
        general.addEntry(entryBuilder.startBooleanToggle(Text.translatableWithFallback("settings.option.rendering","Rendering"), DrawlerClient.needtorender)
                .setDefaultValue(true)
                .setTooltip(Text.translatableWithFallback("settings.tooltip.rendering","If you turn this on, you will see preview of your picture in your left upper corner of the screen"))
                .setSaveConsumer(newValue -> DrawlerClient.needtorender = newValue)
                .build());

        //SCALE
        general.addEntry(entryBuilder.startFloatField(Text.translatableWithFallback("settings.option.scale","scale"), DrawlerClient.scale)
                .setDefaultValue(1)
                .setTooltip(Text.translatableWithFallback("settings.tooltip.scale","scale of rendered picture"))
                .setSaveConsumer(newValue -> DrawlerClient.scale = newValue)
                .build());

        //DElAY
        drawing.addEntry(entryBuilder.startIntField(Text.translatableWithFallback("settings.option.delay","delay"), DrawlerClient.delay)
                .setDefaultValue(200)
                .setTooltip(Text.translatableWithFallback("settings.tooltip.delay","Delay between each action"))
                .setSaveConsumer(newValue -> DrawlerClient.delay = newValue)
                .build());

        //BACK
        drawing.addEntry(entryBuilder.startIntField(Text.translatableWithFallback("settings.option.oneback","Step back"), DrawlerClient.oneback)
                .setDefaultValue(3)
                .setTooltip(Text.translatableWithFallback("settings.tooltip.oneback","step back amount"))
                .setSaveConsumer(newValue -> DrawlerClient.oneback = newValue)
                .build());

        //rule 34
        general.addEntry(entryBuilder.startBooleanToggle(Text.translatableWithFallback("settings.option.rule34","mode 34"), DrawlerClient.mode34)
                .setDefaultValue(true)
                .setTooltip(Text.translatableWithFallback("settings.tooltip.rule34","With this toggle being on, all resources are guaranteed to fit inside one inventory"))
                .setSaveConsumer(newValue -> DrawlerClient.mode34 = newValue)
                .build());

        //Correction
        general.addEntry(entryBuilder.startBooleanToggle(Text.translatableWithFallback("settings.option.correction","Error correction"), DrawlerClient.needtocorrect)
                .setDefaultValue(true)
                .setTooltip(Text.translatableWithFallback("settings.tooltip.correction","After finishing drawing, if this is on it will automatically check for errors and fix them."))
                .setSaveConsumer(newValue -> DrawlerClient.needtocorrect = newValue)
                .build());

        //correction mode
        general.addEntry(entryBuilder.startIntSlider(Text.translatableWithFallback("settings.option.correction_mode","Error correction mode"),0,0,1) // default, lower bound, upper bound
                .setDefaultValue(0)
                .setTooltip(Text.translatableWithFallback("settings.tooltip.correction_mode","Decides how error correction will go"))
                .setSaveConsumer(newValue -> DrawlerClient.correction_mode = newValue)
                .build());


        builder.setFallbackCategory(general);
        return builder.build();
    }
}

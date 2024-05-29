package org.pythonchik.drawler.client;


import me.shedaniel.cloth.clothconfig.shadowed.org.yaml.snakeyaml.DumperOptions;
import me.shedaniel.cloth.clothconfig.shadowed.org.yaml.snakeyaml.Yaml;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder;
import me.shedaniel.clothconfig2.impl.builders.EnumSelectorBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.pythonchik.drawler.Drawler;
import org.pythonchik.drawler.DrawlerConfig;

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
        data.put("drawing_mode",DrawlerClient.drawing_mode);


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

        phrase_mode();

        Yaml yaml = new Yaml();
        try (InputStream inputStream = new FileInputStream(settings_file)) {
            Map<String, Object> data = yaml.load(inputStream);

            DrawlerClient.scale = ((Number) data.getOrDefault("scale", 1)).floatValue();
            DrawlerClient.delay = (Integer) data.getOrDefault("delay", 200);
            DrawlerClient.oneback = (Integer) data.getOrDefault("oneback", 3);
            DrawlerClient.mode34 = (Boolean) data.getOrDefault("mode34", true);
            DrawlerClient.needtocorrect = (Boolean) data.getOrDefault("needtocorrect", true);
            DrawlerClient.correction_mode = (Integer) data.getOrDefault("correction_mode",0);
            DrawlerClient.drawing_mode = (Integer) data.getOrDefault("drawing_mode",0);
            //add line here to load value

        } catch (Exception ignored) {}
    }


    public static void phrase_mode() {
        final String[] modes = new String[]{Text.translatable("settings.drawing_mode.1").getString(),Text.translatable("settings.drawing_mode.2").getString()};

        if (DrawlerClient.drawing_string.equals(modes[0])) {
            DrawlerClient.drawing_mode = 0;
        } else if (DrawlerClient.drawing_string.equals(modes[1])) {
            DrawlerClient.drawing_mode = 1;
        }
        if (DrawlerClient.correction_string.equals(modes[0])) {
            DrawlerClient.correction_mode = 0;
        } else if (DrawlerClient.correction_string.equals(modes[1])) {
            DrawlerClient.correction_mode = 1;
        }
    }

    static Screen create(Screen parent) {

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setDefaultBackgroundTexture(new Identifier("minecraft:textures/block/honeycomb_block.png"))
                .setTitle(Text.translatableWithFallback("settings.title.main","settings"));

        builder.setSavingRunnable(() -> {
            saveSettings();
            phrase_mode();
            Drawler.LOGGER.info("I'm sorry for this, but you'll have to wait more, I don't have a good puzzle yet");
        });


        ConfigCategory general = builder.getOrCreateCategory(Text.translatableWithFallback("settings.title.general","general"));
        ConfigCategory drawing = builder.getOrCreateCategory(Text.translatableWithFallback("settings.title.drawing","drawing"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        //rendering
        general.addEntry(entryBuilder.startBooleanToggle(Text.translatableWithFallback("settings.option.rendering","check your localization file"), DrawlerClient.needtorender)
                .setDefaultValue(true)
                .setTooltip(Text.translatableWithFallback("settings.tooltip.rendering","check your localization file"))
                .setSaveConsumer(newValue -> DrawlerClient.needtorender = newValue)
                .build());

        //SCALE
        general.addEntry(entryBuilder.startFloatField(Text.translatableWithFallback("settings.option.scale","check your localization file"), DrawlerClient.scale)
                .setDefaultValue(1)
                .setTooltip(Text.translatableWithFallback("settings.tooltip.scale","check your localization file"))
                .setSaveConsumer(newValue -> DrawlerClient.scale = newValue)
                .build());

        //DElAY
        drawing.addEntry(entryBuilder.startIntField(Text.translatableWithFallback("settings.option.delay","check your localization file"), DrawlerClient.delay)
                .setDefaultValue(200)
                .setTooltip(Text.translatableWithFallback("settings.tooltip.delay","check your localization file"))
                .setSaveConsumer(newValue -> DrawlerClient.delay = newValue)
                .build());

        //BACK
        drawing.addEntry(entryBuilder.startIntField(Text.translatableWithFallback("settings.option.oneback","check your localization file"), DrawlerClient.oneback)
                .setDefaultValue(3)
                .setTooltip(Text.translatableWithFallback("settings.tooltip.oneback","check your localization file"))
                .setSaveConsumer(newValue -> DrawlerClient.oneback = newValue)
                .build());

        //rule 34
        general.addEntry(entryBuilder.startBooleanToggle(Text.translatableWithFallback("settings.option.rule34","check your localization file"), DrawlerClient.mode34)
                .setDefaultValue(true)
                .setTooltip(Text.translatableWithFallback("settings.tooltip.rule34","check your localization file"))
                .setSaveConsumer(newValue -> DrawlerClient.mode34 = newValue)
                .build());

        //Correction
        general.addEntry(entryBuilder.startBooleanToggle(Text.translatableWithFallback("settings.option.correction","check your localization file"), DrawlerClient.needtocorrect)
                .setDefaultValue(true)
                .setTooltip(Text.translatableWithFallback("settings.tooltip.correction","check your localization file"))
                .setSaveConsumer(newValue -> DrawlerClient.needtocorrect = newValue)
                .build());

        String[] modes = new String[]{Text.translatable("settings.drawing_mode.1").getString(),Text.translatable("settings.drawing_mode.2").getString()}; //change this to number of modes

        drawing.addEntry(entryBuilder.startStringDropdownMenu(Text.translatableWithFallback("settings.option.drawing_mode","check your localization file"),DrawlerClient.drawing_string)
                .setDefaultValue(modes[0])
                .setSelections(Arrays.stream(modes).toList())
                .setTooltip(Text.translatableWithFallback("settings.tooltip.drawing_mode","check your localization file"))
                .setSaveConsumer(newValue -> DrawlerClient.drawing_string = newValue)
                .build());

        drawing.addEntry(entryBuilder.startStringDropdownMenu(Text.translatableWithFallback("settings.option.correction_mode","check your localization file"),DrawlerClient.correction_string)
                .setDefaultValue(modes[0])
                .setSelections(Arrays.stream(modes).toList())
                .setTooltip(Text.translatableWithFallback("settings.tooltip.correction_mode","check your localization file"))
                .setSaveConsumer(newValue -> DrawlerClient.correction_string = newValue)
                .build());

        builder.setFallbackCategory(general);
        return builder.build();
    }
}

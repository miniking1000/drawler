package org.pythonchik.drawler.client;


import me.shedaniel.cloth.clothconfig.shadowed.org.yaml.snakeyaml.DumperOptions;
import me.shedaniel.cloth.clothconfig.shadowed.org.yaml.snakeyaml.Yaml;
import me.shedaniel.clothconfig2.api.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.pythonchik.drawler.Drawler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.*;


public class DrawlerSettings {

    private static final File settings_file = new File("config/saved.yml");

    public static void saveSettings() {
        Map<String,Object> data = new HashMap<>();
        data.put("scale",DrawlerClient.scale);
        data.put("delay",DrawlerClient.delay);
        data.put("mode34",DrawlerClient.mode34);
        data.put("needtocorrect",DrawlerClient.needtocorrect);
        data.put("correction_mode",DrawlerClient.correction_mode);
        data.put("drawing_mode",DrawlerClient.drawing_mode);
        data.put("idDev",DrawlerClient.isDev);
        data.put("needtohighlight",DrawlerClient.needtohighlight);
        data.put("highlightcolor",DrawlerClient.highlightColor);


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
            DrawlerClient.mode34 = (Boolean) data.getOrDefault("mode34", true);
            DrawlerClient.needtocorrect = (Boolean) data.getOrDefault("needtocorrect", true);
            DrawlerClient.correction_mode = (Integer) data.getOrDefault("correction_mode",0);
            DrawlerClient.drawing_mode = (Integer) data.getOrDefault("drawing_mode",0);
            DrawlerClient.isDev = (Boolean) data.getOrDefault("idDev",false);
            DrawlerClient.needtohighlight = (Boolean) data.getOrDefault("needtohighlight",true);
            DrawlerClient.highlightColor = (Integer) data.getOrDefault("highlightcolor",0x80FF0000);
            //add line here to load value

        } catch (Exception ignored) {}
    }


    public static void phrase_mode() {
        final String[] modes = new String[]{Text.translatable("settings.drawing_mode.1").getString(),Text.translatable("settings.drawing_mode.2").getString(),Text.translatable("settings.drawing_mode.3").getString(),Text.translatable("settings.drawing_mode.4").getString()};

        if (DrawlerClient.drawing_string.equals(modes[0])) {
            DrawlerClient.drawing_mode = 0;
        } else if (DrawlerClient.drawing_string.equals(modes[1])) {
            DrawlerClient.drawing_mode = 1;
        } else if (DrawlerClient.drawing_string.equals(modes[2])) {
            DrawlerClient.drawing_mode = 2;
        } else if (DrawlerClient.drawing_string.equals(modes[3])) {
            DrawlerClient.drawing_mode = 3;
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
                .setDefaultBackgroundTexture(Identifier.ofVanilla("textures/block/honeycomb_block.png"))
                .setTitle(Text.translatableWithFallback("settings.title.main","settings"));

        builder.setSavingRunnable(() -> {
            saveSettings();
            phrase_mode();
            Drawler.LOGGER.info("So for this is kind of easy, just know, that its a secure link with a few missing characters(`?`): 8*20*?*16*19://25*15*21*20*21.2*5/14*4!*4*23*1!*h*c*26!*24!*g*?");
        });


        ConfigCategory general = builder.getOrCreateCategory(Text.translatableWithFallback("settings.title.general","general"));
        ConfigCategory drawing = builder.getOrCreateCategory(Text.translatableWithFallback("settings.title.drawing","drawing"));


        ConfigEntryBuilder entryBuilder = builder.entryBuilder();


        //highlighting toggle
        general.addEntry(entryBuilder.startBooleanToggle(Text.translatableWithFallback("settings.option.highlighting","check your localization file"), DrawlerClient.needtohighlight)
                .setDefaultValue(true)
                .setTooltip(Text.translatableWithFallback("settings.tooltip.highlighting","check your localization file"))
                .setSaveConsumer(newValue -> DrawlerClient.needtohighlight = newValue)
                .build());

        //highlighting color
        general.addEntry(entryBuilder.startAlphaColorField(Text.translatableWithFallback("settings.option.highlighting_color","check your localization file"), DrawlerClient.highlightColor)
                .setDefaultValue(0x80FF0000)
                .setTooltip(Text.translatableWithFallback("settings.tooltip.highlighting_color","check your localization file"))
                .setSaveConsumer(newValue -> DrawlerClient.highlightColor = newValue)
                .build());


        //rendering
        general.addEntry(entryBuilder.startBooleanToggle(Text.translatableWithFallback("settings.option.rendering","check your localization file"), DrawlerClient.needtorender)
                .setDefaultValue(false)
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


        //save button
        general.addEntry(entryBuilder.startBooleanToggle(Text.translatableWithFallback("settings.option.save_button", "check your localization file"), DrawlerClient.needtosave)
                .setDefaultValue(false)
                .setTooltip(Text.translatableWithFallback("settings.tooltip.save_button", "check your localization file"))
                .setSaveConsumer(newValue -> DrawlerClient.needtosave = newValue)
                .build());

        //save name
        general.addEntry(entryBuilder.startStrField(Text.translatableWithFallback("settings.option.save_name", "check your localization file"), DrawlerClient.saveingname)
                .setDefaultValue("drawler")
                .setTooltip(Text.translatableWithFallback("settings.tooltip.save_name", "check your localization file"))
                .setSaveConsumer(newValue -> {
                    if (newValue.length() >= 3 && newValue.length() <= 16){
                        DrawlerClient.saveingname = newValue;
                    } else {
                        DrawlerClient.send_translatable("drawing.saving.invalid_name", newValue);
                    }
                })
                .build());

        String[] modes = new String[]{Text.translatable("settings.drawing_mode.1").getString(),Text.translatable("settings.drawing_mode.2").getString(),Text.translatable("settings.drawing_mode.3").getString(),Text.translatable("settings.drawing_mode.4").getString()}; //change this to number of modes
        String[] Cmodes = new String[]{Text.translatable("settings.drawing_mode.1").getString(),Text.translatable("settings.drawing_mode.2").getString()}; //change this to number of modes

        drawing.addEntry(entryBuilder.startStringDropdownMenu(Text.translatableWithFallback("settings.option.drawing_mode","check your localization file"),modes[DrawlerClient.drawing_mode])
                .setDefaultValue(modes[0])
                .setSelections(Arrays.stream(modes).toList())
                .setTooltip(Text.translatableWithFallback("settings.tooltip.drawing_mode","check your localization file"))
                .setSaveConsumer(newValue -> DrawlerClient.drawing_string = newValue)
                .build());

        drawing.addEntry(entryBuilder.startStringDropdownMenu(Text.translatableWithFallback("settings.option.correction_mode","check your localization file"),Cmodes[DrawlerClient.correction_mode])
                .setDefaultValue(Cmodes[0])
                .setSelections(Arrays.stream(Cmodes).toList())
                .setTooltip(Text.translatableWithFallback("settings.tooltip.correction_mode","check your localization file"))
                .setSaveConsumer(newValue -> DrawlerClient.correction_string = newValue)
                .build());

        if (DrawlerClient.isDev) {
            ConfigCategory deeeeev = builder.getOrCreateCategory(Text.translatableWithFallback("settings.title.deeeeev", "dev-cat"));

            //render distance
            deeeeev.addEntry(entryBuilder.startDoubleField(Text.translatableWithFallback("settings.option.depth", "check your localization file"), DrawlerClient.depth)
                    .setDefaultValue(0.64)
                    .setTooltip(Text.translatableWithFallback("settings.tooltip.depth", "check your localization file"))
                    .setSaveConsumer(newValue -> DrawlerClient.depth = newValue)
                    .build());

            //render height
            deeeeev.addEntry(entryBuilder.startDoubleField(Text.translatableWithFallback("settings.option.height", "check your localization file"), DrawlerClient.height)
                    .setDefaultValue(1.122)
                    .setTooltip(Text.translatableWithFallback("settings.tooltip.height", "check your localization file"))
                    .setSaveConsumer(newValue -> DrawlerClient.height = newValue)
                    .build());

            //render side offset
            deeeeev.addEntry(entryBuilder.startDoubleField(Text.translatableWithFallback("settings.option.sideoff", "check your localization file"), DrawlerClient.sideoff)
                    .setDefaultValue(0.5)
                    .setTooltip(Text.translatableWithFallback("settings.tooltip.sideoff", "check your localization file"))
                    .setSaveConsumer(newValue -> DrawlerClient.sideoff = newValue)
                    .build());

            //after button
            deeeeev.addEntry(entryBuilder.startBooleanToggle(Text.translatableWithFallback("settings.option.after", "check your localization file"), DrawlerClient.after)
                    .setDefaultValue(false)
                    .setTooltip(Text.translatableWithFallback("settings.tooltip.after", "check your localization file"))
                    .setSaveConsumer(newValue -> DrawlerClient.after = newValue)
                    .build());

            //debug button
            deeeeev.addEntry(entryBuilder.startBooleanToggle(Text.translatableWithFallback("settings.option.debug", "check your localization file"), DrawlerClient.isDebug)
                    .setDefaultValue(false)
                    .setTooltip(Text.translatableWithFallback("settings.tooltip.debug", "check your localization file"))
                    .setSaveConsumer(newValue -> DrawlerClient.isDebug = newValue)
                    .build());

        }

        builder.setFallbackCategory(general);
        return builder.build();
    }
}

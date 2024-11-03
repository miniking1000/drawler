package org.pythonchik.drawler.sound;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.pythonchik.drawler.Drawler;

public class CustomSounds {
    private CustomSounds() {}

    public static final SoundEvent DEFAULT_DONE = registerSound("default.done");
    public static final SoundEvent DEFAULT_ERROR = registerSound("default.error");
    public static final SoundEvent DISCORD_DONE = registerSound("discord.done");
    public static final SoundEvent DISCORD_ERROR = registerSound("discord.error");
    public static final SoundEvent LOUD_DONE = registerSound("loud.done");
    public static final SoundEvent LOUD_ERROR = registerSound("loud.error");


    private static SoundEvent registerSound(String id) {
        Identifier identifier = new Identifier(Drawler.MOD_ID, id);
        return Registry.register(Registries.SOUND_EVENT, identifier, SoundEvent.of(identifier));
    }


    public static void initialize() {
        Registry.register(Registries.SOUND_EVENT, new Identifier(Drawler.MOD_ID, "default.done"),
                SoundEvent.of(new Identifier(Drawler.MOD_ID, "default.done")));
        Registry.register(Registries.SOUND_EVENT, new Identifier(Drawler.MOD_ID, "default.error"),
                SoundEvent.of(new Identifier(Drawler.MOD_ID, "default.error")));
        Registry.register(Registries.SOUND_EVENT, new Identifier(Drawler.MOD_ID, "discord.done"),
                SoundEvent.of(new Identifier(Drawler.MOD_ID, "discord.done")));
        Registry.register(Registries.SOUND_EVENT, new Identifier(Drawler.MOD_ID, "discord.error"),
                SoundEvent.of(new Identifier(Drawler.MOD_ID, "discord.error")));
        Registry.register(Registries.SOUND_EVENT, new Identifier(Drawler.MOD_ID, "loud.done"),
                SoundEvent.of(new Identifier(Drawler.MOD_ID, "loud.done")));
        Registry.register(Registries.SOUND_EVENT, new Identifier(Drawler.MOD_ID, "loud.error"),
                SoundEvent.of(new Identifier(Drawler.MOD_ID, "loud.error")));
    }

    public static SoundEvent get_by_name(String name) {
        return switch (name.toUpperCase()) {
            case "DEFAULT_DONE" -> DEFAULT_DONE;
            case "DEFAULT_ERROR" -> DEFAULT_ERROR;
            case "DISCORD_DONE" -> DISCORD_DONE;
            case "DISCORD_ERROR" -> DISCORD_ERROR;
            case "LOUD_DONE" -> LOUD_DONE;
            case "LOUD_ERROR" -> LOUD_ERROR;
            default -> null;
        };

    }
}

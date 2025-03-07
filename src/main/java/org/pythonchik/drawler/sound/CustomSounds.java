package org.pythonchik.drawler.sound;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.pythonchik.drawler.Drawler;

public class CustomSounds {
    private CustomSounds() {}

    public static SoundEvent DEFAULT_DONE;
    public static SoundEvent DEFAULT_ERROR;
    public static SoundEvent DISCORD_DONE;
    public static SoundEvent DISCORD_ERROR;
    public static SoundEvent LOUD_DONE;
    public static SoundEvent LOUD_ERROR;


    private static SoundEvent registerSound(String id) {
        Identifier identifier = Identifier.of(Drawler.MOD_ID, id);
        return Registry.register(Registries.SOUND_EVENT, identifier, SoundEvent.of(identifier));
    }

    public static void initialize() {
        DEFAULT_DONE = registerSound("default.done");
        DEFAULT_ERROR = registerSound("default.error");
        DISCORD_DONE = registerSound("discord.done");
        DISCORD_ERROR = registerSound("discord.error");
        LOUD_DONE = registerSound("loud.done");
        LOUD_ERROR = registerSound("loud.error");

        //Registry.register(Registries.SOUND_EVENT, Identifier.of(Drawler.MOD_ID, "default.done"), SoundEvent.of(Identifier.of(Drawler.MOD_ID, "default.done")));
        //Registry.register(Registries.SOUND_EVENT, Identifier.of(Drawler.MOD_ID, "default.error"), SoundEvent.of(Identifier.of(Drawler.MOD_ID, "default.error")));
        //Registry.register(Registries.SOUND_EVENT, Identifier.of(Drawler.MOD_ID, "discord.done"), SoundEvent.of(Identifier.of(Drawler.MOD_ID, "discord.done")));
        //Registry.register(Registries.SOUND_EVENT, Identifier.of(Drawler.MOD_ID, "discord.error"), SoundEvent.of(Identifier.of(Drawler.MOD_ID, "discord.error")));
        //Registry.register(Registries.SOUND_EVENT, Identifier.of(Drawler.MOD_ID, "loud.done"), SoundEvent.of(Identifier.of(Drawler.MOD_ID, "loud.done")));
        //Registry.register(Registries.SOUND_EVENT, Identifier.of(Drawler.MOD_ID, "loud.error"), SoundEvent.of(Identifier.of(Drawler.MOD_ID, "loud.error")));
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

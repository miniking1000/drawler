package org.pythonchik.drawler.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
    @Accessor("backgroundWidth")
    int getBackgroundWidth();

    @Accessor("backgroundHeight")
    int getBackgroundHeight();
}
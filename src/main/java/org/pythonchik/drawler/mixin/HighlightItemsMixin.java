package org.pythonchik.drawler.mixin;


import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.pythonchik.drawler.client.DrawlerClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GenericContainerScreen.class)
public class HighlightItemsMixin {

    @Inject(method = "drawBackground", at = @At("TAIL"))
    private void drawBackground(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo info) {
        GenericContainerScreen screen = (GenericContainerScreen) (Object) this;
        int guiLeft = (screen.width - ((HandledScreenAccessor) screen).getBackgroundWidth()) / 2;
        int guiTop = (screen.height - ((HandledScreenAccessor) screen).getBackgroundHeight()) / 2;

        for (Slot slot : screen.getScreenHandler().slots) {
            ItemStack stack = slot.getStack();
            if (DrawlerClient.getItemsList() != null && DrawlerClient.getItemsList().contains(stack.getItem()) && DrawlerClient.isHightlighting()) {
                int x = guiLeft + slot.x;
                int y = guiTop + slot.y;
                int color = DrawlerClient.getHighlightColor();
                context.fillGradient(x, y, x + 16, y + 16, color, color);
            }
        }
    }
}
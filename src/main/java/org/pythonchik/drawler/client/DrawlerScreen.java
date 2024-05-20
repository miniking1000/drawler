package org.pythonchik.drawler.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.text.DecimalFormat;

public class DrawlerScreen extends Screen {
    private ButtonWidget mode34;
    private ButtonWidget needtorender;
    private ButtonWidget invert;
    private ButtonWidget needtocorrect;
    private ButtonWidget scaleP;
    private ButtonWidget scaleM;
    private ButtonWidget delayP1;
    private ButtonWidget delayN1;
    private ButtonWidget delayP5;
    private ButtonWidget delayN5;
    private ButtonWidget delayP50;
    private ButtonWidget delayN50;
    private ButtonWidget backP;
    private ButtonWidget backN;

    protected DrawlerScreen() {
        super(Text.literal("Drawler Config Menu"));
    }

    @Override
    protected void init() {
        mode34 = ButtonWidget.builder(Text.literal("mode 34"), button -> {
                    DrawlerClient.mode34 = !DrawlerClient.mode34;
                    mode34.setTooltip(Tooltip.of(Text.literal(String.format("При включении данного режима, гарантируется, что все ресурсы поместятся в один инвентарь.\nТекущее значение - %s", DrawlerClient.mode34))));
                })
                .dimensions(width / 2 - 205, 20, 200, 20)
                .tooltip(Tooltip.of(Text.literal(String.format("При включении данного режима, гарантируется, что все ресурсы поместятся в один инвентарь.\nТекущее значение - %s", DrawlerClient.mode34))))
                .build();

        needtorender = ButtonWidget.builder(Text.literal("Render"), button -> {
                    DrawlerClient.needtorender = !DrawlerClient.needtorender;
                    needtorender.setTooltip(Tooltip.of(Text.literal(String.format("При включенном, во время рисования в верхнем левом углу, будет уменьшенная версия изображения.\nТекущее значение - %s",DrawlerClient.needtorender))));
                })
                .dimensions(width / 2 + 5, 20, 200, 20)
                .tooltip(Tooltip.of(Text.literal(String.format("При включении, во время рисования в верхнем левом углу, будет уменьшенная версия изображения.\nТекущее значение - %s",DrawlerClient.needtorender))))
                .build();

        needtocorrect = ButtonWidget.builder(Text.literal("Correction"), button -> {
                    DrawlerClient.needtocorrect = !DrawlerClient.needtocorrect;
                    needtocorrect.setTooltip(Tooltip.of(Text.literal(String.format("При включенном, после рисования будет проведено исправление ошибок если такие есть.\nТекущее значение - %s",DrawlerClient.needtocorrect))));
                })
                .dimensions(width / 2 - 205, 120, 200, 20) //TODO place button
                .tooltip(Tooltip.of(Text.literal(String.format("При включенном, после рисования будет проведено исправление ошибок если такие есть.\nТекущее значение - %s",DrawlerClient.needtocorrect))))
                .build();

        invert = ButtonWidget.builder(Text.literal("invert"), button -> {
                    DrawlerClient.invert = !DrawlerClient.invert;
                    invert.setTooltip(Tooltip.of(Text.literal(String.format("При включении, рисование будет происходить в обратном направлении, нужно для исправления ошибок, если такие есть.\nТекущее значение - %s",DrawlerClient.invert))));
                })
                .dimensions(width / 2 - 205, 70, 200, 20)
                .tooltip(Tooltip.of(Text.literal(String.format("При включении, рисование будет происходить в обратном направлении, нужно для исправления ошибок, если такие есть.\nТекущее значение - %s",DrawlerClient.invert))))
                .build();


        scaleP = ButtonWidget.builder(Text.literal("+"), button -> {
                    DrawlerClient.scale += 0.1f;
                })
                .dimensions(width / 2 + 35, 70, 20, 20)
                .tooltip(Tooltip.of(Text.literal("Увеличивает размер изображения в верхнем левом углу(при его наличии)")))
                .build();

        scaleM = ButtonWidget.builder(Text.literal("-"), button -> {
                    DrawlerClient.scale -= 0.1f;
                })
                .dimensions(width / 2 + 5, 70, 20, 20)
                .tooltip(Tooltip.of(Text.literal("Уменьшает размер изображения в верхнем левом углу(при его наличии)")))
                .build();

        backP = ButtonWidget.builder(Text.literal("+"), button -> {
                    DrawlerClient.oneback += 1;
                })
                .dimensions(width / 2 + 185, 70, 20, 20)
                .tooltip(Tooltip.of(Text.literal("Увеличивает количество пикселей, при шаге назад")))
                .build();

        backN = ButtonWidget.builder(Text.literal("-"), button -> {
                    DrawlerClient.oneback -= 1;
                })
                .dimensions(width / 2 + 155, 70, 20, 20)
                .tooltip(Tooltip.of(Text.literal("Уменьшает количество пикселей, при шаге назад")))
                .build();



        delayN50 = ButtonWidget.builder(Text.literal("---"), button -> {
                    DrawlerClient.delay -= 50;
                })
                .dimensions(width / 2 + 5, 120, 25, 20)
                .tooltip(Tooltip.of(Text.literal("Уменьшает задержку между действиями во время рисования на 50мс")))
                .build();

        delayN5 = ButtonWidget.builder(Text.literal("--"), button -> {
                    DrawlerClient.delay -= 5;
                })
                .dimensions(width / 2 + 40, 120, 25, 20)
                .tooltip(Tooltip.of(Text.literal("Уменьшает задержку между действиями во время рисования на 5мс")))
                .build();

        delayN1 = ButtonWidget.builder(Text.literal("-"), button -> {
                    DrawlerClient.delay -= 1;
                })
                .dimensions(width / 2 + 75, 120, 20, 20)
                .tooltip(Tooltip.of(Text.literal("Уменьшает задержку между действиями во время рисования на 1мс")))
                .build();

        delayP1 = ButtonWidget.builder(Text.literal("+"), button -> {
                    DrawlerClient.delay += 1;
                })
                .dimensions(width / 2 + 110, 120, 20, 20)
                .tooltip(Tooltip.of(Text.literal("Увеличивает задержку между действиями во время рисования на 1мс")))
                .build();

        delayP5 = ButtonWidget.builder(Text.literal("++"), button -> {
                    DrawlerClient.delay += 5;
                })
                .dimensions(width / 2 + 140, 120, 25, 20)
                .tooltip(Tooltip.of(Text.literal("Увеличивает задержку между действиями во время рисования на 5мс")))
                .build();

        delayP50 = ButtonWidget.builder(Text.literal("+++"), button -> {
                    DrawlerClient.delay += 50;
                })
                .dimensions(width / 2 + 175, 120, 25, 20)
                .tooltip(Tooltip.of(Text.literal("Увеличивает задержку между действиями во время рисования на 50мс")))
                .build();




        addDrawableChild(mode34);
        addDrawableChild(needtorender);
        addDrawableChild(invert);
        addDrawableChild(needtocorrect);
        addDrawableChild(scaleP);
        addDrawableChild(scaleM);
        addDrawableChild(backP);
        addDrawableChild(backN);
        addDrawableChild(delayP1);
        addDrawableChild(delayN1);
        addDrawableChild(delayP5);
        addDrawableChild(delayN5);
        addDrawableChild(delayP50);
        addDrawableChild(delayN50);

    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(textRenderer, Text.literal(String.format("размер - %s", new DecimalFormat(".#").format(DrawlerClient.scale))), width / 2+30, 60, 0xffffff);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(String.format("шаг назад на %s",DrawlerClient.oneback)), width / 2+180, 60, 0xffffff);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(String.format("задержка - %s",DrawlerClient.delay)), width / 2+105, 110, 0xffffff);

    }
}

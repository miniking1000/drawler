package org.pythonchik.drawler.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.pythonchik.drawler.Drawler;
import org.pythonchik.drawler.DrawlerConfig;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class DrawlerClient implements ClientModInitializer {
    static String url = "";
    static Random random = new Random();
    static boolean worldrender = false;
    static Boolean isthere = false;
    static float scale = 1;
    static BufferedImage todrawimg;
    static boolean isdrawin = false;
    static double depth = 0.64;
    static double height = 1.122;
    static double sideoff = 0.5;
    static boolean after = false;
    static HashMap<ArrayList<Integer>, ArrayList<Float>> current;
    //static int curx = 0;
    //static int curz = 0;
    static int mapid = -1;
    static boolean needtorender = false;
    static int correction_mode = 0;
    //0 = default, from left to right, up to down...
    //1 = random, just random.
    static int drawing_mode = 0;
    //0 = default, from left to right, up to down...
    //1 = random, just random.
    static String drawing_string = "Осталось с прошлого сохранения";
    static String correction_string = "Осталось с прошлого сохранения";
    static ArrayList<ArrayList<Integer>> pixeldata = new ArrayList<>();
    static boolean mode34 = true;
    static boolean needtocorrect = true;
    static boolean iscorrectin = false;
    static ArrayList<ArrayList<Integer>> tocorrect = new ArrayList<>();
    static int oneback = 3;
    static int delay = 250;
    static int curIND = 0;
    static ScheduledFuture backup = null;
    static HashMap<Item,Integer> ItemMap;
    private static KeyBinding openMenuKeyBinding;
    private static KeyBinding pauseKeyBinding;
    private static KeyBinding gobackKeyBinding;
    private static KeyBinding renderKeyBinding;
    private static final Identifier MAP_CHKRBRD =
            Identifier.of("minecraft:textures/map/map_background.png");
    @Override
    public void onInitializeClient() {

        openMenuKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.drawler.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.drawler.modsettings"
        ));
        pauseKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.drawler.pause",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_KP_1,
                "category.drawler.modsettings"
        ));
        gobackKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.drawler.goback",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_KP_3,
                "category.drawler.modsettings"
        ));
        renderKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.drawler.render",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_KP_2,
                "category.drawler.modsettings"
        ));



        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("reset_drawing")
                    .executes(context -> {
                        url = "";
                        curIND = 0;
                        worldrender = false;
                        mapid = -1;
                        isdrawin = false;
                        needtocorrect = true;
                        isthere = false;
                        todrawimg = null;
                        current = new HashMap<>();
                        send_message("выполнено!...");
                        return 1;
                    }));
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("check_items")
                            .executes(context -> {
                                if (todrawimg != null) {
                                    send_message("Вам не хватает этих предметов:");
                                    check_item();
                                } else {
                                    send_message("Никакое изображение сейчас не рисуется");
                                }
                                return 1;
                            }));
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("FixYourself!")
                    .executes(context -> {
                        check_errors();
                        return 1;
                    }));
        });


        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("set_drawing")
                    .then(ClientCommandManager.argument("x", IntegerArgumentType.integer(0,128))
                            .then(ClientCommandManager.argument("y", IntegerArgumentType.integer(0,128))
                                .executes(context -> {
                                    send_message("Координаты изменены с&c %d &fна&a %d".formatted(curIND,IntegerArgumentType.getInteger(context,"x")+128*IntegerArgumentType.getInteger(context,"y")));
                                    curIND = IntegerArgumentType.getInteger(context,"y")*128+IntegerArgumentType.getInteger(context,"x");
                                    return 1;
                                }))));
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("set_drawing")
                    .then(ClientCommandManager.argument("point", IntegerArgumentType.integer(0,128*128))
                            .executes(context -> {
                                send_message("Координаты изменены с &c %d &fна&a %d".formatted(curIND,IntegerArgumentType.getInteger(context,"point")));
                                curIND = IntegerArgumentType.getInteger(context,"point");
                                return 1;
                            })));
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("set_camera")
                    .then(ClientCommandManager.argument("point", IntegerArgumentType.integer(0,128*128))
                            .executes(context -> {
                                HashMap<ArrayList<Integer>, ArrayList<Float>> current = new HashMap<>();
                                switch (MinecraftClient.getInstance().player.getHorizontalFacing()) {
                                    case EAST -> current = DrawlerConfig.east;
                                    case WEST -> current = DrawlerConfig.west;
                                    case NORTH -> current = DrawlerConfig.north;
                                    case SOUTH -> current = DrawlerConfig.south;
                                    default -> {
                                        send_message("Вам нужно смотреть прямо на холст во время написания координаты");
                                        return 1;
                                    }
                                }
                                int ind = IntegerArgumentType.getInteger(context,"point");
                                ArrayList<Integer> temp = new ArrayList<>();
                                temp.add(ind-(ind/128)*128);
                                temp.add(ind/128);

                                for (HashMap.Entry<ArrayList<Integer>, ArrayList<Float>> list : current.entrySet()) {
                                    if (list.getKey().equals(temp)) {
                                        ArrayList<Float> degree = list.getValue();
                                        MinecraftClient.getInstance().player.setYaw(degree.get(0));
                                        MinecraftClient.getInstance().player.setPitch(degree.get(1));
                                    }
                                }
                                if (!pixeldata.isEmpty()){
                                    for (ArrayList<Integer> entry : pixeldata){
                                        if (entry.get(0).equals(temp.get(0)) && entry.get(1).equals(temp.get(1))){
                                            Item ToFind = DrawlerConfig.items.get(entry.get(2));
                                            if (entry.get(3).equals(1)) { //nothing
                                                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("§7[§6Drawler§7]§r ").copy().append(Text.translatable(ToFind.getTranslationKey()).append(Text.of(" - предмет нужный для рисования данного пикселя"))));
                                            } else if (entry.get(3).equals(2)) { //feather 1
                                                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("§7[§6Drawler§7]§r ").copy().append(Text.translatable(ToFind.getTranslationKey()).append(Text.of(" + ")).append(Text.translatable(Items.FEATHER.getTranslationKey()).append(Text.of(" - предметы нужны для рисования данного пикселя")))));
                                            } else if (entry.get(3).equals(0)) { //coal 1
                                                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("§7[§6Drawler§7]§r ").copy().append(Text.translatable(ToFind.getTranslationKey()).append(Text.of(" + ")).append(Text.translatable(Items.COAL.getTranslationKey()).append(Text.of(" - предметы нужны для рисования данного пикселя")))));
                                            } else if (entry.get(3).equals(3)) { //coal 2
                                                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("§7[§6Drawler§7]§r ").copy().append(Text.translatable(ToFind.getTranslationKey()).append(Text.of(" + ")).append(Text.translatable(Items.COAL.getTranslationKey()).append(Text.of(" + ")).append(Text.translatable(Items.COAL.getTranslationKey()).append(Text.of(" - предметы нужны для рисования данного пикселя"))))));
                                            }
                                            break;
                                        }
                                    }
                                } else {
                                    send_message("Готово!");
                                }
                                return 1;
                            })));
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("set_camera")
                    .then(ClientCommandManager.argument("x", IntegerArgumentType.integer(0,128))
                            .then(ClientCommandManager.argument("y", IntegerArgumentType.integer(0,128))
                                .executes(context -> {
                                    HashMap<ArrayList<Integer>, ArrayList<Float>> current = new HashMap<>();
                                    switch (MinecraftClient.getInstance().player.getHorizontalFacing()) {
                                        case EAST -> current = DrawlerConfig.east;
                                        case WEST -> current = DrawlerConfig.west;
                                        case NORTH -> current = DrawlerConfig.north;
                                        case SOUTH -> current = DrawlerConfig.south;
                                        default -> {
                                            send_message("Вам нужно смотреть прямо на холст во время написания координаты");
                                            return 1;
                                        }
                                    }

                                    ArrayList<Integer> temp = new ArrayList<>();
                                    temp.add(IntegerArgumentType.getInteger(context,"x"));
                                    temp.add(IntegerArgumentType.getInteger(context,"y"));

                                    for (HashMap.Entry<ArrayList<Integer>, ArrayList<Float>> list : current.entrySet()) {
                                        if (list.getKey().equals(temp)) {
                                            ArrayList<Float> degree = list.getValue();
                                            MinecraftClient.getInstance().player.setYaw(degree.get(0));
                                            MinecraftClient.getInstance().player.setPitch(degree.get(1));
                                        }
                                    }
                                    if (!pixeldata.isEmpty()){
                                        for (ArrayList<Integer> entry : pixeldata){
                                            if (entry.get(0).equals(temp.get(0)) && entry.get(1).equals(temp.get(1))){
                                                Item ToFind = DrawlerConfig.items.get(entry.get(2));
                                                if (entry.get(3).equals(1)) { //nothing
                                                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("§7[§6Drawler§7]§r ").copy().append(Text.translatable(ToFind.getTranslationKey()).append(Text.of(" - предмет нужный для рисования данного пикселя"))));
                                                } else if (entry.get(3).equals(2)) { //feather 1
                                                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("§7[§6Drawler§7]§r ").copy().append(Text.translatable(ToFind.getTranslationKey()).append(Text.of(" + ")).append(Text.translatable(Items.FEATHER.getTranslationKey()).append(Text.of(" - предметы нужны для рисования данного пикселя")))));
                                                } else if (entry.get(3).equals(0)) { //coal 1
                                                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("§7[§6Drawler§7]§r ").copy().append(Text.translatable(ToFind.getTranslationKey()).append(Text.of(" + ")).append(Text.translatable(Items.COAL.getTranslationKey()).append(Text.of(" - предметы нужны для рисования данного пикселя")))));
                                                } else if (entry.get(3).equals(3)) { //coal 2
                                                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("§7[§6Drawler§7]§r ").copy().append(Text.translatable(ToFind.getTranslationKey()).append(Text.of(" + ")).append(Text.translatable(Items.COAL.getTranslationKey()).append(Text.of(" + ")).append(Text.translatable(Items.COAL.getTranslationKey()).append(Text.of(" - предметы нужны для рисования данного пикселя"))))));
                                                }
                                                break;
                                            }
                                        }
                                    } else {
                                        send_message("Готово!");
                                    }
                                    return 1;
                            }))));
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("drawpic")
                    .then(ClientCommandManager.argument("mapID",IntegerArgumentType.integer(0))
                            .then(ClientCommandManager.argument("url", StringArgumentType.greedyString())
                                    .executes(context -> {
                                        send_message("Обработка изображения, пожалуйста подождите...");
                                        mapid = IntegerArgumentType.getInteger(context, "mapID");
                                        url = StringArgumentType.getString(context, "url");
                                        ScheduledExecutorService backup = Executors.newScheduledThreadPool(1);
                                        backup.schedule(() -> {
                                            processImage(url);
                                        }, 0, TimeUnit.MILLISECONDS);
                                        backup.shutdown();
                                        return 1;
                                    }))));
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKeyBinding.wasPressed()) {
                //MinecraftClient.getInstance().setScreen(new DrawlerScreen());
                MinecraftClient.getInstance().setScreen(DrawlerSettings.create(MinecraftClient.getInstance().currentScreen));
            }
            while (pauseKeyBinding.wasPressed()){
                if (todrawimg == null) {
                    send_message("Нельзя снять с паузы, если ничего не рисовать");
                    return;
                }
                isdrawin = !isdrawin;
                if (isdrawin) {
                    send_message("Продолжаем рисовать с точки &a%d".formatted(curIND));
                    gonext();
                } else {
                    send_message("Останавливаемся на точке &a%d".formatted(curIND));
                }

            }
            while (gobackKeyBinding.wasPressed()) {
                int togo = oneback;
                while (curIND - togo < 0) { //если Х меньше чем надо пройти
                    togo = togo - curIND - 1; //то убираем Х и еще одну из-за 0
                    curIND =127*127; //ставим его на максимум прошлого ряда
                }
                curIND -=togo; //если же мы можем вычесть, то просто вычитаем
                send_message(String.format("Теперь текущая точка -> &a%d",curIND));
            }
            while (renderKeyBinding.wasPressed()) {
                worldrender = !worldrender;
                send_message("Готово! :(");
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (needtorender) {
               drawContext.drawTexture(MAP_CHKRBRD, (int) (3+scale), (int) (3+scale),0, 1F, 1F, (int) (92*scale),(int) (92*scale),(int) (92*scale)+1,(int) (92*scale)+1);

                if (!isthere) {
                    try {
                        URL imgurl = new URL(url);
                        URLConnection connection = imgurl.openConnection();
                        try (InputStream stream = connection.getInputStream()) {
                            NativeImage image = NativeImage.read(stream);

                            MinecraftClient.getInstance().getTextureManager().registerTexture(Identifier.of("drawler", "urlimg.png"), new NativeImageBackedTexture(image));
                            isthere = true;
                        }
                    } catch (Exception e) {
                        try {
                            MinecraftClient.getInstance().getTextureManager().registerTexture(Identifier.of("drawler", "urlimg.png"), new NativeImageBackedTexture(NativeImage.read(MinecraftClient.getInstance().getResourceManager().getResource(Identifier.of("drawler", "default.png")).get().getInputStream())));
                        } catch (Exception ignored) {
                        }
                    }
                }
                drawContext.drawTexture(Identifier.of("drawler","urlimg.png"), (int) (3+3*scale), (int) (3+3*scale),0, 1F, 1F, (int) (86*scale),(int) (86*scale),(int) (86*scale)+1,(int) (86*scale)+1);

            }
        });


        WorldRenderEvents.END.register(context -> {
            if (worldrender) {
                Camera camera = context.camera();
                MatrixStack matrixStack = new MatrixStack();
                switch (MinecraftClient.getInstance().player.getHorizontalFacing()) {
                    case EAST -> {
                        Vec3d targetPosition = new Vec3d(MinecraftClient.getInstance().player.getX() + depth, MinecraftClient.getInstance().player.getY() + height, MinecraftClient.getInstance().player.getZ() - sideoff);
                        Vec3d transformedPosition = targetPosition.subtract(camera.getPos());

                        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));

                        matrixStack.translate(transformedPosition.x, transformedPosition.y, transformedPosition.z);

                        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(270));


                    }
                    case WEST -> {
                        Vec3d targetPosition = new Vec3d(MinecraftClient.getInstance().player.getX() - depth, MinecraftClient.getInstance().player.getY() + height, MinecraftClient.getInstance().player.getZ() + sideoff);
                        Vec3d transformedPosition = targetPosition.subtract(camera.getPos());

                        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));

                        matrixStack.translate(transformedPosition.x, transformedPosition.y, transformedPosition.z);

                        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));
                    }
                    case NORTH -> {
                        Vec3d targetPosition = new Vec3d(MinecraftClient.getInstance().player.getX() - sideoff, MinecraftClient.getInstance().player.getY() + height, MinecraftClient.getInstance().player.getZ() - depth);
                        Vec3d transformedPosition = targetPosition.subtract(camera.getPos());

                        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));

                        matrixStack.translate(transformedPosition.x, transformedPosition.y, transformedPosition.z);

                        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(0));

                    }
                    case SOUTH -> {
                        Vec3d targetPosition = new Vec3d(MinecraftClient.getInstance().player.getX() + sideoff, MinecraftClient.getInstance().player.getY() + height, MinecraftClient.getInstance().player.getZ() + depth);
                        Vec3d transformedPosition = targetPosition.subtract(camera.getPos());


                        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));

                        matrixStack.translate(transformedPosition.x, transformedPosition.y, transformedPosition.z);

                        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
                    }
                    default -> {
                        worldrender = false;
                        send_message("you can not.");
                    }
                }

                Matrix4f positionMatrix = matrixStack.peek().getPositionMatrix();
                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

                buffer.vertex(positionMatrix, 0, 1, 0).color(1f, 1f, 1f, 1f).texture(0f, 0f);
                buffer.vertex(positionMatrix, 0, 0, 0).color(1f, 1f, 1f, 1f).texture(0f, 1f);
                buffer.vertex(positionMatrix, 1, 0, 0).color(1f, 1f, 1f, 1f).texture(1f, 1f);
                buffer.vertex(positionMatrix, 1, 1, 0).color(1f, 1f, 1f, 1f).texture(1f, 0f);

                RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
                if (!isthere){
                    try {
                        MinecraftClient.getInstance().getTextureManager().registerTexture(Identifier.of("drawler", "urlimg.png"), new NativeImageBackedTexture(NativeImage.read(MinecraftClient.getInstance().getResourceManager().getResource(Identifier.of("drawler", "default.png")).get().getInputStream())));
                        isthere = true;
                    } catch (Exception ignored) {
                    }
                }
                RenderSystem.setShaderTexture(0, Identifier.of("drawler", "urlimg.png"));
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                if (after) {
                    RenderSystem.disableCull();
                    RenderSystem.depthFunc(GL11.GL_ALPHA);
                }
                BufferRenderer.drawWithGlobalProgram(buffer.end());

                RenderSystem.depthFunc(GL11.GL_ALPHA);
                RenderSystem.enableCull();

            }

        }); //end of world render
    } //end of client init



    public static void check_errors(){
        send_message("Начинаем проверку на ошибки");
        tocorrect = new ArrayList<>();
        MapState mapState = MinecraftClient.getInstance().world.getMapState(new MapIdComponent(mapid));

        if (mapState != null) {
            for (int y = 0; y < 128; y++) {
                for (int x = 0; x < 128; x++) {
                    if (!((MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x]) / 4)).id == DrawlerConfig.getColorID(new Color(todrawimg.getRGB(x, y)))) &&
                            ((Byte.toUnsignedInt(mapState.colors[y * 128 + x]) - MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x])) / 4).id * 4) == DrawlerConfig.getColorVariant(new Color(todrawimg.getRGB(x, y)))))) {
                        tocorrect.add(new ArrayList<>(List.of(x, y)));
                        iscorrectin = true;
                        isdrawin = true;
                    }
                }
            }
            if (tocorrect.size() == 0){
                send_message("Ошибок не выявлено! картина готова!");
                //TODO save pick if you said so in config
                //TODO continued drawing in queue
            } else {
                send_message("Проверка ошибок завершена, выявлено %d ошибок. Переходим к исправлению".formatted(tocorrect.size()));
                gonext();
            }
        } else {
            send_message("С картой какая-то ошибка.");
        }
    }
    public static void processImage(String url) {
        try {
            BufferedImage originalImage = ImageIO.read(new URL(url));
            BufferedImage resizedImage = resizeImage(originalImage, 128, 128); //TODO this line will break 2x1 picks, rework that

            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            Drawler.LOGGER.info(player.getHorizontalFacing().toString());
            switch (player.getHorizontalFacing()) {
                case EAST -> current = DrawlerConfig.east;
                case WEST -> current = DrawlerConfig.west;
                case NORTH -> current = DrawlerConfig.north;
                case SOUTH -> current = DrawlerConfig.south;
                default -> {
                    send_message("Вам нужно смотреть прямо на холст во время написания координаты");
                    return;
                }
            }
            HashMap<Color,Integer> ColorMap;
            ArrayList<Color> colors = new ArrayList<>();
            for (Color color : DrawlerConfig.colors){
                colors.add(new Color(color.getRGB())); //creating local list of colors
            }


            do {
                ColorMap = new HashMap<>();
                ItemMap = new HashMap<>();
                for (int y = 0; y < resizedImage.getHeight(); y++) {
                    for (int x = 0; x < resizedImage.getWidth(); x++) {
                        Color pixelColor = new Color(resizedImage.getRGB(x, y));
                        Color minecraftColor = findClosestMinecraftColor(pixelColor,colors);
                        resizedImage.setRGB(x,y,minecraftColor.getRGB());
                        int Cid = DrawlerConfig.getColorID(minecraftColor);
                        ItemMap.put(DrawlerConfig.items.get(Cid), ItemMap.getOrDefault(DrawlerConfig.items.get(Cid),0)+1);
                        ColorMap.put(minecraftColor, ColorMap.getOrDefault(minecraftColor,0)+1);
                    }
                }
                if (mode34 && ItemMap.size() > 34) {

                    //Drawler.LOGGER.info("inside if we got: " + ItemMap + " - itemmap | colormap - " + ColorMap + " | colors - " + colors);
                    int lowest = 16384+1;
                    Color key = null;
                    for (Map.Entry<Color,Integer> entry: ColorMap.entrySet()){
                        if (lowest < entry.getValue()) continue;
                        lowest = entry.getValue();
                        key = entry.getKey();
                    }
                    colors.remove(key);
                    //Drawler.LOGGER.info("removed - " + key + " " + colors + " - colors");
                }
            } while (mode34 && ItemMap.size() > 34);

            ItemMap.put(Items.COAL,1);
            ItemMap.put(Items.FEATHER,1);

            pixeldata = new ArrayList<>();
            if (drawing_mode == 0) {
                ArrayList<Integer> temp = new ArrayList<>();
                for (int y = 0; y < 128; y++) {
                    for (int x = 0; x < 128; x++) {
                        temp.add(x);
                        temp.add(y);
                        temp.add(DrawlerConfig.getColorID(new Color(resizedImage.getRGB(x,y))));
                        temp.add(DrawlerConfig.getColorVariant(new Color(resizedImage.getRGB(x,y))));
                        pixeldata.add(temp);
                        temp = new ArrayList<>();
                        //x cords y cords Cid, Cvr, isChecked, border?
                    }
                }
            } else if (drawing_mode == 1) {
                ArrayList<ArrayList<Integer>> temp = new ArrayList<>();
                ArrayList<Integer> temp2 = new ArrayList<>();
                for (int y = 0; y < 128; y++) {
                    for (int x = 0; x < 128; x++) {
                        temp2.add(x);
                        temp2.add(y);
                        temp2.add(DrawlerConfig.getColorID(new Color(resizedImage.getRGB(x,y))));
                        temp2.add(DrawlerConfig.getColorVariant(new Color(resizedImage.getRGB(x,y))));
                        temp.add(temp2);
                        temp2 = new ArrayList<>();
                        //x cords y cords Cid, Cvr, isChecked
                    }
                }
                while (!temp.isEmpty()){
                    Random random = new Random();
                    int ind = random.nextInt(temp.size());
                    pixeldata.add(temp.get(ind));
                    temp.remove(ind);
                }
            }
            /*
            else if (drawing_mode == 2) {
                ArrayList<ArrayList<Integer>> temp = new ArrayList<>();
                ArrayList<Integer> temp2;
                for (int y = 0; y < 128; y++) {
                    for (int x = 0; x < 128; x++) {
                        temp2 = new ArrayList<>();
                        temp2.add(x);
                        temp2.add(y);
                        temp2.add(DrawlerConfig.getColorID(new Color(resizedImage.getRGB(x,y))));
                        temp2.add(DrawlerConfig.getColorVariant(new Color(resizedImage.getRGB(x,y))));
                        temp.add(temp2);
                        //x cords y cords Cid, Cvr
                    }
                }
                while (temp.size() != 0) {
                    int lowest = 16384 + 1;
                    Color key = null;
                    for (Map.Entry<Color, Integer> entry : ColorMap.entrySet()) {
                        if (lowest < entry.getValue()) continue;
                        lowest = entry.getValue();
                        key = entry.getKey();
                    }
                    Drawler.LOGGER.info("after for colorMap this is the key and value: " + key + " " + lowest);
                    int lowid = DrawlerConfig.getColorID(key);
                    int lowvr = DrawlerConfig.getColorVariant(key);
                    Drawler.LOGGER.info("after ids and vr also temp size - " + lowid + " " + lowvr + " " + temp.size());

                    ArrayList<ArrayList<Integer>> temp3 = new ArrayList<>();
                    for (ArrayList<Integer> tmpentry : temp){
                        temp3.add(tmpentry);
                    }

                    for (ArrayList<Integer> entry : temp3){
                        Drawler.LOGGER.info("inside for " + entry.get(2) + " " + entry.get(3) + " " + entry);
                        if (entry.get(2).equals(lowid) && entry.get(3).equals(lowvr)){
                            pixeldata.add(entry);
                            temp.remove(entry);
                        }
                    }
                    ColorMap.remove(key);
                    Drawler.LOGGER.info("color map removed, here is current: " + ColorMap);
                }

            }
            */
            else { //TODO add more drawing modes
                send_message("Error: Invalid drawing mode value.");
                return;
            }
            byte[] imageBytes = null;
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(resizedImage, "png", baos);
                imageBytes = baos.toByteArray();
            } catch (Exception ignored){}

            NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(imageBytes));
            NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);
            MinecraftClient.getInstance().getTextureManager().registerTexture(Identifier.of("drawler", "urlimg.png"), texture);
            isthere = true;
            todrawimg = resizedImage;
            isdrawin = false;
            if (bacK_check_item()) {
                send_message("Предметы, которые нужно собрать:");
                check_item();
            } else {
                send_message("Ресурсы уже собраны.");
            }
        } catch (Exception ignored) {
            //ignored.printStackTrace();
            send_message("Что-то пошло не так, проверьте ссылку на изображение");
        }
    }
    private static boolean bacK_check_item(){
        if (ItemMap.isEmpty()){
            send_message("Список ресурсов пустой, вы ничего не рисуете...");
            return false;
        }
        for (Item i : ItemMap.keySet()){
            if (!MinecraftClient.getInstance().player.getInventory().contains(new ItemStack(i))){
                return true;
            }
        }
        return false;
    }
    private static void check_item(){
        boolean temp = false;
        if (ItemMap.isEmpty()){
            send_message("Список ресурсов пустой, вы ничего не рисуете...");
            return;
        }
        for (Item i : ItemMap.keySet()){
            if (!MinecraftClient.getInstance().player.getInventory().contains(new ItemStack(i))){
                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("§7[§6Drawler§7]§r ").copy().append(Text.translatable(i.getTranslationKey())));
                temp = true;
            }
        }
        if (!temp){
            send_message("Все ресурсы были уже собраны!");
        }
    }

    private static long key_point(long time){
        return (System.currentTimeMillis()-time);
    }

    public static void gonext(){
        if (isdrawin) {
            if (iscorrectin){
                if (!tocorrect.isEmpty()) {
                    int ind = -1;
                    if (correction_mode == 0) { // default left to right...
                        ind = 0;
                    } else if (correction_mode == 1) {
                        Random rand = new Random();
                        ind = rand.nextInt(tocorrect.size());
                    } else {
                        send_message("Неверное значение режима исправление. очень странно...");
                        return;
                    }
                    draw(tocorrect.get(ind).get(0), tocorrect.get(ind).get(1));
                    tocorrect.remove(ind);
                } else {
                    send_message("Работа над ошибками завершена");
                    isdrawin = false;
                    iscorrectin = false;
                    check_errors();
                }
            } else {
                if (pixeldata.size() > curIND) {
                    int x = pixeldata.get(curIND).get(0);
                    int y = pixeldata.get(curIND).get(1);
                    int Cid = pixeldata.get(y*128+x).get(2);
                    int Cvr = pixeldata.get(y*128+x).get(3);
                    if (MinecraftClient.getInstance().world == null) {
                        send_translatable("drawing.messages.error");
                        isdrawin = false;
                        return;
                    }
                    MapState mapState = MinecraftClient.getInstance().world.getMapState(new MapIdComponent(mapid));
                    if (mapState == null){
                        send_translatable("drawing.messages.id_missing");
                        return;
                    }
                    while (((MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x]) / 4)).id == Cid) &&
                            ((Byte.toUnsignedInt(mapState.colors[y * 128 + x]) - MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x])) / 4).id * 4) == Cvr))) {
                        curIND += 1;
                        x = pixeldata.get(curIND).get(0);
                        y = pixeldata.get(curIND).get(1);
                        Cid = pixeldata.get(y*128+x).get(2);
                        Cvr = pixeldata.get(y*128+x).get(3);
                    }
                    draw(x, y);

                    curIND+=1;
                } else {
                    send_message("пиксели закончились, или индекс слишком большой");
                    if (needtocorrect) check_errors();
                    isdrawin = false;
                    curIND = 0;
                }
            }
        }
    }

    private static void draw(int x, int y) {
        long start_time = System.currentTimeMillis();
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (MinecraftClient.getInstance().world == null) return;
        if (MinecraftClient.getInstance().player == null) return;
        if (MinecraftClient.getInstance().interactionManager == null) return;
        if (MinecraftClient.getInstance().crosshairTarget == null) return;
        MapState mapState = MinecraftClient.getInstance().world.getMapState(new MapIdComponent(mapid));
        if (mapState == null){
            send_translatable("drawing.messages.id_missing");
            return;
        }
        ScheduledExecutorService serv = Executors.newScheduledThreadPool(1);
        backup = serv.schedule(() -> {
            Drawler.LOGGER.info("backup message, what's going on???");
            int togo = oneback;
            while (curIND - togo < 0) { //если Х меньше чем надо пройти
                togo = togo - curIND - 1; //то убираем Х и еще одну из-за 0
                curIND =127*127; //ставим его на максимум прошлого ряда
            }
            curIND -=togo; //если же мы можем вычесть, то просто вычитаем
            DrawlerClient.gonext();
        }, delay*20L, TimeUnit.MILLISECONDS);
        serv.shutdown();
        int Cid = pixeldata.get(y*128+x).get(2); //pixeldata = x,y,Cid,Cvr,isChecked (isInside 0 or border 1 or leftalone 2)
        int Cvr = pixeldata.get(y*128+x).get(3);

        if (((MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x]) / 4)).id == Cid) &&
                ((Byte.toUnsignedInt(mapState.colors[y * 128 + x]) - MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x])) / 4).id * 4) == Cvr))) {
            backup.cancel(true);
            serv.shutdown();
            return;
        }
        Item ToFind = DrawlerConfig.items.get(Cid);
        if (player.getInventory().contains(new ItemStack(ToFind))) {
            swapItem(player.getInventory().indexOf(new ItemStack(ToFind)));
        } else {
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("§7[§6Drawler§7]§r ").copy().append(Text.translatable("drawing.messages.missing",Text.translatable(ToFind.getTranslationKey()))));
            isdrawin = false;
            curIND += 1;
            backup.cancel(true);
            serv.shutdown();
            return;
        }

        if (!((Cvr == 2 && player.getInventory().contains(new ItemStack(Items.FEATHER))) || ((Cvr == 0 || Cvr == 3) && player.getInventory().contains(new ItemStack(Items.COAL))) || Cvr == 1)){
            if (Cvr == 2) send_translatable("drawing.messages.missing",Text.translatable(Items.FEATHER.getTranslationKey()));
            else send_translatable("drawing.messages.missing",Text.translatable(Items.COAL.getTranslationKey()));
            isdrawin = false;
            curIND -= 1;
            backup.cancel(true);
            serv.shutdown();
            return;
        }

        ArrayList<Integer> temp = new ArrayList<>();
        temp.add(x);
        temp.add(y);

        for (HashMap.Entry<ArrayList<Integer>, ArrayList<Float>> list : current.entrySet()) {
            if (list.getKey().equals(temp)) {
                ArrayList<Float> degree = list.getValue();
                player.setYaw(degree.get(0));
                player.setPitch(degree.get(1));
                player.getInventory().updateItems();
                player.getInventory().markDirty();

                ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
                Drawler.LOGGER.info(list.getKey() + " " + list.getValue() + " " + Cid + " " + Cvr + " " + key_point(start_time) + " - before delays");
                Drawler.LOGGER.info(pixeldata.get(y*128+x) + " ");
                service.schedule(() -> {
                    MinecraftClient.getInstance().interactionManager.attackEntity(MinecraftClient.getInstance().player, ((EntityHitResult)MinecraftClient.getInstance().crosshairTarget).getEntity());
                    //Drawler.LOGGER.info(key_point(start_time) + " - I just painted");

                    {
                        if (Cvr == 2) { // feather 1
                            ScheduledExecutorService service2 = Executors.newScheduledThreadPool(1);
                            service2.schedule(() -> {
                                swapItem(player.getInventory().indexOf(new ItemStack(Items.FEATHER)));
                                //Drawler.LOGGER.info(key_point(start_time) + " - I just swapped to feather");
                                }, delay, TimeUnit.MILLISECONDS);
                            service2.shutdown();
                        } else if (Cvr == 3 || Cvr == 0) { //coal 2
                            ScheduledExecutorService service2 = Executors.newScheduledThreadPool(1);
                            service2.schedule(() -> {
                                swapItem(player.getInventory().indexOf(new ItemStack(Items.COAL)));
                                //Drawler.LOGGER.info(key_point(start_time) + " - I just swapped to coal");
                                }, delay, TimeUnit.MILLISECONDS);
                            service2.shutdown();
                        }

                        if (Cvr == 2 || Cvr == 0) { // feather/coal 1
                            ScheduledExecutorService service3 = Executors.newScheduledThreadPool(1);
                            service3.schedule(() -> {
                                MinecraftClient.getInstance().interactionManager.attackEntity(MinecraftClient.getInstance().player, ((EntityHitResult)MinecraftClient.getInstance().crosshairTarget).getEntity());
                                //Drawler.LOGGER.info(key_point(start_time) + " - I just painted with feather(1) or coal(1)");
                                }, delay* 2L, TimeUnit.MILLISECONDS);
                            service3.shutdown();
                        } else if (Cvr == 3) { //coal 2
                            ScheduledExecutorService service3 = Executors.newScheduledThreadPool(1);
                            service3.schedule(() -> {
                                MinecraftClient.getInstance().interactionManager.attackEntity(MinecraftClient.getInstance().player, ((EntityHitResult)MinecraftClient.getInstance().crosshairTarget).getEntity());
                                //Drawler.LOGGER.info(key_point(start_time) + " - I just painted with coal(1/2)");
                                ScheduledExecutorService service4 = Executors.newScheduledThreadPool(1);
                                service4.schedule(() -> {
                                    MinecraftClient.getInstance().interactionManager.attackEntity(MinecraftClient.getInstance().player, ((EntityHitResult)MinecraftClient.getInstance().crosshairTarget).getEntity());
                                    //Drawler.LOGGER.info(key_point(start_time) + " - I just painted with coal(2/2)");
                                    }, delay, TimeUnit.MILLISECONDS);
                                service4.shutdown();
                                }, delay* 2L, TimeUnit.MILLISECONDS);
                            service3.shutdown();
                        }

                        if (Cvr == 2 || Cvr == 0) {  //feather/coal 1
                            ScheduledExecutorService service4 = Executors.newScheduledThreadPool(1);
                            service4.schedule(() -> {
                                backup.cancel(true);
                                serv.shutdown();
                                if ((!((MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x])/4)).id == DrawlerConfig.getColorID(new Color(todrawimg.getRGB(x,y)))) &&
                                        ((Byte.toUnsignedInt(mapState.colors[y * 128 + x])-MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x]))/4).id*4) == DrawlerConfig.getColorVariant(new Color(todrawimg.getRGB(x,y))))))) {
                                    int togo = oneback;
                                    while (curIND - togo < 0) { //если Х меньше чем надо пройти
                                        togo = togo - curIND - 1; //то убираем Х и еще одну из-за 0
                                        curIND =127*127; //ставим его на максимум прошлого ряда
                                    }
                                    curIND -=togo; //если же мы можем вычесть, то просто вычитаем
                                    Drawler.LOGGER.info("now was an else, redrawing???");
                                }
                                gonext();
                            }, delay* 3L, TimeUnit.MILLISECONDS);
                            service4.shutdown();
                        } else if (Cvr == 3) { //coal 2
                            ScheduledExecutorService service4 = Executors.newScheduledThreadPool(1);
                                service4.schedule(() -> {
                                    backup.cancel(true);
                                    serv.shutdown();
                                    if ((!((MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x])/4)).id == DrawlerConfig.getColorID(new Color(todrawimg.getRGB(x,y)))) &&
                                            ((Byte.toUnsignedInt(mapState.colors[y * 128 + x])-MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x]))/4).id*4) == DrawlerConfig.getColorVariant(new Color(todrawimg.getRGB(x,y))))))) {
                                        int togo = oneback;
                                        while (curIND - togo < 0) { //если Х меньше чем надо пройти
                                            togo = togo - curIND - 1; //то убираем Х и еще одну из-за 0
                                            curIND =127*127; //ставим его на максимум прошлого ряда
                                        }
                                        curIND -=togo; //если же мы можем вычесть, то просто вычитаем
                                        Drawler.LOGGER.info("now was an else, redrawing???");
                                    }
                                    gonext();
                                }, delay* 4L, TimeUnit.MILLISECONDS);
                                service4.shutdown();
                            } else {
                                ScheduledExecutorService service4 = Executors.newScheduledThreadPool(1);
                                service4.schedule(() -> {
                                    backup.cancel(true);
                                    serv.shutdown();
                                    if ((!((MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x])/4)).id == DrawlerConfig.getColorID(new Color(todrawimg.getRGB(x,y)))) &&
                                            ((Byte.toUnsignedInt(mapState.colors[y * 128 + x])-MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x]))/4).id*4) == DrawlerConfig.getColorVariant(new Color(todrawimg.getRGB(x,y))))))) {
                                        int togo = oneback;
                                        while (curIND - togo < 0) { //если Х меньше чем надо пройти
                                            togo = togo - curIND - 1; //то убираем Х и еще одну из-за 0
                                            curIND =127*127; //ставим его на максимум прошлого ряда
                                        }
                                        curIND -=togo; //если же мы можем вычесть, то просто вычитаем
                                        Drawler.LOGGER.info("now was an else, redrawing???");
                                    }
                                    gonext();
                                }, delay, TimeUnit.MILLISECONDS);
                                service4.shutdown();
                            }

                        }
                    },delay,TimeUnit.MILLISECONDS);
                    service.shutdown();
                    break;
                }
            }
    }


    private static BufferedImage resizeImage(BufferedImage originalImage, int width, int height) {
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, width, height, null);
        g.dispose();
        return resizedImage;
    }

    private static Color findClosestMinecraftColor(Color pixelColor, ArrayList<Color> colors) {
        Color closestColor = null;
        double minDistance = Double.MAX_VALUE;
        for (Color color : colors) {
            //finding differance
            int redDiff = pixelColor.getRed() - color.getRed();
            int greenDiff = pixelColor.getGreen() - color.getGreen();
            int blueDiff = pixelColor.getBlue() - color.getBlue();
            double distance = Math.sqrt(redDiff*redDiff+greenDiff*greenDiff+blueDiff*blueDiff);
            // -------------
            if (distance < minDistance) {
                minDistance = distance;
                closestColor = color;
            }
        }
        return closestColor;
    }

    /**
     * swaps item in slot 'slot' with main hand
     * @param slot slot ID
     */
    private static void swapItem(int slot) {
        MinecraftClient.getInstance().interactionManager.pickFromInventory(slot);
        MinecraftClient.getInstance().player.getInventory().markDirty();
    }

    /**
     * sends message to player's chat
     * @param message message you want to send to player's chat(with prefix). supports formatting with char '&'
     */
    public static void send_message(String message) {
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of(("&7[&6Drawler&7]&r " + message).replace('&','§')));
    }
    /**
     * sends message to player's chat
     * @param message translatable key you want to send to player's chat(with prefix). supports formatting with char '&'
     * @param args arguments for the translation
     */
    public static void send_translatable(String message, Object... args){
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§7[§6Drawler§7]§r ").append(Text.translatable(message.replace('&','§'),args)));
    }
}


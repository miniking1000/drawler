package org.pythonchik.drawler.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sun.jna.platform.win32.COM.IUnknown;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class DrawlerClient implements ClientModInitializer {
    static String url = "";
    static Boolean isthere = false;
    static float scale = 1;
    static BufferedImage todrawimg;
    static boolean isdrawin = false;
    static HashMap<ArrayList<Integer>, ArrayList<Float>> current;
    static int curx = 0;
    static int curz = 0;
    static int mapid = -1;
    static boolean needtorender = false;
    static int correction_mode = 0;
    //0 = default, from left to right, up to down...
    //1 = random, just random.
    static boolean mode34 = true;
    static boolean needtocorrect = true;
    static boolean iscorrectin = false;
    static ArrayList<ArrayList<Integer>> tocorrect = new ArrayList<>();
    static int oneback = 3;
    static int delay = 250;
    static ScheduledFuture backup = null;
    static HashMap<Item,Integer> ItemMap;
    private static KeyBinding openMenuKeyBinding;
    private static KeyBinding pauseKeyBinding;
    private static KeyBinding gobackKeyBinding;
    private static final Identifier MAP_CHKRBRD =
            new Identifier("minecraft:textures/map/map_background.png");
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




        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("reset_drawing")
                    .executes(context -> {
                        url = "";
                        curx = 0;
                        curz = 0;
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
                                    send_message("Координаты изменены с &cx- %d, y- %d &fна&a x- %d, y- %d".formatted(curx,curz,IntegerArgumentType.getInteger(context,"x"),IntegerArgumentType.getInteger(context,"y")));
                                    curx = IntegerArgumentType.getInteger(context,"x");
                                    curz = IntegerArgumentType.getInteger(context,"y");
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
                                        backup.schedule(() -> processImage(url), 0, TimeUnit.MILLISECONDS);
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
                    send_message("Продолжаем рисовать на координатах &ax- %d, y- %d".formatted(curx,curz));
                    gonext();
                } else {
                    send_message("Останавливаемся на координатах &ax- %d, y- %d".formatted(curx,curz));
                }

            }
            while (gobackKeyBinding.wasPressed()) {
                int togo = oneback;
                while (curx - togo < 0) { //если Х меньше чем надо пройти
                    togo = togo - curx - 1; //то убираем Х и еще одну из-за 0
                    curx =127; //ставим его на максимум прошлого ряда
                    if (curz != 0) {
                        curz -= 1; //убираем ряд или ставим его на другую сторону
                    } else {
                        curz = 127;
                    }
                }
                curx -=togo; //если же мы можем вычесть, то просто вычитаем
                send_message(String.format("Теперь текущие координаты -> &ax - %d, y - %d",curx,curz));
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (needtorender) {
                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder buffer = tessellator.getBuffer();

                RenderSystem.setShaderTexture(0, MAP_CHKRBRD);
                RenderSystem.setShader(GameRenderer::getPositionColorTexProgram);
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE);
                buffer.vertex(3+1*scale, 3+1*scale, 0).color(1f, 1f, 1f, 1f).texture(0f, 0f).next();
                buffer.vertex(3+1*scale, 92 * scale, 0).color(1f, 1f, 1f, 1f).texture(0f, 1f).next();
                buffer.vertex(92 * scale, 92 * scale, 0).color(1f, 1f, 1f, 1f).texture(1f, 1f).next();
                buffer.vertex(92 * scale, 3+1*scale, 0).color(1f, 1f, 1f, 1f).texture(1f, 0f).next();
                tessellator.draw();

                if (!isthere) {
                    try {
                        URL imgurl = new URL(url);
                        URLConnection connection = imgurl.openConnection();
                        try (InputStream stream = connection.getInputStream()) {
                            NativeImage image = NativeImage.read(stream);

                            MinecraftClient.getInstance().getTextureManager().registerTexture(new Identifier("drawler", "urlimg.png"), new NativeImageBackedTexture(image));
                            isthere = true;
                        }
                    } catch (Exception e) {
                        try {
                            MinecraftClient.getInstance().getTextureManager().registerTexture(new Identifier("drawler", "urlimg.png"), new NativeImageBackedTexture(NativeImage.read(MinecraftClient.getInstance().getResourceManager().getResource(new Identifier("drawler", "default.png")).get().getInputStream())));
                        } catch (Exception ignored) {}
                    }
                }


                RenderSystem.setShaderTexture(0, new Identifier("drawler", "urlimg.png"));
                RenderSystem.setShader(GameRenderer::getPositionColorTexProgram);
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE);
                buffer.vertex(6+1*scale, 6+1*scale, 0).color(1f, 1f, 1f, 1f).texture(0f, 0f).next();
                buffer.vertex(6+1*scale, 89 * scale, 0).color(1f, 1f, 1f, 1f).texture(0f, 1f).next();
                buffer.vertex(89 * scale, 89 * scale, 0).color(1f, 1f, 1f, 1f).texture(1f, 1f).next();
                buffer.vertex(89 * scale, 6+1*scale, 0).color(1f, 1f, 1f, 1f).texture(1f, 0f).next();
                tessellator.draw();
            }
        });
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
                draw(curx, curz);
                int togo = 1;
                while (curx + togo > 127) {
                    togo = togo - (127 - curx) - 1;
                    curx = 0;
                    if (curz != 127) {
                        curz += 1;
                    } else {
                        curz = 0;
                        isdrawin = false;
                        send_message("Останавливаемся так как картина предположительно дорисована.");
                        if (needtocorrect) {
                            check_errors();
                        }
                    }
                }
                curx += togo;
            }
        }
    }

    public static void check_errors(){
        send_message("Начинаем проверку на ошибки");
        MapState mapState = MinecraftClient.getInstance().world.getMapState("map_" + mapid);
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

            byte[] imageBytes = null;
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(resizedImage, "png", baos);
                imageBytes = baos.toByteArray();
            } catch (Exception ignored){}

            NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(imageBytes));
            NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);
            MinecraftClient.getInstance().getTextureManager().registerTexture(new Identifier("drawler", "urlimg.png"), texture);
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
            send_message("Что-то пошло не так, проверьте ссылку на изображение");
        }
    }
    private static boolean bacK_check_item(){
        boolean temp = false;
        if (ItemMap.isEmpty()){
            send_message("Список ресурсов пустой, вы ничего не рисуете...");
            return false;
        }
        for (Item i : ItemMap.keySet()){
            if (!MinecraftClient.getInstance().player.getInventory().contains(new ItemStack(i))){
                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("§7[§6Drawler§7]§r ").copy().append(Text.translatable(i.getTranslationKey())));
                temp = true;
            }
        }
        return temp;
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

    private static void draw(int x, int y) {
        long start_time = System.currentTimeMillis();
        PlayerEntity player = MinecraftClient.getInstance().player;
        Color minecraftColor = new Color(todrawimg.getRGB(x, y));
        if (MinecraftClient.getInstance().world == null) return;
        MapState mapState = MinecraftClient.getInstance().world.getMapState("map_" + mapid);
        ScheduledExecutorService serv = Executors.newScheduledThreadPool(1);
        backup = serv.schedule(() -> {
            Drawler.LOGGER.info("backup message, what's going on???");
            DrawlerClient.gonext();
        }, delay*10L, TimeUnit.MILLISECONDS);
        serv.shutdown();
        int Cid = DrawlerConfig.getColorID(minecraftColor);
        int Cvr = DrawlerConfig.getColorVariant(minecraftColor);
        Item ToFind = DrawlerConfig.items.get(Cid);
        if (player.getInventory().contains(new ItemStack(ToFind))) {
            swapItem(player.getInventory().indexOf(new ItemStack(ToFind)));
        } else {
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of("§7[§6Drawler§7]§r ").copy().append(Text.translatable(ToFind.getTranslationKey()).append(Text.of(" - предмет не найден, возьми его в инвентарь для продолжения. Рисование на паузе"))));
            isdrawin = false;
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

                        if (Cvr == 2 || Cvr == 0) { // feather 1
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
                                    while (curx - togo < 0) { //если Х меньше чем надо пройти
                                        togo = togo - curx - 1; //то убираем Х и еще одну из-за 0
                                        curx =127; //ставим его на максимум прошлого ряда
                                        if (curz != 0) {
                                            curz -= 1; //убираем ряд или ставим его на другую сторону
                                        } else {
                                            curz = 127;
                                        }
                                    }
                                    curx -=togo; //если же мы можем вычесть, то просто вычитаем
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
                                        while (curx - togo < 0) { //если Х меньше чем надо пройти
                                            togo = togo - curx - 1; //то убираем Х и еще одну из-за 0
                                            curx =127; //ставим его на максимум прошлого ряда
                                            if (curz != 0) {
                                                curz -= 1; //убираем ряд или ставим его на другую сторону
                                            } else {
                                                curz = 127;
                                            }
                                        }
                                        curx -=togo; //если же мы можем вычесть, то просто вычитаем
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
                                        while (curx - togo < 0) { //если Х меньше чем надо пройти
                                            togo = togo - curx - 1; //то убираем Х и еще одну из-за 0
                                            curx =127; //ставим его на максимум прошлого ряда
                                            if (curz != 0) {
                                                curz -= 1; //убираем ряд или ставим его на другую сторону
                                            } else {
                                                curz = 127;
                                            }
                                        }
                                        curx -=togo; //если же мы можем вычесть, то просто вычитаем
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


    private static void swapItem(int slot) {
        MinecraftClient.getInstance().interactionManager.pickFromInventory(slot);
        MinecraftClient.getInstance().player.getInventory().markDirty();
    }

    public static void send_message(String message) {
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.of(("&7[&6Drawler&7]&r " + message).replace('&','§')));
    }

}


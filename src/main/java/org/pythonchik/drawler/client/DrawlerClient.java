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
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.sound.SoundEvent;
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
import org.pythonchik.drawler.sound.CustomSounds;

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
    static boolean worldrender = false;
    static Boolean isthere = false;
    static float scale = 1;
    static BufferedImage todrawimg;
    static boolean isdrawin = false;
    static double depth = 0.64;
    static double height = 1.122;
    static double sideoff = 0.5;
    static boolean after = false;
    static boolean isDebug = false;
    static boolean isDev = false;
    static boolean needExport = false;
    static boolean back_check = true;
    static HashMap<ArrayList<Integer>, ArrayList<Float>> current;
    //static int curx = 0;
    //static int curz = 0;
    static int mapid = -1;
    static int highlightColor = 0x80FF0000;
    static int correction_mode = 0;
    //0 = default, from left to right, up to down...
    //1 = random, just random.
    static int drawing_mode = 0;
    //0 = default, from left to right, up to down...
    //1 = random, just random.
    //2 = least popular color to most popular
    //3 - most popular color to least popular
    //4 - borders then filling
    static String drawing_string = "";
    static String correction_string = "";
    static String saveingname = "drawler";
    static ArrayList<ArrayList<Integer>> pixeldata = new ArrayList<>();
    static boolean mode34 = true;
    static boolean needtorender = false;
    static boolean needtohighlight = true;
    static boolean needtocorrect = true;
    static boolean needtosave = false;
    static boolean iscorrectin = false;
    static boolean needtosound = true;
    static String soundPack = "default";
    static ArrayList<ArrayList<Integer>> tocorrect = new ArrayList<>();
    static int delay = 250;
    static int curIND = 0;
    static ScheduledFuture backup = null;
    static HashMap<Item,Integer> ItemMap;
    static ArrayList<Item> RenderingItems;
    private static KeyBinding openMenuKeyBinding;
    private static KeyBinding pauseKeyBinding;
    private static KeyBinding renderKeyBinding;
    private static final Identifier MAP_CHKRBRD = Identifier.of("minecraft", "textures/map/map_background.png");

    @Override
    public void onInitializeClient() {

        CustomSounds.initialize();

        {
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
            renderKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.drawler.render",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_KP_2,
                    "category.drawler.modsettings"
            ));
        } //key binds init

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("reset_drawing")
                    .executes(context -> {
                        url = "";
                        curIND = 0;
                        worldrender = false;
                        mapid = -1;
                        isdrawin = false;
                        needtocorrect = true;
                        iscorrectin = false;
                        isthere = false;
                        todrawimg = null;
                        RenderingItems = null;
                        ItemMap = null;
                        needExport = false;
                        tocorrect = new ArrayList<>();
                        ItemMap = new HashMap<>();
                        current = new HashMap<>();
                        send_translatable("drawing.messages.completed");
                        return 1;
                    }));
        }); //reset_drawing command

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("check_items")
                            .executes(context -> {
                                if (todrawimg != null) {
                                    send_translatable("drawing.messages.missing_those");
                                    check_item();
                                    updateRender();
                                } else {
                                    send_translatable("drawing.messages.no_drawing");
                                }
                                return 1;
                            }));
        }); //check_items command

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("FixYourself!")
                    .executes(context -> {
                        check_errors();
                        return 1;
                    }));
        }); //fixyourself command

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("set_drawing")
                    .then(ClientCommandManager.argument("point", IntegerArgumentType.integer(0,128*128))
                            .executes(context -> {
                                send_translatable("drawing.messages.cords_changed",curIND,IntegerArgumentType.getInteger(context,"point"));
                                curIND = IntegerArgumentType.getInteger(context,"point");
                                return 1;
                            })));
        }); //set_drawing <point> command

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("set_drawing")
                    .then(ClientCommandManager.argument("x", IntegerArgumentType.integer(0,128))
                            .then(ClientCommandManager.argument("y", IntegerArgumentType.integer(0,128))
                                .executes(context -> {
                                    send_translatable("drawing.messages.cords_changed",curIND,IntegerArgumentType.getInteger(context,"x")+128*IntegerArgumentType.getInteger(context,"y"));
                                    curIND = IntegerArgumentType.getInteger(context,"y")*128+IntegerArgumentType.getInteger(context,"x");
                                    return 1;
                                }))));
        }); //set_drawing <x> <y> command

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
                                        send_translatable("drawing.messages.no_direction");
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
                                                send_translatable("drawing.messages.item_for_this",I18n.translate(ToFind.getTranslationKey()));
                                            } else if (entry.get(3).equals(2)) { //feather 1
                                                send_translatable("drawing.messages.items_for_this",I18n.translate(ToFind.getTranslationKey()),I18n.translate(Items.FEATHER.getTranslationKey()));
                                            } else if (entry.get(3).equals(0)) { //coal 1
                                                send_translatable("drawing.messages.items_for_this",I18n.translate(ToFind.getTranslationKey()),I18n.translate(Items.COAL.getTranslationKey()));
                                            } else if (entry.get(3).equals(3)) { //coal 2
                                                send_translatable("drawing.messages.itemss_for_this",I18n.translate(ToFind.getTranslationKey()),I18n.translate(Items.COAL.getTranslationKey()),I18n.translate(Items.COAL.getTranslationKey()));
                                            }
                                            break;
                                        }
                                    }
                                } else {
                                    send_translatable("drawing.messages.done");
                                }
                                return 1;
                            })));
        }); //set_camera <point> command

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
                                            send_translatable("drawing.messages.no_direction");
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
                                                    send_translatable("drawing.messages.item_for_this",I18n.translate(ToFind.getTranslationKey()));
                                                } else if (entry.get(3).equals(2)) { //feather 1
                                                    send_translatable("drawing.messages.items_for_this",I18n.translate(ToFind.getTranslationKey()),I18n.translate(Items.FEATHER.getTranslationKey()));
                                                } else if (entry.get(3).equals(0)) { //coal 1
                                                    send_translatable("drawing.messages.items_for_this",I18n.translate(ToFind.getTranslationKey()),I18n.translate(Items.COAL.getTranslationKey()));
                                                } else if (entry.get(3).equals(3)) { //coal 2
                                                    send_translatable("drawing.messages.itemss_for_this",I18n.translate(ToFind.getTranslationKey()),I18n.translate(Items.COAL.getTranslationKey()),I18n.translate(Items.COAL.getTranslationKey()));
                                                }
                                                break;
                                            }
                                        }
                                    } else {
                                        send_translatable("drawing.messages.done");
                                    }
                                    return 1;
                            }))));
        }); //set_camera <x> <y> command

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("drawpic")
                    .then(ClientCommandManager.argument("mapID", IntegerArgumentType.integer(0))
                            .suggests((context, builder) -> {
                                MinecraftClient client = MinecraftClient.getInstance();
                                if (client.player == null) return builder.buildFuture();
                                for (ItemStack stack : new ItemStack[]{client.player.getMainHandStack(), client.player.getOffHandStack()}) {
                                    if (stack.getItem() instanceof FilledMapItem) {
                                        builder.suggest(stack.get(DataComponentTypes.MAP_ID).id());
                                    }
                                }

                                // Suggest map ID from item frame
                                if (client.crosshairTarget instanceof EntityHitResult entityHitResult) {
                                    if (entityHitResult.getEntity() instanceof ItemFrameEntity itemFrame) {
                                        ItemStack stack = itemFrame.getHeldItemStack();
                                        if (stack.getItem() instanceof FilledMapItem) {
                                            builder.suggest(stack.get(DataComponentTypes.MAP_ID).id());
                                        }
                                    }
                                }
                                return builder.buildFuture();
                            })
                            .then(ClientCommandManager.argument("url", StringArgumentType.greedyString())
                                    .executes(context -> {
                                        send_translatable("drawing.messages.processing_image");
                                        mapid = IntegerArgumentType.getInteger(context, "mapID");
                                        //prevent all the errors due to unloaded map by doing one check...
                                        if (MinecraftClient.getInstance().world.getMapState(new MapIdComponent(mapid)) == null) {
                                            send_translatable("drawing.errors.map_id_incorrect");
                                            return 1;
                                        }

                                        url = StringArgumentType.getString(context, "url");
                                        ScheduledExecutorService backup = Executors.newScheduledThreadPool(1);
                                        backup.schedule(() -> {
                                            processImage(url);
                                        }, 0, TimeUnit.MILLISECONDS);
                                        backup.shutdown();
                                        return 1;
                                    }))));
        }); //drawpick command

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKeyBinding.wasPressed()) {
                MinecraftClient.getInstance().setScreen(DrawlerSettings.create(MinecraftClient.getInstance().currentScreen));
            }
            while (pauseKeyBinding.wasPressed()){
                if (todrawimg == null) {
                    send_translatable("drawing.messages.no_drawing_no_pausing");
                    return;
                }
                isdrawin = !isdrawin;
                if (isdrawin) {
                    send_translatable("drawing.messages.continuing_from", curIND);
                    MapState mapState = MinecraftClient.getInstance().world.getMapState(new MapIdComponent(mapid));
                    if (mapState == null) {
                        isdrawin = false;
                        send_translatable("drawing.messages.id_missing");
                        return;
                    }
                    int timeMS = 0;
                    for (int y = 0; y < 128; y++) {
                        for (int x = 0; x < 128; x++) {
                            int Cid = DrawlerConfig.getColorID(new Color(todrawimg.getRGB(x, y)));
                            int Cvr = DrawlerConfig.getColorVariant(new Color(todrawimg.getRGB(x, y)));
                            if (!((MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x]) / 4)).id == Cid) &&
                                    ((Byte.toUnsignedInt(mapState.colors[y * 128 + x]) - MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x])) / 4).id * 4) == Cvr))) {
                                Color color = new Color(todrawimg.getRGB(x, y));
                                if (!(MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x]) / 4)).id == Cid)) {
                                    timeMS += delay * (DrawlerConfig.getColorVariant(color) == 1 ? 2 : DrawlerConfig.getColorVariant(color) == 3 ? 5 : 4);
                                    continue;
                                }
                                // base color is correct:
                                int CCvr = (Byte.toUnsignedInt(mapState.colors[y * 128 + x]) - MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x])) / 4).id * 4);
                                if (CCvr == 1) {
                                    timeMS += delay * ((Cvr == 2 || Cvr == 0) ? 3 : 4);
                                } else if (CCvr == 0) {
                                    timeMS += delay * ((Cvr == 3 || Cvr == 1) ? 3 : 4);
                                } else if (CCvr == 2) {
                                    timeMS += delay * (Cvr == 1 ? 3 : Cvr == 0 ? 4 : 5);
                                } else {
                                    timeMS += delay * (Cvr == 0 ? 3 : Cvr == 1 ? 4 : 5);
                                }
                            }
                        }
                    }
                    send_translatable("drawing.messages.time_remaining",timeMS/3600000,(timeMS/60000)%60,(timeMS/1000)%60);
                    gonext();
                } else {
                    send_translatable("drawing.messages.pausing_on",curIND);
                }
            }

            while (renderKeyBinding.wasPressed()) {
                worldrender = !worldrender;
                send_translatable("drawing.messages.done_sad");
            }
        }); //end of client tick (key presses)

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
        }); //end of hudRenderer

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
                        send_translatable("drawing.messages.YOU_CAN_NOT");
                    }
                }

                Matrix4f positionMatrix = matrixStack.peek().getPositionMatrix();
                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder buffer = tessellator.getBuffer();

                buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE);
                buffer.vertex(positionMatrix, 0, 1, 0).color(1f, 1f, 1f, 1f).texture(0f, 0f).next();
                buffer.vertex(positionMatrix, 0, 0, 0).color(1f, 1f, 1f, 1f).texture(0f, 1f).next();
                buffer.vertex(positionMatrix, 1, 0, 0).color(1f, 1f, 1f, 1f).texture(1f, 1f).next();
                buffer.vertex(positionMatrix, 1, 1, 0).color(1f, 1f, 1f, 1f).texture(1f, 0f).next();

                RenderSystem.setShader(GameRenderer::getPositionColorTexProgram);
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


    public static void check_errors() {
        send_translatable("drawing.messages.start_checking");
        tocorrect = new ArrayList<>();
        MapState mapState = MinecraftClient.getInstance().world.getMapState(new MapIdComponent(mapid));
        int timeMS = 0;
        if (mapState != null) {
            for (int y = 0; y < 128; y++) {
                for (int x = 0; x < 128; x++) {
                    if (!((MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x]) / 4)).id == DrawlerConfig.getColorID(new Color(todrawimg.getRGB(x, y)))) &&
                            ((Byte.toUnsignedInt(mapState.colors[y * 128 + x]) - MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x])) / 4).id * 4) == DrawlerConfig.getColorVariant(new Color(todrawimg.getRGB(x, y)))))) {
                        Color color = new Color(todrawimg.getRGB(x, y));
                        tocorrect.add(new ArrayList<>(List.of(x, y,DrawlerConfig.getColorID(color),DrawlerConfig.getColorVariant(color))));
                        timeMS += delay * (DrawlerConfig.getColorVariant(color) == 1 ? 2 : DrawlerConfig.getColorVariant(color) == 3 ? 5 : 4);
                        iscorrectin = true;
                    }
                }
            }
            if (tocorrect.isEmpty()) {
                send_translatable("correction.messages.no_errors");
                playSound(soundPack + "_done");
                iscorrectin = false;
                isdrawin = false;
                if (needtosave) {
                    if (MinecraftClient.getInstance().player.getInventory().getEmptySlot() == -1){
                        send_translatable("drawing.saving.no_slots");
                        return;
                    }
                    if (saveingname.length() >= 3 && saveingname.length() <= 16) { //name is valid . . kinda?
                        MinecraftClient.getInstance().player.networkHandler.sendChatCommand("art save " + saveingname);
                        send_translatable("drawing.saving.saved_as", saveingname);
                    } else {
                        send_translatable("drawing.saving.invalid_name");
                    }
                }
                //TODO continued drawing in queue
            } else {
                send_translatable("correction.messages.yes_errors__correcting",tocorrect.size(),timeMS/3600000,(timeMS/60000)%60,(timeMS/1000)%60);
                playSound(soundPack + "_error");
                if (backup.isCancelled() || backup.isDone()) {
                    gonext();
                }
            }
        } else {
            send_translatable("drawing.messages.id_missing");
        }
    }

    public static void processImage(String url) {
        try {
            BufferedImage originalImage = ImageIO.read(new URL(url));
            BufferedImage resizedImage = resizeImage(originalImage, 128, 128);

            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            debug(player.getHorizontalFacing().toString());
            switch (player.getHorizontalFacing()) {
                case EAST -> current = DrawlerConfig.east;
                case WEST -> current = DrawlerConfig.west;
                case NORTH -> current = DrawlerConfig.north;
                case SOUTH -> current = DrawlerConfig.south;
                default -> {
                    send_translatable("drawing.messages.no_direction");
                    return;
                }
            } //choosing correct angles

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

                    //debug("inside if we got: " + ItemMap + " - itemmap | colormap - " + ColorMap + " | colors - " + colors);
                    int lowest = 16384+1;
                    Color key = null;
                    for (Map.Entry<Color,Integer> entry: ColorMap.entrySet()){
                        if (lowest < entry.getValue()) continue;
                        lowest = entry.getValue();
                        key = entry.getKey();
                    }
                    colors.remove(key);
                    //debug("removed - " + key + " " + colors + " - colors");
                }
            } while (mode34 && ItemMap.size() > 34);

            ItemMap.put(Items.COAL,1); //TODO don't assume that you need this, and check if there are 1 and 0 or 3 variants in the painting
            ItemMap.put(Items.FEATHER,1);

            pixeldata = getPixeldata(resizedImage);
            if (needExport) {
                needExport = false;
                String result = FormatPixelData(pixeldata);
                if (!result.isEmpty()) {
                    MinecraftClient.getInstance().keyboard.setClipboard(result);
                    delay_translatable("drawing.messages.exported_successfully", 250);
                } else {
                    debug("Error in setting export");
                }
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
            iscorrectin = false;
            tocorrect = new ArrayList<>();
            updateRender();
            if (bacK_check_item()) {
                send_translatable("drawing.messages.item_to_collect");
                check_item();
            } else {
                send_translatable("drawing.messages.items_collected");
            }
        } catch (Exception ignored) {
            send_translatable("drawing.messages.check_img");
        }
    }

    /**
     * returns pixel data, the order of pixels in which you should draw image
     * @param image image, from which pixels will be taken
     * @return ArrayList<ArrayList<Integer>> following this format [[x,y,Color id, Color variant],[x+1,y,Color id, Color variant], [x,y+1,Color id, Color variant], [x+1,y+1, Color id, Color variant]]
     */
    private static ArrayList<ArrayList<Integer>> getPixeldata(BufferedImage image){
        ArrayList<ArrayList<Integer>> pixeldata = new ArrayList<>();
        if (drawing_mode == 0) {
            ArrayList<Integer> temp = new ArrayList<>();
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    temp.add(x);
                    temp.add(y);
                    temp.add(DrawlerConfig.getColorID(new Color(image.getRGB(x,y))));
                    temp.add(DrawlerConfig.getColorVariant(new Color(image.getRGB(x,y))));
                    pixeldata.add(temp);
                    temp = new ArrayList<>();
                    //x cords y cords Cid, Cvr, isChecked, border?
                }
            }
        }  //left to right, top to bottom
        else if (drawing_mode == 1) {
            ArrayList<ArrayList<Integer>> temp = new ArrayList<>();
            ArrayList<Integer> temp2 = new ArrayList<>();
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    temp2.add(x);
                    temp2.add(y);
                    temp2.add(DrawlerConfig.getColorID(new Color(image.getRGB(x, y))));
                    temp2.add(DrawlerConfig.getColorVariant(new Color(image.getRGB(x, y))));
                    temp.add(temp2);
                    temp2 = new ArrayList<>();
                    //x cords y cords Cid, Cvr, isChecked
                }
            }
            while (!temp.isEmpty()) {
                Random random = new Random();
                int ind = random.nextInt(temp.size());
                pixeldata.add(temp.get(ind));
                temp.remove(ind);
            }
        } //random
        else if (drawing_mode == 2) {
            ArrayList<ArrayList<Integer>> temp = new ArrayList<>();
            ArrayList<Integer> temp2;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    temp2 = new ArrayList<>();
                    temp2.add(x);
                    temp2.add(y);
                    temp2.add(DrawlerConfig.getColorID(new Color(image.getRGB(x,y))));
                    temp2.add(DrawlerConfig.getColorVariant(new Color(image.getRGB(x,y))));
                    temp.add(temp2);
                }
            }
            HashMap<Integer,ArrayList<ArrayList<Integer>>> ids = new HashMap<>();
            for (ArrayList<Integer> entry: temp){
                ArrayList<ArrayList<Integer>> indexes = ids.getOrDefault(entry.get(2),new ArrayList<>());
                indexes.add(new ArrayList<Integer>(List.of(entry.get(0),entry.get(1))));
                ids.put(entry.get(2),indexes);
            }
            while (!ids.isEmpty()) {
                int lowest = Integer.MAX_VALUE;
                int lowkey = 0;
                for (Integer key : ids.keySet()){
                    if (ids.get(key).size() < lowest){
                        lowest = ids.get(key).size();
                        lowkey = key;
                    }
                }
                for (ArrayList<Integer> index : ids.get(lowkey)){
                    for (ArrayList<Integer> haha : temp) {
                        if (Objects.equals(haha.get(0), index.get(0)) && Objects.equals(haha.get(1), index.get(1))) {
                            pixeldata.add(haha);
                            break;
                        }
                    }
                }
                ids.remove(lowkey);
            }

        } //least popular color to most
        else if (drawing_mode == 3) {
            ArrayList<ArrayList<Integer>> temp = new ArrayList<>();
            ArrayList<Integer> temp2;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    temp2 = new ArrayList<>();
                    temp2.add(x);
                    temp2.add(y);
                    temp2.add(DrawlerConfig.getColorID(new Color(image.getRGB(x,y))));
                    temp2.add(DrawlerConfig.getColorVariant(new Color(image.getRGB(x,y))));
                    temp.add(temp2);
                }
            }
            HashMap<Integer,ArrayList<ArrayList<Integer>>> ids = new HashMap<>();
            for (ArrayList<Integer> entry: temp){
                ArrayList<ArrayList<Integer>> indexes = ids.getOrDefault(entry.get(2),new ArrayList<>());
                indexes.add(new ArrayList<Integer>(List.of(entry.get(0),entry.get(1))));
                ids.put(entry.get(2),indexes);
            }
            while (!ids.isEmpty()) {
                int lowest = Integer.MIN_VALUE;
                int lowkey = 0;
                for (Integer key : ids.keySet()){
                    if (ids.get(key).size() > lowest){
                        lowest = ids.get(key).size();
                        lowkey = key;
                    }
                }
                for (ArrayList<Integer> index : ids.get(lowkey)){
                    for (ArrayList<Integer> haha : temp) {
                        if (Objects.equals(haha.get(0), index.get(0)) && Objects.equals(haha.get(1), index.get(1))) {
                            pixeldata.add(haha);
                            break;
                        }
                    }
                }
                ids.remove(lowkey);
            }
        } // most popular color to least
        else if (drawing_mode == 4) {
            ArrayList<ArrayList<Integer>> temp = new ArrayList<>();
            ArrayList<Integer> temp2;

            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    temp2 = new ArrayList<>();
                    temp2.add(x);
                    temp2.add(y);
                    temp2.add(DrawlerConfig.getColorID(new Color(image.getRGB(x, y))));
                    temp2.add(DrawlerConfig.getColorVariant(new Color(image.getRGB(x, y))));
                    temp.add(temp2);
                }
            }

            ArrayList<ArrayList<Integer>> list1 = new ArrayList<>();
            ArrayList<ArrayList<Integer>> list2 = new ArrayList<>();

            int[][] neighbors = {
                    {-1, 0}, {1, 0}, // left, right
                    {0, -1}, {0, 1}  // up, down
            };
            for (ArrayList<Integer> pixel : temp) {
                int x = pixel.get(0);
                int y = pixel.get(1);
                int colorId = pixel.get(2);
                int colorVariant = pixel.get(3);
                boolean allNeighborsMatch = true;

                for (int[] dir : neighbors) {
                    int newX = x + dir[0];
                    int newY = y + dir[1];
                    if (newX >= 0 && newX < 128 && newY >= 0 && newY < 128) {
                        ArrayList<Integer> neighbor = getPixelByCoordinates(temp, newX, newY);
                        if (neighbor == null || neighbor.get(2) != colorId || neighbor.get(3) != colorVariant) {
                            allNeighborsMatch = false;
                            break;
                        }
                    }
                }

                if (allNeighborsMatch) {
                    list2.add(pixel);
                } else {
                    list1.add(pixel);
                }
            }

            list1.addAll(list2);

            pixeldata.addAll(list1);
        } // borders  then filling
        else { //TODO add more drawing modes
            send_translatable("drawing.errors.drawing_mode");
            return pixeldata;
        }
        return pixeldata;
    }

    public static ArrayList<Integer> getPixelByCoordinates(ArrayList<ArrayList<Integer>> temp, int x, int y) {
        for (ArrayList<Integer> pixel : temp) {
            if (pixel.get(0) == x && pixel.get(1) == y) {
                return pixel;
            }
        }
        return null;
    }

    private static boolean bacK_check_item(){
        if (ItemMap.isEmpty()){
            send_translatable("drawing.errors.empty_items");
            return false;
        }
        MapState mapState = MinecraftClient.getInstance().world.getMapState(new MapIdComponent(mapid));
        if (mapState != null) {
            for (int y = 0; y < 128; y++) {
                for (int x = 0; x < 128; x++) {
                    if ((!((MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x]) / 4)).id == DrawlerConfig.getColorID(new Color(todrawimg.getRGB(x, y)))) &&
                            ((Byte.toUnsignedInt(mapState.colors[y * 128 + x]) - MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x])) / 4).id * 4) == DrawlerConfig.getColorVariant(new Color(todrawimg.getRGB(x, y))))))
                            && !MinecraftClient.getInstance().player.getInventory().containsAny(Set.of(DrawlerConfig.items.get(DrawlerConfig.getColorID(new Color(todrawimg.getRGB(x,y))))))) {
                        return true;
                    }
                }
            }
            if (!MinecraftClient.getInstance().player.getInventory().containsAny(Set.of(Items.COAL))) {
                send_translatable(Items.COAL.getTranslationKey());
                return true;
            }
            if (!MinecraftClient.getInstance().player.getInventory().containsAny(Set.of(Items.FEATHER))){
                send_translatable(Items.FEATHER.getTranslationKey());
                return true;
            }
        } else {
            send_translatable("drawing.messages.id_missing");
        }
        return false;
    }

    private static void check_item(){
        boolean temp = false;
        if (ItemMap.isEmpty()){
            send_translatable("drawing.errors.empty_items");
            return;
        }
        ArrayList<Item> seen = new ArrayList<>();
        MapState mapState = MinecraftClient.getInstance().world.getMapState(new MapIdComponent(mapid));
        if (mapState != null) {
            for (int y = 0; y < 128; y++) {
                for (int x = 0; x < 128; x++) {
                    if ((!((MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x]) / 4)).id == DrawlerConfig.getColorID(new Color(todrawimg.getRGB(x, y)))) &&
                            ((Byte.toUnsignedInt(mapState.colors[y * 128 + x]) - MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x])) / 4).id * 4) == DrawlerConfig.getColorVariant(new Color(todrawimg.getRGB(x, y))))))
                            && !MinecraftClient.getInstance().player.getInventory().containsAny(Set.of(DrawlerConfig.items.get(DrawlerConfig.getColorID(new Color(todrawimg.getRGB(x,y))))))) {
                        Color color = new Color(todrawimg.getRGB(x, y));
                        if (!seen.contains(DrawlerConfig.items.get(DrawlerConfig.getColorID(color)))) {
                            seen.add(DrawlerConfig.items.get(DrawlerConfig.getColorID(color)));
                            send_translatable(seen.getLast().getTranslationKey());
                            temp = true;
                        }
                    }
                }
            }
            if (!MinecraftClient.getInstance().player.getInventory().containsAny(Set.of(Items.COAL))){
                send_translatable(Items.COAL.getTranslationKey());
                temp = true;
            }
            if (!MinecraftClient.getInstance().player.getInventory().containsAny(Set.of(Items.FEATHER))){
                send_translatable(Items.FEATHER.getTranslationKey());
                temp = true;
            }
        } else {
            send_translatable("drawing.messages.id_missing");
        }
        if (!temp){
            send_translatable("drawing.messages.items_collected");
        }
    }

    private static void updateRender() {
        RenderingItems = new ArrayList<>(List.of(Items.COAL,Items.FEATHER));
        MapState mapState = MinecraftClient.getInstance().world.getMapState(new MapIdComponent(mapid));
        if (mapState != null) {
            for (int y = 0; y < 128; y++) {
                for (int x = 0; x < 128; x++) {
                    if (!((MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x]) / 4)).id == DrawlerConfig.getColorID(new Color(todrawimg.getRGB(x, y)))) &&
                            ((Byte.toUnsignedInt(mapState.colors[y * 128 + x]) - MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x])) / 4).id * 4) == DrawlerConfig.getColorVariant(new Color(todrawimg.getRGB(x, y)))))) {
                        Color color = new Color(todrawimg.getRGB(x, y));
                        RenderingItems.add(DrawlerConfig.items.get(DrawlerConfig.getColorID(color)));
                    }
                }
            }
            if (RenderingItems.size() == 2){
                RenderingItems = new ArrayList<>();
            }
        } else {
            send_translatable("drawing.messages.id_missing");
            RenderingItems = new ArrayList<>();
        }
    }

    public static void gonext(){
        if (isdrawin) {
            if (iscorrectin) {
                if (!tocorrect.isEmpty()) {
                    int ind = -1;
                    if (correction_mode == 0) { // default left to right...
                        ind = 0;
                    } else if (correction_mode == 1) {
                        Random rand = new Random();
                        ind = rand.nextInt(tocorrect.size());
                    } else {
                        send_translatable("drawing.error.correction_mode");
                        return;
                    }
                    draw(tocorrect.get(ind));
                    tocorrect.remove(ind);
                } else {
                    send_translatable("correction.messages.completed");
                    isdrawin = false;
                    iscorrectin = false;
                    check_errors();
                }
            } else {
                if (pixeldata.size() > curIND && curIND >= 0) {
                    int x = pixeldata.get(curIND).get(0);
                    int y = pixeldata.get(curIND).get(1);
                    int Cid = pixeldata.get(curIND).get(2);
                    int Cvr = pixeldata.get(curIND).get(3);
                    debug(curIND + " " + x + " " + y + " " + Cid + " " + Cvr);
                    if (MinecraftClient.getInstance().world == null) {
                        send_translatable("drawing.messages.error");
                        playSound(soundPack + "_error");
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
                        if (!(pixeldata.size() > curIND && curIND >= 0)) {
                            send_translatable("drawing.errors.index_too_big");
                            isdrawin = false;
                            curIND = 0;
                            if (needtocorrect) check_errors();
                            break;
                        } else {
                            x = pixeldata.get(curIND).get(0);
                            y = pixeldata.get(curIND).get(1);
                            Cid = pixeldata.get(curIND).get(2);
                            Cvr = pixeldata.get(curIND).get(3);
                            debug(curIND + " " + x + " " + y + " " + Cid + " " + Cvr + " - map was correct, new values");
                        }
                    }
                    debug("drawing from gonext, curind + pixeldata: " + curIND + " " + pixeldata.get(curIND));
                    draw(pixeldata.get(curIND));
                    curIND+=1;
                } else {
                    send_translatable("drawing.errors.index_too_big");
                    isdrawin = false;
                    curIND = 0;
                    if (needtocorrect) check_errors();
                }
            }
        }
    }

    private static void draw(ArrayList<Integer> data) {
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
            debug("backup message, what's going on???");
            int togo = 2;
            while (curIND - togo < 0) { //если Х меньше чем надо пройти
                togo = togo - curIND - 1; //то убираем Х и еще одну из-за 0
                curIND =127*127; //ставим его на максимум прошлого ряда
            }
            curIND -=togo; //если же мы можем вычесть, то просто вычитаем
            DrawlerClient.gonext();
        }, delay*20L, TimeUnit.MILLISECONDS);

        //(pixel)data = x,y,Cid,Cvr
        int x = data.get(0);
        int y = data.get(1);
        int Cid = data.get(2);
        int Cvr = data.get(3);

        debug(x + " " + y + " " + Cid + " " + Cvr);

        //checking if color's the same as should be, then gonext();
        if (((MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x]) / 4)).id == Cid) &&
                ((Byte.toUnsignedInt(mapState.colors[y * 128 + x]) - MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x])) / 4).id * 4) == Cvr))) {
            backup.cancel(true);
            serv.shutdown();
            curIND = 0;
            gonext();
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
                if (!(MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x]) / 4)).id == Cid)) {
                    //taking item in hand, or returning, if we don't have it
                    Item ToFind = DrawlerConfig.items.get(Cid);
                    if (player.getInventory().containsAny(Set.of(ToFind))) {
                        for (int i = 0;i<36;i++) {
                            if (player.getInventory().getStack(i).getItem().equals(ToFind)) {
                                swapItem(i);
                                break;
                            }
                        }
                    } else {
                        if (back_check) {
                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    if (player.getInventory().containsAny(Set.of(ToFind))) {
                                        curIND = 0;
                                        gonext();
                                    } else {
                                        send_translatable("drawing.messages.missing", I18n.translate(ToFind.getTranslationKey()));
                                        playSound(soundPack + "_error");
                                        isdrawin = false;
                                        curIND -= 1;
                                        backup.cancel(true);
                                        serv.shutdown();
                                    }
                                    return;
                                }
                            }, 1000);
                        } else {
                            send_translatable("drawing.messages.missing", I18n.translate(ToFind.getTranslationKey()));
                            playSound(soundPack + "_error");
                            isdrawin = false;
                            curIND -= 1;
                            backup.cancel(true);
                            serv.shutdown();
                            return;
                        }

                    }

                    // check if we are missing coal of feather
                    if (!((Cvr == 2 && player.getInventory().containsAny(Set.of(Items.FEATHER))) || ((Cvr == 0 || Cvr == 3) && player.getInventory().containsAny(Set.of(Items.COAL))) || Cvr == 1)) {
                        if (back_check) {
                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    if (!((Cvr == 2 && player.getInventory().containsAny(Set.of(Items.FEATHER))) || ((Cvr == 0 || Cvr == 3) && player.getInventory().containsAny(Set.of(Items.COAL))) || Cvr == 1)) {
                                        if (Cvr == 2)
                                            send_translatable("drawing.messages.missing", I18n.translate(Items.FEATHER.getTranslationKey()));
                                        else
                                            send_translatable("drawing.messages.missing", I18n.translate(Items.COAL.getTranslationKey()));
                                        playSound(soundPack + "_error");
                                        isdrawin = false;
                                        curIND -= 1;
                                        backup.cancel(true);
                                        serv.shutdown();
                                    } else {
                                        curIND = 0;
                                        gonext();
                                    }
                                    return;
                                }
                            }, 1000);
                        } else {
                            if (Cvr == 2)
                                send_translatable("drawing.messages.missing", I18n.translate(Items.FEATHER.getTranslationKey()));
                            else
                                send_translatable("drawing.messages.missing", I18n.translate(Items.COAL.getTranslationKey()));
                            playSound(soundPack + "_error");
                            isdrawin = false;
                            curIND -= 1;
                            backup.cancel(true);
                            serv.shutdown();
                        }

                        return;
                    }

                    ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
                    debug(list.getKey() + " " + list.getValue() + " " + Cid + " " + Cvr + " " + key_point(start_time) + " - before delays");
                    debug(data + " - (pixel)data(of curIND or tocorrect(ind))");
                    service.schedule(() -> {
                        MinecraftClient.getInstance().interactionManager.attackEntity(MinecraftClient.getInstance().player, ((EntityHitResult) MinecraftClient.getInstance().crosshairTarget).getEntity());
                        //debug(key_point(start_time) + " - I just painted");

                        {
                            //taking items coal/feather
                            if (Cvr == 2) { // feather 1
                                ScheduledExecutorService service2 = Executors.newScheduledThreadPool(1);
                                service2.schedule(() -> {
                                    for (int i = 0;i<36;i++) {
                                        if (player.getInventory().getStack(i).getItem().equals(Items.FEATHER)) {
                                            swapItem(i);
                                            break;
                                        }
                                    }
                                    debug(key_point(start_time) + " - I just swapped to feather");
                                }, delay, TimeUnit.MILLISECONDS);
                                service2.shutdown();
                            } else if (Cvr == 3 || Cvr == 0) { //coal 2
                                ScheduledExecutorService service2 = Executors.newScheduledThreadPool(1);
                                service2.schedule(() -> {
                                    for (int i = 0;i<36;i++) {
                                        if (player.getInventory().getStack(i).getItem().equals(Items.COAL)) {
                                            swapItem(i);
                                            break;
                                        }
                                    }
                                    debug(key_point(start_time) + " - I just swapped to coal");
                                }, delay, TimeUnit.MILLISECONDS);
                                service2.shutdown();
                            }

                            //shading or lighting
                            if (Cvr == 2 || Cvr == 0) { // feather/coal 1
                                ScheduledExecutorService service3 = Executors.newScheduledThreadPool(1);
                                service3.schedule(() -> {
                                    MinecraftClient.getInstance().interactionManager.attackEntity(MinecraftClient.getInstance().player, ((EntityHitResult) MinecraftClient.getInstance().crosshairTarget).getEntity());
                                    debug(key_point(start_time) + " - I just painted with feather(1) or coal(1)");
                                }, delay * 2L, TimeUnit.MILLISECONDS);
                                service3.shutdown();
                            } else if (Cvr == 3) { //coal 2
                                ScheduledExecutorService service3 = Executors.newScheduledThreadPool(1);
                                service3.schedule(() -> {
                                    MinecraftClient.getInstance().interactionManager.attackEntity(MinecraftClient.getInstance().player, ((EntityHitResult) MinecraftClient.getInstance().crosshairTarget).getEntity());
                                    debug(key_point(start_time) + " - I just painted with coal(1/2)");
                                    ScheduledExecutorService service4 = Executors.newScheduledThreadPool(1);
                                    service4.schedule(() -> {
                                        MinecraftClient.getInstance().interactionManager.attackEntity(MinecraftClient.getInstance().player, ((EntityHitResult) MinecraftClient.getInstance().crosshairTarget).getEntity());
                                        debug(key_point(start_time) + " - I just painted with coal(2/2)");
                                    }, delay, TimeUnit.MILLISECONDS);
                                    service4.shutdown();
                                }, delay * 2L, TimeUnit.MILLISECONDS);
                                service3.shutdown();
                            }

                            //going next
                            if (Cvr == 2 || Cvr == 0) {  //feather/coal 1
                                ScheduledExecutorService service4 = Executors.newScheduledThreadPool(1);
                                service4.schedule(() -> {
                                    backup.cancel(true);
                                    serv.shutdown();

                                /*
                                if ((!((MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x])/4)).id == DrawlerConfig.getColorID(new Color(todrawimg.getRGB(x,y)))) &&
                                        ((Byte.toUnsignedInt(mapState.colors[y * 128 + x])-MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x]))/4).id*4) == DrawlerConfig.getColorVariant(new Color(todrawimg.getRGB(x,y))))))) {
                                    int togo = 3;
                                    while (curIND - togo < 0) { //если Х меньше чем надо пройти
                                        togo = togo - curIND - 1; //то убираем Х и еще одну из-за 0
                                        curIND =127*127; //ставим его на максимум прошлого ряда
                                    }
                                    curIND -=togo; //если же мы можем вычесть, то просто вычитаем
                                    debug("now was an else, redrawing???");
                                }
                                 */
                                    debug("updating render");
                                    updateRender();
                                    debug("going next");
                                    gonext();
                                }, delay * 3L, TimeUnit.MILLISECONDS);
                                service4.shutdown();
                            } else if (Cvr == 3) { //coal 2
                                ScheduledExecutorService service4 = Executors.newScheduledThreadPool(1);
                                service4.schedule(() -> {
                                    backup.cancel(true);
                                    serv.shutdown();

                                    /*
                                    if ((!((MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x])/4)).id == DrawlerConfig.getColorID(new Color(todrawimg.getRGB(x,y)))) &&
                                            ((Byte.toUnsignedInt(mapState.colors[y * 128 + x])-MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x]))/4).id*4) == DrawlerConfig.getColorVariant(new Color(todrawimg.getRGB(x,y))))))) {
                                        int togo = 3;
                                        while (curIND - togo < 0) { //если Х меньше чем надо пройти
                                            togo = togo - curIND - 1; //то убираем Х и еще одну из-за 0
                                            curIND =127*127; //ставим его на максимум прошлого ряда
                                        }
                                        curIND -=togo; //если же мы можем вычесть, то просто вычитаем
                                        debug("now was an else, redrawing???");
                                    }
                                     */
                                    debug("updating render");
                                    updateRender();
                                    debug("going next");
                                    gonext();
                                }, delay * 4L, TimeUnit.MILLISECONDS);
                                service4.shutdown();
                            } else {
                                ScheduledExecutorService service4 = Executors.newScheduledThreadPool(1);
                                service4.schedule(() -> {
                                    backup.cancel(true);
                                    serv.shutdown();

                                    /*
                                    if ((!((MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x])/4)).id == DrawlerConfig.getColorID(new Color(todrawimg.getRGB(x,y)))) &&
                                            ((Byte.toUnsignedInt(mapState.colors[y * 128 + x])-MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x]))/4).id*4) == DrawlerConfig.getColorVariant(new Color(todrawimg.getRGB(x,y))))))) {
                                        int togo = 3;
                                        while (curIND - togo < 0) { //если Х меньше чем надо пройти
                                            togo = togo - curIND - 1; //то убираем Х и еще одну из-за 0
                                            curIND =127*127; //ставим его на максимум прошлого ряда
                                        }
                                        curIND -= togo; //если же мы можем вычесть, то просто вычитаем
                                        debug("now was an else, redrawing???");
                                    }
                                     */

                                    debug("updating render");
                                    updateRender();
                                    debug("going next");
                                    gonext();
                                }, delay, TimeUnit.MILLISECONDS);
                                service4.shutdown();
                            }
                        }
                    }, delay, TimeUnit.MILLISECONDS);
                    service.shutdown();
                } else {
                    //base color is correct
                    int CCvr = (Byte.toUnsignedInt(mapState.colors[y * 128 + x]) - MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x])) / 4).id * 4); //variant, that is on the map
                    if (CCvr == 1) {
                        if (!((Cvr == 2 && player.getInventory().containsAny(Set.of(Items.FEATHER))) || ((Cvr == 0 || Cvr == 3) && player.getInventory().containsAny(Set.of(Items.COAL))) || Cvr == 1)) {
                            if (back_check) {
                                new Timer().schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        if (!((Cvr == 2 && player.getInventory().containsAny(Set.of(Items.FEATHER))) || ((Cvr == 0 || Cvr == 3) && player.getInventory().containsAny(Set.of(Items.COAL))) || Cvr == 1)) {
                                            if (Cvr == 2)
                                                send_translatable("drawing.messages.missing", I18n.translate(Items.FEATHER.getTranslationKey()));
                                            else
                                                send_translatable("drawing.messages.missing", I18n.translate(Items.COAL.getTranslationKey()));
                                            playSound(soundPack + "_error");
                                            isdrawin = false;
                                            curIND -= 1;
                                            backup.cancel(true);
                                            serv.shutdown();
                                        } else {
                                            curIND = 0;
                                            gonext();
                                        }
                                    }
                                }, 1000);
                            } else {
                                if (Cvr == 2)
                                    send_translatable("drawing.messages.missing", I18n.translate(Items.FEATHER.getTranslationKey()));
                                else
                                    send_translatable("drawing.messages.missing", I18n.translate(Items.COAL.getTranslationKey()));
                                playSound(soundPack + "_error");
                                isdrawin = false;
                                curIND -= 1;
                                backup.cancel(true);
                                serv.shutdown();
                                return;
                            }

                            return;
                        }

                        debug(list.getKey() + " " + list.getValue() + " " + Cid + " " + Cvr + " " + key_point(start_time) + " - before delays");
                        debug(data + " - (pixel)data(of curIND or tocorrect(ind))");
                        debug("last 2 messages was from CCvr == 1, correct base, I'm just doing old stuff as we have a base here!");
                        //taking items coal/feather
                        if (Cvr == 2) { // feather 1
                            ScheduledExecutorService service2 = Executors.newScheduledThreadPool(1);
                            service2.schedule(() -> {
                                for (int i = 0;i<36;i++) {
                                    if (player.getInventory().getStack(i).getItem().equals(Items.FEATHER)) {
                                        swapItem(i);
                                        break;
                                    }
                                }
                                debug(key_point(start_time) + " - I just swapped to feather");
                            }, delay, TimeUnit.MILLISECONDS);
                            service2.shutdown();
                        } else if (Cvr == 3 || Cvr == 0) { //coal 2
                            ScheduledExecutorService service2 = Executors.newScheduledThreadPool(1);
                            service2.schedule(() -> {
                                for (int i = 0;i<36;i++) {
                                    if (player.getInventory().getStack(i).getItem().equals(Items.COAL)) {
                                        swapItem(i);
                                        break;
                                    }
                                }
                                debug(key_point(start_time) + " - I just swapped to coal");
                            }, delay, TimeUnit.MILLISECONDS);
                            service2.shutdown();
                        }

                        //shading or lighting
                        if (Cvr == 2 || Cvr == 0) { // feather/coal 1
                            ScheduledExecutorService service3 = Executors.newScheduledThreadPool(1);
                            service3.schedule(() -> {
                                MinecraftClient.getInstance().interactionManager.attackEntity(MinecraftClient.getInstance().player, ((EntityHitResult) MinecraftClient.getInstance().crosshairTarget).getEntity());
                                debug(key_point(start_time) + " - I just painted with feather(1) or coal(1)");
                            }, delay * 2L, TimeUnit.MILLISECONDS);
                            service3.shutdown();
                        } else if (Cvr == 3) { //coal 2
                            ScheduledExecutorService service3 = Executors.newScheduledThreadPool(1);
                            service3.schedule(() -> {
                                MinecraftClient.getInstance().interactionManager.attackEntity(MinecraftClient.getInstance().player, ((EntityHitResult) MinecraftClient.getInstance().crosshairTarget).getEntity());
                                debug(key_point(start_time) + " - I just painted with coal(1/2)");
                                ScheduledExecutorService service4 = Executors.newScheduledThreadPool(1);
                                service4.schedule(() -> {
                                    MinecraftClient.getInstance().interactionManager.attackEntity(MinecraftClient.getInstance().player, ((EntityHitResult) MinecraftClient.getInstance().crosshairTarget).getEntity());
                                    debug(key_point(start_time) + " - I just painted with coal(2/2)");
                                }, delay, TimeUnit.MILLISECONDS);
                                service4.shutdown();
                            }, delay * 2L, TimeUnit.MILLISECONDS);
                            service3.shutdown();
                        }

                        //going next
                        {
                            ScheduledExecutorService service4 = Executors.newScheduledThreadPool(1);
                            service4.schedule(() -> {
                                backup.cancel(true);
                                serv.shutdown();

                                /*
                                if ((!((MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x])/4)).id == DrawlerConfig.getColorID(new Color(todrawimg.getRGB(x,y)))) &&
                                        ((Byte.toUnsignedInt(mapState.colors[y * 128 + x])-MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x]))/4).id*4) == DrawlerConfig.getColorVariant(new Color(todrawimg.getRGB(x,y))))))) {
                                    int togo = 3;
                                    while (curIND - togo < 0) { //если Х меньше чем надо пройти
                                        togo = togo - curIND - 1; //то убираем Х и еще одну из-за 0
                                        curIND =127*127; //ставим его на максимум прошлого ряда
                                    }
                                    curIND -=togo; //если же мы можем вычесть, то просто вычитаем
                                    debug("now was an else, redrawing???");
                                }
                                 */
                                debug("updating render");
                                updateRender();
                                debug("going next");
                                gonext();
                            }, delay * ((Cvr == 2 || Cvr == 0) ? 3L : 4L), TimeUnit.MILLISECONDS);
                            service4.shutdown();
                        }

                    } else if (CCvr == 0) {
                        if (!(((Cvr == 2 || Cvr == 1) && player.getInventory().containsAny(Set.of(Items.FEATHER))) || (Cvr == 3 && player.getInventory().containsAny(Set.of(Items.COAL))) || Cvr == 0)) {
                            if (back_check) {
                                new Timer().schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        if (!(((Cvr == 2 || Cvr == 1) && player.getInventory().containsAny(Set.of(Items.FEATHER))) || (Cvr == 3 && player.getInventory().containsAny(Set.of(Items.COAL))) || Cvr == 0)) {
                                            if (Cvr == 2 || Cvr == 1)
                                                send_translatable("drawing.messages.missing", I18n.translate(Items.FEATHER.getTranslationKey()));
                                            else
                                                send_translatable("drawing.messages.missing", I18n.translate(Items.COAL.getTranslationKey()));
                                            playSound(soundPack + "_error");
                                            isdrawin = false;
                                            curIND -= 1;
                                            backup.cancel(true);
                                            serv.shutdown();
                                        } else {
                                            curIND = 0;
                                            gonext();
                                        }
                                    }
                                }, 1000);
                            } else {
                                if (Cvr == 2 || Cvr == 1)
                                    send_translatable("drawing.messages.missing", I18n.translate(Items.FEATHER.getTranslationKey()));
                                else
                                    send_translatable("drawing.messages.missing", I18n.translate(Items.COAL.getTranslationKey()));
                                playSound(soundPack + "_error");
                                isdrawin = false;
                                curIND -= 1;
                                backup.cancel(true);
                                serv.shutdown();
                                return;
                            }

                            return;
                        }

                        //taking items coal/feather
                        if (Cvr == 2 || Cvr == 1) { // feather 1 or 2
                            ScheduledExecutorService service2 = Executors.newScheduledThreadPool(1);
                            service2.schedule(() -> {
                                for (int i = 0;i<36;i++) {
                                    if (player.getInventory().getStack(i).getItem().equals(Items.FEATHER)) {
                                        swapItem(i);
                                        break;
                                    }
                                }
                                debug(key_point(start_time) + " - I just swapped to feather (CCvr == 0)");
                            }, delay, TimeUnit.MILLISECONDS);
                            service2.shutdown();
                        } else if (Cvr == 3) { //coal 3
                            ScheduledExecutorService service2 = Executors.newScheduledThreadPool(1);
                            service2.schedule(() -> {
                                for (int i = 0;i<36;i++) {
                                    if (player.getInventory().getStack(i).getItem().equals(Items.COAL)) {
                                        swapItem(i);
                                        break;
                                    }
                                }
                                debug(key_point(start_time) + " - I just swapped to coal (CCvr == 0)");
                            }, delay, TimeUnit.MILLISECONDS);
                            service2.shutdown();
                        }

                        //shading or lighting
                        if (Cvr == 3 || Cvr == 1) { // feather/coal 1
                            ScheduledExecutorService service3 = Executors.newScheduledThreadPool(1);
                            service3.schedule(() -> {
                                MinecraftClient.getInstance().interactionManager.attackEntity(MinecraftClient.getInstance().player, ((EntityHitResult) MinecraftClient.getInstance().crosshairTarget).getEntity());
                                debug(key_point(start_time) + " - I just painted with feather(1) or coal(1) CCvr == 0");
                            }, delay * 2L, TimeUnit.MILLISECONDS);
                            service3.shutdown();
                        } else if (Cvr == 2) { //feather 2
                            ScheduledExecutorService service3 = Executors.newScheduledThreadPool(1);
                            service3.schedule(() -> {
                                MinecraftClient.getInstance().interactionManager.attackEntity(MinecraftClient.getInstance().player, ((EntityHitResult) MinecraftClient.getInstance().crosshairTarget).getEntity());
                                debug(key_point(start_time) + " - I just painted with feather(1/2) CCvr == 0");
                                ScheduledExecutorService service4 = Executors.newScheduledThreadPool(1);
                                service4.schedule(() -> {
                                    MinecraftClient.getInstance().interactionManager.attackEntity(MinecraftClient.getInstance().player, ((EntityHitResult) MinecraftClient.getInstance().crosshairTarget).getEntity());
                                    debug(key_point(start_time) + " - I just painted with feather(2/2) CCvr == 0");
                                }, delay, TimeUnit.MILLISECONDS);
                                service4.shutdown();
                            }, delay * 2L, TimeUnit.MILLISECONDS);
                            service3.shutdown();
                        }

                        //going next
                        {
                            ScheduledExecutorService service4 = Executors.newScheduledThreadPool(1);
                            service4.schedule(() -> {
                                backup.cancel(true);
                                serv.shutdown();

                            /*
                                if ((!((MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x])/4)).id == DrawlerConfig.getColorID(new Color(todrawimg.getRGB(x,y)))) &&
                                        ((Byte.toUnsignedInt(mapState.colors[y * 128 + x])-MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x]))/4).id*4) == DrawlerConfig.getColorVariant(new Color(todrawimg.getRGB(x,y))))))) {
                                    int togo = 3;
                                    while (curIND - togo < 0) { //если Х меньше чем надо пройти
                                        togo = togo - curIND - 1; //то убираем Х и еще одну из-за 0
                                        curIND =127*127; //ставим его на максимум прошлого ряда
                                    }
                                    curIND -=togo; //если же мы можем вычесть, то просто вычитаем
                                    debug("now was an else, redrawing???");
                                }
                                 */
                                debug("updating render");
                                updateRender();
                                debug("going next");
                                gonext();
                            }, delay * ((Cvr == 3 || Cvr == 1) ? 3L : 4L), TimeUnit.MILLISECONDS);
                            service4.shutdown();
                        }

                    } else if (CCvr == 2) {

                        if (!(((Cvr == 0 || Cvr == 1 || Cvr == 3) && player.getInventory().containsAny(Set.of(Items.COAL))) || Cvr == 2)) {
                            if (back_check) {
                                new Timer().schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        if (!(((Cvr == 0 || Cvr == 1 || Cvr == 3) && player.getInventory().containsAny(Set.of(Items.COAL))) || Cvr == 2)) {
                                            send_translatable("drawing.messages.missing", I18n.translate(Items.COAL.getTranslationKey()));
                                            isdrawin = false;
                                            curIND -= 1;
                                            backup.cancel(true);
                                            serv.shutdown();
                                        } else {
                                            curIND = 0;
                                            gonext();
                                        }
                                    }
                                }, 1000);
                            } else {
                                send_translatable("drawing.messages.missing", I18n.translate(Items.COAL.getTranslationKey()));
                                playSound(soundPack + "_error");
                                isdrawin = false;
                                curIND -= 1;
                                backup.cancel(true);
                                serv.shutdown();
                                return;
                            }

                            return;
                        }

                        //taking item coal
                        ScheduledExecutorService service2 = Executors.newScheduledThreadPool(1);
                        service2.schedule(() -> {
                            for (int i = 0;i<36;i++) {
                                if (player.getInventory().getStack(i).getItem().equals(Items.COAL)) {
                                    swapItem(i);
                                    break;
                                }
                            }
                            debug(key_point(start_time) + " - I just swapped to coal (CCvr == 2)");
                        }, delay, TimeUnit.MILLISECONDS);
                        service2.shutdown();

                        //shading
                        if (Cvr == 1) { // feather/coal 1
                            ScheduledExecutorService service3 = Executors.newScheduledThreadPool(1);
                            service3.schedule(() -> {
                                MinecraftClient.getInstance().interactionManager.attackEntity(MinecraftClient.getInstance().player, ((EntityHitResult) MinecraftClient.getInstance().crosshairTarget).getEntity());
                                debug(key_point(start_time) + " - I just painted with coal(1) CCvr == 2");
                            }, delay * 2L, TimeUnit.MILLISECONDS);
                            service3.shutdown();
                        } else if (Cvr == 0) { //feather 2
                            ScheduledExecutorService service3 = Executors.newScheduledThreadPool(1);
                            service3.schedule(() -> {
                                MinecraftClient.getInstance().interactionManager.attackEntity(MinecraftClient.getInstance().player, ((EntityHitResult) MinecraftClient.getInstance().crosshairTarget).getEntity());
                                debug(key_point(start_time) + " - I just painted with feather(1/2) CCvr == 2");
                                ScheduledExecutorService service4 = Executors.newScheduledThreadPool(1);
                                service4.schedule(() -> {
                                    MinecraftClient.getInstance().interactionManager.attackEntity(MinecraftClient.getInstance().player, ((EntityHitResult) MinecraftClient.getInstance().crosshairTarget).getEntity());
                                    debug(key_point(start_time) + " - I just painted with feather(2/2) CCvr == 2");
                                }, delay, TimeUnit.MILLISECONDS);
                                service4.shutdown();
                            }, delay * 2L, TimeUnit.MILLISECONDS);
                            service3.shutdown();
                        } else if (Cvr == 3) {
                            ScheduledExecutorService service3 = Executors.newScheduledThreadPool(1);
                            service3.schedule(() -> {
                                MinecraftClient.getInstance().interactionManager.attackEntity(MinecraftClient.getInstance().player, ((EntityHitResult) MinecraftClient.getInstance().crosshairTarget).getEntity());
                                debug(key_point(start_time) + " - I just painted with feather(1/3) CCvr == 2");
                                ScheduledExecutorService service4 = Executors.newScheduledThreadPool(1);
                                service4.schedule(() -> {
                                    MinecraftClient.getInstance().interactionManager.attackEntity(MinecraftClient.getInstance().player, ((EntityHitResult) MinecraftClient.getInstance().crosshairTarget).getEntity());
                                    debug(key_point(start_time) + " - I just painted with feather(2/3) CCvr == 2");
                                }, delay, TimeUnit.MILLISECONDS);
                                service4.schedule(() -> {
                                    MinecraftClient.getInstance().interactionManager.attackEntity(MinecraftClient.getInstance().player, ((EntityHitResult) MinecraftClient.getInstance().crosshairTarget).getEntity());
                                    debug(key_point(start_time) + " - I just painted with feather(3/3) CCvr == 2");
                                }, delay *2L, TimeUnit.MILLISECONDS);
                                service4.shutdown();
                            }, delay * 2L, TimeUnit.MILLISECONDS); //completed in 4
                            service3.shutdown();
                        }

                        //going next
                        {
                            ScheduledExecutorService service4 = Executors.newScheduledThreadPool(1);
                            service4.schedule(() -> {
                                backup.cancel(true);
                                serv.shutdown();

                                /*
                                if ((!((MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x])/4)).id == DrawlerConfig.getColorID(new Color(todrawimg.getRGB(x,y)))) &&
                                        ((Byte.toUnsignedInt(mapState.colors[y * 128 + x])-MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x]))/4).id*4) == DrawlerConfig.getColorVariant(new Color(todrawimg.getRGB(x,y))))))) {
                                    int togo = 3;
                                    while (curIND - togo < 0) { //если Х меньше чем надо пройти
                                        togo = togo - curIND - 1; //то убираем Х и еще одну из-за 0
                                        curIND =127*127; //ставим его на максимум прошлого ряда
                                    }
                                    curIND -=togo; //если же мы можем вычесть, то просто вычитаем
                                    debug("now was an else, redrawing???");
                                }
                                 */
                                debug("updating render");
                                updateRender();
                                debug("going next");
                                gonext();
                            }, delay * (Cvr == 1 ? 3L : Cvr == 0 ? 4L : 5L), TimeUnit.MILLISECONDS);
                            service4.shutdown();
                        }

                    } else {
                        {
                            if (!(((Cvr == 0 || Cvr == 1 || Cvr == 2) && player.getInventory().containsAny(Set.of(Items.FEATHER))) || Cvr == 3)) {
                                send_translatable("drawing.messages.missing", I18n.translate(Items.FEATHER.getTranslationKey()));
                                isdrawin = false;
                                curIND -= 1;
                                backup.cancel(true);
                                serv.shutdown();
                                return;
                            }

                            //taking item feather
                            ScheduledExecutorService service2 = Executors.newScheduledThreadPool(1);
                            service2.schedule(() -> {
                                for (int i = 0;i<36;i++) {
                                    if (player.getInventory().getStack(i).getItem().equals(Items.FEATHER)) {
                                        swapItem(i);
                                        break;
                                    }
                                }
                                debug(key_point(start_time) + " - I just swapped to feather (CCvr == 3)");
                            }, delay, TimeUnit.MILLISECONDS);
                            service2.shutdown();

                            //lighting
                            if (Cvr == 0) { // feather 1
                                ScheduledExecutorService service3 = Executors.newScheduledThreadPool(1);
                                service3.schedule(() -> {
                                    MinecraftClient.getInstance().interactionManager.attackEntity(MinecraftClient.getInstance().player, ((EntityHitResult) MinecraftClient.getInstance().crosshairTarget).getEntity());
                                    debug(key_point(start_time) + " - I just painted with feather(1) CCvr == 3");
                                }, delay * 2L, TimeUnit.MILLISECONDS);
                                service3.shutdown();
                            } else if (Cvr == 1) { //feather 2
                                ScheduledExecutorService service3 = Executors.newScheduledThreadPool(1);
                                service3.schedule(() -> {
                                    MinecraftClient.getInstance().interactionManager.attackEntity(MinecraftClient.getInstance().player, ((EntityHitResult) MinecraftClient.getInstance().crosshairTarget).getEntity());
                                    debug(key_point(start_time) + " - I just painted with feather(1/2) CCvr == 3");
                                    ScheduledExecutorService service4 = Executors.newScheduledThreadPool(1);
                                    service4.schedule(() -> {
                                        MinecraftClient.getInstance().interactionManager.attackEntity(MinecraftClient.getInstance().player, ((EntityHitResult) MinecraftClient.getInstance().crosshairTarget).getEntity());
                                        debug(key_point(start_time) + " - I just painted with feather(2/2) CCvr == 3");
                                    }, delay, TimeUnit.MILLISECONDS);
                                    service4.shutdown();
                                }, delay * 2L, TimeUnit.MILLISECONDS);
                                service3.shutdown();
                            } else if (Cvr == 2) {
                                ScheduledExecutorService service3 = Executors.newScheduledThreadPool(1);
                                service3.schedule(() -> {
                                    MinecraftClient.getInstance().interactionManager.attackEntity(MinecraftClient.getInstance().player, ((EntityHitResult) MinecraftClient.getInstance().crosshairTarget).getEntity());
                                    debug(key_point(start_time) + " - I just painted with feather(1/3) CCvr == 3");
                                    ScheduledExecutorService service4 = Executors.newScheduledThreadPool(1);
                                    service4.schedule(() -> {
                                        MinecraftClient.getInstance().interactionManager.attackEntity(MinecraftClient.getInstance().player, ((EntityHitResult) MinecraftClient.getInstance().crosshairTarget).getEntity());
                                        debug(key_point(start_time) + " - I just painted with feather(2/3) CCvr == 3");
                                    }, delay, TimeUnit.MILLISECONDS);
                                    service4.schedule(() -> {
                                        MinecraftClient.getInstance().interactionManager.attackEntity(MinecraftClient.getInstance().player, ((EntityHitResult) MinecraftClient.getInstance().crosshairTarget).getEntity());
                                        debug(key_point(start_time) + " - I just painted with feather(3/3) CCvr == 3");
                                    }, delay *2L, TimeUnit.MILLISECONDS);
                                    service4.shutdown();
                                }, delay * 2L, TimeUnit.MILLISECONDS); //completed in 4
                                service3.shutdown();
                            }

                            //going next
                            {
                                ScheduledExecutorService service4 = Executors.newScheduledThreadPool(1);
                                service4.schedule(() -> {
                                    backup.cancel(true);
                                    serv.shutdown();

                                /*
                                if ((!((MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x])/4)).id == DrawlerConfig.getColorID(new Color(todrawimg.getRGB(x,y)))) &&
                                        ((Byte.toUnsignedInt(mapState.colors[y * 128 + x])-MapColor.get((Byte.toUnsignedInt(mapState.colors[y * 128 + x]))/4).id*4) == DrawlerConfig.getColorVariant(new Color(todrawimg.getRGB(x,y))))))) {
                                    int togo = 3;
                                    while (curIND - togo < 0) { //если Х меньше чем надо пройти
                                        togo = togo - curIND - 1; //то убираем Х и еще одну из-за 0
                                        curIND =127*127; //ставим его на максимум прошлого ряда
                                    }
                                    curIND -=togo; //если же мы можем вычесть, то просто вычитаем
                                    debug("now was an else, redrawing???");
                                }
                                 */
                                    debug("updating render");
                                    updateRender();
                                    debug("going next");
                                    gonext();
                                }, delay * (Cvr == 0 ? 3L : Cvr == 1 ? 4L : 5L), TimeUnit.MILLISECONDS);
                                service4.shutdown();
                            }

                        }
                    }
                }
                break;
            }
        }
    }


    public static void playSound(String soundName) {
        if (needtosound) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                SoundEvent soundEvent = CustomSounds.get_by_name(soundName);
                if (soundEvent != null) {
                    client.player.playSound(soundEvent);
                } else {
                    debug("sound " + soundName + " was not found");
                }
            }
        }
    }

    public static String FormatPixelData(ArrayList<ArrayList<Integer>> pixeldata) {
        if (pixeldata.size() != 128*128) {
            return ""; // Incorrect format -> error
        }

        StringBuilder bitStream = new StringBuilder();
        for (ArrayList<Integer> pixel : pixeldata) {
            if (pixel.size() != 4) return ""; // error in one of the pixels, return all as error
            String binaryString = getBinaryString(pixel);
            bitStream.append(binaryString);
        }
        // bitStream expected length 360448 bits or 45056 bytes

        return Base64.getEncoder().encodeToString(bitStringToByteArray(bitStream.toString())); // expected length ~60k
    }

    private static String getBinaryString(ArrayList<Integer> pixel) {
        int x = pixel.get(0) & 0b1111111; // 7 bits (0-127)
        int y = pixel.get(1) & 0b1111111; // 7 bits (0-127)
        int colorId = pixel.get(2) & 0b111111; // 6 bits (0-63 (62 colors))
        int variant = pixel.get(3) & 0b11; // 2 bits (0-3)
        // xxxxxxx yyyyyyy ididid vv
        // 7+6+2       6+2   2    0
        int packedValue = (x << 15) | (y << 8) | (colorId << 2) | variant;
        return String.format("%22s", Integer.toBinaryString(packedValue)).replace(' ', '0');
    }

    private static byte[] bitStringToByteArray(String bitString) {
        int byteCount = (bitString.length() + 7) / 8;
        byte[] byteArray = new byte[byteCount];

        for (int i = 0; i < bitString.length(); i += 8) {
            int end = Math.min(i + 8, bitString.length());
            String byteChunk = bitString.substring(i, end);
            byteArray[i / 8] = (byte) Integer.parseInt(byteChunk, 2);
        }

        return byteArray;
    }

    //tech functions (they all probably won't break)

    /**
     * Color, witch we should highlight slots with
     * @return hightlightColor field
     */
    public static int getHighlightColor() {return highlightColor;}

    /**
     * weather we should highlight slots or not
     * @return needtohightlight field
     */
    public static boolean isHightlighting(){return needtohighlight;}

    /**
     * RenderingItems field as an ArrayList
     * @return an ArrayList of Item 's that we should highlight
     */
    public static ArrayList<Item> getItemsList(){
        if (RenderingItems == null) return null;
        return RenderingItems;
    }

    /**
     * returns how much time (in MS) passed since time
     * @param time how much time passed since this value
     * @return how much time has passed
     */
    private static long key_point(long time){return (System.currentTimeMillis()-time);}

    /**
     * resizes image to a given width and height
     * @param originalImage original image, that you want to resize
     * @param width width of result image
     * @param height height of result image
     * @return resized image
     */
    private static BufferedImage resizeImage(BufferedImage originalImage, int width, int height) {
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, width, height, null);
        g.dispose();
        return resizedImage;
    }

    /**
     * finds closes available color in a subset of minecraft colors
     * @param pixelColor color, to witch return color be the closest to
     * @param colors a subset of minecraft colors
     * @return a Color that is closes to a given pixelColor from colors
     */
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
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§7[§6Drawler§7]§r ").append(Text.literal(I18n.translate(message, args))));
    }

    /**
     * sends message to player's chat
     * @param message translatable key you want to send to player's chat(with prefix). supports formatting with char '&'
     * @param args arguments for the translation
     */
    public static void delay_translatable(String message, int delay, Object... args) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§7[§6Drawler§7]§r ").append(Text.literal(I18n.translate(message, args))));
            }
        }, delay);
    }

    /**
     * prints message to console
     * @param message message that you want to print
     */
    public static void debug(String message) {
        if (isDebug) Drawler.LOGGER.info(message);
    }

}


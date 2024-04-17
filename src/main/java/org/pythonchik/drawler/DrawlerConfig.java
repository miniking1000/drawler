package org.pythonchik.drawler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import java.awt.*;
import java.io.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class DrawlerConfig {
    private static final String WEST_RESOURCE_PATH = "/assets/drawler/west.ser";
    private static final String EAST_RESOURCE_PATH = "/assets/drawler/east.ser";
    private static final String NORTH_RESOURCE_PATH = "/assets/drawler/north.ser";
    private static final String SOUTH_RESOURCE_PATH = "/assets/drawler/south.ser";
    private static final String MAPPINGS_RESOURCE_PATH = "/assets/drawler/mappings.json";

    public static HashMap<ArrayList<Integer>, ArrayList<Float>> west = new HashMap<>();
    public static HashMap<ArrayList<Integer>, ArrayList<Float>> east = new HashMap<>();
    public static HashMap<ArrayList<Integer>, ArrayList<Float>> north = new HashMap<>();
    public static HashMap<ArrayList<Integer>, ArrayList<Float>> south = new HashMap<>();
    public static HashMap<Color,ArrayList<Integer>> Clors = new HashMap<>();
    public static HashMap<Integer, Item> items = new HashMap<>();
    public static ArrayList<Color> colors = new ArrayList<>();


    public static int getColorID(Color color) {
        for (Map.Entry<Color, ArrayList<Integer>> entry : Clors.entrySet()) {
            if (entry.getKey().equals(color)) {
                return entry.getValue().get(0);
            }
        }
        return 0;
    }

    public static int getColorVariant(Color color) {
        for (Map.Entry<Color, ArrayList<Integer>> entry : Clors.entrySet()) {
            if (entry.getKey().equals(color)) {
                return entry.getValue().get(1);
            }
        }
        return 0;
    }
    private static void loadSerializedData(String resourcePath, HashMap<ArrayList<Integer>, ArrayList<Float>> dataMap) {
        try (InputStream inputStream = DrawlerConfig.class.getResourceAsStream(resourcePath);
             ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
             dataMap = (HashMap<ArrayList<Integer>, ArrayList<Float>>) objectInputStream.readObject();
        } catch (Exception ignored) {}
    }
    public static void loadConfig() {
        File west_path = new File(WEST_RESOURCE_PATH);
        if (west_path.exists()) {
            try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(west_path))) {
                west = (HashMap<ArrayList<Integer>, ArrayList<Float>>) inputStream.readObject();
            } catch (Exception ignored) {}
        } else {
            try (InputStream inputStream = DrawlerConfig.class.getResourceAsStream(WEST_RESOURCE_PATH);
                 ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
                west = (HashMap<ArrayList<Integer>, ArrayList<Float>>) objectInputStream.readObject();
            } catch (Exception ignored) {}
        }

        File east_path = new File(EAST_RESOURCE_PATH);
        if (east_path.exists()) {
            try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(east_path))) {
                east = (HashMap<ArrayList<Integer>, ArrayList<Float>>) inputStream.readObject();
            } catch (Exception ignored) {}
        } else {
            try (InputStream inputStream = DrawlerConfig.class.getResourceAsStream(EAST_RESOURCE_PATH);
                 ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
                east = (HashMap<ArrayList<Integer>, ArrayList<Float>>) objectInputStream.readObject();
            } catch (Exception ignored) {}
        }

        File north_path = new File(NORTH_RESOURCE_PATH);
        if (north_path.exists()) {
            try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(north_path))) {
                north = (HashMap<ArrayList<Integer>, ArrayList<Float>>) inputStream.readObject();
            } catch (Exception ignored) {}
        } else {
            try (InputStream inputStream = DrawlerConfig.class.getResourceAsStream(NORTH_RESOURCE_PATH);
                 ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
                north = (HashMap<ArrayList<Integer>, ArrayList<Float>>) objectInputStream.readObject();
            } catch (Exception ignored) {}
        }

        File south_path = new File(SOUTH_RESOURCE_PATH);
        if (south_path.exists()) {
            try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(south_path))) {
                south = (HashMap<ArrayList<Integer>, ArrayList<Float>>) inputStream.readObject();
            } catch (Exception ignored) {}
        } else {
            try (InputStream inputStream = DrawlerConfig.class.getResourceAsStream(SOUTH_RESOURCE_PATH);
                 ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
                south = (HashMap<ArrayList<Integer>, ArrayList<Float>>) objectInputStream.readObject();
            } catch (Exception ignored) {}
        }

        try (InputStream inputStream = DrawlerConfig.class.getResourceAsStream(MAPPINGS_RESOURCE_PATH);
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream)) {
            JsonObject obj = JsonParser.parseReader(inputStreamReader).getAsJsonObject();
            JsonObject version = obj.get("1.20").getAsJsonObject();
            for (String key : version.keySet()) {
                JsonObject colorObj = version.get(key).getAsJsonObject();
                JsonArray colorArr = colorObj.get("colors").getAsJsonArray();
                int id = Integer.parseInt(key);
                for (int variant = 0; variant < colorArr.size(); variant++) {
                    int rgb = colorArr.get(variant).getAsInt();
                    Color color = new Color(rgb, false);
                    ArrayList<Integer> temp = new ArrayList<>();
                    temp.add(id);
                    temp.add(variant);
                    Clors.put(color, temp);
                    if (id != 0) {
                        colors.add(color);
                    }
                }
            }
        } catch (Exception ignored) {}


        items.put(0,Items.ENDER_EYE);
        items.put(1,Items.GRASS);
        items.put(2,Items.PUMPKIN_SEEDS);
        items.put(3,Items.COBWEB);
        items.put(4,Items.RED_DYE);
        items.put(5,Items.ICE);
        items.put(6,Items.LIGHT_GRAY_DYE);
        items.put(7,Items.OAK_LEAVES);
        items.put(8,Items.SNOW);
        items.put(9,Items.GRAY_DYE);
        items.put(10,Items.MELON_SEEDS);
        items.put(11,Items.GHAST_TEAR);
        items.put(12,Items.LAPIS_BLOCK);
        items.put(13,Items.DARK_OAK_LOG);
        items.put(14,Items.BONE_MEAL);
        items.put(15,Items.ORANGE_DYE);
        items.put(16,Items.MAGENTA_DYE);
        items.put(17,Items.LIGHT_BLUE_DYE);
        items.put(18,Items.YELLOW_DYE);
        items.put(19,Items.LIME_DYE);
        items.put(20,Items.PINK_DYE);
        items.put(21,Items.FLINT);
        items.put(22,Items.GUNPOWDER);
        items.put(23,Items.CYAN_DYE);
        items.put(24,Items.PURPLE_DYE);
        items.put(25,Items.LAPIS_LAZULI);
        items.put(26,Items.COCOA_BEANS);
        items.put(27,Items.GREEN_DYE);
        items.put(28,Items.BRICK);
        items.put(29,Items.INK_SAC);
        items.put(30,Items.GOLD_NUGGET);
        items.put(31,Items.PRISMARINE_CRYSTALS);
        items.put(32,Items.LAPIS_ORE);
        items.put(33,Items.EMERALD);
        items.put(34,Items.BIRCH_WOOD);
        items.put(35,Items.NETHER_WART);
        items.put(36,Items.EGG);
        items.put(37,Items.MAGMA_CREAM);
        items.put(38,Items.BEETROOT);
        items.put(39,Items.MYCELIUM);
        items.put(40,Items.GLOWSTONE_DUST);
        items.put(41,Items.SLIME_BALL);
        items.put(42,Items.SPIDER_EYE);
        items.put(43,Items.SOUL_SAND);
        items.put(44,Items.BROWN_MUSHROOM);
        items.put(45,Items.IRON_NUGGET);
        items.put(46,Items.CHORUS_FRUIT);
        items.put(47,Items.PURPUR_BLOCK);
        items.put(48,Items.PODZOL);
        items.put(49,Items.POISONOUS_POTATO);
        items.put(50,Items.APPLE);
        items.put(51,Items.CHARCOAL);
        items.put(52,Items.CRIMSON_NYLIUM);
        items.put(53,Items.CRIMSON_STEM);
        items.put(54,Items.CRIMSON_HYPHAE);
        items.put(55,Items.WARPED_NYLIUM);
        items.put(56,Items.WARPED_STEM);
        items.put(57,Items.WARPED_HYPHAE);
        items.put(58,Items.WARPED_WART_BLOCK);
        items.put(59,Items.COBBLED_DEEPSLATE);
        items.put(60,Items.RAW_IRON);
        items.put(61,Items.GLOW_LICHEN);
        items.put(62,Items.COAL);
        items.put(63,Items.FEATHER);
    }
}


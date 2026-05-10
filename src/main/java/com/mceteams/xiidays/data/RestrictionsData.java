package com.mceteams.xiidays.data;

import com.google.gson.JsonObject;

public class RestrictionsData {
    private static final String DOMAIN = "restrictions";

    private static JsonObject get() {
        return DataManager.load(DOMAIN);
    }

    private static JsonObject getItems() {
        JsonObject obj = get();
        if (!obj.has("items")) {
            obj.add("items", new JsonObject());
            DataManager.save(DOMAIN);
        }
        return obj.getAsJsonObject("items");
    }

    private static JsonObject getBlocks() {
        JsonObject obj = get();
        if (!obj.has("blocks")) {
            obj.add("blocks", new JsonObject());
            DataManager.save(DOMAIN);
        }
        return obj.getAsJsonObject("blocks");
    }

    public static boolean isItemAllowed(String itemId) {
        JsonObject items = getItems();
        return !items.has(itemId) || items.get(itemId).getAsBoolean();
    }

    public static void setItemAllowed(String itemId, boolean allowed) {
        getItems().addProperty(itemId, allowed);
        DataManager.save(DOMAIN);
    }

    public static boolean isBlockAllowed(String blockId) {
        JsonObject blocks = getBlocks();
        return !blocks.has(blockId) || blocks.get(blockId).getAsBoolean();
    }

    public static void setBlockAllowed(String blockId, boolean allowed) {
        getBlocks().addProperty(blockId, allowed);
        DataManager.save(DOMAIN);
    }
}

package com.mceteams.xiidays.data;

import com.google.gson.JsonObject;

public class DayCycleData {
    private static final String DOMAIN = "day_cycle";
    private static JsonObject data;

    private static JsonObject get() {
        if (data == null) data = DataManager.load(DOMAIN);
        return data;
    }

    public static boolean isInProgress() {
        return getJsonBool("in_progress", false);
    }

    public static void setInProgress(boolean val) {
        get().addProperty("in_progress", val);
        DataManager.save(DOMAIN);
    }

    public static int getCurrentDay() {
        return getJsonInt("current_day", 0);
    }

    public static void setCurrentDay(int day) {
        get().addProperty("current_day", day);
        DataManager.save(DOMAIN);
    }

    public static void incrementDay() {
        setCurrentDay(getCurrentDay() + 1);
    }

    private static boolean getJsonBool(String key, boolean def) {
        JsonObject obj = get();
        return obj.has(key) ? obj.get(key).getAsBoolean() : def;
    }

    private static int getJsonInt(String key, int def) {
        JsonObject obj = get();
        return obj.has(key) ? obj.get(key).getAsInt() : def;
    }
}

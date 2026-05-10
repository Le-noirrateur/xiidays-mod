package com.mceteams.xiidays.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class TeamStatsData {
    private static final String DOMAIN = "team_stats";

    private static JsonObject get() {
        return DataManager.load(DOMAIN);
    }

    private static JsonObject getTeam(int teamId) {
        JsonObject obj = get();
        String key = String.valueOf(teamId);
        if (!obj.has(key)) {
            obj.add(key, new JsonObject());
            DataManager.save(DOMAIN);
        }
        return obj.getAsJsonObject(key);
    }

    public static boolean hasTeam(int teamId) {
        return get().has(String.valueOf(teamId));
    }

    public static void initTeam(int teamId) {
        JsonObject team = getTeam(teamId);
        if (!team.has("points")) {
            team.addProperty("points", 0);
            team.add("history", new JsonArray());
            team.addProperty("kills", 0);
            team.addProperty("kill_streak", 0);
            team.addProperty("max_kill_streak", 0);
            team.addProperty("pointgain", 0);
            team.addProperty("pointloss", 0);
            team.addProperty("blocks_mined", 0);
            team.addProperty("damage_dealt", 0);
            team.addProperty("damage_received", 0);
            DataManager.save(DOMAIN);
        }
    }

    public static void removeTeam(int teamId) {
        JsonObject obj = get();
        obj.remove(String.valueOf(teamId));
        DataManager.save(DOMAIN);
    }

    public static int getPoints(int teamId) {
        JsonObject team = getTeam(teamId);
        return team.has("points") ? team.get("points").getAsInt() : 0;
    }

    public static void setPoints(int teamId, int points) {
        getTeam(teamId).addProperty("points", points);
        DataManager.save(DOMAIN);
    }

    public static int getKills(int teamId) {
        JsonObject team = getTeam(teamId);
        return team.has("kills") ? team.get("kills").getAsInt() : 0;
    }

    public static void setKills(int teamId, int kills) {
        getTeam(teamId).addProperty("kills", kills);
        DataManager.save(DOMAIN);
    }

    public static void incrementKills(int teamId) {
        setKills(teamId, getKills(teamId) + 1);
    }

    public static int getKillStreak(int teamId) {
        JsonObject team = getTeam(teamId);
        return team.has("kill_streak") ? team.get("kill_streak").getAsInt() : 0;
    }

    public static void setKillStreak(int teamId, int streak) {
        getTeam(teamId).addProperty("kill_streak", streak);
        DataManager.save(DOMAIN);
    }

    public static int getMaxKillStreak(int teamId) {
        JsonObject team = getTeam(teamId);
        return team.has("max_kill_streak") ? team.get("max_kill_streak").getAsInt() : 0;
    }

    public static void setMaxKillStreak(int teamId, int streak) {
        getTeam(teamId).addProperty("max_kill_streak", streak);
        DataManager.save(DOMAIN);
    }

    public static void updateMaxKillStreak(int teamId) {
        int current = getKillStreak(teamId);
        int max = getMaxKillStreak(teamId);
        if (current > max) setMaxKillStreak(teamId, current);
    }

    public static int getPointGain(int teamId) {
        JsonObject team = getTeam(teamId);
        return team.has("pointgain") ? team.get("pointgain").getAsInt() : 0;
    }

    public static void addPointGain(int teamId, int amount) {
        int current = getPointGain(teamId);
        getTeam(teamId).addProperty("pointgain", current + amount);
        DataManager.save(DOMAIN);
    }

    public static int getPointLoss(int teamId) {
        JsonObject team = getTeam(teamId);
        return team.has("pointloss") ? team.get("pointloss").getAsInt() : 0;
    }

    public static void addPointLoss(int teamId, int amount) {
        int current = getPointLoss(teamId);
        getTeam(teamId).addProperty("pointloss", current + amount);
        DataManager.save(DOMAIN);
    }

    public static int getBlocksMined(int teamId) {
        JsonObject team = getTeam(teamId);
        return team.has("blocks_mined") ? team.get("blocks_mined").getAsInt() : 0;
    }

    public static void incrementBlocksMined(int teamId) {
        int current = getBlocksMined(teamId);
        getTeam(teamId).addProperty("blocks_mined", current + 1);
        DataManager.save(DOMAIN);
    }

    public static int getDamageDealt(int teamId) {
        JsonObject team = getTeam(teamId);
        return team.has("damage_dealt") ? team.get("damage_dealt").getAsInt() : 0;
    }

    public static void addDamageDealt(int teamId, int amount) {
        int current = getDamageDealt(teamId);
        getTeam(teamId).addProperty("damage_dealt", current + amount);
        DataManager.save(DOMAIN);
    }

    public static int getDamageReceived(int teamId) {
        JsonObject team = getTeam(teamId);
        return team.has("damage_received") ? team.get("damage_received").getAsInt() : 0;
    }

    public static void addDamageReceived(int teamId, int amount) {
        int current = getDamageReceived(teamId);
        getTeam(teamId).addProperty("damage_received", current + amount);
        DataManager.save(DOMAIN);
    }

    public static String[] getPointHistory(int teamId) {
        JsonObject team = getTeam(teamId);
        if (!team.has("history")) return new String[0];
        JsonArray arr = team.getAsJsonArray("history");
        String[] result = new String[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            result[i] = arr.get(i).getAsString();
        }
        return result;
    }

    public static void pushPointHistory(int teamId, String entry) {
        JsonObject team = getTeam(teamId);
        JsonArray arr;
        if (!team.has("history")) {
            arr = new JsonArray();
            team.add("history", arr);
        } else {
            arr = team.getAsJsonArray("history");
        }
        arr.add(entry);
        if (arr.size() > 3) {
            JsonArray trimmed = new JsonArray();
            for (int i = arr.size() - 3; i < arr.size(); i++) {
                trimmed.add(arr.get(i));
            }
            team.add("history", trimmed);
        }
        DataManager.save(DOMAIN);
    }

    public static int getTeamCount() {
        JsonObject obj = get();
        int count = 0;
        for (String key : obj.keySet()) {
            if (obj.get(key).isJsonObject()) count++;
        }
        return count;
    }
}

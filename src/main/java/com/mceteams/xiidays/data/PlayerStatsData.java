package com.mceteams.xiidays.data;

import com.google.gson.JsonObject;

public class PlayerStatsData {
    private static final String DOMAIN = "player_stats";

    private static JsonObject get() {
        return DataManager.load(DOMAIN);
    }

    private static JsonObject getPlayer(String uuid) {
        JsonObject obj = get();
        if (!obj.has(uuid)) {
            obj.add(uuid, new JsonObject());
            DataManager.save(DOMAIN);
        }
        return obj.getAsJsonObject(uuid);
    }

    public static int getTeamPoints(String uuid) {
        JsonObject player = getPlayer(uuid);
        return player.has("team_points") ? player.get("team_points").getAsInt() : 0;
    }

    public static void setTeamPoints(String uuid, int points) {
        getPlayer(uuid).addProperty("team_points", points);
        DataManager.save(DOMAIN);
    }

    public static void addTeamPoints(String uuid, int amount) {
        setTeamPoints(uuid, getTeamPoints(uuid) + amount);
    }

    public static int getKills(String uuid) {
        JsonObject player = getPlayer(uuid);
        return player.has("kills") ? player.get("kills").getAsInt() : 0;
    }

    public static void setKills(String uuid, int kills) {
        getPlayer(uuid).addProperty("kills", kills);
        DataManager.save(DOMAIN);
    }

    public static void incrementKills(String uuid) {
        setKills(uuid, getKills(uuid) + 1);
    }

    public static int getDeaths(String uuid) {
        JsonObject player = getPlayer(uuid);
        return player.has("deaths") ? player.get("deaths").getAsInt() : 0;
    }

    public static void setDeaths(String uuid, int deaths) {
        getPlayer(uuid).addProperty("deaths", deaths);
        DataManager.save(DOMAIN);
    }

    public static void incrementDeaths(String uuid) {
        setDeaths(uuid, getDeaths(uuid) + 1);
    }

    public static int getBlocksMined(String uuid) {
        JsonObject player = getPlayer(uuid);
        return player.has("blocks_mined") ? player.get("blocks_mined").getAsInt() : 0;
    }

    public static void setBlocksMined(String uuid, int mined) {
        getPlayer(uuid).addProperty("blocks_mined", mined);
        DataManager.save(DOMAIN);
    }

    public static void incrementBlocksMined(String uuid) {
        setBlocksMined(uuid, getBlocksMined(uuid) + 1);
    }

    public static int getDamageDealt(String uuid) {
        JsonObject player = getPlayer(uuid);
        return player.has("damage_dealt") ? player.get("damage_dealt").getAsInt() : 0;
    }

    public static void addDamageDealt(String uuid, int amount) {
        int current = getDamageDealt(uuid);
        getPlayer(uuid).addProperty("damage_dealt", current + amount);
        DataManager.save(DOMAIN);
    }
}

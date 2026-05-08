package com.mceteams.xiidays.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mceteams.xiidays.XIIDays;

import java.util.UUID;

public class TeamData {
    private static final String DOMAIN = "teams";
    private static JsonObject data;

    private static JsonObject get() {
        if (data == null) data = DataManager.load(DOMAIN);
        return data;
    }

    private static JsonObject getEntries() {
        JsonObject obj = get();
        if (!obj.has("entries")) {
            obj.add("entries", new JsonObject());
            DataManager.save(DOMAIN);
        }
        return obj.getAsJsonObject("entries");
    }

    private static JsonObject getTeamData(int teamId) {
        JsonObject obj = get();
        String key = String.valueOf(teamId);
        if (!obj.has(key)) {
            obj.add(key, new JsonObject());
            DataManager.save(DOMAIN);
        }
        return obj.getAsJsonObject(key);
    }

    private static int getNextId() {
        JsonObject obj = get();
        if (!obj.has("next_id")) {
            obj.addProperty("next_id", 1);
            DataManager.save(DOMAIN);
        }
        return obj.get("next_id").getAsInt();
    }

    private static void setNextId(int id) {
        get().addProperty("next_id", id);
        DataManager.save(DOMAIN);
    }

    public static int createEntry(String teamName) {
        int teamId = getNextId();
        setNextId(teamId + 1);

        getEntries().addProperty(teamName, teamId);
        JsonObject team = getTeamData(teamId);
        team.addProperty("uuid", UUID.randomUUID().toString());
        team.add("members", new JsonArray());
        JsonObject config = new JsonObject();
        config.addProperty("eliminated", false);
        config.addProperty("finalpos", 0);
        config.addProperty("core_destroyed", false);
        team.add("config", config);
        DataManager.save(DOMAIN);

        TeamStatsData.initTeam(teamId);
        XIIDays.LOGGER.info("Created team {} (ID: {})", teamName, teamId);
        return teamId;
    }

    public static void deleteEntry(int teamId) {
        JsonObject entries = getEntries();
        String nameToRemove = null;
        for (String name : entries.keySet()) {
            if (entries.get(name).getAsInt() == teamId) {
                nameToRemove = name;
                break;
            }
        }
        if (nameToRemove != null) {
            entries.remove(nameToRemove);
        }
        get().remove(String.valueOf(teamId));
        TeamStatsData.removeTeam(teamId);
        DataManager.save(DOMAIN);
    }

    public static int getTeamId(String teamName) {
        JsonObject entries = getEntries();
        if (entries.has(teamName)) {
            return entries.get(teamName).getAsInt();
        }
        return 0;
    }

    public static String getTeamName(int teamId) {
        JsonObject entries = getEntries();
        for (String name : entries.keySet()) {
            if (entries.get(name).getAsInt() == teamId) {
                return name;
            }
        }
        return null;
    }

    public static String[] getAllTeamNames() {
        JsonObject entries = getEntries();
        return entries.keySet().toArray(new String[0]);
    }

    public static int getTeamCount() {
        return getEntries().size();
    }

    public static boolean teamExists(String teamName) {
        return getEntries().has(teamName);
    }

    public static JsonArray getMembers(int teamId) {
        JsonObject team = getTeamData(teamId);
        if (!team.has("members")) {
            team.add("members", new JsonArray());
            DataManager.save(DOMAIN);
        }
        return team.getAsJsonArray("members");
    }

    public static void addMember(int teamId, String playerUUID) {
        JsonArray members = getMembers(teamId);
        members.add(playerUUID);
        DataManager.save(DOMAIN);
    }

    public static boolean removeMember(int teamId, String playerUUID) {
        JsonArray members = getMembers(teamId);
        JsonArray newMembers = new JsonArray();
        boolean removed = false;
        for (int i = 0; i < members.size(); i++) {
            String uuid = members.get(i).getAsString();
            if (uuid.equals(playerUUID)) {
                removed = true;
            } else {
                newMembers.add(uuid);
            }
        }
        if (removed) {
            getTeamData(teamId).add("members", newMembers);
            DataManager.save(DOMAIN);
        }
        return removed;
    }

    public static boolean isPlayerInTeam(String playerUUID, int teamId) {
        JsonArray members = getMembers(teamId);
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).getAsString().equals(playerUUID)) {
                return true;
            }
        }
        return false;
    }

    public static String getPlayerTeam(String playerUUID) {
        for (String teamName : getAllTeamNames()) {
            int teamId = getTeamId(teamName);
            if (teamId > 0 && isPlayerInTeam(playerUUID, teamId)) {
                return teamName;
            }
        }
        return null;
    }

    public static int getPlayerTeamId(String playerUUID) {
        String teamName = getPlayerTeam(playerUUID);
        return teamName != null ? getTeamId(teamName) : 0;
    }

    private static JsonObject getConfig(int teamId) {
        JsonObject team = getTeamData(teamId);
        if (!team.has("config")) {
            team.add("config", new JsonObject());
            DataManager.save(DOMAIN);
        }
        return team.getAsJsonObject("config");
    }

    public static String getTeamUUID(int teamId) {
        JsonObject config = getConfig(teamId);
        if (!config.has("uuid")) {
            config.addProperty("uuid", UUID.randomUUID().toString());
            DataManager.save(DOMAIN);
        }
        return config.get("uuid").getAsString();
    }

    public static boolean isEliminated(int teamId) {
        JsonObject config = getConfig(teamId);
        return config.has("eliminated") && config.get("eliminated").getAsBoolean();
    }

    public static void setEliminated(int teamId, boolean val) {
        getConfig(teamId).addProperty("eliminated", val);
        DataManager.save(DOMAIN);
    }

    public static int getFinalPosition(int teamId) {
        JsonObject config = getConfig(teamId);
        return config.has("finalpos") ? config.get("finalpos").getAsInt() : 0;
    }

    public static void setFinalPosition(int teamId, int pos) {
        getConfig(teamId).addProperty("finalpos", pos);
        DataManager.save(DOMAIN);
    }

    public static boolean isCoreDestroyed(int teamId) {
        JsonObject config = getConfig(teamId);
        return config.has("core_destroyed") && config.get("core_destroyed").getAsBoolean();
    }

    public static void setCoreDestroyed(int teamId, boolean val) {
        getConfig(teamId).addProperty("core_destroyed", val);
        DataManager.save(DOMAIN);
    }

    public static String getSpawn(int teamId) {
        JsonObject config = getConfig(teamId);
        return config.has("spawn") ? config.get("spawn").getAsString() : null;
    }

    public static void setSpawn(int teamId, String coords) {
        getConfig(teamId).addProperty("spawn", coords);
        DataManager.save(DOMAIN);
    }

    public static String getCore(int teamId) {
        JsonObject config = getConfig(teamId);
        return config.has("core") ? config.get("core").getAsString() : null;
    }

    public static void setCore(int teamId, String coords) {
        getConfig(teamId).addProperty("core", coords);
        DataManager.save(DOMAIN);
    }

    public static int getMazeProgress(int teamId) {
        JsonObject config = getConfig(teamId);
        return config.has("maze_progress") ? config.get("maze_progress").getAsInt() : 0;
    }

    public static void setMazeProgress(int teamId, int progress) {
        getConfig(teamId).addProperty("maze_progress", progress);
        DataManager.save(DOMAIN);
    }

    public static boolean isMazeSolved(int teamId) {
        JsonObject config = getConfig(teamId);
        return config.has("maze_solved") && config.get("maze_solved").getAsBoolean();
    }

    public static void setMazeSolved(int teamId, boolean val) {
        getConfig(teamId).addProperty("maze_solved", val);
        DataManager.save(DOMAIN);
    }

    public static String getFreeCamZoneMin(int teamId) {
        JsonObject config = getConfig(teamId);
        return config.has("fcz_min") ? config.get("fcz_min").getAsString() : null;
    }

    public static void setFreeCamZoneMin(int teamId, String coords) {
        JsonObject config = getConfig(teamId);
        if (coords != null) {
            config.addProperty("fcz_min", coords);
        } else {
            config.remove("fcz_min");
        }
        DataManager.save(DOMAIN);
    }

    public static String getFreeCamZoneMax(int teamId) {
        JsonObject config = getConfig(teamId);
        return config.has("fcz_max") ? config.get("fcz_max").getAsString() : null;
    }

    public static void setFreeCamZoneMax(int teamId, String coords) {
        JsonObject config = getConfig(teamId);
        if (coords != null) {
            config.addProperty("fcz_max", coords);
        } else {
            config.remove("fcz_max");
        }
        DataManager.save(DOMAIN);
    }
}

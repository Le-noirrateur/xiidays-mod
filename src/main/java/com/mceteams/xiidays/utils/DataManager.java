package com.mceteams.xiidays.utils;

import com.google.gson.*;
import com.mceteams.xiidays.XIIDaysManagerMod;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DATA_FILE = "xiidays_data.json";
    private static JsonObject mainData = null;

    /**
     * Obtient le chemin du fichier de données principal
     */
    private static Path getDataFilePath() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            XIIDaysManagerMod.LOGGER.error("Server is null, using fallback path");
            return Paths.get(DATA_FILE);
        }

        Path worldPath = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
        Path dataPath = worldPath.resolve("xiidaysdata");

        // Créer le dossier s'il n'existe pas
        try {
            Files.createDirectories(dataPath);
        } catch (IOException e) {
            XIIDaysManagerMod.LOGGER.error("Failed to create data directory: {}", e.getMessage());
        }

        return dataPath.resolve(DATA_FILE);
    }

    /**
     * Charge les données depuis le fichier
     */
    private static void loadData() {
        if (mainData != null) {
            return; // Déjà chargé
        }

        try {
            Path filePath = getDataFilePath();

            if (!Files.exists(filePath)) {
                // Créer un fichier vide
                mainData = new JsonObject();
                saveData();
                XIIDaysManagerMod.LOGGER.info("Created new data file: {}", DATA_FILE);
                return;
            }

            try (FileReader reader = new FileReader(filePath.toFile())) {
                mainData = JsonParser.parseReader(reader).getAsJsonObject();
                XIIDaysManagerMod.LOGGER.debug("Loaded data file: {}", DATA_FILE);
            }

        } catch (IOException e) {
            XIIDaysManagerMod.LOGGER.error("Failed to load data file: {}", e.getMessage());
            mainData = new JsonObject();
        }
    }

    /**
     * Sauvegarde les données dans le fichier
     */
    private static boolean saveData() {
        if (mainData == null) {
            return false;
        }

        try {
            Path filePath = getDataFilePath();

            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                GSON.toJson(mainData, writer);
                XIIDaysManagerMod.LOGGER.debug("Saved data file: {}", DATA_FILE);
                return true;
            }

        } catch (IOException e) {
            XIIDaysManagerMod.LOGGER.error("Failed to save data file: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Ajoute une table de données (si elle n'existe pas déjà)
     *
     * @param dataTable Le nom de la table
     */
    public static void dataAdd(String dataTable) {
        loadData();

        if (mainData.has(dataTable)) {
            XIIDaysManagerMod.LOGGER.debug("DataTable '{}' already exists", dataTable);
            return;
        }

        mainData.add(dataTable, new JsonObject());
        boolean saved = saveData();

        if (saved) {
            XIIDaysManagerMod.LOGGER.info("Created DataTable: {}", dataTable);
        }

    }

    /**
     * Modifie ou crée une valeur dans une table
     *
     * @param dataTable Le nom de la table
     * @param dataName  Le nom de la donnée
     * @param value     La valeur à stocker
     */
    public static void dataModify(String dataTable, String dataName, String value) {
        loadData();

        // Créer la table si elle n'existe pas
        if (!mainData.has(dataTable)) {
            mainData.add(dataTable, new JsonObject());
            XIIDaysManagerMod.LOGGER.debug("Auto-created DataTable: {}", dataTable);
        }

        JsonObject table = mainData.getAsJsonObject(dataTable);
        boolean isNew = !table.has(dataName);

        table.addProperty(dataName, value);
        boolean saved = saveData();

        if (saved) {
            if (isNew) {
                XIIDaysManagerMod.LOGGER.info("Created {}.{} = {}", dataTable, dataName, value);
            } else {
                XIIDaysManagerMod.LOGGER.info("Modified {}.{} = {}", dataTable, dataName, value);
            }
        }

    }

    /**
     * Modifie ou crée une valeur numérique dans une table
     *
     * @param dataTable Le nom de la table
     * @param dataName  Le nom de la donnée
     * @param value     La valeur numérique à stocker
     */
    public static void dataModify(String dataTable, String dataName, int value) {
        loadData();

        // Créer la table si elle n'existe pas
        if (!mainData.has(dataTable)) {
            mainData.add(dataTable, new JsonObject());
            XIIDaysManagerMod.LOGGER.debug("Auto-created DataTable: {}", dataTable);
        }

        JsonObject table = mainData.getAsJsonObject(dataTable);
        boolean isNew = !table.has(dataName);

        table.addProperty(dataName, value);
        boolean saved = saveData();

        if (saved) {
            if (isNew) {
                XIIDaysManagerMod.LOGGER.info("Created {}.{} = {}", dataTable, dataName, value);
            } else {
                XIIDaysManagerMod.LOGGER.info("Modified {}.{} = {}", dataTable, dataName, value);
            }
        }

    }

    /**
     * Modifie ou crée une valeur booléenne dans une table
     *
     * @param dataTable Le nom de la table
     * @param dataName  Le nom de la donnée
     * @param value     La valeur booléenne à stocker
     */
    public static void dataModify(String dataTable, String dataName, boolean value) {
        loadData();

        // Créer la table si elle n'existe pas
        if (!mainData.has(dataTable)) {
            mainData.add(dataTable, new JsonObject());
            XIIDaysManagerMod.LOGGER.debug("Auto-created DataTable: {}", dataTable);
        }

        JsonObject table = mainData.getAsJsonObject(dataTable);
        boolean isNew = !table.has(dataName);

        table.addProperty(dataName, value);
        boolean saved = saveData();

        if (saved) {
            if (isNew) {
                XIIDaysManagerMod.LOGGER.info("Created {}.{} = {}", dataTable, dataName, value);
            } else {
                XIIDaysManagerMod.LOGGER.info("Modified {}.{} = {}", dataTable, dataName, value);
            }
        }

    }

    /**
     * Lit une valeur depuis une table
     * @param dataTable Le nom de la table
     * @param dataName Le nom de la donnée
     * @return La valeur sous forme de String, ou null si non trouvée
     */
    public static String dataRead(String dataTable, String dataName) {
        loadData();

        if (!mainData.has(dataTable)) {
            XIIDaysManagerMod.LOGGER.debug("DataTable '{}' does not exist", dataTable);
            return null;
        }

        JsonObject table = mainData.getAsJsonObject(dataTable);
        if (!table.has(dataName)) {
            XIIDaysManagerMod.LOGGER.debug("DataName '{}.{}' does not exist", dataTable, dataName);
            return null;
        }

        JsonElement element = table.get(dataName);
        return element.isJsonPrimitive() ? element.getAsString() : element.toString();
    }

    /**
     * Lit une valeur numérique depuis une table
     * @param dataTable Le nom de la table
     * @param dataName Le nom de la donnée
     * @param defaultValue Valeur par défaut si non trouvée
     * @return La valeur numérique
     */
    public static int dataReadInt(String dataTable, String dataName, int defaultValue) {
        loadData();

        if (!mainData.has(dataTable)) {
            return defaultValue;
        }

        JsonObject table = mainData.getAsJsonObject(dataTable);
        if (!table.has(dataName)) {
            return defaultValue;
        }

        try {
            return table.get(dataName).getAsInt();
        } catch (Exception e) {
            XIIDaysManagerMod.LOGGER.warn("Failed to read int value for {}.{}: {}", dataTable, dataName, e.getMessage());
            return defaultValue;
        }
    }

    /**
     * Lit une valeur booléenne depuis une table
     * @param dataTable Le nom de la table
     * @param dataName Le nom de la donnée
     * @param defaultValue Valeur par défaut si non trouvée
     * @return La valeur booléenne
     */
    public static boolean dataReadBoolean(String dataTable, String dataName, boolean defaultValue) {
        loadData();

        if (!mainData.has(dataTable)) {
            return defaultValue;
        }

        JsonObject table = mainData.getAsJsonObject(dataTable);
        if (!table.has(dataName)) {
            return defaultValue;
        }

        try {
            return table.get(dataName).getAsBoolean();
        } catch (Exception e) {
            XIIDaysManagerMod.LOGGER.warn("Failed to read boolean value for {}.{}: {}", dataTable, dataName, e.getMessage());
            return defaultValue;
        }
    }

    /**
     * Supprime une table entière
     *
     * @param dataTable Le nom de la table à supprimer
     */
    public static void dataDelete(String dataTable) {
        loadData();

        if (!mainData.has(dataTable)) {
            XIIDaysManagerMod.LOGGER.debug("DataTable '{}' does not exist", dataTable);
            return;
        }

        mainData.remove(dataTable);
        boolean saved = saveData();

        if (saved) {
            XIIDaysManagerMod.LOGGER.info("Deleted DataTable: {}", dataTable);
        }

    }

    /**
     * Supprime une donnée spécifique d'une table (clé entière)
     *
     * @param dataTable Le nom de la table
     * @param dataName  Le nom de la donnée à supprimer
     */
    public static void dataRemove(String dataTable, String dataName) {
        loadData();

        if (!mainData.has(dataTable)) {
            XIIDaysManagerMod.LOGGER.debug("DataTable '{}' does not exist", dataTable);
            return;
        }

        JsonObject table = mainData.getAsJsonObject(dataTable);
        if (!table.has(dataName)) {
            XIIDaysManagerMod.LOGGER.debug("DataName '{}.{}' does not exist", dataTable, dataName);
            return;
        }

        table.remove(dataName);
        boolean saved = saveData();

        if (saved) {
            XIIDaysManagerMod.LOGGER.info("Removed {}.{}", dataTable, dataName);
        }

    }

    /**
     * Supprime une valeur spécifique dans une donnée (tableau ou valeur simple)
     * @param dataTable Le nom de la table
     * @param dataName Le nom de la donnée
     * @param value La valeur à supprimer
     * @return true si la valeur a été supprimée, false sinon
     */
    public static boolean dataRemove(String dataTable, String dataName, String value) {
        loadData();

        if (!mainData.has(dataTable)) {
            XIIDaysManagerMod.LOGGER.debug("DataTable '{}' does not exist", dataTable);
            return false;
        }

        JsonObject table = mainData.getAsJsonObject(dataTable);
        if (!table.has(dataName)) {
            XIIDaysManagerMod.LOGGER.debug("DataName '{}.{}' does not exist", dataTable, dataName);
            return false;
        }

        JsonElement element = table.get(dataName);

        // Si c'est un tableau → supprime la valeur dedans
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            JsonArray newArray = new JsonArray();
            boolean removed = false;

            for (JsonElement e : array) {
                if (e.isJsonPrimitive() && e.getAsString().equals(value)) {
                    removed = true;
                } else {
                    newArray.add(e);
                }
            }

            if (removed) {
                table.add(dataName, newArray);
                boolean saved = saveData();
                if (saved) {
                    XIIDaysManagerMod.LOGGER.info("Removed value '{}' from {}.{}", value, dataTable, dataName);
                }
                return saved;
            } else {
                XIIDaysManagerMod.LOGGER.debug("Value '{}' not found in {}.{}", value, dataTable, dataName);
                return false;
            }
        }

        // Si c'est une valeur simple → supprime uniquement si elle correspond
        if (element.isJsonPrimitive() && element.getAsString().equals(value)) {
            table.remove(dataName);
            boolean saved = saveData();
            if (saved) {
                XIIDaysManagerMod.LOGGER.info("Removed {}.{} = {}", dataTable, dataName, value);
            }
            return saved;
        }

        XIIDaysManagerMod.LOGGER.debug("DataName '{}.{}' is not an array and does not match value '{}'", dataTable, dataName, value);
        return false;
    }





    /**
     * Vérifie si une table existe
     * @param dataTable Le nom de la table
     * @return true si la table existe
     */
    public static boolean hasTable(String dataTable) {
        loadData();
        return mainData.has(dataTable);
    }

    /**
     * Vérifie si une donnée existe dans une table
     * @param dataTable Le nom de la table
     * @param dataName Le nom de la donnée
     * @return true si la donnée existe
     */
    public static boolean hasData(String dataTable, String dataName) {
        loadData();

        if (!mainData.has(dataTable)) {
            return false;
        }

        return mainData.getAsJsonObject(dataTable).has(dataName);
    }

    /**
     * Obtient tous les noms de tables
     * @return Array des noms de tables
     */
    public static String[] getAllTables() {
        loadData();
        return mainData.keySet().toArray(new String[0]);
    }

    /**
     * Obtient tous les noms de données d'une table
     * @param dataTable Le nom de la table
     * @return Array des noms de données, ou array vide si la table n'existe pas
     */
    public static String[] getAllDataNames(String dataTable) {
        loadData();

        if (!mainData.has(dataTable)) {
            return new String[0];
        }

        JsonObject table = mainData.getAsJsonObject(dataTable);
        return table.keySet().toArray(new String[0]);
    }

    /**
     * Force le rechargement des données depuis le fichier
     */
    public static void reloadData() {
        mainData = null;
        loadData();
        XIIDaysManagerMod.LOGGER.info("Data reloaded from file");
    }

    /**
     * Force la sauvegarde des données
     */
    public static boolean forceSave() {
        loadData();
        return saveData();
    }
}

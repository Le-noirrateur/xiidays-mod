package com.mceteams.xiidays.utils;

import com.mceteams.xiidays.events.PointsChangedEvent;
import com.mceteams.xiidays.network.OpenScoreboardPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.*;
import java.util.stream.Collectors;

import static com.mceteams.xiidays.XIIDays.LOGGER;
import static com.mceteams.xiidays.utils.TeamManager.getTeamName;

@EventBusSubscriber(modid = "xiidays")
public class ScoreboardManager {
    // Map teamId → dernier classement connu
    private static final Map<Integer, TeamRankingEntry> lastRankings = new HashMap<>();

    // Durée d'affichage des flèches (en ms)
    private static final long ARROW_DISPLAY_DURATION = 20000; // 20 secondes

    /**
     * Récupère le classement actuel des équipes avec leur historique
     * Cette méthode construit la liste à partir des données en mémoire (lastRankings)
     * qui sont mises à jour automatiquement par updateRankings()
     * @return Liste triée des équipes avec leur rang actuel et précédent
     */
    public static List<TeamRankingEntry> getRankings() {
        // Récupérer le leaderboard actuel depuis PointsManager (déjà trié par points DESC)
        List<Map.Entry<Integer, Integer>> leaderboard = PointsManager.getLeaderboard();

        // Transformer chaque entrée du leaderboard en TeamRankingEntry
        return leaderboard.stream()
                .map(entry -> {
                    int teamId = entry.getKey();
                    int points = entry.getValue();

                    // Récupérer l'entrée stockée en mémoire (contient l'historique)
                    TeamRankingEntry storedEntry = lastRankings.get(teamId);

                    // Si l'équipe existe déjà en mémoire, retourner ses infos
                    if (storedEntry != null) {
                        return storedEntry;
                    }

                    // Sinon, créer une nouvelle entrée (première fois qu'on voit cette équipe).
                    int currentRank = leaderboard.indexOf(entry) + 1;
                    return new TeamRankingEntry(
                            teamId,
                            getTeamName(teamId),
                            points,
                            currentRank,
                            currentRank, // previousRank = currentRank (pas de changement)
                            0,           // pas de timestamp
                            RankChange.NONE
                    );
                })
                .collect(Collectors.toList());
    }


    // Enum pour les changements
    public enum RankChange {
        UP, DOWN, NONE
    }

    // Classe de données (peut être static inner class)
    public record TeamRankingEntry(int teamId, String teamName, int points, int currentRank, int previousRank,
                                   long lastChangeTime, RankChange change) {
    }

    @SubscribeEvent
    public static void onPointsChanged(PointsChangedEvent event) {
        updateRankings();
    }

    private static void updateRankings() {
        // 1. Récupérer nouveau classement
        List<Map.Entry<Integer, Integer>> newLeaderboard = PointsManager.getLeaderboard();

        // 2. Pour chaque équipe, comparer avec ancien
        for (int i = 0; i < newLeaderboard.size(); i++) {
            int teamId = newLeaderboard.get(i).getKey();
            int newRank = i + 1;

            TeamRankingEntry oldEntry = lastRankings.get(teamId);
            int oldRank = (oldEntry != null) ? oldEntry.currentRank : newRank;

            // 3. Déterminer le changement
            RankChange change = RankChange.NONE;
            if (newRank < oldRank) change = RankChange.UP;
            if (newRank > oldRank) change = RankChange.DOWN;

            // 4. Créer nouvelle entrée
            TeamRankingEntry newEntry = new TeamRankingEntry(
                    teamId,
                    getTeamName(teamId),
                    newLeaderboard.get(i).getValue(),
                    newRank,
                    oldRank,
                    change != RankChange.NONE ? System.currentTimeMillis() : 0,
                    change
            );

            lastRankings.put(teamId, newEntry);
        }
    }

    // Vérifie si la flèche doit encore être affichée
    public static boolean shouldShowArrow(TeamRankingEntry entry) {
        if (entry.change == RankChange.NONE) return false;
        long elapsed = System.currentTimeMillis() - entry.lastChangeTime;
        return elapsed < ARROW_DISPLAY_DURATION;
    }

    /**
     * Récupère toutes les données du scoreboard pour un joueur
     * @param player Le joueur qui demande les données
     * @return ScoreboardData contenant la liste des équipes triées et l'équipe du joueur
     */
    public static ScoreboardData getScoreboardData(ServerPlayer player) {
        List<OpenScoreboardPacket.TeamData> teams = new ArrayList<>();

        // Parcourir toutes les équipes enregistrées
        for (String teamName : TeamManager.getAllTeams()) {
            int teamId = TeamManager.getTeamId(teamName);

            if (teamId == 0) continue; // Équipe invalide ou inexistante

            // Récupérer les données depuis DataManager
            String uuid = DataManager.dataRead("team_" + teamId + "_config", "uuid");

            // Vérification pour éviter les null
            if (uuid == null || uuid.isEmpty()) {
                // Générer un UUID si inexistant
                uuid = UUID.randomUUID().toString();
                DataManager.dataModify("team_" + teamId + "_config", "uuid", uuid);
                LOGGER.warn("Team {} had no UUID, generated new one: {}", teamName, uuid);
            }

            int points = DataManager.dataReadInt("team_" + teamId + "_points", "total", 0);
            boolean coreAlive = !DataManager.dataReadBoolean("team_" + teamId + "_config", "team_eliminated", false);

            // Ajouter l'équipe à la liste
            teams.add(new OpenScoreboardPacket.TeamData(teamName, uuid, points, coreAlive));
        }

        // Trier les équipes par points (ordre décroissant)
        teams.sort(Comparator.comparingInt(OpenScoreboardPacket.TeamData::points).reversed());

        // Récupérer l'équipe du joueur
        String playerTeam = TeamManager.getPlayerCurrentTeam(player.getUUID().toString());
        if (playerTeam == null || playerTeam.isEmpty()) {
            playerTeam = "Aucune"; // Le joueur n'a pas d'équipe
        }

        return new ScoreboardData(teams, playerTeam);
    }

    /**
     * Record contenant les données du scoreboard
     * @param teams Liste des équipes avec leurs données
     * @param playerTeam Nom de l'équipe du joueur
     */
    public record ScoreboardData(List<OpenScoreboardPacket.TeamData> teams, String playerTeam) {}
}
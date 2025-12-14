package com.mceteams.xiidays.client;

import java.util.*;

/**
 * Système côté client pour tracker les changements de rang
 * Persiste même quand le GUI est fermé
 */
public class ClientRankTracker {

    // Durée d'affichage des flèches en millisecondes
    public static final long ARROW_DURATION_MS = 5000; // 5 secondes

    // Map teamName → RankChangeInfo
    private static final Map<String, RankChangeInfo> rankChanges = new HashMap<>();

    // Détection des batailles : paires d'équipes qui échangent
    private static final Map<String, BattleInfo> battles = new HashMap<>();

    // Dernier classement connu pour détecter les changements
    private static List<String> lastRanking = new ArrayList<>();

    /**
     * Met à jour le classement et détecte les changements
     * Appelé quand on reçoit les données du serveur
     */
    public static void updateRanking(List<String> newRanking) {
        if (lastRanking.isEmpty()) {
            lastRanking = new ArrayList<>(newRanking);
            return;
        }

        long now = System.currentTimeMillis();

        // Détecter les changements de position
        for (int newPos = 0; newPos < newRanking.size(); newPos++) {
            String teamName = newRanking.get(newPos);
            int oldPos = lastRanking.indexOf(teamName);

            if (oldPos == -1) continue; // Nouvelle équipe

            if (newPos < oldPos) {
                // L'équipe est montée
                registerChange(teamName, RankChangeType.UP, oldPos, newPos, now);
            } else if (newPos > oldPos) {
                // L'équipe est descendue
                registerChange(teamName, RankChangeType.DOWN, oldPos, newPos, now);
            }
        }

        // Détecter les batailles (2 équipes qui échangent)
        detectBattles(newRanking, now);

        lastRanking = new ArrayList<>(newRanking);
    }

    /**
     * Enregistre un changement de rang
     */
    private static void registerChange(String teamName, RankChangeType type, int oldPos, int newPos, long timestamp) {
        RankChangeInfo existing = rankChanges.get(teamName);

        if (existing != null && !existing.isExpired()) {
            // Un changement existe déjà et n'est pas expiré
            // On le remplace par le nouveau
            rankChanges.put(teamName, new RankChangeInfo(type, oldPos, newPos, timestamp));
        } else {
            rankChanges.put(teamName, new RankChangeInfo(type, oldPos, newPos, timestamp));
        }
    }

    /**
     * Détecte les batailles entre équipes
     */
    private static void detectBattles(List<String> newRanking, long now) {
        // Chercher des paires d'équipes qui ont échangé leurs positions
        for (int i = 0; i < newRanking.size(); i++) {
            for (int j = i + 1; j < newRanking.size(); j++) {
                String team1 = newRanking.get(i);
                String team2 = newRanking.get(j);

                int oldPos1 = lastRanking.indexOf(team1);
                int oldPos2 = lastRanking.indexOf(team2);

                if (oldPos1 == -1 || oldPos2 == -1) continue;

                // Si les équipes ont échangé leurs positions
                if (oldPos1 == j && oldPos2 == i) {
                    String battleKey = getBattleKey(team1, team2);
                    BattleInfo existing = battles.get(battleKey);

                    if (existing != null && !existing.isExpired()) {
                        // Bataille déjà en cours, on incrémente
                        existing.incrementExchanges(now);
                    } else {
                        // Nouvelle bataille
                        battles.put(battleKey, new BattleInfo(team1, team2, now));
                    }
                }
            }
        }
    }

    /**
     * Génère une clé unique pour une paire d'équipes
     */
    private static String getBattleKey(String team1, String team2) {
        if (team1.compareTo(team2) < 0) {
            return team1 + ":" + team2;
        }
        return team2 + ":" + team1;
    }

    /**
     * Récupère le changement de rang pour une équipe (si actif)
     */
    public static RankChangeInfo getActiveChange(String teamName) {
        RankChangeInfo info = rankChanges.get(teamName);
        if (info != null && !info.isExpired()) {
            return info;
        }
        return null;
    }

    /**
     * Vérifie si deux équipes sont en bataille
     */
    public static BattleInfo getActiveBattle(String team1, String team2) {
        String key = getBattleKey(team1, team2);
        BattleInfo battle = battles.get(key);
        if (battle != null && !battle.isExpired()) {
            return battle;
        }
        return null;
    }

    /**
     * Récupère toutes les batailles actives impliquant une équipe
     */
    public static BattleInfo getActiveBattleFor(String teamName) {
        for (BattleInfo battle : battles.values()) {
            if (!battle.isExpired() && battle.involves(teamName)) {
                return battle;
            }
        }
        return null;
    }

    /**
     * Nettoie les entrées expirées
     */
    public static void cleanup() {
        rankChanges.entrySet().removeIf(e -> e.getValue().isExpired());
        battles.entrySet().removeIf(e -> e.getValue().isExpired());
    }

    /**
     * Réinitialise le tracker (nouvelle partie par exemple)
     */
    public static void reset() {
        rankChanges.clear();
        battles.clear();
        lastRanking.clear();
    }

    // ===== CLASSES INTERNES =====

    public enum RankChangeType {
        UP,   // Montée
        DOWN, // Descente
        NONE  // Pas de changement
    }

    public static class RankChangeInfo {
        private final RankChangeType type;
        private final int oldPosition;
        private final int newPosition;
        private final long timestamp;

        public RankChangeInfo(RankChangeType type, int oldPos, int newPos, long timestamp) {
            this.type = type;
            this.oldPosition = oldPos;
            this.newPosition = newPos;
            this.timestamp = timestamp;
        }

        public RankChangeType getType() { return type; }
        public int getOldPosition() { return oldPosition; }
        public int getNewPosition() { return newPosition; }
        public long getTimestamp() { return timestamp; }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > ARROW_DURATION_MS;
        }

        /**
         * Temps restant en millisecondes
         */
        public long getRemainingTime() {
            long elapsed = System.currentTimeMillis() - timestamp;
            return Math.max(0, ARROW_DURATION_MS - elapsed);
        }

        /**
         * Progression de 0.0 (début) à 1.0 (fin)
         */
        public float getProgress() {
            long elapsed = System.currentTimeMillis() - timestamp;
            return Math.min(1.0f, (float) elapsed / ARROW_DURATION_MS);
        }
    }

    public static class BattleInfo {
        private final String team1;
        private final String team2;
        private long lastExchangeTime;
        private int exchangeCount;

        public BattleInfo(String team1, String team2, long timestamp) {
            this.team1 = team1;
            this.team2 = team2;
            this.lastExchangeTime = timestamp;
            this.exchangeCount = 1;
        }

        public void incrementExchanges(long timestamp) {
            this.exchangeCount++;
            this.lastExchangeTime = timestamp;
        }

        public String getTeam1() { return team1; }
        public String getTeam2() { return team2; }
        public int getExchangeCount() { return exchangeCount; }

        public boolean involves(String teamName) {
            return team1.equalsIgnoreCase(teamName) || team2.equalsIgnoreCase(teamName);
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - lastExchangeTime > ARROW_DURATION_MS;
        }

        public long getRemainingTime() {
            long elapsed = System.currentTimeMillis() - lastExchangeTime;
            return Math.max(0, ARROW_DURATION_MS - elapsed);
        }
    }
}

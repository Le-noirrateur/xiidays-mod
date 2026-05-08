package com.mceteams.xiidays.player;

import java.util.*;

/**
 * Système côté client pour tracker les changements de rang
 * Persiste même quand le GUI est fermé
 */
public class ClientRankTracker {

    // Durée d'affichage des flèches en millisecondes
    public static final long ARROW_DURATION_MS = 5000; // 5 secondes

    // Durée avant qu'une bataille expire (pas d'échange)
    public static final long BATTLE_TIMEOUT_MS = 10000; // 10 secondes

    // Nombre minimum d'échanges pour activer le mode bataille
    public static final int MIN_EXCHANGES_FOR_BATTLE = 3;

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

        // Vérifier si des batailles doivent être terminées
        checkBattleEndings(newRanking);

        lastRanking = new ArrayList<>(newRanking);
    }

    /**
     * Enregistre un changement de rang
     */
    private static void registerChange(String teamName, RankChangeType type, int oldPos, int newPos, long timestamp) {
        rankChanges.put(teamName, new RankChangeInfo(type, oldPos, newPos, timestamp));
    }

    /**
     * Détecte les batailles entre équipes
     * Une bataille nécessite que 2 équipes adjacentes échangent leurs positions
     */
    private static void detectBattles(List<String> newRanking, long now) {
        // Chercher des paires d'équipes adjacentes qui ont échangé leurs positions
        for (int i = 0; i < newRanking.size() - 1; i++) {
            String teamAtI = newRanking.get(i);
            String teamAtIPlus1 = newRanking.get(i + 1);

            int oldPosTeamI = lastRanking.indexOf(teamAtI);
            int oldPosTeamIPlus1 = lastRanking.indexOf(teamAtIPlus1);

            if (oldPosTeamI == -1 || oldPosTeamIPlus1 == -1) continue;

            // Vérifier si c'est un échange adjacent (pas un grand saut)
            // L'équipe à la position i était à i+1, et celle à i+1 était à i
            boolean isAdjacentExchange = (oldPosTeamI == i + 1) && (oldPosTeamIPlus1 == i);

            if (isAdjacentExchange) {
                String battleKey = getBattleKey(teamAtI, teamAtIPlus1);
                BattleInfo existing = battles.get(battleKey);

                if (existing != null && !existing.isExpired()) {
                    // Bataille en cours, on incrémente et met à jour les positions
                    existing.incrementExchanges(now, i, i + 1);
                } else {
                    // Nouvelle bataille potentielle
                    battles.put(battleKey, new BattleInfo(teamAtI, teamAtIPlus1, now, i, i + 1));
                }
            }
        }
    }

    /**
     * Vérifie si des batailles doivent être terminées
     * Une bataille se termine si une des équipes sort des positions contestées
     */
    private static void checkBattleEndings(List<String> newRanking) {
        Iterator<Map.Entry<String, BattleInfo>> iterator = battles.entrySet().iterator();

        while (iterator.hasNext()) {
            BattleInfo battle = iterator.next().getValue();

            int pos1 = newRanking.indexOf(battle.getTeam1());
            int pos2 = newRanking.indexOf(battle.getTeam2());

            // Si une équipe n'est plus trouvée, terminer la bataille
            if (pos1 == -1 || pos2 == -1) {
                iterator.remove();
                continue;
            }

            // Si les équipes ne sont plus adjacentes, terminer la bataille
            if (Math.abs(pos1 - pos2) != 1) {
                iterator.remove();
                continue;
            }

            // Si les équipes sont sorties des positions de bataille originales
            int minBattlePos = battle.getMinPosition();
            int maxBattlePos = battle.getMaxPosition();

            boolean team1InRange = (pos1 >= minBattlePos && pos1 <= maxBattlePos);
            boolean team2InRange = (pos2 >= minBattlePos && pos2 <= maxBattlePos);

            if (!team1InRange || !team2InRange) {
                iterator.remove();
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
        if (battle != null && !battle.isExpired() && battle.isActive()) {
            return battle;
        }
        return null;
    }

    /**
     * Récupère la bataille active impliquant une équipe (si elle atteint le seuil)
     */
    public static BattleInfo getActiveBattleFor(String teamName) {
        for (BattleInfo battle : battles.values()) {
            if (!battle.isExpired() && battle.isActive() && battle.involves(teamName)) {
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

        /**
         * Vérifie si c'est un "grand saut" (plus de 1 position)
         */
        public boolean isBigJump() {
            return Math.abs(newPosition - oldPosition) > 1;
        }

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
        private int position1; // Position de la bataille
        private int position2; // Position adjacente

        public BattleInfo(String team1, String team2, long timestamp, int pos1, int pos2) {
            this.team1 = team1;
            this.team2 = team2;
            this.lastExchangeTime = timestamp;
            this.exchangeCount = 1;
            this.position1 = Math.min(pos1, pos2);
            this.position2 = Math.max(pos1, pos2);
        }

        public void incrementExchanges(long timestamp, int pos1, int pos2) {
            this.exchangeCount++;
            this.lastExchangeTime = timestamp;
            // Mettre à jour les positions
            this.position1 = Math.min(pos1, pos2);
            this.position2 = Math.max(pos1, pos2);
        }

        public String getTeam1() { return team1; }
        public String getTeam2() { return team2; }
        public int getExchangeCount() { return exchangeCount; }
        public int getMinPosition() { return position1; }
        public int getMaxPosition() { return position2; }

        public boolean involves(String teamName) {
            return team1.equalsIgnoreCase(teamName) || team2.equalsIgnoreCase(teamName);
        }

        /**
         * La bataille est "active" seulement si le seuil minimum d'échanges est atteint
         */
        public boolean isActive() {
            return exchangeCount >= MIN_EXCHANGES_FOR_BATTLE;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - lastExchangeTime > BATTLE_TIMEOUT_MS;
        }

        public long getRemainingTime() {
            long elapsed = System.currentTimeMillis() - lastExchangeTime;
            return Math.max(0, BATTLE_TIMEOUT_MS - elapsed);
        }
    }
}

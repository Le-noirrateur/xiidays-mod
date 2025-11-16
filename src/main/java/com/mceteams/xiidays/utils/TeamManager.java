package com.mceteams.xiidays.utils;

import com.mceteams.xiidays.blocks.BlockRegistry;
import com.mceteams.xiidays.blocks.teamCore.teamCore;
import com.mceteams.xiidays.blocks.teamSpawner.teamSpawner;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.UUID;

import static com.mceteams.xiidays.XIIDaysManagerMod.LOGGER;
import static com.mceteams.xiidays.utils.DataManager.*;

public class TeamManager {
    /**
     * Ajoute un membre à une équipe
     * @param teamName le Nom de l'équipe
     * @param player Le joueur
     * @return 1 si a fonctionné, > 1 si une erreur s'est produit
     */
    public static int addMember(String teamName, Player player) {
        

        if (hasData("Teams", teamName)) {
            int teamID = dataReadInt("Teams", teamName, 0);

            if (teamID == 0) {
                return 3; // Impossible de charger l'ID de team
            }

            String[] TeamMembers = getAllDataNames("team_" + teamID + "_members");
            int newMemberID = TeamMembers.length + 1;

            String playerUUID = player.getUUID().toString();

            if (isPlayerInTeam(playerUUID, teamID)) {
                return 4; // Joueur déjà dans cette équipe
            }

            String currentTeam = getPlayerCurrentTeam(playerUUID);
            if (currentTeam != null && !currentTeam.equals(teamName)) {
                return 5; // Joueur déjà dans une autre équipe
            }

            // Ajoute le joueur
            dataModify("team_" + teamID + "_members", "member_" + newMemberID, playerUUID);

            return 1; // Membre Ajouté
        } else {
            return 2; // Team inexistant
        }
    }

    /**
     * Retire un membre d'une équipe
     * @param teamName Nom de l'équipe
     * @param player Le joueur
     * @return 1 si a fonctionné, > 1 si une erreur s'est produit
     */
    public static int remMember(String teamName, Player player) {
        

        if (hasData("Teams", teamName)) {
            int teamID = dataReadInt("Teams", teamName, 0);

            if (teamID == 0) {
                return 3; // Impossible de charger l'ID de team
            }

            String[] teamMembers = getAllDataNames("team_" + teamID + "_members");
            String playerUUID = player.getUUID().toString();

            if (!isPlayerInTeam(playerUUID, teamID)) {
                return 4; // Joueur n'est pas dans cette équipe
            }


            String currentTeam = getPlayerCurrentTeam(playerUUID);
            if (currentTeam != null && !currentTeam.equals(teamName)) {
                return 5; // Joueur déjà dans une autre équipe
            }

            for (String memberKey : teamMembers) {
                String memberUUID = dataRead("team_" + teamID + "_members", memberKey);
                if (playerUUID.equals(memberUUID)) {
                    dataRemove("team_" + teamID + "_members", memberKey);
                    return 1; // Membre retiré avec succès
                }
            }

            return 6; // Par sécurité : joueur non trouvé dans la liste
        } else {
            return 2; // Team inexistante
        }
    }

    /**
     * Supprime une équipe
     * @param teamName nom de l'équipe
     * @return 1 si a fonctionné, > 1 si une erreur s'est produit
     */
    public static int remTeam(String teamName) {
        int teamID = dataReadInt("Teams", teamName, 0);

        if (teamID == 0) {
            return 2; // Impossible de charger l'ID de team
        }

        dataDelete(teamName);
        dataDelete("team_" + teamID + "_members");
        dataDelete("team_" + teamID + "_points");
        dataDelete("team_" + teamID + "_stats");
        dataDelete("team_" + teamID + "_config");

        return 1;
    }

    /**
     * place le block et le spawn de l'équipe
     * @param teamName Nom de l'équipe
     * @param pos Position du placement du block
     * @param level Le monde dans lequel le block doit être placé
     * @return 1 si a fonctionné, > 1 si une erreur s'est produit
     */
    public static int placeTeamSpawn(String teamName, BlockPos pos, Level level) {
        int x = pos.getX();
        int y = pos.getY() + 1;
        int z = pos.getZ();

        BlockPos newPos = new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ());

        BlockState blockState = BlockRegistry.TEAM_SPAWNER.get().defaultBlockState();
        boolean blockPlaced = level.setBlock(newPos, blockState, 3);

        if (blockPlaced) {
            BlockEntity blockEntity = level.getBlockEntity(newPos);
            if (blockEntity instanceof teamSpawner teamSpawn) {
                int teamId = TeamManager.getTeamId(teamName);
                teamSpawn.setTeamId(teamId);

                level.getChunkAt(newPos).setUnsaved(true);

                dataModify(teamName, "spawn", x + "," + y + "," + z);
                return 1;
            } else {
                return 2; // Erreur : Le block placé ne possède pas de BlockEntity valide
            }
        } else {
            return 3; // Impossible de placer le block de spawn à cette position
        }
    }

    public static String getTeamName(int teamId) {
        String[] allTeams = getAllDataNames("Teams");

        for (String teamName : allTeams) {
            int id = dataReadInt("Teams", teamName, 0);
            if (id == teamId) {
                return teamName;
            }
        }

        return null; // aucune team avec cet ID
    }


    /**
     * place le block et le core de l'équipe
     * @param teamName Nom de l'équipe
     * @param pos Position du placement du block
     * @param level Le monde dans lequel le block doit être placé
     * @return 1 si a fonctionné, > 1 si une erreur s'est produit
     */
    public static int placeTeamCore(String teamName, BlockPos pos, Level level) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        BlockState blockState = BlockRegistry.TEAM_CORE.get().defaultBlockState();
        boolean blockPlaced = level.setBlock(pos, blockState, 3);

        if (blockPlaced) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof teamCore teamCore) {
                int teamId = TeamManager.getTeamId(teamName);
                teamCore.setTeamId(teamId);
                teamCore.setPuzzleSolved(false);

                level.getChunkAt(pos).setUnsaved(true);

                dataModify(teamName, "core", x + "," + y + "," + z);
                return 1;
            } else {
                return 2; // Erreur : Le block placé ne possède pas de BlockEntity valide
            }
        } else {
            return 3; // Impossible de placer le block de core à cette position
        }
    }

    /**
     * Vérifie si un joueur est déjà dans une équipe spécifique
     * @param playerUUID UUID du joueur
     * @param teamID ID de l'équipe
     * @return true si le joueur est dans cette équipe
     */
    public static boolean isPlayerInTeam(String playerUUID, int teamID) {
        

        String[] teamMembers = getAllDataNames("team_" + teamID + "_members");

        for (String memberKey : teamMembers) {
            String memberUUID = dataRead("team_" + teamID + "_members", memberKey);
            if (playerUUID.equals(memberUUID)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Récupère toutes les équipes existantes
     * @return Tableau des noms des équipes
     */
    public static String[] getAllTeams() {
        return getAllDataNames("Teams"); // Récupère toutes les clés de la table "Teams"
    }

    /**
     * Envoie un message à toute une équipe
     * @param teamName Nom de l'équipe
     * @param message String du message
     */
    public static void sendMessageToTeam(String teamName, Component message) {
        int teamId = getTeamId(teamName);
        if (teamId == 0) return;

        String[] members = getAllDataNames("team_" + teamId + "_members");
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        for (String memberKey : members) {
            String uuidStr = dataRead("team_" + teamId + "_members", memberKey);
            if (uuidStr == null) continue;

            try {
                java.util.UUID uuid = java.util.UUID.fromString(uuidStr);
                ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                if (player != null) {
                    player.sendSystemMessage(message);
                }
            } catch (IllegalArgumentException e) {
                // UUID invalide, on skip
            }
        }
    }

    /**
     * Envoie un message à toute une équipe
     * @param teamId Nom de l'équipe
     * @param message String du message
     * @param exclude Player
     */
    public static void sendMessageToTeam(int teamId, Component message, UUID exclude) {
        if (teamId == 0) return;

        String[] members = getAllDataNames("team_" + teamId + "_members");
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        for (String memberKey : members) {
            String uuidStr = dataRead("team_" + teamId + "_members", memberKey);
            if (uuidStr == null) continue;

            try {
                UUID uuid = UUID.fromString(uuidStr);
                ServerPlayer player = server.getPlayerList().getPlayer(uuid);

                if (uuid.equals(exclude)) continue;
                assert player != null;

                player.sendSystemMessage(message);

            } catch (IllegalArgumentException e) {
                // UUID invalide, on skip
            }
        }
    }

    /**
     * Élimine une équipe et calcule sa position finale
     * @param teamName Nom de l'équipe éliminée
     * @return true si succès
     */
    public static boolean eliminateTeam(String teamName) {
        int teamId = getTeamId(teamName);
        if (teamId == 0) return false;

        // Vérifier si déjà éliminée
        if (DataManager.dataReadBoolean("team_" + teamId + "_config", "team_eliminated", false)) {
            LOGGER.warn("Team {} already eliminated!", teamName);
            return false;
        }

        // Marquer comme éliminée
        DataManager.dataModify("team_" + teamId + "_config", "team_eliminated", true);

        // CALCUL : finalpos = total_teams - teams_restantes
        int totalTeams = getAllTeams().length;
        int remainingTeams = countRemainingTeams(); // Compte APRÈS élimination
        int finalPosition = totalTeams - remainingTeams;

        DataManager.dataModify("team_" + teamId + "_config", "finalpos", finalPosition);

        LOGGER.info("Team {} eliminated - Position: {} ({} teams remaining)",
                teamName, finalPosition, remainingTeams);

        // 🔥 SI UNE SEULE ÉQUIPE RESTE → FIN DE PARTIE
        if (remainingTeams == 1) {
            String winnerTeam = getLastRemainingTeam();
            if (winnerTeam != null) {
                setWinningTeam(winnerTeam);
                triggerEndScreen(winnerTeam);
            }
        }

        return true;
    }

    /**
     * Compte les équipes encore en vie
     */
    private static int countRemainingTeams() {
        String[] allTeams = getAllTeams();
        int count = 0;

        for (String teamName : allTeams) {
            int teamId = getTeamId(teamName);
            if (teamId == 0) continue;

            boolean eliminated = DataManager.dataReadBoolean(
                    "team_" + teamId + "_config",
                    "team_eliminated",
                    false
            );

            if (!eliminated) count++;
        }

        return count;
    }

    /**
     * Récupère la dernière équipe en vie
     */
    private static String getLastRemainingTeam() {
        String[] allTeams = getAllTeams();

        for (String teamName : allTeams) {
            int teamId = getTeamId(teamName);
            if (teamId == 0) continue;

            boolean eliminated = DataManager.dataReadBoolean(
                    "team_" + teamId + "_config",
                    "team_eliminated",
                    false
            );

            if (!eliminated) return teamName;
        }

        return null;
    }

    /**
     * Définit l'équipe gagnante (finalpos = 1)
     */
    private static void setWinningTeam(String teamName) {
        int teamId = getTeamId(teamName);
        if (teamId == 0) return;

        DataManager.dataModify("team_" + teamId + "_config", "finalpos", 1);

        LOGGER.info("🏆 Team {} wins the game!", teamName);
    }

    /**
     * Déclenche l'écran de fin pour tous les joueurs
     */
    private static void triggerEndScreen(String winningTeam) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        // Envoyer à tous les joueurs connectés
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // Côté serveur : envoyer packet custom pour ouvrir GUI côté client
            // Ou utiliser un command pour ouvrir
            player.sendSystemMessage(Component.literal("§6§l=== FIN DE PARTIE ==="));
            player.sendSystemMessage(Component.literal("§eÉquipe gagnante : §6§l" + winningTeam));

            // TODO: Envoyer packet pour ouvrir EndScoreboardScreen côté client
            // PacketDistributor.sendToPlayer(player, new OpenEndScreenPacket(winningTeam));
        }
    }

    /**
     * Récupère la position finale d'une équipe
     */
    public static int getFinalPosition(String teamName) {
        int teamId = getTeamId(teamName);
        if (teamId == 0) return 999;

        return DataManager.dataReadInt("team_" + teamId + "_config", "finalpos", 999);
    }

    /**
     * Récupère l'équipe gagnante
     */
    public static String getWinningTeam() {
        String[] allTeams = getAllTeams();

        for (String teamName : allTeams) {
            if (getFinalPosition(teamName) == 1) {
                return teamName;
            }
        }

        return null;
    }

    /**
     * Récupère l'équipe actuelle d'un joueur
     * @param playerUUID UUID du joueur
     * @return Le nom de l'équipe ou null si le joueur n'est dans aucune équipe
     */
    public static String getPlayerCurrentTeam(String playerUUID) {
        

        String[] allTeams = getAllDataNames("Teams");

        for (String teamName : allTeams) {
            int teamID = dataReadInt("Teams", teamName, 0);
            if (teamID > 0 && isPlayerInTeam(playerUUID, teamID)) {
                return teamName;
            }
        }

        return null; // Joueur dans aucune équipe
    }

    /**
     * Récupère l'identifiant relatif à une équipe.
     * @param teamName nom de l'équipe
     * @return identifiant de l'équipe.
     */
    public static int getTeamId(String teamName) {
        

        if (hasData("Teams", teamName)) {
            return dataReadInt("Teams", teamName, 0);
        } else {
            return 0;
        }
    }

    /**
     * Créer une équipe avec le nom demandé.
     * @param teamName nom d'une équipe
     * @return retourne 1 si tout a fonctionné.
     */
    public static int createTeam(String teamName) {
        if (!hasData("Teams", teamName)) {
            String[] teams = getAllDataNames("Teams");
            int newTeamId = teams.length + 1;

            dataAdd("team_" + newTeamId + "_members");
            dataAdd("team_" + newTeamId + "_config");
            dataAdd("team_" + newTeamId + "_points");
            dataAdd("team_" + newTeamId + "_stats");

            dataModify("team_" + newTeamId + "_config", "core_destroyed", false);
            dataModify("team_" + newTeamId + "_config", "team_eliminated", false);

            dataModify("Teams", teamName, newTeamId);

            // Rafraîchir le leaderboard
            PointsManager.refreshTeamPoints();

            return 1; // Créé
        } else {
            return 2; // Nom de teams existe déjà
        }
    }
}

package com.mceteams.xiidays.utils;

import com.mceteams.xiidays.enums.PointType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;


import static com.mceteams.xiidays.XIIDaysManagerMod.LOGGER;
import static com.mceteams.xiidays.XIIDaysManagerMod.MODID;
import static com.mceteams.xiidays.utils.DataManager.*;

@EventBusSubscriber(modid = MODID)
public class RespawnManager {
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Vérifier si un jour est en cours
        if (!DaysManager.isDayInProgress()) {
            return;
        }

        // Récupérer l'équipe du joueur
        String teamName = TeamManager.getPlayerCurrentTeam(player.getUUID().toString());
        BlockPos spawnPos = getTeamSpawnPosition(teamName);

        // Vérifie que l'équipe existe
        if (teamName == null) {
            return;
        }

        //  Vérifie si les coords de spawn existent
        if (spawnPos == null) {
            return;
        }

        DataManager.dataModify("player_" + player.getUUID() + "_stats", "deaths",
                DataManager.dataReadInt("player_" + player.getUUID() + "_stats", "deaths", 0) + 1);

        // Entrée en mode spectateur (en attendant le respawn)
        player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);


        if (DaysManager.getCurrentDay() < 6) {
            scheduleRespawnTeleport(player, spawnPos, teamName);
        }

        int teamId = TeamManager.getTeamId(teamName);
        if (teamId > 0) {
            PointsManager.addPoints(teamId, PointType.DEATH, player);
        }
    }

    // Récupère la position de spawn de l'équipe
    private static BlockPos getTeamSpawnPosition(String teamName) {
        reloadData();

        String spawnData = dataRead(teamName, "spawn");
        if (spawnData == null) {
            return null;
        }

        try {
            String[] coords = spawnData.split(",");
            if (coords.length != 3) {
                LOGGER.error("Invalid spawn data format for team {}: {}", teamName, spawnData);
                return null;
            }

            int x = Integer.parseInt(coords[0].trim());
            int y = Integer.parseInt(coords[1].trim());
            int z = Integer.parseInt(coords[2].trim());

            return new BlockPos(x, y, z);

        } catch (NumberFormatException e) {
            LOGGER.error("Failed to parse spawn coordinates for team {}: {}", teamName, e.getMessage());
            return null;
        }
    }

    // Planifie la téléportation du joueur après un délai avec effets visuels
    private static void scheduleRespawnTeleport(ServerPlayer player, BlockPos spawnPos, String teamName) {
        ServerLevel level = player.serverLevel();

        // Téléportation après délai
        level.getServer().execute(() -> {
            try {
                int death_count = dataReadInt("player_" + player.getUUID() + "_stats", "deaths", 1);
                Thread.sleep(3000L * death_count);

                // Vérifier que le joueur est toujours connecté
                if (player.hasDisconnected()) return;

                // Téléportation
                player.teleportTo(level,
                        spawnPos.getX() + 0.5,
                        spawnPos.getY(),
                        spawnPos.getZ() + 0.5,
                        player.getYRot(),
                        player.getXRot()
                );
            } catch (InterruptedException e) {
                LOGGER.error("Respawn teleportation interrupted for player {}", player.getName().getString());
                Thread.currentThread().interrupt();
            }
        });
    }

    // Vérifie si le spawn est valide (espace libre)
    public static boolean isSpawnValid(Level level, BlockPos spawnPos) {
        // Vérifier si le bloc au-dessus du spawn est libre
        BlockPos abovePos = spawnPos.above();
        BlockPos twoAbovePos = spawnPos.above(2);

        return level.getBlockState(abovePos).isAir() &&
                level.getBlockState(twoAbovePos).isAir();
    }

    // Force la téléportation immédiate au spawn de l'équipe
    public static boolean forceRespawn(ServerPlayer player) {
        String playerUUID = player.getUUID().toString();
        String teamName = TeamManager.getPlayerCurrentTeam(playerUUID);

        if (teamName == null) {
            return false;
        }

        BlockPos spawnPos = getTeamSpawnPosition(teamName);

        if (spawnPos == null) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        player.teleportTo(level,
                spawnPos.getX() + 0.5,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5,
                player.getYRot(),
                player.getXRot()
        );

        return true;
    }
}
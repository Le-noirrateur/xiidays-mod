package com.mceteams.xiidays.game;

import com.mceteams.xiidays.data.DayCycleData;
import com.mceteams.xiidays.data.TeamData;
import com.mceteams.xiidays.network.DayEndScorePayload;
import com.mceteams.xiidays.network.DayNotificationPayload;
import com.mceteams.xiidays.spectator.SpectateManager;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.mceteams.xiidays.XIIDays.LOGGER;

public class DaysManager {
    public static boolean isDayInProgress() {
        return DayCycleData.isInProgress();
    }

    public static int getCurrentDay() {
        return DayCycleData.getCurrentDay();
    }

    public static boolean start(CommandContext<CommandSourceStack> context) {
        try {
            if (TeamManager.isGameOver()) {
                context.getSource().sendSystemMessage(Component.literal("§cLa partie est déjà terminée !"));
                return false;
            }
            if (!DayCycleData.isInProgress() && DayCycleData.getCurrentDay() < 12) {
                DayCycleData.setInProgress(true);
                DayCycleData.incrementDay();

                try {
                    context.getSource().getServer().getPlayerList()
                            .broadcastAll(new ClientboundSetTitleTextPacket(Component.literal("§43")));

                    for (Player player : context.getSource().getServer().getPlayerList().getPlayers()) {
                        if (player instanceof ServerPlayer serverPlayer) {
                            serverPlayer.removeEffectNoUpdate(MobEffects.DARKNESS);
                            serverPlayer.removeEffectNoUpdate(MobEffects.BLINDNESS);
                            serverPlayer.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, true, false));
                            serverPlayer.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 255, true, false));
                            serverPlayer.playNotifySound(SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.MASTER, 1.0f, 1.0f);
                        }
                    }

                    TimeUnit.SECONDS.sleep(1);

                    context.getSource().getServer().getPlayerList()
                            .broadcastAll(new ClientboundSetTitleTextPacket(Component.literal("§62")));

                    for (Player player : context.getSource().getServer().getPlayerList().getPlayers()) {
                        player.playNotifySound(SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.MASTER, 1.0f, 1.0f);
                    }

                    TimeUnit.SECONDS.sleep(1);

                    context.getSource().getServer().getPlayerList()
                            .broadcastAll(new ClientboundSetTitleTextPacket(Component.literal("§21")));

                    for (Player player : context.getSource().getServer().getPlayerList().getPlayers()) {
                        player.playNotifySound(SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.MASTER, 1.0f, 1.0f);
                    }

                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                var notifStart = new DayNotificationPayload(DayNotificationPayload.NotificationType.START,
                        DayCycleData.getCurrentDay(), DayCycleData.getCurrentDay() - 1);
                for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
                    PacketDistributor.sendToPlayer(player, notifStart);
                }

                int days = DayCycleData.getCurrentDay();

                if (days > 6) {
                    SpectateManager.handlePhase2DayStart();
                }

                // Teleport all alive players to their team spawn at day start
                for (Player player : context.getSource().getServer().getPlayerList().getPlayers()) {
                    if (player instanceof ServerPlayer serverPlayer) {
                        if (serverPlayer.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) {
                            serverPlayer.setGameMode(GameType.SURVIVAL);
                        }
                        String teamName = TeamManager.getPlayerCurrentTeam(serverPlayer.getUUID().toString());
                        if (teamName == null) continue;
                        int teamId = TeamData.getTeamId(teamName);
                        if (teamId <= 0) continue;
                        String spawnData = TeamData.getSpawn(teamId);
                        if (spawnData == null) continue;
                        try {
                            String[] parts = spawnData.split(",");
                            BlockPos spawnPos = new BlockPos(
                                    Integer.parseInt(parts[0].trim()),
                                    Integer.parseInt(parts[1].trim()),
                                    Integer.parseInt(parts[2].trim())
                            );
                            serverPlayer.teleportTo(serverPlayer.serverLevel(),
                                    spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                                    serverPlayer.getYRot(), serverPlayer.getXRot());
                        } catch (Exception ignored) {
                        }
                    }
                }

                Component msg;

                if (days == 1) {
                    context.getSource().getServer().getPlayerList()
                            .broadcastSystemMessage(Component.literal("§c§l[§r§6§lBienvenue dans XII Days§r§c§l]\n\nVotre objectif durant ces 12 jours\nest de récupérer un maximum d'objets\nou de blocs afin de protéger votre\nbase des équipes adverse.§r\n\nVous trouverez quelque objets §2bonus§r\npermettant à votre équipe d'avoir des\n§davantages sur les autres§r, tel que des\ncolis qui tombe quelque fois.\n\n§3§lLes six premier jours sont une phase\nde préparation, les six dernier, de\ncombat\n\n"), false);
                    msg = Component.literal("Le premier jour a commencé.");
                } else {
                    msg = Component.literal("Le " + days + "e jour a commencé.");
                }

                context.getSource().getServer().getPlayerList()
                        .broadcastSystemMessage(msg, false);
                context.getSource().getServer().getPlayerList().broadcastAll(
                        new ClientboundSystemChatPacket(msg, true));

                CrateManager.startDrops();

                for (Player player : context.getSource().getServer().getPlayerList().getPlayers()) {
                    player.playNotifySound(SoundEvents.ENDER_DRAGON_GROWL, SoundSource.MASTER, 1.0f, 1.0f);
                    player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1.0f, 2.0f);
                }
            } else {
                Objects.requireNonNull(context.getSource().getPlayer()).playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, 0.5f);

                if (DayCycleData.isInProgress()) {
                    context.getSource().sendSystemMessage(Component.literal("§cUne journée est déjà en cours"));
                } else if (DayCycleData.getCurrentDay() < 12) {
                    context.getSource().sendSystemMessage(Component.literal("§cLe nombre de jours a dépassé le nombre possible ( > 12 )"));
                } else {
                    context.getSource().sendSystemMessage(Component.literal("§cUn problème inconnu s'est passé, le problème viens de l'analyse des propriétés des jours, il faut vérifier vos paramètres, impossible de lancer la journée."));
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return false;
        }
        return true;
    }

    public static boolean stop(boolean sendDayEndHud) {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return false;

            DayCycleData.setInProgress(false);
            CrateManager.stopDrops();

            if (sendDayEndHud) {
                var notifEnd = new DayNotificationPayload(DayNotificationPayload.NotificationType.END,
                        DayCycleData.getCurrentDay(), DayCycleData.getCurrentDay());
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    PacketDistributor.sendToPlayer(player, notifEnd);
                }

                // Send day-end rankings to all players
                List<Map.Entry<Integer, Integer>> leaderboard = PointsManager.getLeaderboard();
                List<DayEndScorePayload.TeamEntry> entries = new ArrayList<>();
                for (Map.Entry<Integer, Integer> entry : leaderboard) {
                    int teamId = entry.getKey();
                    String name = TeamData.getTeamName(teamId);
                    int pts = entry.getValue();
                    var rankEntry = ScoreboardManager.getRankings().stream()
                            .filter(r -> r.teamId() == teamId)
                            .findFirst().orElse(null);
                    int rankChange = 0;
                    if (rankEntry != null && ScoreboardManager.shouldShowArrow(rankEntry)) {
                        rankChange = switch (rankEntry.change()) {
                            case UP -> 1;
                            case DOWN -> -1;
                            case NONE -> 0;
                        };
                    }
                    entries.add(new DayEndScorePayload.TeamEntry(name, pts, rankChange));
                }
                var scorePayload = new DayEndScorePayload(entries);
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    PacketDistributor.sendToPlayer(player, scorePayload);
                }
            }

            if (sendDayEndHud && DayCycleData.getCurrentDay() <= 6) {
                SpectateManager.respawnAllSpectators();
            }

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 99999, 255, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 99999, 255, true, false));
            }

            server.getPlayerList()
                    .broadcastSystemMessage(Component.literal("§cLe jour est terminé"), false);

            if (sendDayEndHud) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    player.playNotifySound(SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.MASTER, 1.0f, 0.5f);
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return false;
        }
        return true;
    }

    public static boolean stop() {
        return stop(true);
    }

    public static boolean stop(CommandContext<CommandSourceStack> context) {
        return stop(true);
    }
}

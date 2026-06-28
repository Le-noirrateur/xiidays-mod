package com.mceteams.xiidays.spectator;

import com.mceteams.xiidays.data.PlayerStatsData;
import com.mceteams.xiidays.data.TeamData;
import com.mceteams.xiidays.data.TeamStatsData;
import com.mceteams.xiidays.game.*;
import com.mceteams.xiidays.network.DeathNotificationPayload;
import com.mceteams.xiidays.network.SpectatorStatusPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

import static com.mceteams.xiidays.XIIDays.LOGGER;
import static com.mceteams.xiidays.XIIDays.MODID;

@EventBusSubscriber(modid = MODID)
public class SpectateManager {

    // ──────────────────────────────────────────────
    // Constants
    // ──────────────────────────────────────────────

    private static final int RESPAWN_DELAY_MAX = 20;
    private static final int DEFAULT_FCZ_RADIUS = 30;

    // ──────────────────────────────────────────────
    // State
    // ──────────────────────────────────────────────

    public enum SpectatorMode { TEAMMATE_WATCH, BASE_SPECTATE, FREE_SPECTATE }

    /** All players currently in any spectator mode */
    private static final Set<UUID> spectatingPlayers = new HashSet<>();
    /** Map: spectator UUID → target UUID (only meaningful in TEAMMATE_WATCH) */
    private static final Map<UUID, UUID> spectatorTargets = new HashMap<>();
    /** Map: spectator UUID → current mode */
    private static final Map<UUID, SpectatorMode> spectatorModes = new HashMap<>();

    // ──────────────────────────────────────────────
    // FreeCamZone (kept for convenience)
    // ──────────────────────────────────────────────

    public static class FreeCamZone {
        public final BlockPos min;
        public final BlockPos max;

        public FreeCamZone(BlockPos min, BlockPos max) {
            this.min = min;
            this.max = max;
        }

        public boolean isInside(BlockPos pos) {
            return pos.getX() >= min.getX() && pos.getX() <= max.getX() &&
                    pos.getY() >= min.getY() && pos.getY() <= max.getY() &&
                    pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
        }

        public BlockPos clamp(BlockPos pos) {
            return new BlockPos(
                    Math.max(min.getX(), Math.min(max.getX(), pos.getX())),
                    Math.max(min.getY(), Math.min(max.getY(), pos.getY())),
                    Math.max(min.getZ(), Math.min(max.getZ(), pos.getZ()))
            );
        }
    }

    // ──────────────────────────────────────────────
    // Saved Inventory (first 3 days)
    // ──────────────────────────────────────────────

    private static final Map<UUID, SavedInventory> savedInventories = new HashMap<>();

    public record SavedInventory(List<ItemStack> items, int xpLevels, float xpProgress) {}

    private static boolean isOreItem(ItemStack stack) {
        return stack.is(Items.RAW_IRON) || stack.is(Items.RAW_GOLD) || stack.is(Items.RAW_COPPER)
                || stack.is(Items.IRON_INGOT) || stack.is(Items.GOLD_INGOT) || stack.is(Items.COPPER_INGOT)
                || stack.is(Items.DIAMOND) || stack.is(Items.EMERALD)
                || stack.is(Items.LAPIS_LAZULI) || stack.is(Items.REDSTONE)
                || stack.is(Items.COAL) || stack.is(Items.AMETHYST_SHARD)
                || stack.is(Items.NETHERITE_SCRAP) || stack.is(Items.NETHERITE_INGOT);
    }

    private static void savePlayerInventory(ServerPlayer player) {
        Inventory inv = player.getInventory();
        List<ItemStack> saved = new ArrayList<>();
        Level level = player.level();
        BlockPos pos = player.blockPosition();

        // Save main inventory + armor + offhand
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            if (isOreItem(stack)) {
                // Drop ore items on the ground
                level.addFreshEntity(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), stack.copy()));
            } else {
                saved.add(stack.copy());
            }
        }

        savedInventories.put(player.getUUID(), new SavedInventory(
                saved, player.experienceLevel, player.experienceProgress
        ));

        player.getInventory().clearContent();
        player.setExperienceLevels(0);
        player.experienceProgress = 0.0f;
    }

    private static void restorePlayerInventory(ServerPlayer player) {
        SavedInventory saved = savedInventories.remove(player.getUUID());
        if (saved == null) return;

        player.getInventory().clearContent();
        for (int i = 0; i < saved.items.size() && i < player.getInventory().getContainerSize(); i++) {
            player.getInventory().setItem(i, saved.items.get(i));
        }
        player.setExperienceLevels(saved.xpLevels);
        player.experienceProgress = saved.xpProgress;
    }

    // ──────────────────────────────────────────────
    // Events
    // ──────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!DaysManager.isDayInProgress()) return;

        String playerUUID = player.getUUID().toString();
        String teamName = TeamManager.getPlayerCurrentTeam(playerUUID);

        if (teamName == null) {
            LOGGER.warn("Player {} died without a team", player.getName().getString());
            return;
        }

        spectatingPlayers.add(player.getUUID());

        int teamId = TeamManager.getTeamId(teamName);
        if (teamId > 0) {
            PlayerStatsData.incrementDeaths(playerUUID);
            TeamStatsData.setKillStreak(teamId, 0);
            PointsManager.addPoints(teamId, PointType.DEATH, player);
        }

        int deathDelay = 0;
        if (DaysManager.getCurrentDay() <= 6) {
            int deaths = PlayerStatsData.getDeaths(playerUUID);
            deathDelay = Math.min(deaths * 3, RESPAWN_DELAY_MAX);
        }
        PacketDistributor.sendToPlayer(player, new DeathNotificationPayload(deathDelay));

        player.playSound(SoundEvents.ANVIL_BREAK, 1.0f, 1.0f);

        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            String attackerTeamName = TeamManager.getPlayerCurrentTeam(attacker.getUUID().toString());
            int attackerTeamId = TeamManager.getTeamId(attackerTeamName);

            if (attackerTeamId > 0) {
                TeamStatsData.incrementKills(attackerTeamId);
                PlayerStatsData.incrementKills(attacker.getUUID().toString());
                int newStreak = TeamStatsData.getKillStreak(attackerTeamId) + 1;
                TeamStatsData.setKillStreak(attackerTeamId, newStreak);
                TeamStatsData.updateMaxKillStreak(attackerTeamId);

                PointsManager.addPoints(attackerTeamId, PointType.KILL, attacker);
                if (!TeamStatsData.isFirstBloodClaimed()) {
                    TeamStatsData.setFirstBloodClaimed(true);
                    PointsManager.addPoints(attackerTeamId, PointType.FIRST_BLOOD, attacker);
                }
                PointsManager.addPoints(attackerTeamId, PointType.KILL_STREAK, attacker);
            }
        }

        if (DaysManager.getCurrentDay() <= 3) {
            savePlayerInventory(player);
        }

        LOGGER.info("Player {} died, entering spectator mode (phase {})",
                player.getName().getString(),
                DaysManager.getCurrentDay() <= 6 ? "preparation" : "combat");
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!spectatingPlayers.contains(player.getUUID())) return;

        int currentDay = DaysManager.getCurrentDay();
        ServerPlayer aliveTeammate = findAliveTeammate(player);
        String playerUUID = player.getUUID().toString();
        int teamId = TeamManager.getTeamId(TeamManager.getPlayerCurrentTeam(playerUUID));

        if (aliveTeammate != null) {
            enterTeammateWatch(player, aliveTeammate);
        } else {
            enterBaseSpectate(player);
        }

        if (currentDay <= 6) {
            schedulePhase1Respawn(player);
        } else {
            if (teamId > 0 && TeamData.isCoreDestroyed(teamId)) {
                enterFreeSpectate(player);
                player.sendSystemMessage(Component.literal(
                        "§c§lVotre cœur est détruit !\n" +
                        "§7Vous êtes en mode spectateur libre mais ne pouvez pas entrer dans les bases adverses.\n" +
                        "§7Utilisez §e[Shift]§7 pour changer de coéquipier."
                ));
            } else {
                player.sendSystemMessage(Component.literal(
                        "§c§lVous êtes mort en phase de combat !\n" +
                        "§7Votre équipe doit utiliser un §6Totem de Revivalité§7 pour vous faire respawn,\n" +
                        "§7ou attendez le début du jour suivant.\n" +
                        "§7Utilisez §e[Shift]§7 pour changer de coéquipier."
                ));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer spectator)) return;
        if (!spectatingPlayers.contains(spectator.getUUID())) return;
        if (spectator.tickCount % 20 != 0) return;

        SpectatorMode mode = spectatorModes.get(spectator.getUUID());
        if (mode == null) return;

        // Phase 2: if core was destroyed mid-day, switch to free spectate immediately
        if (mode != SpectatorMode.FREE_SPECTATE && DaysManager.getCurrentDay() > 6) {
            int teamId = TeamManager.getTeamId(TeamManager.getPlayerCurrentTeam(spectator.getUUID().toString()));
            if (teamId > 0 && TeamData.isCoreDestroyed(teamId)) {
                enterFreeSpectate(spectator);
                spectator.sendSystemMessage(Component.literal(
                        "§c§lVotre cœur a été détruit !\n" +
                        "§7Passage en mode spectateur libre."));
                return;
            }
        }

        switch (mode) {
            case TEAMMATE_WATCH -> tickTeammateWatch(spectator);
            case BASE_SPECTATE -> tickBaseSpectate(spectator);
            case FREE_SPECTATE -> tickFreeSpectate(spectator);
        }
    }

    // ──────────────────────────────────────────────
    // Per‑mode ticks
    // ──────────────────────────────────────────────

    private static void tickTeammateWatch(ServerPlayer spectator) {
        UUID targetUUID = spectatorTargets.get(spectator.getUUID());

        // Re‑attach if they detached (pressed Q etc.)
        if (targetUUID != null) {
            ServerPlayer target = getPlayerByUUID(targetUUID);
            if (target != null && spectator.getCamera() != target) {
                spectator.setCamera(target);
            }
        }

        // Check target validity
        ServerPlayer target = targetUUID != null ? getPlayerByUUID(targetUUID) : null;
        String spectatorTeam = TeamManager.getPlayerCurrentTeam(spectator.getUUID().toString());

        if (target == null || target.hasDisconnected() ||
                target.gameMode.getGameModeForPlayer() == GameType.SPECTATOR ||
                !spectatorTeam.equals(TeamManager.getPlayerCurrentTeam(target.getUUID().toString()))) {

            ServerPlayer newTarget = findAliveTeammate(spectator);
            if (newTarget != null) {
                enterTeammateWatch(spectator, newTarget);
            } else {
                enterBaseSpectate(spectator);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static void tickBaseSpectate(ServerPlayer spectator) {
        // If a teammate came back alive, switch to watch mode
        ServerPlayer alive = findAliveTeammate(spectator);
        if (alive != null) {
            enterTeammateWatch(spectator, alive);
            return;
        }

        // Maintain flight and noclip (adventure mode resets these every tick)
        if (!spectator.getAbilities().mayfly) {
            spectator.getAbilities().mayfly = true;
            spectator.getAbilities().flying = true;
            spectator.onUpdateAbilities();
        }
        spectator.noPhysics = true;

        enforceBaseZone(spectator);
    }

    private static void tickFreeSpectate(ServerPlayer spectator) {
        // If a teammate came back alive (totem revive), switch to watch mode
        ServerPlayer alive = findAliveTeammate(spectator);
        if (alive != null) {
            enterTeammateWatch(spectator, alive);
            return;
        }

        enforceNoBaseZone(spectator);
    }

    // ──────────────────────────────────────────────
    // Mode transitions
    // ──────────────────────────────────────────────

    private static void enterTeammateWatch(ServerPlayer spectator, ServerPlayer target) {
        spectatorModes.put(spectator.getUUID(), SpectatorMode.TEAMMATE_WATCH);

        spectator.setGameMode(GameType.SPECTATOR);
        clearSpectatorEffects(spectator);

        spectatePlayer(spectator, target);

        sendSpectatorStatus(spectator);
    }

    @SuppressWarnings("deprecation")
    private static void enterBaseSpectate(ServerPlayer spectator) {
        spectatorModes.put(spectator.getUUID(), SpectatorMode.BASE_SPECTATE);

        spectator.setGameMode(GameType.ADVENTURE);
        spectator.setInvulnerable(true);
        spectator.noPhysics = true;
        spectator.getAbilities().mayfly = true;
        spectator.getAbilities().flying = true;
        spectator.onUpdateAbilities();
        applySpectatorEffects(spectator);
        spectatorTargets.remove(spectator.getUUID());
        spectator.setCamera(spectator);

        sendSpectatorStatus(spectator);

        String playerUUID = spectator.getUUID().toString();
        int teamId = TeamManager.getTeamId(TeamManager.getPlayerCurrentTeam(playerUUID));
        if (teamId > 0) {
            String spawnData = TeamData.getSpawn(teamId);
            if (spawnData != null) {
                BlockPos spawn = parseBlockPos(spawnData);
                if (spawn != null) {
                    spectator.teleportTo(spectator.level(), spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, Set.of(), spectator.getYRot(), spectator.getXRot(), false);
                }
            }
        }

        spectator.sendSystemMessage(Component.literal(
                "§7Aucun coéquipier en vie. Vous êtes à votre base en attente de respawn.\n" +
                "§7Vous pouvez vous déplacer librement dans la zone."));
    }

    private static void enterFreeSpectate(ServerPlayer spectator) {
        spectatorModes.put(spectator.getUUID(), SpectatorMode.FREE_SPECTATE);

        spectator.setGameMode(GameType.SPECTATOR);
        clearSpectatorEffects(spectator);
        spectatorTargets.remove(spectator.getUUID());
        spectator.setCamera(spectator);

        sendSpectatorStatus(spectator);

        // Teleport to world spawn as a neutral vantage point
        BlockPos worldSpawn = spectator.level().getRespawnData().pos();
        spectator.teleportTo(spectator.level(), worldSpawn.getX() + 0.5, 100, worldSpawn.getZ() + 0.5, Set.of(), spectator.getYRot(), spectator.getXRot(), false);

        spectator.sendSystemMessage(Component.literal(
                "§7Mode spectateur libre. Vous pouvez voler mais ne pouvez pas entrer dans les bases."));
    }

    // ──────────────────────────────────────────────
    // Zone enforcement
    // ──────────────────────────────────────────────

    private static void enforceBaseZone(ServerPlayer spectator) {
        String teamName = TeamManager.getPlayerCurrentTeam(spectator.getUUID().toString());
        if (teamName == null) return;
        int teamId = TeamData.getTeamId(teamName);
        if (teamId <= 0) return;

        BlockPos pos = spectator.blockPosition();
        BlockPos clamped = clampToZone(pos, teamId);

        if (clamped != null && !clamped.equals(pos)) {
            spectator.teleportTo(spectator.level(), clamped.getX() + 0.5, clamped.getY(), clamped.getZ() + 0.5, Set.of(), spectator.getYRot(), spectator.getXRot(), false);
            spectator.sendSystemMessage(Component.literal("§cVous ne pouvez pas quitter votre zone !"), true);
        }
    }

    private static void enforceNoBaseZone(ServerPlayer spectator) {
        BlockPos pos = spectator.blockPosition();

        for (String otherTeam : TeamData.getAllTeamNames()) {
            int otherId = TeamData.getTeamId(otherTeam);
            if (otherId <= 0) continue;

            if (isInsideAnyZone(pos, otherId)) {
                BlockPos worldSpawn = spectator.level().getRespawnData().pos();
                spectator.teleportTo(spectator.level(), worldSpawn.getX() + 0.5, 100, worldSpawn.getZ() + 0.5, Set.of(), spectator.getYRot(), spectator.getXRot(), false);
                spectator.sendSystemMessage(Component.literal("§cVous ne pouvez pas entrer dans les bases !"), true);
                return;
            }
        }
    }

    /**
     * Returns the position clamped to the team's FCZ (or fallback spawn radius),
     * or {@code null} if no zone is defined at all.
     */
    private static BlockPos clampToZone(BlockPos pos, int teamId) {
        String minStr = TeamData.getFreeCamZoneMin(teamId);
        String maxStr = TeamData.getFreeCamZoneMax(teamId);

        if (minStr != null && maxStr != null) {
            FreeCamZone zone = new FreeCamZone(parseBlockPos(minStr), parseBlockPos(maxStr));
            return zone.clamp(pos);
        }

        // Fallback: 30‑block radius from spawn
        String spawnStr = TeamData.getSpawn(teamId);
        if (spawnStr != null) {
            BlockPos spawn = parseBlockPos(spawnStr);
            if (spawn != null) {
                int dx = pos.getX() - spawn.getX();
                int dz = pos.getZ() - spawn.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > DEFAULT_FCZ_RADIUS) {
                    double ratio = DEFAULT_FCZ_RADIUS / dist;
                    return new BlockPos(
                            (int) Math.round(spawn.getX() + dx * ratio),
                            pos.getY(),
                            (int) Math.round(spawn.getZ() + dz * ratio)
                    );
                }
            }
        }

        return pos; // inside zone, no clamp
    }

    private static boolean isInsideAnyZone(BlockPos pos, int teamId) {
        String minStr = TeamData.getFreeCamZoneMin(teamId);
        String maxStr = TeamData.getFreeCamZoneMax(teamId);

        if (minStr != null && maxStr != null) {
            FreeCamZone zone = new FreeCamZone(parseBlockPos(minStr), parseBlockPos(maxStr));
            return zone.isInside(pos);
        }

        String spawnStr = TeamData.getSpawn(teamId);
        if (spawnStr != null) {
            BlockPos spawn = parseBlockPos(spawnStr);
            if (spawn != null) {
                int dx = Math.abs(pos.getX() - spawn.getX());
                int dz = Math.abs(pos.getZ() - spawn.getZ());
                return dx <= DEFAULT_FCZ_RADIUS && dz <= DEFAULT_FCZ_RADIUS;
            }
        }

        return false;
    }

    // ──────────────────────────────────────────────
    // Respawn timer (phase 1)
    // ──────────────────────────────────────────────

    private static void schedulePhase1Respawn(ServerPlayer player) {
        int deaths = PlayerStatsData.getDeaths(player.getUUID().toString());
        int delaySeconds = Math.min(deaths * 3, RESPAWN_DELAY_MAX);

        for (int i = 0; i < delaySeconds; i++) {
            final int secondsLeft = delaySeconds - i;
            TaskScheduler.schedule(i * 20, () -> {
                if (player.hasDisconnected()) return;
                player.sendSystemMessage(Component.literal(
                        "§eRespawn dans §c" + secondsLeft + "§e seconde" + (secondsLeft > 1 ? "s" : "")
                ), true);
            });
        }

        TaskScheduler.schedule(delaySeconds * 20, () -> {
            if (player.hasDisconnected()) return;
            if (spectatingPlayers.contains(player.getUUID())) {
                respawnPlayer(player);
                player.sendSystemMessage(Component.literal("§aVous avez été respawn !"));
            }
        });
    }

    // ──────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────

    public static void spectatePlayer(ServerPlayer spectator, ServerPlayer target) {
        if (target == null) {
            spectator.setCamera(spectator);
            spectatorTargets.remove(spectator.getUUID());
            return;
        }

        String spectatorTeam = TeamManager.getPlayerCurrentTeam(spectator.getUUID().toString());
        String targetTeam = TeamManager.getPlayerCurrentTeam(target.getUUID().toString());

        if (spectatorTeam == null || !spectatorTeam.equals(targetTeam)) {
            LOGGER.warn("Spectator {} tried to watch enemy player {}",
                    spectator.getName().getString(),
                    target.getName().getString());
            spectator.sendSystemMessage(Component.literal("§cVous ne pouvez pas spectater un joueur ennemi !"));
            return;
        }

        spectator.setCamera(target);
        spectatorTargets.put(spectator.getUUID(), target.getUUID());
        spectatorModes.put(spectator.getUUID(), SpectatorMode.TEAMMATE_WATCH);
        spectator.sendSystemMessage(Component.literal("§eSpectate : §a" + target.getName().getString()), true);
    }

    public static void switchTeammate(ServerPlayer spectator, boolean next) {
        if (spectatorModes.get(spectator.getUUID()) != SpectatorMode.TEAMMATE_WATCH) {
            spectator.sendSystemMessage(Component.literal("§cAucun coéquipier en vie à spectater !"), true);
            return;
        }

        String spectatorTeam = TeamManager.getPlayerCurrentTeam(spectator.getUUID().toString());
        if (spectatorTeam == null) return;

        List<ServerPlayer> teammates = getAliveTeammates(spectator);
        if (teammates.isEmpty()) {
            spectator.sendSystemMessage(Component.literal("§cAucun coéquipier en vie !"), true);
            enterBaseSpectate(spectator);
            return;
        }

        UUID currentTarget = spectatorTargets.get(spectator.getUUID());
        int currentIndex = -1;

        for (int i = 0; i < teammates.size(); i++) {
            if (teammates.get(i).getUUID().equals(currentTarget)) {
                currentIndex = i;
                break;
            }
        }

        int newIndex;
        if (next) {
            newIndex = (currentIndex + 1) % teammates.size();
        } else {
            newIndex = (currentIndex - 1 + teammates.size()) % teammates.size();
        }

        spectatePlayer(spectator, teammates.get(newIndex));
    }

    private static void sendSpectatorStatus(ServerPlayer player) {
        SpectatorMode mode = spectatorModes.get(player.getUUID());
        if (mode == null) {
            PacketDistributor.sendToPlayer(player, new SpectatorStatusPayload(SpectatorStatusPayload.MODE_NONE, ""));
            return;
        }
        int modeId = switch (mode) {
            case TEAMMATE_WATCH -> SpectatorStatusPayload.MODE_TEAMMATE_WATCH;
            case BASE_SPECTATE -> SpectatorStatusPayload.MODE_BASE_SPECTATE;
            case FREE_SPECTATE -> SpectatorStatusPayload.MODE_FREE_SPECTATE;
        };
        UUID targetId = spectatorTargets.get(player.getUUID());
        String targetName = "";
        if (targetId != null) {
            ServerPlayer target = player.level().getServer().getPlayerList().getPlayer(targetId);
            if (target != null) targetName = target.getName().getString();
        }
        PacketDistributor.sendToPlayer(player, new SpectatorStatusPayload(modeId, targetName));
    }

    /**
     * Force-respawns a player from any spectator mode.
     * Returns true if the player was successfully respawned.
     */
    public static boolean respawnPlayer(ServerPlayer player) {
        if (!spectatingPlayers.contains(player.getUUID())) return false;

        spectatingPlayers.remove(player.getUUID());
        spectatorTargets.remove(player.getUUID());
        spectatorModes.remove(player.getUUID());

        PacketDistributor.sendToPlayer(player, new SpectatorStatusPayload(SpectatorStatusPayload.MODE_NONE, ""));

        player.setInvulnerable(false);
        clearSpectatorEffects(player);

        String teamName = TeamManager.getPlayerCurrentTeam(player.getUUID().toString());
        if (teamName != null) {
            int teamId = TeamManager.getTeamId(teamName);
            String spawnData = TeamData.getSpawn(teamId);
            if (spawnData != null) {
                BlockPos spawnPos = parseBlockPos(spawnData);
                if (spawnPos != null) {
                    player.teleportTo(player.level(), spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, Set.of(), player.getYRot(), player.getXRot(), false);
                }
            }
        }

        player.setGameMode(GameType.SURVIVAL);
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);

        restorePlayerInventory(player);

        return true;
    }

    public static void respawnAllSpectators() {
        Set<UUID> toRespawn = new HashSet<>(spectatingPlayers);
        for (UUID uuid : toRespawn) {
            ServerPlayer player = getPlayerByUUID(uuid);
            if (player != null) respawnPlayer(player);
        }
        spectatingPlayers.clear();
        spectatorTargets.clear();
        spectatorModes.clear();
    }

    /**
     * Called at day start in phase 2. Respawns spectators whose team core is
     * still alive; others enter free spectate mode.
     */
    public static void handlePhase2DayStart() {
        Set<UUID> toRespawn = new HashSet<>();
        for (UUID uuid : spectatingPlayers) {
            int teamId = TeamManager.getTeamId(TeamManager.getPlayerCurrentTeam(uuid.toString()));
            ServerPlayer player = getPlayerByUUID(uuid);
            if (player == null) continue;

            if (teamId > 0 && !TeamData.isCoreDestroyed(teamId)) {
                toRespawn.add(uuid);
            } else {
                enterFreeSpectate(player);
                player.sendSystemMessage(Component.literal(
                        "§cVotre cœur est détruit, vous restez en spectateur libre."));
            }
        }

        for (UUID uuid : toRespawn) {
            ServerPlayer player = getPlayerByUUID(uuid);
            if (player != null) {
                respawnPlayer(player);
                player.sendSystemMessage(Component.literal("§aUn nouveau jour commence, vous avez été respawn !"));
            }
        }
    }

    public static boolean isSpectating(ServerPlayer player) {
        return spectatingPlayers.contains(player.getUUID());
    }

    public static int getSpectatorCount() {
        return spectatingPlayers.size();
    }

    public static Map<String, Integer> getSpectatorsByTeam() {
        Map<String, Integer> counts = new HashMap<>();
        for (UUID uuid : spectatingPlayers) {
            ServerPlayer player = getPlayerByUUID(uuid);
            if (player != null) {
                String team = TeamManager.getPlayerCurrentTeam(uuid.toString());
                if (team != null) {
                    counts.put(team, counts.getOrDefault(team, 0) + 1);
                }
            }
        }
        return counts;
    }

    // ──────────────────────────────────────────────
    // Internal helpers
    // ──────────────────────────────────────────────

    /**
     * Finds a teammate who is alive (SURVIVAL mode) on the spectator's team.
     */
    private static ServerPlayer findAliveTeammate(ServerPlayer spectator) {
        List<ServerPlayer> teammates = getAliveTeammates(spectator);
        return teammates.isEmpty() ? null : teammates.get(0);
    }

    /**
     * Returns all teammates (excluding self) who are in SURVIVAL mode.
     */
    private static List<ServerPlayer> getAliveTeammates(ServerPlayer spectator) {
        String spectatorTeam = TeamManager.getPlayerCurrentTeam(spectator.getUUID().toString());
        if (spectatorTeam == null) return Collections.emptyList();

        List<ServerPlayer> teammates = new ArrayList<>();
        for (ServerPlayer player : spectator.level().getServer().getPlayerList().getPlayers()) {
            if (player.getUUID().equals(spectator.getUUID())) continue;
            if (player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) continue;

            String playerTeam = TeamManager.getPlayerCurrentTeam(player.getUUID().toString());
            if (spectatorTeam.equals(playerTeam)) {
                teammates.add(player);
            }
        }
        return teammates;
    }

    private static void applySpectatorEffects(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, -1, 0, true, false));
    }

    private static void clearSpectatorEffects(ServerPlayer player) {
        player.removeEffect(MobEffects.INVISIBILITY);
    }

    private static BlockPos parseBlockPos(String coords) {
        try {
            String[] parts = coords.split(",");
            return new BlockPos(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim())
            );
        } catch (Exception e) {
            LOGGER.error("Invalid BlockPos string: {}", coords);
            return null;
        }
    }

    private static ServerPlayer getPlayerByUUID(UUID uuid) {
        assert net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer() != null;
        for (ServerPlayer player : net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            if (player.getUUID().equals(uuid)) return player;
        }
        return null;
    }
}

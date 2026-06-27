package com.mceteams.xiidays.commands;

import com.mceteams.xiidays.data.DataManager;
import com.mceteams.xiidays.data.DayCycleData;
import com.mceteams.xiidays.data.TeamData;
import com.mceteams.xiidays.game.DaysManager;
import com.mceteams.xiidays.game.ScoreboardManager;
import com.mceteams.xiidays.game.TeamManager;
import com.mceteams.xiidays.network.OpenAdminMenuPacket;
import com.mceteams.xiidays.network.OpenScoreboardPacket;
import com.mceteams.xiidays.network.PacketHandler;
import com.mceteams.xiidays.restriction.RestrictionsManager;
import com.mceteams.xiidays.spectator.SpectateManager;
import com.mceteams.xiidays.visual.ZoneVisualizer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvents;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.Objects;

public class XiidaysCommand {

    private static final SuggestionProvider<CommandSourceStack> TEAM_SUGGESTIONS =
            (ctx, builder) -> {
                for (String team : TeamData.getAllTeamNames()) {
                    builder.suggest(team);
                }
                return builder.buildFuture();
            };

    private static final SuggestionProvider<CommandSourceStack> TEAM_MANAGER_SUGGESTIONS =
            (ctx, builder) -> {
                for (String team : TeamManager.getAllTeams()) {
                    builder.suggest(team);
                }
                return builder.buildFuture();
            };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = dispatcher.register(
                Commands.literal("xiidays")
                        // ──────────────────────────────────────
                        //  DAY
                        // ──────────────────────────────────────
                        .then(Commands.literal("day")
                                .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_OWNER))
                                .then(Commands.literal("start")
                                        .executes(ctx -> {
                                            if (!DaysManager.start(ctx)) {
                                                Objects.requireNonNull(ctx.getSource().getPlayer()).playSound(
                                                        SoundEvents.NOTE_BLOCK_BASS.value(), 1f, 0.5f);
                                                ctx.getSource().sendSystemMessage(Component.literal("§cUne erreur s'est produite lors du démarrage du jour."));
                                                return 0;
                                            }
                                            return 1;
                                        })
                                )
                                .then(Commands.literal("stop")
                                        .executes(ctx -> {
                                            if (!DaysManager.stop(ctx)) {
                                                Objects.requireNonNull(ctx.getSource().getPlayer()).playSound(
                                                        SoundEvents.NOTE_BLOCK_BASS.value(), 1f, 1f);
                                                ctx.getSource().sendSystemMessage(Component.literal("§cUne erreur s'est produite lors de l'arrêt du jour."));
                                                return 0;
                                            }
                                            return 1;
                                        })
                                )
                                .then(Commands.literal("status")
                                        .executes(ctx -> {
                                            int day = DayCycleData.getCurrentDay();
                                            boolean running = DayCycleData.isInProgress();
                                            Objects.requireNonNull(ctx.getSource().getPlayer()).playSound(
                                                    SoundEvents.NOTE_BLOCK_HAT.value(), 1f, 1f);
                                            ctx.getSource().sendSystemMessage(Component.literal(
                                                    "§7Jour : §e" + day + " §7| En cours : " + (running ? "§a✓" : "§c✗")));
                                            return 1;
                                        })
                                )
                                .then(Commands.literal("set")
                                        .then(Commands.argument("day", IntegerArgumentType.integer(1, 12))
                                                .executes(ctx -> {
                                                    int value = IntegerArgumentType.getInteger(ctx, "day");
                                                    int oldDay = DayCycleData.getCurrentDay();
                                                    DayCycleData.setCurrentDay(value);
                                                    ctx.getSource().sendSystemMessage(Component.literal(
                                                            "§7Jour modifié : §c" + oldDay + " §7→ §a" + value));
                                                    if (DayCycleData.isInProgress()) {
                                                        ctx.getSource().getServer().getPlayerList().broadcastSystemMessage(
                                                                Component.literal("§7Le jour actuel passe au §a§l" + value + "§7 jour"), false);
                                                        for (Player p : ctx.getSource().getServer().getPlayerList().getPlayers()) {
                                                            p.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 1f, 1f);
                                                        }
                                                    }
                                                    return 1;
                                                })
                                        )
                                )
                        )
                        // ──────────────────────────────────────
                        //  TEAM
                        // ──────────────────────────────────────
                        .then(Commands.literal("team")
                                .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_OWNER))
                                .then(Commands.literal("create")
                                        .then(Commands.argument("name", StringArgumentType.string())
                                                .executes(ctx -> {
                                                    String name = StringArgumentType.getString(ctx, "name");
                                                    if (name.length() < 3) {
                                                        Objects.requireNonNull(ctx.getSource().getPlayer()).playSound(
                                                                SoundEvents.NOTE_BLOCK_BASS.value(), 1f, .5f);
                                                        ctx.getSource().sendSystemMessage(Component.literal("§cNom trop court (min 3 caractères)"));
                                                        return 1;
                                                    }
                                                    int result = TeamManager.createTeam(name);
                                                    if (result == 1) {
                                                        Objects.requireNonNull(ctx.getSource().getPlayer()).playSound(
                                                                SoundEvents.NOTE_BLOCK_PLING.value(), 1f, 2f);
                                                        ctx.getSource().sendSystemMessage(Component.literal("§aÉquipe \"" + name + "\" créée !"));
                                                    } else if (result == 2) {
                                                        Objects.requireNonNull(ctx.getSource().getPlayer()).playSound(
                                                                SoundEvents.NOTE_BLOCK_BASS.value(), 1f, .5f);
                                                        ctx.getSource().sendSystemMessage(Component.literal("§cL'équipe \"" + name + "\" existe déjà"));
                                                    }
                                                    return 1;
                                                })
                                        )
                                )
                                .then(Commands.literal("remove")
                                        .then(Commands.literal("member")
                                                .then(Commands.argument("team", StringArgumentType.string()).suggests(TEAM_MANAGER_SUGGESTIONS)
                                                        .then(Commands.argument("player", EntityArgument.player())
                                                                .executes(ctx -> removeMember(ctx))
                                                        )
                                                )
                                        )
                                        .then(Commands.literal("team")
                                                .then(Commands.argument("team", StringArgumentType.string()).suggests(TEAM_MANAGER_SUGGESTIONS)
                                                        .executes(ctx -> {
                                                            String teamName = StringArgumentType.getString(ctx, "team");
                                                            TeamManager.remTeam(teamName);
                                                            ctx.getSource().sendSystemMessage(Component.literal("§aÉquipe \"" + teamName + "\" supprimée"));
                                                            return 1;
                                                        })
                                                )
                                        )
                                )
                                .then(Commands.literal("eliminate")
                                        .then(Commands.argument("team", StringArgumentType.string()).suggests(TEAM_MANAGER_SUGGESTIONS)
                                                .executes(ctx -> {
                                                    String teamName = StringArgumentType.getString(ctx, "team");
                                                    if (TeamManager.eliminateTeam(teamName)) {
                                                        ctx.getSource().sendSystemMessage(Component.literal("§aÉquipe " + teamName + " éliminée !"));
                                                        return 1;
                                                    } else {
                                                        ctx.getSource().sendFailure(Component.literal("§cImpossible d'éliminer l'équipe"));
                                                        return 0;
                                                    }
                                                })
                                        )
                                )
                                .then(Commands.literal("revive")
                                        .then(Commands.argument("team", StringArgumentType.string()).suggests(TEAM_MANAGER_SUGGESTIONS)
                                                .executes(ctx -> {
                                                    String teamName = StringArgumentType.getString(ctx, "team");
                                                    if (TeamManager.reviveTeam(teamName)) {
                                                        ctx.getSource().sendSystemMessage(Component.literal("§aÉquipe " + teamName + " réhabilitée !"));
                                                        return 1;
                                                    } else {
                                                        ctx.getSource().sendFailure(Component.literal("§cImpossible de réhabiliter l'équipe (déjà en vie ou inexistante)"));
                                                        return 0;
                                                    }
                                                })
                                        )
                                )
                                .then(Commands.literal("add")
                                        .then(Commands.argument("team", StringArgumentType.string()).suggests(TEAM_MANAGER_SUGGESTIONS)
                                                .then(Commands.argument("player", EntityArgument.player())
                                                        .executes(ctx -> addMember(ctx))
                                                )
                                        )
                                )
                                .then(Commands.literal("spawn")
                                        .then(Commands.argument("team", StringArgumentType.string()).suggests(TEAM_MANAGER_SUGGESTIONS)
                                                .executes(ctx -> {
                                                    ServerPlayer sender = ctx.getSource().getPlayerOrException();
                                                    String teamName = StringArgumentType.getString(ctx, "team");
                                                    BlockPos pos = sender.getOnPos();
                                                    int result = TeamManager.placeTeamSpawn(teamName, pos, ctx.getSource().getLevel());
                                                    return handleSpawnResult(ctx, teamName, result);
                                                })
                                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                        .executes(ctx -> {
                                                            String teamName = StringArgumentType.getString(ctx, "team");
                                                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                                                            int result = TeamManager.placeTeamSpawn(teamName, pos, ctx.getSource().getLevel());
                                                            return handleSpawnResult(ctx, teamName, result);
                                                        })
                                                )
                                        )
                                )
                                .then(Commands.literal("core")
                                        .then(Commands.argument("team", StringArgumentType.string()).suggests(TEAM_MANAGER_SUGGESTIONS)
                                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                        .executes(ctx -> {
                                                            String teamName = StringArgumentType.getString(ctx, "team");
                                                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                                                            int result = TeamManager.placeTeamCore(teamName, pos, ctx.getSource().getLevel());
                                                            return handleCoreResult(ctx, teamName, result);
                                                        })
                                                )
                                        )
                                )
                                .then(Commands.literal("list")
                                        .executes(ctx -> {
                                            String[] teams = TeamManager.getAllTeams();
                                            if (teams.length == 0) {
                                                ctx.getSource().sendSystemMessage(Component.literal("§7Aucune équipe"));
                                            } else {
                                                ctx.getSource().sendSystemMessage(Component.literal("§6=== Équipes (" + teams.length + ") ==="));
                                                for (String t : teams) {
                                                    int id = TeamData.getTeamId(t);
                                                    ctx.getSource().sendSystemMessage(Component.literal(
                                                            " §e" + t + " §7(ID: " + id + ")"));
                                                }
                                            }
                                            return 1;
                                        })
                                )
                        )
                        // ──────────────────────────────────────
                        //  SPEC  (spectator management)
                        // ──────────────────────────────────────
                        .then(Commands.literal("spec")
                                .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_OWNER))
                                .then(Commands.literal("respawn")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                                    ServerPlayer sender = ctx.getSource().getPlayerOrException();
                                                    if (!SpectateManager.isSpectating(target)) {
                                                        sender.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1f, 0.5f);
                                                        ctx.getSource().sendFailure(Component.literal("§cLe joueur n'est pas en mode spectateur"));
                                                        return 0;
                                                    }
                                                    if (SpectateManager.respawnPlayer(target)) {
                                                        sender.playSound(SoundEvents.PLAYER_LEVELUP, 1f, 2f);
                                                        ctx.getSource().sendSystemMessage(Component.literal("§a" + target.getName().getString() + " a été respawn"));
                                                        target.sendSystemMessage(Component.literal("§aVous avez été respawn par un administrateur"));
                                                        return 1;
                                                    } else {
                                                        sender.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1f, 0.5f);
                                                        ctx.getSource().sendFailure(Component.literal("§cImpossible de respawn le joueur"));
                                                        return 0;
                                                    }
                                                })
                                        )
                                )
                                .then(Commands.literal("respawnall")
                                        .executes(ctx -> {
                                            ServerPlayer sender = ctx.getSource().getPlayerOrException();
                                            int count = SpectateManager.getSpectatorCount();
                                            if (count == 0) {
                                                sender.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1f, 0.5f);
                                                ctx.getSource().sendSystemMessage(Component.literal("§cAucun spectateur à respawn"));
                                                return 0;
                                            }
                                            SpectateManager.respawnAllSpectators();
                                            sender.playSound(SoundEvents.PLAYER_LEVELUP, 1f, 2f);
                                            ctx.getSource().getServer().getPlayerList().broadcastSystemMessage(
                                                    Component.literal("§aTous les spectateurs ont été respawn"), false);
                                            return 1;
                                        })
                                )
                                .then(Commands.literal("list")
                                        .executes(ctx -> {
                                            ServerPlayer sender = ctx.getSource().getPlayerOrException();
                                            int total = SpectateManager.getSpectatorCount();
                                            if (total == 0) {
                                                sender.playSound(SoundEvents.NOTE_BLOCK_HAT.value(), 1f, 1f);
                                                ctx.getSource().sendSystemMessage(Component.literal("§eAucun spectateur actuellement"));
                                                return 1;
                                            }
                                            Map<String, Integer> byTeam = SpectateManager.getSpectatorsByTeam();
                                            sender.playSound(SoundEvents.NOTE_BLOCK_HAT.value(), 1f, 1f);
                                            ctx.getSource().sendSystemMessage(Component.literal("§e=== Spectateurs (" + total + ") ==="));
                                            for (Map.Entry<String, Integer> e : byTeam.entrySet()) {
                                                ctx.getSource().sendSystemMessage(Component.literal(" §7" + e.getKey() + " : §f" + e.getValue() + " joueur(s)"));
                                            }
                                            return 1;
                                        })
                                )
                        )
                        // ──────────────────────────────────────
                        //  ZONE  (FCZ management + visualization)
                        // ──────────────────────────────────────
                        .then(Commands.literal("zone")
                                .then(Commands.literal("set")
                                        .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_OWNER))
                                        .then(Commands.argument("team", StringArgumentType.string()).suggests(TEAM_SUGGESTIONS)
                                                .then(Commands.argument("pos1", BlockPosArgument.blockPos())
                                                        .then(Commands.argument("pos2", BlockPosArgument.blockPos())
                                                                .executes(ctx -> {
                                                                    ServerPlayer sender = ctx.getSource().getPlayerOrException();
                                                                    String teamName = StringArgumentType.getString(ctx, "team");
                                                                    BlockPos p1 = BlockPosArgument.getLoadedBlockPos(ctx, "pos1");
                                                                    BlockPos p2 = BlockPosArgument.getLoadedBlockPos(ctx, "pos2");
                                                                    if (!TeamData.teamExists(teamName)) {
                                                                        sender.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1f, 0.5f);
                                                                        ctx.getSource().sendFailure(Component.literal("§cL'équipe \"" + teamName + "\" n'existe pas !"));
                                                                        return 0;
                                                                    }
                                                                    int teamId = TeamData.getTeamId(teamName);
                                                                    int x1 = Math.min(p1.getX(), p2.getX()), y1 = Math.min(p1.getY(), p2.getY()), z1 = Math.min(p1.getZ(), p2.getZ());
                                                                    int x2 = Math.max(p1.getX(), p2.getX()), y2 = Math.max(p1.getY(), p2.getY()), z2 = Math.max(p1.getZ(), p2.getZ());
                                                                    TeamData.setFreeCamZoneMin(teamId, x1 + "," + y1 + "," + z1);
                                                                    TeamData.setFreeCamZoneMax(teamId, x2 + "," + y2 + "," + z2);
                                                                    int sx = x2 - x1 + 1, sy = y2 - y1 + 1, sz = z2 - z1 + 1;
                                                                    sender.playSound(SoundEvents.PLAYER_LEVELUP, 1f, 2f);
                                                                    ctx.getSource().sendSystemMessage(Component.literal(
                                                                            "§a§lZone définie pour \"" + teamName + "\"\n" +
                                                                                    "§7Coin 1 : §e" + x1 + ", " + y1 + ", " + z1 + "\n" +
                                                                                    "§7Coin 2 : §e" + x2 + ", " + y2 + ", " + z2 + "\n" +
                                                                                    "§7Dimensions : §b" + sx + "§7×§b" + sy + "§7×§b" + sz + " blocs\n" +
                                                                                    "§7Volume : §b" + (sx * sy * sz) + " blocs³"));
                                                                    return 1;
                                                                })
                                                        )
                                                )
                                        )
                                )
                                .then(Commands.literal("remove")
                                        .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_OWNER))
                                        .then(Commands.argument("team", StringArgumentType.string()).suggests(TEAM_SUGGESTIONS)
                                                .executes(ctx -> {
                                                    ServerPlayer sender = ctx.getSource().getPlayerOrException();
                                                    String teamName = StringArgumentType.getString(ctx, "team");
                                                    if (!TeamData.teamExists(teamName)) {
                                                        sender.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1f, 0.5f);
                                                        ctx.getSource().sendFailure(Component.literal("§cL'équipe \"" + teamName + "\" n'existe pas !"));
                                                        return 0;
                                                    }
                                                    int teamId = TeamData.getTeamId(teamName);
                                                    TeamData.setFreeCamZoneMin(teamId, null);
                                                    TeamData.setFreeCamZoneMax(teamId, null);
                                                    sender.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1f, 1f);
                                                    ctx.getSource().sendSystemMessage(Component.literal("§aZone supprimée pour \"" + teamName + "\""));
                                                    return 1;
                                                })
                                        )
                                )
                                .then(Commands.literal("show")
                                        .executes(ctx -> {
                                            ServerPlayer sender = ctx.getSource().getPlayerOrException();
                                            ZoneVisualizer.startViewing(sender, -1);
                                            sender.playSound(SoundEvents.PLAYER_LEVELUP, 1f, 2f);
                                            ctx.getSource().sendSystemMessage(Component.literal("§aAffichage de toutes les zones activé"));
                                            return 1;
                                        })
                                        .then(Commands.argument("team", StringArgumentType.string()).suggests(TEAM_SUGGESTIONS)
                                                .executes(ctx -> {
                                                    ServerPlayer sender = ctx.getSource().getPlayerOrException();
                                                    String teamName = StringArgumentType.getString(ctx, "team");
                                                    if (!TeamData.teamExists(teamName)) {
                                                        sender.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1f, 0.5f);
                                                        ctx.getSource().sendFailure(Component.literal("§cL'équipe \"" + teamName + "\" n'existe pas !"));
                                                        return 0;
                                                    }
                                                    int teamId = TeamData.getTeamId(teamName);
                                                    ZoneVisualizer.startViewing(sender, teamId);
                                                    sender.playSound(SoundEvents.PLAYER_LEVELUP, 1f, 2f);
                                                    ctx.getSource().sendSystemMessage(Component.literal("§aAffichage de la zone de \"" + teamName + "\" activé"));
                                                    return 1;
                                                })
                                        )
                                )
                                .then(Commands.literal("hide")
                                        .executes(ctx -> {
                                            ServerPlayer sender = ctx.getSource().getPlayerOrException();
                                            ZoneVisualizer.stopViewing(sender);
                                            sender.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1f, 1f);
                                            ctx.getSource().sendSystemMessage(Component.literal("§eAffichage des zones désactivé"));
                                            return 1;
                                        })
                                )
                        )
                        // ──────────────────────────────────────
                        //  RESTRICT  (items / blocks / bypass)
                        // ──────────────────────────────────────
                        .then(Commands.literal("restrict")
                                .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_OWNER))
                                .then(Commands.literal("status")
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            ItemStack held = player.getMainHandItem();
                                            if (held.isEmpty()) {
                                                ctx.getSource().sendSystemMessage(Component.literal("§cVous ne tenez rien en main."));
                                                return 0;
                                            }
                                            Item heldItem = held.getItem();
                                            boolean allowed = RestrictionsManager.isAllowedCompletely(heldItem);
                                            Component status = RestrictionsManager.getStatusComponent(heldItem);
                                            ServerLevel level = player.level();
                                            double x = player.getX(), y = player.getY() + 1.5, z = player.getZ();
                                            if (allowed) {
                                                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 10, 0.3, 0.5, 0.3, 0.01);
                                                player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
                                            } else {
                                                level.sendParticles(ParticleTypes.ANGRY_VILLAGER, x, y, z, 10, 0.3, 0.5, 0.3, 0.01);
                                                player.playSound(SoundEvents.ANVIL_BREAK, 1f, 1f);
                                            }
                                            ctx.getSource().sendSystemMessage(Component.literal("§eStatut : ").append(status));
                                            return 1;
                                        })
                                )
                                .then(Commands.literal("items")
                                        .then(Commands.literal("disallow")
                                                .executes(ctx -> restrictItem(ctx, false))
                                        )
                                        .then(Commands.literal("allow")
                                                .executes(ctx -> restrictItem(ctx, true))
                                        )
                                )
                                .then(Commands.literal("blocks")
                                        .then(Commands.literal("disallow")
                                                .executes(ctx -> restrictBlockHeld(ctx, false))
                                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                        .executes(ctx -> restrictBlockPos(ctx, false))
                                                )
                                        )
                                        .then(Commands.literal("allow")
                                                .executes(ctx -> restrictBlockHeld(ctx, true))
                                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                        .executes(ctx -> restrictBlockPos(ctx, true))
                                                )
                                        )
                                )
                                .then(Commands.literal("bypass")
                                        .executes(ctx -> toggleSelfBypass(ctx))
                                        .then(Commands.literal("on")
                                                .executes(ctx -> { toggleSelfBypassForce(ctx, true); return 1; })
                                        )
                                        .then(Commands.literal("off")
                                                .executes(ctx -> { toggleSelfBypassForce(ctx, false); return 1; })
                                        )
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> toggleOtherBypass(ctx))
                                                .then(Commands.literal("on")
                                                        .executes(ctx -> { toggleOtherBypassForce(ctx, true); return 1; })
                                                )
                                                .then(Commands.literal("off")
                                                        .executes(ctx -> { toggleOtherBypassForce(ctx, false); return 1; })
                                                )
                                        )
                                )
                        )
                        // ──────────────────────────────────────
                        //  DATA
                        // ──────────────────────────────────────
                        .then(Commands.literal("data")
                                .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_OWNER))
                                .then(Commands.literal("reload")
                                        .executes(ctx -> {
                                            DataManager.reloadAll();
                                            ctx.getSource().sendSystemMessage(Component.literal("§eDonnées rechargées depuis les fichiers !"));
                                            return 1;
                                        })
                                )
                                .then(Commands.literal("save")
                                        .executes(ctx -> {
                                            DataManager.saveAll();
                                            ctx.getSource().sendSystemMessage(Component.literal("§eDonnées sauvegardées !"));
                                            return 1;
                                        })
                                )
                                .then(Commands.literal("domains")
                                        .executes(ctx -> {
                                            String[] domains = DataManager.getAllDomains();
                                            if (domains.length == 0) {
                                                ctx.getSource().sendSystemMessage(Component.literal("§7Aucun domaine chargé"));
                                            } else {
                                                ctx.getSource().sendSystemMessage(Component.literal("§eDomaines : §f" + String.join(", ", domains)));
                                            }
                                            return 1;
                                        })
                                )
                        )
                        // ──────────────────────────────────────
                        //  SCORE  (ouvre le scoreboard)
                        // ──────────────────────────────────────
                        .then(Commands.literal("score")
                                .requires(CommandSourceStack::isPlayer)
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    ScoreboardManager.ScoreboardData data = ScoreboardManager.getScoreboardData(player);
                                    PacketHandler.sendToClient(new OpenScoreboardPacket(data.teams(), data.playerTeam()), player);
                                    player.playSound(SoundEvents.UI_TOAST_IN, 1f, 2f);
                                    return 1;
                                })
                        )
                        // ──────────────────────────────────────
                        //  ADMIN MENU
                        // ──────────────────────────────────────
                        .then(Commands.literal("admin")
                                .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_OWNER))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    PacketDistributor.sendToPlayer(player, new OpenAdminMenuPacket());
                                    return 1;
                                })
                        )
                        // ──────────────────────────────────────
                        //  HELP (default when no subcommand)
                        // ──────────────────────────────────────
                        .executes(ctx -> {
                            ctx.getSource().sendSystemMessage(Component.literal(
                                    "§6╔══════ XII Days — Commandes ══════╗\n" +
                                            "§e/xiidays day §7start|stop|status|set\n" +
                                            "§e/xiidays team §7create|remove|add|spawn|core|eliminate|revive|list\n" +
                                            "§e/xiidays spec §7respawn <player>|respawnall|list\n" +
                                            "§e/xiidays zone §7set|remove|show|hide\n" +
                                            "§e/xiidays restrict §7status|items|blocks|bypass\n" +
                                            "§e/xiidays data §7reload|save|domains\n" +
                                            "§e/xiidays score §7<ouvre le scoreboard>\n" +
                                            "§e/xiidays admin §7<menu d'administration>\n" +
                                            "§6╚══════════════════════════════════╝"));
                            return 1;
                        })
        );

        // ── Alias /xd → /xiidays ──────────────────────
        dispatcher.register(Commands.literal("xd").redirect(root));
    }

    // ──────────────────────────────────────────────
    //  Team helpers
    // ──────────────────────────────────────────────

    private static int handleSpawnResult(CommandContext<CommandSourceStack> ctx, String teamName, int result) throws CommandSyntaxException {
        return switch (result) {
            case 1 -> {
                Objects.requireNonNull(ctx.getSource().getPlayer()).playSound(
                        SoundEvents.PLAYER_LEVELUP, 1f, 2f);
                ctx.getSource().sendSystemMessage(Component.literal("§aSpawn de \"" + teamName + "\" défini"));
                yield 1;
            }
            case 2 -> {
                Objects.requireNonNull(ctx.getSource().getPlayer()).playSound(
                        SoundEvents.NOTE_BLOCK_BASS.value(), 1f, 0.5f);
                ctx.getSource().sendFailure(Component.literal("§cErreur : BlockEntity invalide"));
                yield 0;
            }
            case 3 -> {
                Objects.requireNonNull(ctx.getSource().getPlayer()).playSound(
                        SoundEvents.NOTE_BLOCK_BASS.value(), 1f, 0.5f);
                ctx.getSource().sendFailure(Component.literal("§cImpossible de placer le spawn"));
                yield 0;
            }
            default -> 0;
        };
    }

    private static int handleCoreResult(CommandContext<CommandSourceStack> ctx, String teamName, int result) throws CommandSyntaxException {
        return switch (result) {
            case 1 -> {
                Objects.requireNonNull(ctx.getSource().getPlayer()).playSound(
                        SoundEvents.PLAYER_LEVELUP, 1f, 2f);
                ctx.getSource().sendSystemMessage(Component.literal("§aCœur de \"" + teamName + "\" défini"));
                yield 1;
            }
            case 2 -> {
                Objects.requireNonNull(ctx.getSource().getPlayer()).playSound(
                        SoundEvents.NOTE_BLOCK_BASS.value(), 1f, 0.5f);
                ctx.getSource().sendFailure(Component.literal("§cErreur : BlockEntity invalide"));
                yield 0;
            }
            case 3 -> {
                Objects.requireNonNull(ctx.getSource().getPlayer()).playSound(
                        SoundEvents.NOTE_BLOCK_BASS.value(), 1f, 0.5f);
                ctx.getSource().sendFailure(Component.literal("§cImpossible de placer le cœur"));
                yield 0;
            }
            default -> 0;
        };
    }

    private static int removeMember(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String teamName = StringArgumentType.getString(ctx, "team");
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        int result = TeamManager.remMember(teamName, player);
        ServerPlayer sender = ctx.getSource().getPlayerOrException();
        return switch (result) {
            case 1 -> {
                sender.playSound(SoundEvents.PLAYER_LEVELUP, 1f, 2f);
                ctx.getSource().sendSystemMessage(Component.literal("§a" + player.getName().getString() + " retiré de " + teamName));
                yield 1;
            }
            case 2 -> {
                sender.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1f, .5f);
                ctx.getSource().sendSystemMessage(Component.literal("§cÉquipe \"" + teamName + "\" inexistante"));
                yield 0;
            }
            default -> {
                sender.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1f, .5f);
                ctx.getSource().sendSystemMessage(Component.literal("§cErreur"));
                yield 0;
            }
        };
    }

    private static int addMember(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String teamName = StringArgumentType.getString(ctx, "team");
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        int result = TeamManager.addMember(teamName, player);
        var sender = ctx.getSource().getPlayerOrException();
        return switch (result) {
            case 1 -> {
                sender.playSound(SoundEvents.PLAYER_LEVELUP, 1f, 2f);
                ctx.getSource().sendSystemMessage(Component.literal("§a" + player.getName().getString() + " ajouté à " + teamName));
                yield 1;
            }
            case 2 -> {
                sender.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1f, .5f);
                ctx.getSource().sendSystemMessage(Component.literal("§cÉquipe \"" + teamName + "\" inexistante"));
                yield 0;
            }
            case 4 -> {
                sender.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1f, .5f);
                ctx.getSource().sendSystemMessage(Component.literal("§c" + player.getName().getString() + " déjà dans cette équipe"));
                yield 0;
            }
            case 5 -> {
                sender.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1f, .5f);
                ctx.getSource().sendSystemMessage(Component.literal("§c" + player.getName().getString() + " déjà dans " + TeamManager.getPlayerCurrentTeam(player.getUUID().toString())));
                yield 0;
            }
            default -> {
                sender.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1f, .5f);
                ctx.getSource().sendSystemMessage(Component.literal("§cErreur inconnue"));
                yield 0;
            }
        };
    }

    // ──────────────────────────────────────────────
    //  Restrict helpers
    // ──────────────────────────────────────────────

    private static int restrictItem(CommandContext<CommandSourceStack> ctx, boolean allow) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Item held = player.getMainHandItem().getItem();
        Identifier key = BuiltInRegistries.ITEM.getKey(held);
        RestrictionsManager.setItemsAccess(allow, held);
        if (held instanceof BlockItem bi) {
            RestrictionsManager.setBlockAccess(allow, bi.getBlock());
            ctx.getSource().sendSystemMessage(Component.literal(
                    "§eBloc associé " + bi.getBlock().getName().getString() + " également " + (allow ? "autorisé" : "interdit")));
        }
        player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1f, 2f);
        ctx.getSource().sendSystemMessage(Component.literal(
                (allow ? "§a" : "§c") + key + (allow ? " autorisé" : " interdit")));
        return 1;
    }

    private static int restrictBlockHeld(CommandContext<CommandSourceStack> ctx, boolean allow) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Item held = player.getMainHandItem().getItem();
        if (held instanceof BlockItem bi) {
            Block block = bi.getBlock();
            RestrictionsManager.setBlockAccess(allow, block);
            RestrictionsManager.setItemsAccess(allow, held);
            player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1f, 2f);
            ctx.getSource().sendSystemMessage(Component.literal(
                    (allow ? "§a" : "§c") + "Bloc " + block.getName().getString() + (allow ? " autorisé" : " interdit")));
            return 1;
        }
        player.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1f, .5f);
        ctx.getSource().sendSystemMessage(Component.literal("§cTenez un bloc en main"));
        return 0;
    }

    private static int restrictBlockPos(CommandContext<CommandSourceStack> ctx, boolean allow) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        Block block = ctx.getSource().getLevel().getBlockState(pos).getBlock();
        RestrictionsManager.setBlockAccess(allow, block);
        Item asItem = block.asItem();
        if (asItem != Items.AIR) {
            RestrictionsManager.setItemsAccess(allow, asItem);
        }
        player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1f, 2f);
        ctx.getSource().sendSystemMessage(Component.literal(
                (allow ? "§a" : "§c") + "Bloc " + block.getName().getString() + (allow ? " autorisé" : " interdit")));
        return 1;
    }

    private static int toggleSelfBypass(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (RestrictionsManager.hasBypass(player)) {
            RestrictionsManager.removeBypassPlayer(player);
            ctx.getSource().sendSystemMessage(Component.literal("§cBypass désactivé"));
        } else {
            RestrictionsManager.addBypassPlayer(player);
            ctx.getSource().sendSystemMessage(Component.literal("§aBypass activé"));
        }
        player.playSound(SoundEvents.PLAYER_LEVELUP, 1f, 2f);
        return 1;
    }

    private static void toggleSelfBypassForce(CommandContext<CommandSourceStack> ctx, boolean on) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (on == RestrictionsManager.hasBypass(player)) {
            player.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1f, .5f);
            ctx.getSource().sendSystemMessage(Component.literal(on ? "§cDéjà en bypass" : "§cPas en bypass"));
            return;
        }
        if (on) RestrictionsManager.addBypassPlayer(player);
        else RestrictionsManager.removeBypassPlayer(player);
        player.playSound(SoundEvents.PLAYER_LEVELUP, 1f, 2f);
        ctx.getSource().sendSystemMessage(Component.literal(on ? "§aBypass activé" : "§cBypass désactivé"));
    }

    private static int toggleOtherBypass(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        ServerPlayer sender = ctx.getSource().getPlayerOrException();
        if (RestrictionsManager.hasBypass(target)) {
            RestrictionsManager.removeBypassPlayer(target);
            sender.playSound(SoundEvents.PLAYER_LEVELUP, 1f, 2f);
            ctx.getSource().sendSystemMessage(Component.literal("§cBypass désactivé pour " + target.getName().getString()));
        } else {
            RestrictionsManager.addBypassPlayer(target);
            sender.playSound(SoundEvents.PLAYER_LEVELUP, 1f, 2f);
            ctx.getSource().sendSystemMessage(Component.literal("§aBypass activé pour " + target.getName().getString()));
        }
        return 1;
    }

    private static void toggleOtherBypassForce(CommandContext<CommandSourceStack> ctx, boolean on) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        ServerPlayer sender = ctx.getSource().getPlayerOrException();
        if (on == RestrictionsManager.hasBypass(target)) {
            sender.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1f, .5f);
            ctx.getSource().sendSystemMessage(Component.literal(
                    on ? "§c" + target.getName().getString() + " déjà en bypass" : "§c" + target.getName().getString() + " pas en bypass"));
            return;
        }
        if (on) RestrictionsManager.addBypassPlayer(target);
        else RestrictionsManager.removeBypassPlayer(target);
        sender.playSound(SoundEvents.PLAYER_LEVELUP, 1f, 2f);
        ctx.getSource().sendSystemMessage(Component.literal(
                on ? "§a" + target.getName().getString() + " est en bypass" : "§c" + target.getName().getString() + " n'est plus en bypass"));
    }
}

package com.mceteams.xiidays.commands;

import com.mceteams.xiidays.utils.ZoneVisualizer;
import com.mceteams.xiidays.utils.data.TeamData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class SpectatorZoneCommand {

    private static final SuggestionProvider<CommandSourceStack> TEAM_SUGGESTIONS =
            (context, builder) -> {
                for (String team : TeamData.getAllTeamNames()) {
                    builder.suggest(team);
                }
                return builder.buildFuture();
            };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("xspectator")
                .requires(CommandSourceStack::isPlayer)

                // ── zone set ──────────────────────────────────────
                .then(Commands.literal("zone")
                        .then(Commands.literal("set")
                                .then(Commands.argument("team", StringArgumentType.string())
                                        .suggests(TEAM_SUGGESTIONS)
                                        .then(Commands.argument("pos1", BlockPosArgument.blockPos())
                                                .then(Commands.argument("pos2", BlockPosArgument.blockPos())
                                                        .executes(ctx -> {
                                                            ServerPlayer sender = ctx.getSource().getPlayerOrException();
                                                            String teamName = StringArgumentType.getString(ctx, "team");
                                                            BlockPos pos1 = BlockPosArgument.getLoadedBlockPos(ctx, "pos1");
                                                            BlockPos pos2 = BlockPosArgument.getLoadedBlockPos(ctx, "pos2");

                                                            if (!TeamData.teamExists(teamName)) {
                                                                sender.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, 0.5f);
                                                                ctx.getSource().sendFailure(Component.literal("§cL'équipe \"" + teamName + "\" n'existe pas !"));
                                                                return 0;
                                                            }

                                                            int teamId = TeamData.getTeamId(teamName);
                                                            int x1 = Math.min(pos1.getX(), pos2.getX());
                                                            int y1 = Math.min(pos1.getY(), pos2.getY());
                                                            int z1 = Math.min(pos1.getZ(), pos2.getZ());
                                                            int x2 = Math.max(pos1.getX(), pos2.getX());
                                                            int y2 = Math.max(pos1.getY(), pos2.getY());
                                                            int z2 = Math.max(pos1.getZ(), pos2.getZ());

                                                            TeamData.setFreeCamZoneMin(teamId, x1 + "," + y1 + "," + z1);
                                                            TeamData.setFreeCamZoneMax(teamId, x2 + "," + y2 + "," + z2);

                                                            int sizeX = x2 - x1 + 1;
                                                            int sizeY = y2 - y1 + 1;
                                                            int sizeZ = z2 - z1 + 1;

                                                            sender.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1f, 2f);
                                                            ctx.getSource().sendSystemMessage(Component.literal(
                                                                    "§a§lZone définie pour \"" + teamName + "\"\n" +
                                                                    "§7Coin 1 : §e" + x1 + ", " + y1 + ", " + z1 + "\n" +
                                                                    "§7Coin 2 : §e" + x2 + ", " + y2 + ", " + z2 + "\n" +
                                                                    "§7Dimensions : §b" + sizeX + "§7×§b" + sizeY + "§7×§b" + sizeZ + " blocs\n" +
                                                                    "§7Volume : §b" + (sizeX * sizeY * sizeZ) + " blocs³"));
                                                            return 1;
                                                        })
                                                )
                                        )
                                )
                        )

                        // ── zone remove ───────────────────────────────
                        .then(Commands.literal("remove")
                                .then(Commands.argument("team", StringArgumentType.string())
                                        .suggests(TEAM_SUGGESTIONS)
                                        .executes(ctx -> {
                                            ServerPlayer sender = ctx.getSource().getPlayerOrException();
                                            String teamName = StringArgumentType.getString(ctx, "team");

                                            if (!TeamData.teamExists(teamName)) {
                                                sender.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, 0.5f);
                                                ctx.getSource().sendFailure(Component.literal("§cL'équipe \"" + teamName + "\" n'existe pas !"));
                                                return 0;
                                            }

                                            int teamId = TeamData.getTeamId(teamName);
                                            TeamData.setFreeCamZoneMin(teamId, null);
                                            TeamData.setFreeCamZoneMax(teamId, null);

                                            sender.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, 1f);
                                            ctx.getSource().sendSystemMessage(Component.literal("§aZone supprimée pour \"" + teamName + "\""));
                                            return 1;
                                        })
                                )
                        )
                )

                // ── show [team] ──────────────────────────────────
                .then(Commands.literal("show")
                        .executes(ctx -> {
                            ServerPlayer sender = ctx.getSource().getPlayerOrException();
                            ZoneVisualizer.startViewing(sender, -1);
                            sender.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1f, 2f);
                            ctx.getSource().sendSystemMessage(Component.literal("§aAffichage de toutes les zones FCZ activé"));
                            return 1;
                        })
                        .then(Commands.argument("team", StringArgumentType.string())
                                .suggests(TEAM_SUGGESTIONS)
                                .executes(ctx -> {
                                    ServerPlayer sender = ctx.getSource().getPlayerOrException();
                                    String teamName = StringArgumentType.getString(ctx, "team");

                                    if (!TeamData.teamExists(teamName)) {
                                        sender.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, 0.5f);
                                        ctx.getSource().sendFailure(Component.literal("§cL'équipe \"" + teamName + "\" n'existe pas !"));
                                        return 0;
                                    }

                                    int teamId = TeamData.getTeamId(teamName);
                                    ZoneVisualizer.startViewing(sender, teamId);
                                    sender.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1f, 2f);
                                    ctx.getSource().sendSystemMessage(Component.literal("§aAffichage de la zone de \"" + teamName + "\" activé"));
                                    return 1;
                                })
                        )
                )

                // ── hide ────────────────────────────────────────────
                .then(Commands.literal("hide")
                        .executes(ctx -> {
                            ServerPlayer sender = ctx.getSource().getPlayerOrException();
                            ZoneVisualizer.stopViewing(sender);
                            sender.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, 1f);
                            ctx.getSource().sendSystemMessage(Component.literal("§eAffichage des zones désactivé"));
                            return 1;
                        })
                )
        );
    }
}

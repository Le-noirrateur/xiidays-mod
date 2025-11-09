package com.mceteams.xiidays.commands;

import com.mceteams.xiidays.utils.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;

import java.util.Map;
import java.util.Objects;

import static com.mceteams.xiidays.utils.DataManager.*;

public class CommandRegistry {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        // #########
        // ## DAY ##
        // #########

        dispatcher.register(Commands.literal("xday")
                .requires(CommandSourceStack::isPlayer)
                .requires(source -> source.hasPermission(4))

                // Start
                .then(Commands.literal("start")
                        .executes(context -> {
                            if (!DaysManager.start(context)) {
                                Objects.requireNonNull(context.getSource().getPlayer()).playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, 0.5f);
                                context.getSource().sendSystemMessage(Component.literal("§cUne erreur s'est produite lors du démarrage du jour."));
                                return 0;
                            }

                            return 1;
                        })
                )

                // stop
                .then(Commands.literal("stop")
                        .executes(context -> {
                            if (!DaysManager.stop(context)) {
                                Objects.requireNonNull(context.getSource().getPlayer()).playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, 1f);
                                context.getSource().sendSystemMessage(Component.literal("§cUne erreur s'est produite lors de l'arrêt du jour."));
                                return 0;
                            }

                            return 1;
                        })
                )

                // status
                .then(Commands.literal("status")
                        .executes(context -> {

                            int Day = dataReadInt("days", "currentDay", 0);
                            boolean isInProgress = dataReadBoolean("days", "isInProgress", false);

                            Objects.requireNonNull(context.getSource().getPlayer()).playNotifySound(SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.MASTER, 1f, 1f);
                            context.getSource().sendSystemMessage(Component.literal("Jours: " + Day + ", En cours: " + isInProgress));
                            return 1;
                        })
                )

                // set
                .then(Commands.literal("set")
                        .then(Commands.argument("integer", IntegerArgumentType.integer())
                                .executes(context -> {
                                    int value = IntegerArgumentType.getInteger(context, "integer");
                                    int OldDay = dataReadInt("days", "currentDay", 0);
                                    dataModify("days", "currentDay", value);

                                    context.getSource().sendSystemMessage(Component.literal("Vous avez modifié le jour actuel de §8§l" + OldDay + "§r à §2§l" + value));

                                    if (dataReadBoolean("days", "isInProgress", false)) {
                                        context.getSource().getServer().getPlayerList()
                                                .broadcastSystemMessage(Component.literal("Le jour actuel n'est plus, nous désormais le §2§l" + value + " jour"), false);

                                        for (Player player : context.getSource().getServer().getPlayerList().getPlayers()) {
                                            player.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.MASTER, 1f, 1f);
                                        }
                                    }

                                    return 1;
                                })
                        )
                )
        );

        // ##########
        // ## TEAM ##
        // ##########

        dispatcher.register(Commands.literal("xteam")
                .requires(CommandSourceStack::isPlayer)
                .requires(source -> source.hasPermission(4))

                // create
                .then(Commands.literal("create")
                        .then(Commands.argument("TeamName", StringArgumentType.string())
                                .executes(context -> {
                                    String value = StringArgumentType.getString(context, "TeamName");

                                    if (value.length() < 3) {
                                        Objects.requireNonNull(context.getSource().getPlayer()).playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f);
                                        context.getSource().sendSystemMessage(Component.literal("§cVeuillez spécifier un nom d'équipe valide !"));
                                        return 1;
                                    }

                                    int tCreated = TeamManager.createTeam(value);

                                    if (tCreated == 1) {
                                        Objects.requireNonNull(context.getSource().getPlayer()).playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.MASTER, 1f, 2f);
                                        context.getSource().sendSystemMessage(Component.literal("L'équipe " + value + " a été crée !"));
                                    } else if (tCreated == 2) {
                                        Objects.requireNonNull(context.getSource().getPlayer()).playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f);
                                        context.getSource().sendSystemMessage(Component.literal("§cL'équipe \"" + value + "\" existe déjà" ));
                                    }

                                    return 1;
                                })
                        )
                )

                // remove
                .then(Commands.literal("remove")
                        .then(Commands.literal("member")
                                .then(Commands.argument("TeamName", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            for (String team : TeamManager.getAllTeams()) {
                                                builder.suggest(team);
                                            }
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(context -> {
                                                    String teamName = StringArgumentType.getString(context, "TeamName");
                                                    ServerPlayer player = EntityArgument.getPlayer(context, "player");

                                                    int tAdded = TeamManager.remMember(teamName, player);

                                                    switch (tAdded) {
                                                        case 0:
                                                            Objects.requireNonNull(context.getSource().getPlayer()).playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f);
                                                            context.getSource().sendSystemMessage(Component.literal("§cUne erreur inconnue est survenue !"));
                                                            return 0;
                                                        case 1:
                                                            Objects.requireNonNull(context.getSource().getPlayer()).playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1f, 2f);
                                                            context.getSource().sendSystemMessage(Component.literal("Le joueur \"" + player.getName().getString() + "\" a été retiré de l'équipe " + teamName));
                                                            return 1;
                                                        case 2:
                                                            Objects.requireNonNull(context.getSource().getPlayer()).playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f);
                                                            context.getSource().sendSystemMessage(Component.literal("§cL'équipe \"" + teamName + "\" n'existe pas !" ));
                                                            return 0;
                                                        case 3:
                                                            Objects.requireNonNull(context.getSource().getPlayer()).playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f);
                                                            context.getSource().sendSystemMessage(Component.literal("§cUne erreur est survenue, impossible de récupérer en interne l'identifiant de " + teamName));
                                                            return 0;
                                                        case 4:
                                                            Objects.requireNonNull(context.getSource().getPlayer()).playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f);
                                                            context.getSource().sendSystemMessage(Component.literal("§cLe joueur n'est pas présent dans cette équipe"));
                                                            return 0;
                                                        case 5:
                                                            Objects.requireNonNull(context.getSource().getPlayer()).playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f);
                                                            context.getSource().sendSystemMessage(Component.literal("§cLe joueur n'est pas dans cette équipe !"));
                                                            return 0;
                                                        case 6:
                                                            Objects.requireNonNull(context.getSource().getPlayer()).playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f);
                                                            context.getSource().sendSystemMessage(Component.literal("§cUne erreur inconnue s'est produite !"));
                                                            return 0;
                                                        default:
                                                            return 0;
                                                    }
                                                })
                                        )
                                )
                        )

                        .then(Commands.literal("team")
                                .then(Commands.argument("TeamName", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            for (String team : TeamManager.getAllTeams()) {
                                                builder.suggest(team);
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> 1)
                                )
                        )
                )

                // modify
                .then(Commands.literal("modify")
                        // Set spawn point with player position
                        .then(Commands.literal("spawn")
                                .then(Commands.argument("TeamName", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            for (String team : TeamManager.getAllTeams()) {
                                                builder.suggest(team);
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            String teamName = StringArgumentType.getString(context, "TeamName");
                                            BlockPos pos = Objects.requireNonNull(context.getSource().getPlayer()).
                                                    getOnPos();
                                            int setSpawn = TeamManager.placeTeamSpawn(teamName, pos, context.getSource().getLevel());

                                            switch (setSpawn) {
                                                case 1:
                                                    context.getSource().getPlayer().playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1f, 2f);
                                                    context.getSource().sendSystemMessage(Component.literal("Emplacement du spawn de l'équipe \"" + teamName + "\" défini"));
                                                    return 1;
                                                case 2:
                                                    context.getSource().getPlayer().playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, 0.5f);
                                                    context.getSource().sendFailure(Component.literal("§cErreur : Le block placé ne possède pas de BlockEntity valide"));
                                                    return 0;
                                                case 3:
                                                    context.getSource().getPlayer().playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, 0.5f);
                                                    context.getSource().sendFailure(Component.literal("§cImpossible de placer le block de spawn à cette position"));
                                                    return 0;
                                                default:
                                                    return 0;
                                            }
                                        })
                                )
                        )

                        // Set spawn point with position
                        .then(Commands.literal("spawn")
                                .then(Commands.argument("TeamName", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            for (String team : TeamManager.getAllTeams()) {
                                                builder.suggest(team);
                                            }
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                .executes(context -> {
                                                    String teamName = StringArgumentType.getString(context, "TeamName");
                                                    BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
                                                    int setSpawn = TeamManager.placeTeamSpawn(teamName, pos, context.getSource().getLevel());

                                                    switch (setSpawn) {
                                                        case 1:
                                                            Objects.requireNonNull(context.getSource().getPlayer()).playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1f, 2f);
                                                            context.getSource().sendSystemMessage(Component.literal("Emplacement du spawn de l'équipe \"" + teamName + "\" défini"));
                                                            return 1;
                                                        case 2:
                                                            Objects.requireNonNull(context.getSource().getPlayer()).playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, 0.5f);
                                                            context.getSource().sendFailure(Component.literal("§cErreur : Le block placé ne possède pas de BlockEntity valide"));
                                                            return 0;
                                                        case 3:
                                                            Objects.requireNonNull(context.getSource().getPlayer()).playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, 0.5f);
                                                            context.getSource().sendFailure(Component.literal("§cImpossible de placer le block de spawn à cette position"));
                                                            return 0;
                                                        default:
                                                            return 0;
                                                    }
                                                })
                                        )
                                )
                        )

                        // Set core location
                        .then(Commands.literal("core")
                                .then(Commands.argument("TeamName", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            for (String team : TeamManager.getAllTeams()) {
                                                builder.suggest(team);
                                            }
                                            return builder.buildFuture();
                                        })
                                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                            .executes(context -> {
                                                String teamName = StringArgumentType.getString(context, "TeamName");
                                                BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");

                                                int setCore = TeamManager.placeTeamCore(teamName, pos, context.getSource().getLevel());

                                                switch (setCore) {
                                                    case 1:
                                                        Objects.requireNonNull(context.getSource().getPlayer()).playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1f, 2f);
                                                        context.getSource().sendSystemMessage(Component.literal("Emplacement du coeur de l'équipe \"" + teamName + "\" défini"));
                                                        return 1;
                                                    case 2:
                                                        Objects.requireNonNull(context.getSource().getPlayer()).playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, 0.5f);
                                                        context.getSource().sendFailure(Component.literal("§cErreur : Le block placé ne possède pas de BlockEntity valide"));
                                                        return 0;
                                                    case 3:
                                                        Objects.requireNonNull(context.getSource().getPlayer()).playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, 0.5f);
                                                        context.getSource().sendFailure(Component.literal("§cImpossible de placer le block coeur à cette position"));
                                                        return 0;
                                                    default:
                                                        return 0;
                                                }
                                            })
                                    )
                                )
                        )
                )

                // add
                .then(Commands.literal("add")
                        .then(Commands.argument("TeamName", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    for (String team : TeamManager.getAllTeams()) {
                                        builder.suggest(team);
                                    }
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> {
                                            String teamName = StringArgumentType.getString(context, "TeamName");
                                            ServerPlayer player = EntityArgument.getPlayer(context, "player");

                                            int tAdded = TeamManager.addMember(teamName, player);

                                            switch (tAdded) {
                                                case 0:
                                                    Objects.requireNonNull(context.getSource().getPlayer()).playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f);
                                                    context.getSource().sendSystemMessage(Component.literal("§cUne erreur inconnue est survenue !"));
                                                    return 0;
                                                case 1:
                                                    Objects.requireNonNull(context.getSource().getPlayer()).playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1f, 2f);
                                                    context.getSource().sendSystemMessage(Component.literal("Le joueur \"" + player.getName().getString() + "\" a été ajouté à l'équipe " + teamName));
                                                    return 1;
                                                case 2:
                                                    Objects.requireNonNull(context.getSource().getPlayer()).playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f);
                                                    context.getSource().sendSystemMessage(Component.literal("§cL'équipe \"" + teamName + "\" n'existe pas !" ));
                                                    return 0;
                                                case 3:
                                                    Objects.requireNonNull(context.getSource().getPlayer()).playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f);
                                                    context.getSource().sendSystemMessage(Component.literal("§cUne erreur est survenue, impossible de récupérer en interne l'identifiant de " + teamName));
                                                    return 0;
                                                case 4:
                                                    Objects.requireNonNull(context.getSource().getPlayer()).playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f);
                                                    context.getSource().sendSystemMessage(Component.literal("§cLe joueur est déjà présent dans cette équipe"));
                                                    return 0;
                                                case 5:
                                                    Objects.requireNonNull(context.getSource().getPlayer()).playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f);
                                                    context.getSource().sendSystemMessage(Component.literal("§cImpossible d'ajouter le joueur " + player.getName().getString() + " car il est présent dans l'équipe " + TeamManager.getPlayerCurrentTeam(player.getUUID().toString()) + " !"));
                                                    return 0;
                                                default:
                                                    return 0;
                                            }
                                        })
                                )
                        )
                )
        );


        // ##########
        // ## GAME ##
        // ##########

        dispatcher.register(Commands.literal("xgame")
                .requires(CommandSourceStack::isPlayer)
                .requires(source -> source.hasPermission(4))

                .then(Commands.literal("data")
                        // /xgame data modify <table> <key> <value>
                        .then(Commands.literal("modify")
                                .then(Commands.argument("table", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            // Suggest all tables
                                            for (String table : DataManager.getAllTables()) {
                                                builder.suggest(table);
                                            }
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("key", StringArgumentType.string())
                                                .suggests((context, builder) -> {
                                                    String table = StringArgumentType.getString(context, "table");
                                                    for (String key : DataManager.getAllDataNames(table)) {
                                                        builder.suggest(key);
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                .then(Commands.argument("value", StringArgumentType.string())
                                                        .executes(context -> {
                                                            String table = StringArgumentType.getString(context, "table");
                                                            String key = StringArgumentType.getString(context, "key");
                                                            String value = StringArgumentType.getString(context, "value");

                                                            DataManager.dataModify(table, key, value);

                                                            context.getSource().sendSystemMessage(Component.literal("§aDonnée modifiée : " + table + "." + key + " = " + value));

                                                            return 1;
                                                        })
                                                )
                                        )
                                )
                        )

                        // /xgame data reload
                        .then(Commands.literal("reload")
                                .executes(context -> {
                                    DataManager.reloadData();
                                    context.getSource().sendSystemMessage(Component.literal("§eData rechargées depuis le fichier !"));
                                    return 1;
                                })
                        )

                        // /xgame data save
                        .then(Commands.literal("save")
                                .executes(context -> {
                                    DataManager.forceSave();
                                    context.getSource().sendSystemMessage(Component.literal("§eData sauvegardées !"));
                                    return 1;
                                })
                        )
                )

                .then(Commands.literal("restrictions")
                        // ----- Status -----
                        .then(Commands.literal("status")
                                .executes(context -> {
                                ServerPlayer player = context.getSource().getPlayer();
                                assert player != null;

                                ItemStack heldStack = player.getMainHandItem();

                                if (heldStack.isEmpty()) {
                                    context.getSource().sendSystemMessage(Component.literal("§cVous ne tenez rien en main."));
                                    return 0;
                                }

                                Item held = heldStack.getItem();
                                boolean allowed = RestrictionsManager.isAllowedCompletely(held);
                                Component status = RestrictionsManager.getStatusComponent(held);

                                // --- Effet visuel + sonore ---
                                ServerLevel level = player.serverLevel();

                                double x = player.getX();
                                double y = player.getY() + 1.5;
                                double z = player.getZ();

                                if (allowed) {
                                    // Vert = autorisé
                                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 10, 0.3, 0.5, 0.3, 0.01);
                                    player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.MASTER, 1f, 1.5f);
                                } else {
                                    // Rouge = interdit
                                    level.sendParticles(ParticleTypes.ANGRY_VILLAGER, x, y, z, 10, 0.3, 0.5, 0.3, 0.01);
                                    player.playNotifySound(SoundEvents.ANVIL_BREAK, SoundSource.MASTER, 1f, 1f);
                                }

                                context.getSource().sendSystemMessage(Component.literal("§eStatut de l'objet tenu : ").append(status));

                                return 1;
                            })
                        )

                        // ----- Items -----
                        .then(Commands.literal("items")
                                // /xgame items disallow
                                .then(Commands.literal("disallow")
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();

                                            Item held = player.getMainHandItem().getItem();
                                            ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(held);

                                            RestrictionsManager.setItemsAccess(false, held);

                                            // Si c’est un BlockItem, on bloque aussi le block correspondant
                                            if (held instanceof BlockItem blockItem) {
                                                RestrictionsManager.setBlockAccess(false, blockItem.getBlock());
                                                context.getSource().sendSystemMessage(Component.literal("§eBloc associé " + blockItem.getBlock().getName().getString() + " également interdit"));
                                            }

                                            player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.MASTER, 1f, 2f);
                                            context.getSource().sendSystemMessage(Component.literal("§cL'item " + itemKey + " est désormais interdit"));
                                            return 1;
                                        })
                                )

                                // /xgame items allow
                                .then(Commands.literal("allow")
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();

                                            Item held = player.getMainHandItem().getItem();
                                            ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(held);

                                            RestrictionsManager.setItemsAccess(true, held);

                                            // Si c’est un BlockItem, on autorise aussi le block correspondant
                                            if (held instanceof BlockItem blockItem) {
                                                RestrictionsManager.setBlockAccess(true, blockItem.getBlock());
                                                context.getSource().sendSystemMessage(Component.literal("§eBloc associé " + blockItem.getBlock().getName().getString() + " également autorisé"));
                                            }

                                            player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.MASTER, 1f, 2f);
                                            context.getSource().sendSystemMessage(Component.literal("§aL'item " + itemKey + " est désormais autorisé"));
                                            return 1;
                                        })
                                )
                        )

                        // ----- Blocks -----
                        .then(Commands.literal("blocks")
                                // /xgame blocks disallow [block]
                                .then(Commands.literal("disallow")
                                        .executes(context -> {
                                            // Cas sans argument → prend le bloc tenu en main
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            Item held = player.getMainHandItem().getItem();

                                            if (held instanceof BlockItem blockItem) {
                                                Block block = blockItem.getBlock();

                                                RestrictionsManager.setBlockAccess(false, block);
                                                RestrictionsManager.setItemsAccess(false, held);

                                                player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.MASTER, 1f, 2f);
                                                context.getSource().sendSystemMessage(Component.literal("§cLe bloc " + block.getName().getString() + " et son item associé sont désormais interdits"));
                                                return 1;
                                            } else {
                                                context.getSource().sendSystemMessage(Component.literal("§cVous devez tenir un bloc en main ou préciser une position."));
                                                player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f);
                                                return 0;
                                            }
                                        })

                                        // Cas avec position explicite
                                        .then(Commands.argument("block", BlockPosArgument.blockPos())
                                                .executes(context -> {
                                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                                    BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "block");
                                                    Block block = context.getSource().getLevel().getBlockState(pos).getBlock();

                                                    RestrictionsManager.setBlockAccess(false, block);

                                                    // Si le bloc a un item associé, on le bloque aussi
                                                    Item asItem = block.asItem();
                                                    if (asItem != Items.AIR) {
                                                        RestrictionsManager.setItemsAccess(false, asItem);
                                                        context.getSource().sendSystemMessage(Component.literal("§eItem associé " + BuiltInRegistries.ITEM.getKey(asItem) + " également interdit"));
                                                    }

                                                    player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.MASTER, 1f, 2f);
                                                    context.getSource().sendSystemMessage(Component.literal("§cLe bloc " + block.getName().getString() + " est désormais interdit"));
                                                    return 1;
                                                })
                                        )
                                )

                                // /xgame blocks allow [block]
                                .then(Commands.literal("allow")
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            Item held = player.getMainHandItem().getItem();

                                            if (held instanceof BlockItem blockItem) {
                                                Block block = blockItem.getBlock();

                                                RestrictionsManager.setBlockAccess(true, block);
                                                RestrictionsManager.setItemsAccess(true, held);

                                                player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.MASTER, 1f, 2f);
                                                context.getSource().sendSystemMessage(Component.literal("§aLe bloc " + block.getName().getString() + " et son item associé sont désormais autorisés"));
                                                return 1;
                                            } else {
                                                context.getSource().sendSystemMessage(Component.literal("§cVous devez tenir un bloc en main ou préciser une position."));
                                                player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f);
                                                return 0;
                                            }
                                        })

                                        .then(Commands.argument("block", BlockPosArgument.blockPos())
                                                .executes(context -> {
                                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                                    BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "block");
                                                    Block block = context.getSource().getLevel().getBlockState(pos).getBlock();

                                                    RestrictionsManager.setBlockAccess(true, block);

                                                    Item asItem = block.asItem();
                                                    if (asItem != Items.AIR) {
                                                        RestrictionsManager.setItemsAccess(true, asItem);
                                                        context.getSource().sendSystemMessage(Component.literal("§eItem associé " + BuiltInRegistries.ITEM.getKey(asItem) + " également autorisé"));
                                                    }

                                                    player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.MASTER, 1f, 2f);
                                                    context.getSource().sendSystemMessage(Component.literal("§aLe bloc " + block.getName().getString() + " est désormais autorisé"));
                                                    return 1;
                                                })
                                        )
                                )
                        )

                        // ----- Bypass -----
                        .then(Commands.literal("bypass")
                                // /xgame bypass
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();

                                    if (RestrictionsManager.hasBypass(player)) {
                                        RestrictionsManager.removeBypassPlayer(player);
                                        player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1f, 2f);
                                        context.getSource().sendSystemMessage(Component.literal("§cBypass désactivé"));
                                    } else {
                                        RestrictionsManager.addBypassPlayer(player);
                                        player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1f, 2f);
                                        context.getSource().sendSystemMessage(Component.literal("§aBypass activé"));
                                    }

                                    return 1;
                                })

                                // /xgame bypass on
                                .then(Commands.literal("on")
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();

                                            if (RestrictionsManager.hasBypass(player)) {
                                                context.getSource().sendSystemMessage(Component.literal("§cVous êtes déjà en bypass"));
                                                player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f);
                                            } else {
                                                RestrictionsManager.addBypassPlayer(player);
                                                player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1f, 2f);
                                                context.getSource().sendSystemMessage(Component.literal("§aBypass activé"));
                                            }

                                            return 1;
                                        })
                                )

                                // /xgame bypass off
                                .then(Commands.literal("off")
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();

                                            if (!RestrictionsManager.hasBypass(player)) {
                                                context.getSource().sendSystemMessage(Component.literal("§cVous n'êtes pas en bypass"));
                                                player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f);
                                            } else {
                                                RestrictionsManager.removeBypassPlayer(player);
                                                player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1f, 2f);
                                                context.getSource().sendSystemMessage(Component.literal("§cBypass désactivé"));
                                            }

                                            return 1;
                                        })
                                )

                                // /xgame bypass <player>
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> {
                                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                            ServerPlayer sender = context.getSource().getPlayerOrException();

                                            if (RestrictionsManager.hasBypass(target)) {
                                                RestrictionsManager.removeBypassPlayer(target);
                                                sender.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1f, 2f);
                                                context.getSource().sendSystemMessage(Component.literal("§cBypass désactivé pour " + target.getName().getString()));
                                            } else {
                                                RestrictionsManager.addBypassPlayer(target);
                                                sender.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1f, 2f);
                                                context.getSource().sendSystemMessage(Component.literal("§aBypass activé pour " + target.getName().getString()));
                                            }

                                            return 1;
                                        })

                                        // /xgame bypass <player> on
                                        .then(Commands.literal("on")
                                                .executes(context -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                                    ServerPlayer sender = context.getSource().getPlayerOrException();

                                                    if (RestrictionsManager.hasBypass(target)) {
                                                        sender.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f);
                                                        context.getSource().sendSystemMessage(Component.literal("§c" + target.getName().getString() + " est déjà en bypass"));
                                                    } else {
                                                        RestrictionsManager.addBypassPlayer(target);
                                                        sender.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1f, 2f);
                                                        context.getSource().sendSystemMessage(Component.literal("§a" + target.getName().getString() + " est désormais en bypass"));
                                                    }

                                                    return 1;
                                                })
                                        )

                                        // /xgame bypass <player> off
                                        .then(Commands.literal("off")
                                                .executes(context -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                                    ServerPlayer sender = context.getSource().getPlayerOrException();

                                                    if (!RestrictionsManager.hasBypass(target)) {
                                                        sender.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, .5f);
                                                        context.getSource().sendSystemMessage(Component.literal("§c" + target.getName().getString() + " n'est pas en bypass"));
                                                    } else {
                                                        RestrictionsManager.removeBypassPlayer(target);
                                                        sender.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1f, 2f);
                                                        context.getSource().sendSystemMessage(Component.literal("§c" + target.getName().getString() + " n'est plus en bypass"));
                                                    }

                                                    return 1;
                                                })
                                        )
                                )
                        )
                )

                .then(Commands.literal("spectator")
                        .then(Commands.literal("players") // Sous-commande pour la gestion des spectateurs
                                .then(Commands.literal("respawn")

                                        // Respawn un spectateur
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(context -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                                    ServerPlayer sender = context.getSource().getPlayerOrException();

                                                    if (!SpectateManager.isSpectating(target)) {
                                                        sender.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, 0.5f);
                                                        context.getSource().sendFailure(Component.literal("§cLe joueur n'est pas en mode spectateur"));
                                                        return 0;
                                                    }

                                                    if (SpectateManager.respawnPlayer(target)) {
                                                        sender.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1f, 2f);
                                                        context.getSource().sendSystemMessage(Component.literal("§a" + target.getName().getString() + " a été respawn"));
                                                        target.sendSystemMessage(Component.literal("§aVous avez été respawn par un administrateur"));
                                                        return 1;
                                                    } else {
                                                        sender.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, 0.5f);
                                                        context.getSource().sendFailure(Component.literal("§cImpossible de respawn le joueur"));
                                                        return 0;
                                                    }
                                                })
                                        )
                                )

                                        // Respawn tous les spectateurs
                                        .then(Commands.literal("respawnall")
                                                .executes(context -> {
                                                    ServerPlayer sender = context.getSource().getPlayerOrException();
                                                    int count = SpectateManager.getSpectatorCount();

                                                    if (count == 0) {
                                                        sender.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, 0.5f);
                                                        context.getSource().sendSystemMessage(Component.literal("§cAucun spectateur à respawn"));
                                                        return 0;
                                                    }

                                                    SpectateManager.respawnAllSpectators();

                                                    sender.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1f, 2f);
                                                    context.getSource().getServer().getPlayerList().broadcastSystemMessage(
                                                            Component.literal("§aTous les spectateurs ont été respawn"), false);

                                                    return 1;
                                                })
                                        )

                                        // Liste des spectateurs
                                        .then(Commands.literal("list")
                                                .executes(context -> {
                                                    ServerPlayer sender = context.getSource().getPlayerOrException();
                                                    int total = SpectateManager.getSpectatorCount();

                                                    if (total == 0) {
                                                        sender.playNotifySound(SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.MASTER, 1f, 1f);
                                                        context.getSource().sendSystemMessage(Component.literal("§eAucun spectateur actuellement"));
                                                        return 1;
                                                    }

                                                    Map<String, Integer> byTeam = SpectateManager.getSpectatorsByTeam();

                                                    sender.playNotifySound(SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.MASTER, 1f, 1f);
                                                    context.getSource().sendSystemMessage(Component.literal("§e=== Spectateurs (" + total + ") ==="));

                                                    for (Map.Entry<String, Integer> entry : byTeam.entrySet()) {
                                                        context.getSource().sendSystemMessage(Component.literal(
                                                                "§7" + entry.getKey() + " : §f" + entry.getValue() + " joueur(s)"
                                                        ));
                                                    }

                                                    return 1;
                                                })
                                        )
                                )
                        )

                        .then(Commands.literal("settings")
                                .then(Commands.literal("fcz")
                                        .then(Commands.argument("TeamName", StringArgumentType.string())
                                                .suggests((context, builder) -> {
                                                    for (String team : TeamManager.getAllTeams()) {
                                                        builder.suggest(team);
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                .then(Commands.argument("corner1", BlockPosArgument.blockPos())
                                                        .then(Commands.argument("corner2", BlockPosArgument.blockPos())
                                                                .executes(context -> {
                                                                    String teamName = StringArgumentType.getString(context, "TeamName");
                                                                    BlockPos corner1 = BlockPosArgument.getLoadedBlockPos(context, "corner1");
                                                                    BlockPos corner2 = BlockPosArgument.getLoadedBlockPos(context, "corner2");
                                                                    ServerPlayer sender = context.getSource().getPlayerOrException();

                                                                    // Vérifier que l'équipe existe
                                                                    if (!DataManager.hasData("Teams", teamName)) {
                                                                        sender.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, 0.5f);
                                                                        context.getSource().sendFailure(Component.literal("§cL'équipe \"" + teamName + "\" n'existe pas !"));
                                                                        return 0;
                                                                    }

                                                                    // Définir la zone
                                                                    SpectateManager.setFreeCamZone(teamName, corner1, corner2);

                                                                    sender.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1f, 2f);
                                                                    context.getSource().sendSystemMessage(Component.literal(
                                                                            "§aZone de free cam définie pour l'équipe \"" + teamName + "\"\n" +
                                                                                    "§7Coin 1 : §e" + corner1.getX() + ", " + corner1.getY() + ", " + corner1.getZ() + "\n" +
                                                                                    "§7Coin 2 : §e" + corner2.getX() + ", " + corner2.getY() + ", " + corner2.getZ()
                                                                    ));

                                                                    return 1;
                                                                })
                                                        )
                                                )
                                        )
                                )
                        )
        );

        // ##############
        // ## SPECTATE ##
        // ##############

        dispatcher.register(Commands.literal("xspectate")
                .requires(CommandSourceStack::isPlayer)
                .requires(source -> source.hasPermission(0))

                .then(Commands.literal("switch")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();

                            if (player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
                                context.getSource().sendFailure(Component.literal("§cVous devez être en spectateur"));
                                return 0;
                            }

                            NativeCameraController.switchToNextTeammate(player);
                            context.getSource().sendSystemMessage(Component.literal("§aChangement de vue"));
                            return 1;
                        })
                )

                .then(Commands.literal("mode")
                        .then(Commands.literal("teammate")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    SpectateController.setMode(player, SpectateController.SpectateMode.TEAMMATE);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("freecam")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    SpectateController.setMode(player, SpectateController.SpectateMode.FREECAM_LIMITED);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("cinematic")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    SpectateController.setMode(player, SpectateController.SpectateMode.CINEMATIC);
                                    CinematicManager.startCinematic(player, "default");
                                    return 1;
                                })
                        )
                )

                .then(Commands.literal("toggle")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();

                            if (player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
                                context.getSource().sendFailure(Component.literal("§cVous devez être en spectateur"));
                                return 0;
                            }

                            if (!DaysManager.isDayInProgress()) {
                                context.getSource().sendFailure(Component.literal("§cLe toggle est désactivé hors jour actif"));
                                return 0;
                            }

                            SpectateManager.toggleSpectateMode(player);
                            return 1;
                        })
                )
        );
    }
}

package com.mceteams.xiidays.utils;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.mceteams.xiidays.XIIDays.LOGGER;
import static com.mceteams.xiidays.utils.DataManager.*;

public class DaysManager {
    public static boolean isDayInProgress() {
        
        return dataReadBoolean("days", "isInProgress", false);
    }

    public static int getCurrentDay() {
        
        return dataReadInt("days", "currentDay", 0);
    }

    public static boolean start(CommandContext<CommandSourceStack> context) {
        try {
            

            // Vérifie si un jour est déjà en cours ou si
            if (!dataReadBoolean("days", "isInProgress", false) && dataReadInt("days", "currentDay", 0) < 12) {
                dataModify("days", "isInProgress", true);
                dataModify("days", "currentDay", dataReadInt("days", "currentDay", 0) + 1);

                try {

                    context.getSource().getServer().getPlayerList()
                            .broadcastAll(new ClientboundSetTitleTextPacket(Component.literal("§43"))); // Timer ( 3s )

                    // Retire les effets de blindness et applique un blindness de 3 secondes pour le lancement aux joueurs
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
                            .broadcastAll(new ClientboundSetTitleTextPacket(Component.literal("§62"))); // Timer ( 2s )

                    for (Player player : context.getSource().getServer().getPlayerList().getPlayers()) {
                        player.playNotifySound(SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.MASTER, 1.0f, 1.0f);
                    }

                    TimeUnit.SECONDS.sleep(1);

                    context.getSource().getServer().getPlayerList()
                            .broadcastAll(new ClientboundSetTitleTextPacket(Component.literal("§21"))); // Timer ( 1s )

                    for (Player player : context.getSource().getServer().getPlayerList().getPlayers()) {
                        player.playNotifySound(SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.MASTER, 1.0f, 1.0f);
                    }

                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                // Démarrage du jour
                int days = dataReadInt("days", "currentDay", 0);
                Component msg;

                if (days == 1) { // Si le jour est égal à 1 alors c'est le début d'une nouvelle aventure ( messages )
                    context.getSource().getServer().getPlayerList()
                            .broadcastAll(new ClientboundSetTitleTextPacket(Component.literal("§lDébut de l'aventure")));

                    context.getSource().getServer().getPlayerList()
                            .broadcastSystemMessage(Component.literal("§c§l[§r§6§lBienvenue dans XII Days§r§c§l]\n\nVotre objectif durant ces 12 jours\nest de récupérer un maximum d'objets\nou de blocs afin de protéger votre\nbase des équipes adverse.§r\n\nVous trouverez quelque objets §2bonus§r\npermettant à votre équipe d'avoir des\n§davantages sur les autres§r, tel que des\ncolis qui tombe quelque fois.\n\n§3§lLes six premier jours sont une phase\nde préparation, les six dernier, de\ncombat\n\n"), false);

                    msg = Component.literal("Le premier jour a commencé.");
                } else { // Sinon démarrer un jour normal ( messages )
                    context.getSource().getServer().getPlayerList()
                            .broadcastAll(new ClientboundSetTitleTextPacket(Component.literal("§lDébut du jour")));
                    msg = Component.literal("Le " + days + "e jour a commencé.");
                }

                context.getSource().getServer().getPlayerList()
                        .broadcastSystemMessage(msg, false);

                context.getSource().getServer().getPlayerList().broadcastAll(
                        new ClientboundSystemChatPacket(msg, true));

                // son
                for (Player player : context.getSource().getServer().getPlayerList().getPlayers()) {
                    player.playNotifySound(SoundEvents.ENDER_DRAGON_GROWL, SoundSource.MASTER, 1.0f, 1.0f);
                    player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1.0f, 2.0f);
                }
            } else { // erreurs
                Objects.requireNonNull(context.getSource().getPlayer()).playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.MASTER, 1f, 0.5f);

                if (dataReadBoolean("days", "isInProgress", false)) {
                    context.getSource().sendSystemMessage(Component.literal("§cUne journée est déjà en cours"));
                } else if (dataReadInt("days", "currentDay", 0) < 12) {
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

    public static boolean stop(CommandContext<CommandSourceStack> context) {
        try {
            

            // Termine le jour dans les données
            dataModify("days", "isInProgress", false);

            // Respawn tous les spectateurs avant d'appliquer les effets
            SpectateManager.respawnAllSpectators();

            // Ajoute l'effet de blindness aux joueurs vivants
            for (Player player : context.getSource().getServer().getPlayerList().getPlayers()) {
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 99999, 255, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 99999, 255, true, false));
            }

            // Message de fin de jour
            context.getSource().getServer().getPlayerList()
                    .broadcastSystemMessage(Component.literal("§cLe jour est terminé"), false);

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return false;
        }

        return true;
    }
}

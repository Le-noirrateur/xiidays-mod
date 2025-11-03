package com.mceteams.xiidays.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import static com.mceteams.xiidays.utils.NotifyOptions.notifyPlayer;

public class PlayersHandler {
    // #################################################################################################################
    // Fonctions
    // #################################################################################################################

    // #################################################################################################################
    // Events
    // #################################################################################################################

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof ServerPlayer serverPlayer) {
            if (DaysManager.isDayInProgress()) {
                serverPlayer.removeEffectNoUpdate(net.minecraft.world.effect.MobEffects.DARKNESS);
                serverPlayer.removeEffectNoUpdate(net.minecraft.world.effect.MobEffects.BLINDNESS);

                serverPlayer.sendSystemMessage(Component.literal("Bienvenu(e) §l" + serverPlayer.getName().getString() + "§r, le jour §l" + DaysManager.getCurrentDay() + "§r est en cours !"));
                serverPlayer.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.MASTER, 1.0f, 1.0f);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerAttackPlayer(AttackEntityEvent event) {
        Entity attacker = event.getEntity();
        Entity target = event.getTarget();

        if (attacker instanceof ServerPlayer serverAttacker && target instanceof ServerPlayer serverTarget) {
            String attackerTeam = TeamManager.getPlayerCurrentTeam(serverAttacker.getUUID().toString());
            String targetTeam = TeamManager.getPlayerCurrentTeam(serverTarget.getUUID().toString());

            // Vérifie si un jour est en cours
            if (!DaysManager.isDayInProgress()) {
                event.setCanceled(true);
                return;
            }

            // Si les deux joueurs sont dans la même équipe, annule l'attaque
            if (attackerTeam != null && attackerTeam.equals(targetTeam)) {
                event.setCanceled(true);
                notifyPlayer(serverAttacker, "§cVous ne pouvez pas attaquer un membre de votre équipe !", new NotifyOptions().sound(SoundEvents.LAVA_EXTINGUISH, SoundSource.MASTER, 1.0f, 1.0f).actionBar(true));
                serverAttacker.playNotifySound(SoundEvents.LAVA_EXTINGUISH, SoundSource.MASTER, 1.0f, 1.0f);
            }
        }
    }
}

package com.mceteams.xiidays.player;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NotifyOptions {
    // === Paramètres par défaut ===
    private boolean actionBar = false;
    private SoundEvent sound = SoundEvents.NOTE_BLOCK_PLING.value();
    private SoundSource source = SoundSource.MASTER;
    private float volume = 1f;
    private float pitch = 2f;

    // === Cooldown des messages ===
    private static final long MESSAGE_COOLDOWN = 2000; // 2 secondes
    private static final Map<UUID, Long> lastMessageTime = new HashMap<>();

    // === Méthodes "builder" ===
    public NotifyOptions sound(SoundEvent sound, SoundSource source, float volume, float pitch) {
        this.sound = sound;
        this.source = source;
        this.volume = volume;
        this.pitch = pitch;
        return this;
    }

    public NotifyOptions actionBar(boolean actionBar) {
        this.actionBar = actionBar;
        return this;
    }

    // === Méthode statique pour envoi simple ===
    public static void notifyPlayer(ServerPlayer player, String message, NotifyOptions options) {
        if (!canSendMessage(player.getUUID())) return;

        player.displayClientMessage(Component.literal(message), options.actionBar);
        player.playNotifySound(options.sound, options.source, options.volume, options.pitch);
    }

    // === Surcharge pratique (valeurs par défaut) ===
    public static void notifyPlayer(ServerPlayer player, String message) {
        notifyPlayer(player, message, new NotifyOptions());
    }

    // === Gestion du cooldown ===
    private static boolean canSendMessage(UUID playerUUID) {
        long now = System.currentTimeMillis();
        Long last = lastMessageTime.get(playerUUID);

        if (last == null || now - last >= MESSAGE_COOLDOWN) {
            lastMessageTime.put(playerUUID, now);
            return true;
        }
        return false;
    }
}

package com.mceteams.xiidays.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import static com.mceteams.xiidays.XIIDays.MODID;

public record AdminActionPayload(String action, String target, int value) implements CustomPacketPayload {

    public static final Type<AdminActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "admin_action"));

    public AdminActionPayload(String action) {
        this(action, "", 0);
    }

    public AdminActionPayload(String action, String target) {
        this(action, target, 0);
    }

    public static final StreamCodec<FriendlyByteBuf, AdminActionPayload> CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, AdminActionPayload packet) {
            buf.writeUtf(packet.action);
            buf.writeUtf(packet.target);
            buf.writeInt(packet.value);
        }

        @Override
        public @NotNull AdminActionPayload decode(FriendlyByteBuf buf) {
            return new AdminActionPayload(buf.readUtf(), buf.readUtf(), buf.readInt());
        }
    };

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AdminActionPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!player.hasPermissions(4)) {
                player.sendSystemMessage(Component.literal("§cVous n'avez pas la permission !"));
                return;
            }
            executeAction(packet, player);
        });
    }

    private static void runCmd(ServerPlayer player, String cmd) {
        player.getServer().getCommands().performPrefixedCommand(
                player.createCommandSourceStack(), cmd);
    }

    private static void executeAction(AdminActionPayload packet, ServerPlayer player) {
        switch (packet.action) {
            case "day_start" -> runCmd(player, "xiidays day start");
            case "day_stop" -> runCmd(player, "xiidays day stop");
            case "day_set" -> {
                int val;
                try {
                    val = Integer.parseInt(packet.target);
                } catch (NumberFormatException e) {
                    val = packet.value;
                }
                if (val >= 1 && val <= 12) {
                    runCmd(player, "xiidays day set " + val);
                } else {
                    player.sendSystemMessage(Component.literal("§cLe jour doit être entre 1 et 12"));
                }
            }
            case "team_create" -> {
                if (!packet.target.isEmpty()) runCmd(player, "xiidays team create " + packet.target);
            }
            case "team_eliminate" -> {
                if (!packet.target.isEmpty()) runCmd(player, "xiidays team eliminate " + packet.target);
            }
            case "team_revive" -> {
                if (!packet.target.isEmpty()) runCmd(player, "xiidays team revive " + packet.target);
            }
            case "team_remove" -> {
                if (!packet.target.isEmpty()) runCmd(player, "xiidays team remove team " + packet.target);
            }
            case "team_add" -> {
                if (!packet.target.isEmpty()) {
                    String[] parts = packet.target.split(" ", 2);
                    if (parts.length == 2) {
                        runCmd(player, "xiidays team add " + parts[0] + " " + parts[1]);
                    } else {
                        player.sendSystemMessage(Component.literal("§cFormat: <equipe> <joueur>"));
                    }
                }
            }
            case "team_remove_member" -> {
                if (!packet.target.isEmpty()) {
                    String[] parts = packet.target.split(" ", 2);
                    if (parts.length == 2) {
                        runCmd(player, "xiidays team remove member " + parts[0] + " " + parts[1]);
                    } else {
                        player.sendSystemMessage(Component.literal("§cFormat: <equipe> <joueur>"));
                    }
                }
            }
            case "team_spawn" -> {
                if (!packet.target.isEmpty()) runCmd(player, "xiidays team spawn " + packet.target);
            }
            case "team_core" -> {
                if (!packet.target.isEmpty()) runCmd(player, "xiidays team core " + packet.target + " ~ ~ ~");
            }
            case "score_open" -> runCmd(player, "xiidays score");
            case "spec_respawnall" -> runCmd(player, "xiidays spec respawnall");
            case "data_save" -> runCmd(player, "xiidays data save");
            case "data_reload" -> runCmd(player, "xiidays data reload");
            default -> {
                player.sendSystemMessage(Component.literal("§cAction inconnue : " + packet.action));
            }
        }

    }
}

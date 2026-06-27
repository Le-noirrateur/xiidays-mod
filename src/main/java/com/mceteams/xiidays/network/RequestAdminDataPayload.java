package com.mceteams.xiidays.network;

import com.mceteams.xiidays.data.DayCycleData;
import com.mceteams.xiidays.data.TeamData;
import com.mceteams.xiidays.game.TeamManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import static com.mceteams.xiidays.XIIDays.MODID;

public record RequestAdminDataPayload(String dataType) implements CustomPacketPayload {

    public static final Type<RequestAdminDataPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(MODID, "request_admin_data"));

    public static final StreamCodec<FriendlyByteBuf, RequestAdminDataPayload> CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, RequestAdminDataPayload packet) {
            buf.writeUtf(packet.dataType);
        }

        @Override
        public @NotNull RequestAdminDataPayload decode(FriendlyByteBuf buf) {
            return new RequestAdminDataPayload(buf.readUtf());
        }
    };

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestAdminDataPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!player.permissions().hasPermission(Permissions.COMMANDS_OWNER)) return;

            String data = switch (packet.dataType) {
                case "day_status" -> {
                    int day = DayCycleData.getCurrentDay();
                    boolean running = DayCycleData.isInProgress();
                    yield "§7Jour: §e" + day + " §7| " + (running ? "§aEn cours" : "§cArrêté");
                }
                case "team_list" -> {
                    String[] teams = TeamManager.getAllTeams();
                    if (teams.length == 0) yield "§7Aucune équipe";
                    StringBuilder sb = new StringBuilder();
                    for (String t : teams) {
                        if (!sb.isEmpty()) sb.append("\n");
                        int id = TeamData.getTeamId(t);
                        sb.append(" §e").append(t).append(" §7(ID: ").append(id).append(")");
                    }
                    yield sb.toString();
                }
                default -> "";
            };

            PacketDistributor.sendToPlayer(player, new AdminDataResponsePayload(packet.dataType, data));
        });
    }
}

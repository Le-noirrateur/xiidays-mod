package com.mceteams.xiidays.network;

import com.mceteams.xiidays.screen.EndScoreboardScreen;
import net.minecraft.client.Minecraft;
public class ClientEndScoreboardHandler {

    public static void openEndScoreboard(OpenEndScoreboardPacket packet) {
        Minecraft.getInstance().setScreen(new EndScoreboardScreen(
                packet.winningTeamName(),
                packet.teamStats(),
                packet.mvp()
        ));
    }
}

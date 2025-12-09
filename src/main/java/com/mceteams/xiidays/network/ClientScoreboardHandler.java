package com.mceteams.xiidays.network;

import com.mceteams.xiidays.client.ScoreboardScreen;
import com.mceteams.xiidays.utils.ScoreboardManager;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * Gère l'ouverture du scoreboard côté CLIENT uniquement
 * Cette classe ne sera JAMAIS chargée sur le serveur
 */
@OnlyIn(Dist.CLIENT)
public class ClientScoreboardHandler {

    /**
     * Ouvre l'écran du scoreboard avec les données reçues
     */
    public static void openScoreboard(OpenScoreboardPacket packet) {
        List<ScoreboardScreen.TeamScore> teamScores = new ArrayList<>();

        for (OpenScoreboardPacket.TeamData data : packet.teams()) {
            // Convertir int rankChange en enum RankChange
            ScoreboardManager.RankChange rankChange = switch (data.rankChange()) {
                case 1 -> ScoreboardManager.RankChange.UP;
                case -1 -> ScoreboardManager.RankChange.DOWN;
                default -> ScoreboardManager.RankChange.NONE;
            };

            teamScores.add(new ScoreboardScreen.TeamScore(
                    data.teamName(),
                    data.uuid(),
                    data.points(),
                    data.coreAlive(),
                    rankChange
            ));
        }

        Minecraft.getInstance().setScreen(new ScoreboardScreen(teamScores, packet.playerTeam()));
    }
}
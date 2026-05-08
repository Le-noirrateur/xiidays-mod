package com.mceteams.xiidays.network;

import com.mceteams.xiidays.player.ClientRankTracker;
import com.mceteams.xiidays.screen.ScoreboardScreen;
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
        // Extraire la liste des noms d'équipes dans l'ordre du classement
        List<String> rankingOrder = new ArrayList<>();
        for (OpenScoreboardPacket.TeamData data : packet.teams()) {
            rankingOrder.add(data.teamName());
        }

        // Mettre à jour le tracker côté client (détecte les changements)
        ClientRankTracker.updateRanking(rankingOrder);
        ClientRankTracker.cleanup(); // Nettoyer les entrées expirées

        // Construire la liste pour l'écran
        List<ScoreboardScreen.TeamScore> teamScores = new ArrayList<>();
        for (OpenScoreboardPacket.TeamData data : packet.teams()) {
            teamScores.add(new ScoreboardScreen.TeamScore(
                    data.teamName(),
                    data.uuid(),
                    data.points(),
                    data.coreAlive()
            ));
        }

        Minecraft.getInstance().setScreen(new ScoreboardScreen(teamScores, packet.playerTeam()));
    }
}
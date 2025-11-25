package com.mceteams.xiidays.menus;

import com.mceteams.xiidays.utils.DataManager;
import com.mceteams.xiidays.utils.TeamManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static com.mceteams.xiidays.XIIDays.MODID;

public class EndScoreboardScreen extends Screen {

    // Textures
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/end_scoreboard_bg.png");
    private static final ResourceLocation MVP_FRAME = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/mvp_frame.png");
    private static final ResourceLocation TROPHY = ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/trophy.png");

    private final String winningTeam;
    private final MVPData mvpData;
    private final List<TeamStats> teamStatsList;
    private TeamStats hoveredTeam = null;

    public EndScoreboardScreen(String winningTeam) {
        super(Component.literal("Fin de Partie"));
        this.winningTeam = winningTeam;
        this.mvpData = calculateMVP();
        this.teamStatsList = loadTeamStats();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Background principal
        renderBackground(graphics, mouseX, mouseY, partialTick);

        // Titre "FIN DE PARTIE"
        graphics.drawCenteredString(font, "§6§lFIN DE PARTIE", width / 2, 20, 0xFFFFFF);

        // Équipe gagnante (gauche)
        renderWinningTeam(graphics);

        // MVP (droite)
        renderMVP(graphics);

        // Liste des équipes (centre bas)
        renderTeamList(graphics, mouseX, mouseY);

        // Tooltip si hover équipe
        if (hoveredTeam != null) {
            renderTeamStatsTooltip(graphics, mouseX, mouseY, hoveredTeam);
        }
    }

    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Fond semi-transparent noir
        graphics.fill(0, 0, width, height, 0xCC000000);

        // Background texture si existe
        RenderSystem.setShaderTexture(0, BACKGROUND);
        graphics.blit(BACKGROUND, 0, 0, 0, 0, width, height, width, height);
    }

    private void renderWinningTeam(GuiGraphics graphics) {
        int x = 50;
        int y = 80;

        // Trophée
        graphics.blit(TROPHY, x, y, 0, 0, 64, 64, 64, 64);

        // Texte équipe gagnante
        graphics.drawString(font, "§e§lÉQUIPE GAGNANTE", x + 80, y + 10, 0xFFD700);
        graphics.drawString(font, "§6" + winningTeam, x + 80, y + 30, 0xFFFFFF);

        int teamId = TeamManager.getTeamId(winningTeam);
        if (teamId > 0) {
            int points = DataManager.dataReadInt("team_" + teamId + "_points", "total", 0);
            graphics.drawString(font, "§7Points totaux: §f" + points, x + 80, y + 50, 0xAAAAAA);
        }
    }

    private void renderMVP(GuiGraphics graphics) {
        int x = width - 250;
        int y = 80;

        // Frame MVP
        graphics.blit(MVP_FRAME, x, y, 0, 0, 200, 250, 200, 250);

        // Titre MVP avec police custom (voir plus bas)
        graphics.drawCenteredString(font, "§6§lMVP DE LA PARTIE", x + 100, y + 10, 0xFFD700);

        // Skin du joueur MVP (64x64)
        if (mvpData != null) {
            // Render skin (voir méthode dédiée)
            renderPlayerSkin(graphics, x + 68, y + 40, mvpData.playerUUID);

            // Nom joueur
            graphics.drawCenteredString(font, "§e" + mvpData.playerName, x + 100, y + 120, 0xFFFFFF);

            // Stats
            graphics.drawCenteredString(font, "§7Points apportés: §a" + mvpData.pointsContributed, x + 100, y + 140, 0xAAAAAA);
            graphics.drawCenteredString(font, "§7Kills: §c" + mvpData.kills, x + 100, y + 155, 0xAAAAAA);
            graphics.drawCenteredString(font, "§7Équipe: §6" + mvpData.teamName, x + 100, y + 170, 0xAAAAAA);
        }
    }

    private void renderTeamList(GuiGraphics graphics, int mouseX, int mouseY) {
        int startX = width / 2 - 150;
        int startY = height - 200;
        int barWidth = 300;
        int barHeight = 30;

        graphics.drawCenteredString(font, "§e§lCLASSEMENT FINAL", width / 2, startY - 20, 0xFFD700);

        hoveredTeam = null;

        for (int i = 0; i < teamStatsList.size(); i++) {
            TeamStats team = teamStatsList.get(i);
            int y = startY + (i * (barHeight + 5));

            // Détection hover
            boolean isHovered = mouseX >= startX && mouseX <= startX + barWidth &&
                    mouseY >= y && mouseY <= y + barHeight;

            if (isHovered) hoveredTeam = team;

            // Couleur barre
            int color = getRankColor(i + 1, isHovered);
            graphics.fill(startX, y, startX + barWidth, y + barHeight, color);

            // Médaille
            String medal = getRankMedal(i + 1);
            graphics.drawString(font, medal + " #" + (i + 1), startX + 10, y + 10, 0xFFFFFF);

            // Nom équipe
            graphics.drawString(font, team.teamName, startX + 80, y + 10, 0xFFFFFF);

            // Points
            graphics.drawString(font, "§6" + team.points + " pts", startX + barWidth - 80, y + 10, 0xFFD700);
        }
    }

    private void renderTeamStatsTooltip(GuiGraphics graphics, int mouseX, int mouseY, TeamStats team) {
        int tooltipWidth = 200;
        int tooltipHeight = 120;
        int x = mouseX + 15;
        int y = mouseY - 60;

        // Ajustement si déborde écran
        if (x + tooltipWidth > width) x = mouseX - tooltipWidth - 15;
        if (y < 0) y = 10;

        // Fond tooltip
        graphics.fill(x, y, x + tooltipWidth, y + tooltipHeight, 0xE0000000);
        graphics.fill(x, y, x + tooltipWidth, y + 2, 0xFFFFD700); // Bordure or

        // Titre
        graphics.drawString(font, "§6§l" + team.teamName, x + 10, y + 10, 0xFFD700);

        // Stats détaillées
        graphics.drawString(font, "§7Points: §f" + team.points, x + 10, y + 30, 0xAAAAAA);
        graphics.drawString(font, "§7Kills: §c" + team.kills, x + 10, y + 45, 0xFF5555);
        graphics.drawString(font, "§7Morts: §8" + team.deaths, x + 10, y + 60, 0x888888);
        graphics.drawString(font, "§7Blocs minés: §e" + team.blocksMined, x + 10, y + 75, 0xFFFF55);
        graphics.drawString(font, "§7Dégâts infligés: §4" + team.damageDealt, x + 10, y + 90, 0xAA0000);
    }

    private void renderPlayerSkin(GuiGraphics graphics, int x, int y, UUID playerUUID) {
        Minecraft mc = Minecraft.getInstance();
        assert mc.level != null;
        Player player = mc.level.getPlayerByUUID(playerUUID);

        if (player != null) {
            // Render la tête du joueur
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(64 / 8f, 64 / 8f, 1);

            mc.getEntityRenderDispatcher().getRenderer(player)
                    .render(player, 0, 0, graphics.pose(), graphics.bufferSource(), 15728880);

            graphics.pose().popPose();
        }
    }

    // ===== CALCUL MVP =====

    private MVPData calculateMVP() {
        String[] allTeams = TeamManager.getAllTeams();
        MVPData bestMVP = null;
        int maxPoints = 0;

        for (String teamName : allTeams) {
            int teamId = TeamManager.getTeamId(teamName);
            if (teamId == 0) continue;

            String[] members = DataManager.getAllDataNames("team_" + teamId + "_members");

            for (String memberKey : members) {
                String playerUUID = DataManager.dataRead("team_" + teamId + "_members", memberKey);
                int points = DataManager.dataReadInt("player_" + playerUUID + "_stats", "team_points", 0);

                if (points > maxPoints) {
                    maxPoints = points;

                    // Récupérer infos joueur
                    Minecraft mc = Minecraft.getInstance();
                    assert mc.level != null;
                    assert playerUUID != null;
                    Player player = mc.level.getPlayerByUUID(UUID.fromString(playerUUID));
                    String playerName = player != null ? player.getName().getString() : "Joueur inconnu";

                    int kills = DataManager.dataReadInt("player_" + playerUUID + "_stats", "kills", 0);

                    bestMVP = new MVPData(
                            UUID.fromString(playerUUID),
                            playerName,
                            teamName,
                            maxPoints,
                            kills
                    );
                }
            }
        }

        return bestMVP;
    }

    private List<TeamStats> loadTeamStats() {
        String[] allTeams = TeamManager.getAllTeams();
        List<TeamStats> stats = new ArrayList<>();

        for (String teamName : allTeams) {
            int teamId = TeamManager.getTeamId(teamName);
            if (teamId == 0) continue;

            int finalPos = DataManager.dataReadInt("team_" + teamId + "_config", "finalpos", 999);
            int points = DataManager.dataReadInt("team_" + teamId + "_points", "total", 0);
            int kills = DataManager.dataReadInt("team_" + teamId + "_stats", "kills", 0);
            int deaths = DataManager.dataReadInt("team_" + teamId + "_stats", "deaths", 0);
            int blocksMined = DataManager.dataReadInt("team_" + teamId + "_stats", "blocks_mined", 0);
            int damageDealt = DataManager.dataReadInt("team_" + teamId + "_stats", "damage_dealt", 0);

            stats.add(new TeamStats(teamName, points, kills, deaths, blocksMined, damageDealt, finalPos));
        }

        // 🔥 TRIER PAR POSITION FINALE (1er, 2ème, 3ème...)
        stats.sort(Comparator.comparingInt(s -> s.finalPos));

        return stats;
    }

    private static class TeamStats {
        String teamName;
        int points;
        int kills;
        int deaths;
        int blocksMined;
        int damageDealt;
        int finalPos; // 🆕 NOUVEAU

        TeamStats(String name, int pts, int k, int d, int bm, int dd, int fp) {
            this.teamName = name;
            this.points = pts;
            this.kills = k;
            this.deaths = d;
            this.blocksMined = bm;
            this.damageDealt = dd;
            this.finalPos = fp;
        }
    }

    private int getRankColor(int rank, boolean hovered) {
        int base = switch(rank) {
            case 1 -> 0xFFD700; // Or
            case 2 -> 0xC0C0C0; // Argent
            case 3 -> 0xCD7F32; // Bronze
            default -> 0x404040; // Gris
        };

        return hovered ? (base | 0xFF000000) : (0xAA000000 | (base & 0x00FFFFFF));
    }

    private String getRankMedal(int rank) {
        return switch(rank) {
            case 1 -> "🥇";
            case 2 -> "🥈";
            case 3 -> "🥉";
            default -> "  ";
        };
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Ne met pas le jeu en pause
    }

    // ===== CLASSES DE DONNÉES =====

    private static class MVPData {
        UUID playerUUID;
        String playerName;
        String teamName;
        int pointsContributed;
        int kills;

        MVPData(UUID uuid, String name, String team, int points, int kills) {
            this.playerUUID = uuid;
            this.playerName = name;
            this.teamName = team;
            this.pointsContributed = points;
            this.kills = kills;
        }
    }
}
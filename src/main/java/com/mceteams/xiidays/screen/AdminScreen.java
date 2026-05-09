package com.mceteams.xiidays.screen;

import com.mceteams.xiidays.network.AdminActionPayload;
import com.mceteams.xiidays.network.RequestAdminDataPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class AdminScreen extends Screen {

    private static final int PAGE_HOME = 0;
    private static final int PAGE_DAYS = 1;
    private static final int PAGE_TEAMS = 2;
    private static final int PAGE_SPECTATORS = 3;
    private static final int PAGE_DATA = 4;

    public static final Map<String, String> cachedData = new HashMap<>();
    public static final Set<String> requestedData = ConcurrentHashMap.newKeySet();

    private int currentPage = PAGE_HOME;
    private final Deque<Integer> pageHistory = new ArrayDeque<>();
    private final List<Entry> entries = new ArrayList<>();
    private boolean showHomeButton = false;

    private EditBox inputBox;
    private String pendingInputAction = null;

    private String feedbackMessage = "";
    private long feedbackEndTime = 0;

    private record Entry(ItemStack icon, String name, String action, List<String> staticLore, int x, int y, int w, int h) {}

    public AdminScreen() {
        super(Component.literal("XII Days - Administration"));
    }

    @Override
    protected void init() {
        super.init();
        rebuild();
    }

    @Override
    protected void renderBlurredBackground(float partialTick) {
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);

        int cx = width / 2;
        int panelY = height / 2 - 140;

        graphics.fill(cx - 210, panelY - 10, cx + 210, panelY + 270, 0xFF1A1A2E);
        graphics.fill(cx - 210, panelY - 10, cx + 210, panelY - 7, 0xFFFFD700);

        String title = switch (currentPage) {
            case PAGE_DAYS -> "§6§lGESTION DES JOURS";
            case PAGE_TEAMS -> "§6§lGESTION DES ÉQUIPES";
            case PAGE_SPECTATORS -> "§6§lGESTION DES SPECTATEURS";
            case PAGE_DATA -> "§6§lGESTION DES DONNÉES";
            default -> "§6§lADMINISTRATION";
        };
        graphics.drawCenteredString(font, title, cx, panelY + 8, 0xFFD700);

        // Render items
        Entry hovered = null;
        for (Entry e : entries) {
            renderEntry(graphics, e, mouseX, mouseY);
            if (mouseX >= e.x() && mouseX <= e.x() + e.w() && mouseY >= e.y() && mouseY <= e.y() + e.h()) {
                hovered = e;
            }
        }

        // Back button
        if (!pageHistory.isEmpty()) {
            int bx = cx - 85;
            int by = panelY + 240;
            boolean bh = mouseX >= bx && mouseX <= bx + 70 && mouseY >= by && mouseY <= by + 16;
            graphics.fill(bx, by, bx + 70, by + 16, bh ? 0xFF444466 : 0xFF333355);
            graphics.drawCenteredString(font, "§7← Retour", bx + 35, by + 3, 0xAAAAAA);
        }

        // Home button
        if (!pageHistory.isEmpty()) {
            int hx = cx + 15;
            int hy = panelY + 240;
            boolean hh = mouseX >= hx && mouseX <= hx + 70 && mouseY >= hy && mouseY <= hy + 16;
            graphics.fill(hx, hy, hx + 70, hy + 16, hh ? 0xFF444466 : 0xFF333355);
            graphics.drawCenteredString(font, "§6Accueil", hx + 35, hy + 3, 0xFFD700);
        }

        if (inputBox != null) {
            inputBox.render(graphics, mouseX, mouseY, partialTick);
            String prompt = switch (pendingInputAction != null ? pendingInputAction : "") {
                case "day_set" -> "§7Entrez le numéro du jour (1-12) :";
                case "team_create" -> "§7Entrez le nom de la nouvelle équipe :";
                case "team_eliminate" -> "§7Entrez le nom de l'équipe à éliminer :";
                case "team_revive" -> "§7Entrez le nom de l'équipe à réhabiliter :";
                case "team_remove" -> "§7Entrez le nom de l'équipe à supprimer :";
                case "team_add" -> "§7Entrez: <equipe> <joueur>";
                case "team_remove_member" -> "§7Entrez: <equipe> <joueur>";
                case "team_spawn" -> "§7Entrez le nom de l'équipe :";
                case "team_core" -> "§7Entrez le nom de l'équipe :";
                default -> "§7Entrez la valeur :";
            };
            graphics.drawCenteredString(font, prompt, cx, panelY + 220, 0xAAAAAA);
            graphics.drawCenteredString(font, "§8[Entrée: valider  |  ESC: annuler]", cx, panelY + 235, 0x555555);
        }

        if (System.currentTimeMillis() < feedbackEndTime) {
            graphics.drawCenteredString(font, feedbackMessage, cx, panelY + 245, 0xFFFFFF);
        }

        // Tooltip for hovered entry
        if (hovered != null) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal(hovered.name()));
            for (String line : hovered.staticLore()) {
                tooltip.add(Component.literal(line));
            }
            if (hovered.action().startsWith("request:")) {
                String dataType = hovered.action().substring(8);
                String data = cachedData.get(dataType);
                if (data != null) {
                    tooltip.add(Component.literal("§8─ ─ ─ ─ ─ ─"));
                    for (String line : data.split("\n")) {
                        tooltip.add(Component.literal(line));
                    }
                }
            }
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        }

        graphics.drawCenteredString(font, "§8[ESC pour fermer]", cx, panelY + 260, 0x555555);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderEntry(GuiGraphics graphics, Entry e, int mx, int my) {
        int x = e.x();
        int y = e.y();
        int w = e.w();
        int h = e.h();
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        int bg = hover ? 0xFF6B6B8B : 0xFF4A4A6A;
        int bd = hover ? 0xFF8B8BAA : 0xFF5A5A7A;
        // Background
        graphics.fill(x, y, x + w, y + h, bg);
        // 1px border with 2px corner radius
        graphics.fill(x + 2, y, x + w - 2, y + 1, bd);
        graphics.fill(x + 2, y + h - 1, x + w - 2, y + h, bd);
        graphics.fill(x, y + 2, x + 1, y + h - 2, bd);
        graphics.fill(x + w - 1, y + 2, x + w, y + h - 2, bd);
        // Icon centered
        graphics.renderItem(e.icon(), x + 3, y + 3);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (inputBox != null && inputBox.isFocused()) {
            if (keyCode == 257) {
                String value = inputBox.getValue().strip();
                if (!value.isEmpty() && pendingInputAction != null) {
                    PacketDistributor.sendToServer(new AdminActionPayload(pendingInputAction, value));
                    feedbackMessage = "§aAction envoyée !";
                    feedbackEndTime = System.currentTimeMillis() + 2000;
                }
                exitInputMode();
                rebuild();
                return true;
            }
            if (keyCode == 256) {
                exitInputMode();
                rebuild();
                return true;
            }
            return inputBox.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (inputBox != null) {
            inputBox.mouseClicked(mouseX, mouseY, button);
            return super.mouseClicked(mouseX, mouseY, button);
        }

        // Check item clicks
        for (Entry e : entries) {
            if (mouseX >= e.x() && mouseX <= e.x() + e.w() && mouseY >= e.y() && mouseY <= e.y() + e.h()) {
                handleAction(e.action());
                return true;
            }
        }

        // Back button
        if (!pageHistory.isEmpty()) {
            int cx = width / 2;
            int panelY = height / 2 - 140;
            int bx = cx - 85;
            int by = panelY + 240;
            if (mouseX >= bx && mouseX <= bx + 70 && mouseY >= by && mouseY <= by + 16) {
                goBack();
                return true;
            }
            int hx = cx + 15;
            if (mouseX >= hx && mouseX <= hx + 70 && mouseY >= by && mouseY <= by + 16) {
                goHome();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    // ── Layout ──

    private int cx() { return width / 2; }
    private int panelY() { return height / 2 - 140; }
    private int itemX(int col) { return cx() - 89 + col * 52; }
    private int itemY(int row) { return panelY() + 42 + row * 36; }

    private void addEntry(int col, int row, ItemStack icon, String name, String action, String... lore) {
        int x = itemX(col);
        int y = itemY(row);
        entries.add(new Entry(icon, name, action, List.of(lore), x, y, 22, 22));
    }

    private void rebuild() {
        entries.clear();
        switch (currentPage) {
            case PAGE_HOME -> buildHome();
            case PAGE_DAYS -> buildDays();
            case PAGE_TEAMS -> buildTeams();
            case PAGE_SPECTATORS -> buildSpectators();
            case PAGE_DATA -> buildData();
        }
    }

    // ── Pages ──

    private void buildHome() {
        addEntry(0, 0, Items.CLOCK.getDefaultInstance(), "§eJours", "page:1", "§7Gérer les cycles de jour");
        addEntry(1, 0, Items.DIAMOND.getDefaultInstance(), "§bÉquipes", "page:2", "§7Gérer les équipes");
        addEntry(2, 0, Items.ENDER_EYE.getDefaultInstance(), "§aSpectateurs", "page:3", "§7Gérer les spectateurs");
        addEntry(3, 0, Items.BOOK.getDefaultInstance(), "§dDonnées", "page:4", "§7Sauvegarder / Recharger");

        addEntry(0, 2, Items.COMPASS.getDefaultInstance(), "§6Scoreboard", "score_open", "§7Ouvrir le scoreboard");
        addEntry(1, 2, Items.TOTEM_OF_UNDYING.getDefaultInstance(), "§cRespawn All", "spec_respawnall", "§7Respawn tous les spectateurs");
        addEntry(2, 2, Items.WRITABLE_BOOK.getDefaultInstance(), "§aSauvegarder", "data_save", "§7Sauvegarder les données");
    }

    private void buildDays() {
        addEntry(0, 0, Items.LIME_DYE.getDefaultInstance(), "§aDémarrer le jour", "day_start", "§7Commence le cycle du jour");
        addEntry(1, 0, Items.RED_DYE.getDefaultInstance(), "§cArrêter le jour", "day_stop", "§7Arrête le cycle du jour");
        addEntry(2, 0, Items.SUGAR.getDefaultInstance(), "§6Définir jour", "day_set", "§7Définit le jour (1-12)");
        addEntry(3, 0, Items.CLOCK.getDefaultInstance(), "§eStatut du jour", "request:day_status", "§7Cliquez pour le statut");
    }

    private void buildTeams() {
        addEntry(0, 0, Items.EMERALD.getDefaultInstance(), "§aCréer équipe", "team_create", "§7Crée une nouvelle équipe");
        addEntry(1, 0, Items.REDSTONE_BLOCK.getDefaultInstance(), "§cÉliminer équipe", "team_eliminate", "§7Élimine une équipe");
        addEntry(2, 0, Items.LIME_WOOL.getDefaultInstance(), "§aRéhabiliter équipe", "team_revive", "§7Réhabilite une équipe");
        addEntry(3, 0, Items.BARRIER.getDefaultInstance(), "§cSupprimer équipe", "team_remove", "§7Supprime définitivement");

        addEntry(0, 1, Items.PLAYER_HEAD.getDefaultInstance(), "§bAjouter membre", "team_add", "§7Ajoute un joueur");
        addEntry(1, 1, Items.SKELETON_SKULL.getDefaultInstance(), "§cRetirer membre", "team_remove_member", "§7Retire un joueur");
        addEntry(2, 1, Items.ENDER_PEARL.getDefaultInstance(), "§dDéfinir spawn", "team_spawn", "§7Définit le spawn");
        addEntry(3, 1, Items.BEACON.getDefaultInstance(), "§6Définir cœur", "team_core", "§7Définit le cœur");

        addEntry(3, 2, Items.PAPER.getDefaultInstance(), "§eListe équipes", "request:team_list", "§7Cliquez pour la liste");
    }

    private void buildSpectators() {
        addEntry(0, 0, Items.TOTEM_OF_UNDYING.getDefaultInstance(), "§cRespawn All", "spec_respawnall", "§7Respawn tous les spectateurs");
    }

    private void buildData() {
        addEntry(0, 0, Items.WRITABLE_BOOK.getDefaultInstance(), "§aSauvegarder", "data_save", "§7Sauvegarde toutes les données");
        addEntry(1, 0, Items.WRITTEN_BOOK.getDefaultInstance(), "§eRecharger", "data_reload", "§7Recharge depuis les fichiers");
    }

    // ── Actions ──

    private void handleAction(String tag) {
        if (tag == null || tag.isEmpty()) return;

        if (tag.startsWith("page:")) {
            navigateTo(Integer.parseInt(tag.substring(5)));
            return;
        }

        if ("back".equals(tag)) {
            goBack();
            return;
        }

        if (tag.startsWith("request:")) {
            String dataType = tag.substring(8);
            requestedData.add(dataType);
            PacketDistributor.sendToServer(new RequestAdminDataPayload(dataType));
            feedbackMessage = "§eDemande en cours...";
            feedbackEndTime = System.currentTimeMillis() + 3000;
            return;
        }

        // Check if it's an input action (needs inline EditBox)
        if (isInputAction(tag)) {
            enterInputMode(tag);
            return;
        }

        // Regular action: send to server
        PacketDistributor.sendToServer(new AdminActionPayload(tag));
        feedbackMessage = "§aCommande exécutée !";
        feedbackEndTime = System.currentTimeMillis() + 2000;
    }

    private boolean isInputAction(String tag) {
        return switch (tag) {
            case "day_set", "team_create", "team_eliminate", "team_revive",
                 "team_remove", "team_add", "team_remove_member", "team_spawn",
                 "team_core" -> true;
            default -> false;
        };
    }

    private void navigateTo(int page) {
        pageHistory.push(currentPage);
        currentPage = page;
        cachedData.clear();
        rebuild();
    }

    private void goBack() {
        if (!pageHistory.isEmpty()) {
            currentPage = pageHistory.pop();
        }
        cachedData.clear();
        rebuild();
    }

    private void goHome() {
        pageHistory.clear();
        currentPage = PAGE_HOME;
        cachedData.clear();
        rebuild();
    }

    // ── Input mode ──

    private void enterInputMode(String action) {
        pendingInputAction = action;
        feedbackMessage = "";
        feedbackEndTime = 0;
        entries.clear();

        int cx = cx();
        inputBox = new EditBox(font, cx - 80, height / 2 - 10, 160, 20, Component.literal(""));
        inputBox.setMaxLength(100);
        inputBox.setFocused(true);
        addWidget(inputBox);
    }

    private void exitInputMode() {
        pendingInputAction = null;
        removeWidget(inputBox);
        inputBox = null;
    }
}

package com.mceteams.xiidays.screen;

import com.mceteams.xiidays.network.AdminActionPayload;
import com.mceteams.xiidays.network.RequestAdminDataPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
        super(Component.translatable("xiidays.admin.title"));
    }

    @Override
    protected void init() {
        super.init();
        rebuild();
    }

    @Override
    protected void renderBlurredBackground(GuiGraphics guiGraphics) {
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);

        int cx = width / 2;
        int panelY = height / 2 - 140;

        graphics.fill(cx - 210, panelY - 10, cx + 210, panelY + 270, 0xFF1A1A2E);
        graphics.fill(cx - 210, panelY - 10, cx + 210, panelY - 7, 0xFFFFD700);

        Component title = switch (currentPage) {
            case PAGE_DAYS -> Component.translatable("xiidays.admin.page_days");
            case PAGE_TEAMS -> Component.translatable("xiidays.admin.page_teams");
            case PAGE_SPECTATORS -> Component.translatable("xiidays.admin.page_spectators");
            case PAGE_DATA -> Component.translatable("xiidays.admin.page_data");
            default -> Component.translatable("xiidays.admin.page_home");
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
            graphics.drawCenteredString(font, Component.translatable("xiidays.admin.back"), bx + 35, by + 3, 0xAAAAAA);
        }

        // Home button
        if (!pageHistory.isEmpty()) {
            int hx = cx + 15;
            int hy = panelY + 240;
            boolean hh = mouseX >= hx && mouseX <= hx + 70 && mouseY >= hy && mouseY <= hy + 16;
            graphics.fill(hx, hy, hx + 70, hy + 16, hh ? 0xFF444466 : 0xFF333355);
            graphics.drawCenteredString(font, Component.translatable("xiidays.admin.home"), hx + 35, hy + 3, 0xFFD700);
        }

        if (inputBox != null) {
            inputBox.render(graphics, mouseX, mouseY, partialTick);
            Component prompt = switch (pendingInputAction != null ? pendingInputAction : "") {
                case "day_set" -> Component.translatable("xiidays.admin.input_day_set");
                case "team_create" -> Component.translatable("xiidays.admin.input_team_create");
                case "team_eliminate" -> Component.translatable("xiidays.admin.input_team_eliminate");
                case "team_revive" -> Component.translatable("xiidays.admin.input_team_revive");
                case "team_remove" -> Component.translatable("xiidays.admin.input_team_remove");
                case "team_add" -> Component.translatable("xiidays.admin.input_team_add");
                case "team_remove_member" -> Component.translatable("xiidays.admin.input_team_remove_member");
                case "team_spawn" -> Component.translatable("xiidays.admin.input_team_spawn");
                case "team_core" -> Component.translatable("xiidays.admin.input_team_core");
                default -> Component.translatable("xiidays.admin.input_default");
            };
            graphics.drawCenteredString(font, prompt.getString(), cx, panelY + 220, 0xAAAAAA);
            graphics.drawCenteredString(font, Component.translatable("xiidays.admin.input_instructions"), cx, panelY + 235, 0x555555);
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
                    tooltip.add(Component.translatable("xiidays.admin.tooltip_separator"));
                    for (String line : data.split("\n")) {
                        tooltip.add(Component.literal(line));
                    }
                }
            }
            graphics.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
        }

        graphics.drawCenteredString(font, Component.translatable("xiidays.admin.close"), cx, panelY + 260, 0x555555);

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
    public boolean keyPressed(KeyEvent event) {
        if (inputBox != null && inputBox.isFocused()) {
            if (event.key() == 257) {
                String value = inputBox.getValue().strip();
                if (!value.isEmpty() && pendingInputAction != null) {
                    ClientPacketDistributor.sendToServer(new AdminActionPayload(pendingInputAction, value));
                    feedbackMessage = Component.translatable("xiidays.admin.feedback_sent").getString();
                    feedbackEndTime = System.currentTimeMillis() + 2000;
                }
                exitInputMode();
                rebuild();
                return true;
            }
            if (event.key() == 256) {
                exitInputMode();
                rebuild();
                return true;
            }
            return inputBox.keyPressed(event);
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isOverlay) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (inputBox != null) {
            inputBox.mouseClicked(event, isOverlay);
            return super.mouseClicked(event, isOverlay);
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

        return super.mouseClicked(event, isOverlay);
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
        addEntry(0, 0, Items.CLOCK.getDefaultInstance(), Component.translatable("xiidays.admin.home.days").getString(), "page:1", Component.translatable("xiidays.admin.home.days_lore").getString());
        addEntry(1, 0, Items.DIAMOND.getDefaultInstance(), Component.translatable("xiidays.admin.home.teams").getString(), "page:2", Component.translatable("xiidays.admin.home.teams_lore").getString());
        addEntry(2, 0, Items.ENDER_EYE.getDefaultInstance(), Component.translatable("xiidays.admin.home.spectators").getString(), "page:3", Component.translatable("xiidays.admin.home.spectators_lore").getString());
        addEntry(3, 0, Items.BOOK.getDefaultInstance(), Component.translatable("xiidays.admin.home.data").getString(), "page:4", Component.translatable("xiidays.admin.home.data_lore").getString());

        addEntry(0, 2, Items.COMPASS.getDefaultInstance(), Component.translatable("xiidays.admin.home.scoreboard").getString(), "score_open", Component.translatable("xiidays.admin.home.scoreboard_lore").getString());
        addEntry(1, 2, Items.TOTEM_OF_UNDYING.getDefaultInstance(), Component.translatable("xiidays.admin.home.respawn_all").getString(), "spec_respawnall", Component.translatable("xiidays.admin.home.respawn_all_lore").getString());
        addEntry(2, 2, Items.WRITABLE_BOOK.getDefaultInstance(), Component.translatable("xiidays.admin.home.save").getString(), "data_save", Component.translatable("xiidays.admin.home.save_lore").getString());
    }

    private void buildDays() {
        addEntry(0, 0, Items.LIME_DYE.getDefaultInstance(), Component.translatable("xiidays.admin.days.start").getString(), "day_start", Component.translatable("xiidays.admin.days.start_lore").getString());
        addEntry(1, 0, Items.RED_DYE.getDefaultInstance(), Component.translatable("xiidays.admin.days.stop").getString(), "day_stop", Component.translatable("xiidays.admin.days.stop_lore").getString());
        addEntry(2, 0, Items.SUGAR.getDefaultInstance(), Component.translatable("xiidays.admin.days.set").getString(), "day_set", Component.translatable("xiidays.admin.days.set_lore").getString());
        addEntry(3, 0, Items.CLOCK.getDefaultInstance(), Component.translatable("xiidays.admin.days.status").getString(), "request:day_status", Component.translatable("xiidays.admin.days.status_lore").getString());
    }

    private void buildTeams() {
        addEntry(0, 0, Items.EMERALD.getDefaultInstance(), Component.translatable("xiidays.admin.teams.create").getString(), "team_create", Component.translatable("xiidays.admin.teams.create_lore").getString());
        addEntry(1, 0, Items.REDSTONE_BLOCK.getDefaultInstance(), Component.translatable("xiidays.admin.teams.eliminate").getString(), "team_eliminate", Component.translatable("xiidays.admin.teams.eliminate_lore").getString());
        addEntry(2, 0, Items.LIME_WOOL.getDefaultInstance(), Component.translatable("xiidays.admin.teams.revive").getString(), "team_revive", Component.translatable("xiidays.admin.teams.revive_lore").getString());
        addEntry(3, 0, Items.BARRIER.getDefaultInstance(), Component.translatable("xiidays.admin.teams.remove").getString(), "team_remove", Component.translatable("xiidays.admin.teams.remove_lore").getString());

        addEntry(0, 1, Items.PLAYER_HEAD.getDefaultInstance(), Component.translatable("xiidays.admin.teams.add_member").getString(), "team_add", Component.translatable("xiidays.admin.teams.add_member_lore").getString());
        addEntry(1, 1, Items.SKELETON_SKULL.getDefaultInstance(), Component.translatable("xiidays.admin.teams.remove_member").getString(), "team_remove_member", Component.translatable("xiidays.admin.teams.remove_member_lore").getString());
        addEntry(2, 1, Items.ENDER_PEARL.getDefaultInstance(), Component.translatable("xiidays.admin.teams.set_spawn").getString(), "team_spawn", Component.translatable("xiidays.admin.teams.set_spawn_lore").getString());
        addEntry(3, 1, Items.BEACON.getDefaultInstance(), Component.translatable("xiidays.admin.teams.set_core").getString(), "team_core", Component.translatable("xiidays.admin.teams.set_core_lore").getString());

        addEntry(3, 2, Items.PAPER.getDefaultInstance(), Component.translatable("xiidays.admin.teams.list").getString(), "request:team_list", Component.translatable("xiidays.admin.teams.list_lore").getString());
    }

    private void buildSpectators() {
        addEntry(0, 0, Items.TOTEM_OF_UNDYING.getDefaultInstance(), Component.translatable("xiidays.admin.spectators.respawn_all").getString(), "spec_respawnall", Component.translatable("xiidays.admin.spectators.respawn_all_lore").getString());
    }

    private void buildData() {
        addEntry(0, 0, Items.WRITABLE_BOOK.getDefaultInstance(), Component.translatable("xiidays.admin.data.save").getString(), "data_save", Component.translatable("xiidays.admin.data.save_lore").getString());
        addEntry(1, 0, Items.WRITTEN_BOOK.getDefaultInstance(), Component.translatable("xiidays.admin.data.reload").getString(), "data_reload", Component.translatable("xiidays.admin.data.reload_lore").getString());
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
            ClientPacketDistributor.sendToServer(new RequestAdminDataPayload(dataType));
            feedbackMessage = Component.translatable("xiidays.admin.feedback_loading").getString();
            feedbackEndTime = System.currentTimeMillis() + 3000;
            return;
        }

        // Check if it's an input action (needs inline EditBox)
        if (isInputAction(tag)) {
            enterInputMode(tag);
            return;
        }

        // Regular action: send to server
        ClientPacketDistributor.sendToServer(new AdminActionPayload(tag));
        feedbackMessage = Component.translatable("xiidays.admin.feedback_executed").getString();
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

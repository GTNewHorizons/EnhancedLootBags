package eu.usrv.enhancedlootbags.client.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import eu.usrv.enhancedlootbags.EnhancedLootBags;
import eu.usrv.enhancedlootbags.StatHelper;
import eu.usrv.enhancedlootbags.client.ClientLootBagStats;
import eu.usrv.enhancedlootbags.client.LootBagStatsClientHandler;
import eu.usrv.enhancedlootbags.core.LootGroupsHandler;
import eu.usrv.enhancedlootbags.core.serializer.LootGroups;
import eu.usrv.enhancedlootbags.core.serializer.LootGroups.LootGroup;
import eu.usrv.enhancedlootbags.core.stats.LootBagStats;
import eu.usrv.enhancedlootbags.core.stats.LootBagStats.DropStats;
import eu.usrv.enhancedlootbags.core.stats.LootBagStats.GroupStats;

public class GuiLootBagStats extends GuiScreen {

    private static final int ALL_BAGS_ID = Integer.MIN_VALUE;

    private static final int MAX_SCREEN_WIDTH = 480;
    private static final int HEADER_HEIGHT = 24;
    private static final int NAV_WIDTH = 140;
    private static final int NAV_SEARCH_HEIGHT = 20;
    private static final int NAV_FOOTER_HEIGHT = 16;
    private static final int NAV_ROW_HEIGHT = 18;
    private static final int PADDING = 10;
    private static final int SLOT_SIZE = 20;
    private static final int SCROLL_STEP = 18;
    private static final int SECTION_HEADER_HEIGHT = 13;
    private static final int SECTION_GAP = 8;

    private static final int COLOR_BACKGROUND = 0xE8121212;
    private static final int COLOR_LINE = 0x30FFFFFF;
    private static final int COLOR_HOVER = 0x18FFFFFF;
    private static final int COLOR_ACCENT = 0x00D5FF;
    private static final int COLOR_TEXT = 0xFFFFFF;
    private static final int COLOR_BODY = 0xD2D2D2;
    private static final int COLOR_DIM = 0x808080;
    private static final int COLOR_FAINT = 0x505050;
    private static final int COLOR_SCROLLBAR = 0x50FFFFFF;
    private static final int COLOR_OUTSIDE = 0x80000000;

    private enum SortMode {
        AMOUNT,
        TIMES,
        NAME
    }

    private static int selectedID = ALL_BAGS_ID;
    private static boolean showUnopened = true;
    private static SortMode sortMode = SortMode.AMOUNT;

    private int panelLeft;
    private int panelRight;
    private int panelTop;
    private int panelBottom;

    private GuiTextField searchField;

    private LootBagStats stats;
    private final List<BagEntry> allBags = new ArrayList<>();
    private final List<BagEntry> visibleBags = new ArrayList<>();
    private BagEntry selectedBag;
    private final List<DropEntry> drops = new ArrayList<>();
    private final List<DropEntry> mainDrops = new ArrayList<>();
    private final List<DropEntry> trashDrops = new ArrayList<>();
    private final List<Section> sections = new ArrayList<>();

    private int navScroll = 0;
    private int gridScroll = 0;
    private boolean draggingNav = false;
    private boolean draggingGrid = false;

    @Override
    public void initGui() {
        int panelWidth = Math.min(width, MAX_SCREEN_WIDTH);
        panelLeft = (width - panelWidth) / 2;
        panelRight = panelLeft + panelWidth;

        int margin = Math.max(8, Math.min(height / 8, 40));
        panelTop = margin;
        panelBottom = height - margin;

        Keyboard.enableRepeatEvents(true);
        String oldSearch = searchField == null ? "" : searchField.getText();
        searchField = new GuiTextField(
                fontRendererObj,
                panelLeft + PADDING,
                getHeaderBottom() + 6,
                NAV_WIDTH - 2 * PADDING,
                10);
        searchField.setEnableBackgroundDrawing(false);
        searchField.setMaxStringLength(64);
        searchField.setTextColor(COLOR_BODY);
        searchField.setText(oldSearch);

        reloadStats();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void updateScreen() {
        searchField.updateCursorCounter();

        if (ClientLootBagStats.getStats() != stats) {
            reloadStats();
        }
    }

    private void reloadStats() {
        stats = ClientLootBagStats.getStats();
        allBags.clear();

        Map<Integer, BagEntry> byID = new HashMap<>();
        LootGroups groups = EnhancedLootBags.LootGroupHandler.getLootGroupsClient();
        if (groups != null) {
            for (LootGroup grp : groups.getLootTable()) {
                BagEntry entry = new BagEntry(
                        grp.getGroupID(),
                        StatCollector.translateToLocal(grp.getGroupName()),
                        grp.getGroupRarity(),
                        createBagStack(grp.getGroupID()),
                        stats.getGroup(grp.getGroupID()));
                byID.put(entry.id, entry);
                allBags.add(entry);
            }
        }

        for (GroupStats grpStats : stats.getGroups()) {
            if (byID.containsKey(grpStats.getGroupID())) {
                continue;
            }
            BagEntry entry = new BagEntry(
                    grpStats.getGroupID(),
                    String.format(StatHelper.get("gui.stats.unknown_bag"), grpStats.getGroupID()),
                    EnumRarity.common,
                    createBagStack(grpStats.getGroupID()),
                    grpStats);
            byID.put(entry.id, entry);
            allBags.add(entry);
        }

        allBags.sort(
                Comparator.comparingInt((BagEntry e) -> -e.getOpened())
                        .thenComparing(e -> e.name, String.CASE_INSENSITIVE_ORDER));
        allBags.add(
                0,
                new BagEntry(
                        ALL_BAGS_ID,
                        StatHelper.get("gui.stats.all_bags"),
                        EnumRarity.common,
                        new ItemStack(Blocks.chest),
                        null));

        updateVisibleBags();

        selectedBag = allBags.get(0);
        for (BagEntry entry : allBags) {
            if (entry.id == selectedID) {
                selectedBag = entry;
            }
        }
        updateDrops();
    }

    private static ItemStack createBagStack(int groupID) {
        Item bag = LootGroupsHandler.getLootBagItem();
        return bag == null ? new ItemStack(Blocks.chest) : new ItemStack(bag, 1, groupID);
    }

    private void updateVisibleBags() {
        visibleBags.clear();
        String filter = searchField.getText().trim().toLowerCase(Locale.ROOT);
        for (BagEntry entry : allBags) {
            if (entry.id != ALL_BAGS_ID) {
                if (!showUnopened && entry.getOpened() == 0) {
                    continue;
                }
                if (!filter.isEmpty() && !entry.name.toLowerCase(Locale.ROOT).contains(filter)) {
                    continue;
                }
            }
            visibleBags.add(entry);
        }
        navScroll = clamp(navScroll, 0, getNavMaxScroll());
    }

    private void updateDrops() {
        drops.clear();
        if (selectedBag.id == ALL_BAGS_ID) {
            Map<String, DropEntry> merged = new HashMap<>();
            for (GroupStats grp : stats.getGroups()) {
                for (DropStats drop : grp.getDrops()) {
                    if (drop.getDisplayStack() == null) {
                        continue;
                    }
                    String key = (drop.isTrash() ? "T:" : "M:") + getStackKey(drop.getDisplayStack());
                    DropEntry entry = merged.get(key);
                    if (entry == null) {
                        entry = new DropEntry(drop.getDisplayStack(), drop.isTrash());
                        merged.put(key, entry);
                        drops.add(entry);
                    }
                    entry.itemCount += drop.getItemCount();
                    entry.timesDropped += drop.getTimesDropped();
                }
            }
        } else if (selectedBag.stats != null) {
            for (DropStats drop : selectedBag.stats.getDrops()) {
                if (drop.getDisplayStack() == null) {
                    continue;
                }
                DropEntry entry = new DropEntry(drop.getDisplayStack(), drop.isTrash());
                entry.itemCount = drop.getItemCount();
                entry.timesDropped = drop.getTimesDropped();
                drops.add(entry);
            }
        }

        switch (sortMode) {
            case AMOUNT:
                drops.sort(Comparator.comparingLong((DropEntry e) -> -e.itemCount).thenComparing(e -> e.name));
                break;
            case TIMES:
                drops.sort(Comparator.comparingInt((DropEntry e) -> -e.timesDropped).thenComparing(e -> e.name));
                break;
            case NAME:
                drops.sort(Comparator.comparing((DropEntry e) -> e.name, String.CASE_INSENSITIVE_ORDER));
                break;
        }

        mainDrops.clear();
        trashDrops.clear();
        for (DropEntry drop : drops) {
            (drop.trash ? trashDrops : mainDrops).add(drop);
        }
        sections.clear();
        if (!mainDrops.isEmpty()) {
            sections.add(new Section(StatHelper.get("gui.stats.section_main"), mainDrops));
        }
        if (!trashDrops.isEmpty()) {
            sections.add(new Section(StatHelper.get("gui.stats.section_trash"), trashDrops));
        }
        gridScroll = clamp(gridScroll, 0, getGridMaxScroll());
    }

    private static String getStackKey(ItemStack stack) {
        return String.format(
                "%s:%d:%s",
                Item.itemRegistry.getNameForObject(stack.getItem()),
                stack.getItemDamage(),
                stack.stackTagCompound == null ? "" : stack.stackTagCompound.toString());
    }

    private long getSelectedOpened() {
        return selectedBag.id == ALL_BAGS_ID ? stats.getTotalOpened() : selectedBag.getOpened();
    }

    private long getSelectedTotalItems() {
        long total = 0;
        for (DropEntry drop : drops) {
            total += drop.itemCount;
        }
        return total;
    }

    private long getSelectedTotalTimes() {
        return getTimes(drops);
    }

    private static long getTimes(List<DropEntry> drops) {
        long total = 0;
        for (DropEntry drop : drops) {
            total += drop.timesDropped;
        }
        return total;
    }

    private double getLuckPercent() {
        long total = getSelectedTotalTimes();
        return total == 0 ? -1 : 100.0D * getTimes(mainDrops) / total;
    }

    private static int getLuckColor(double percent) {
        double value = Math.max(0, Math.min(100, percent)) / 100.0D;
        int red = 0xE0;
        int green = 0xC8;
        int r = value < 0.5D ? red : (int) (red * (1.0D - value) * 2.0D + 0x30 * (value - 0.5D) * 2.0D);
        int g = value < 0.5D ? (int) (0x30 + (green - 0x30) * value * 2.0D) : green;
        return (r & 255) << 16 | (g & 255) << 8 | 0x30;
    }

    private int getHeaderBottom() {
        return panelTop + HEADER_HEIGHT;
    }

    private int getNavTop() {
        return getHeaderBottom() + NAV_SEARCH_HEIGHT;
    }

    private int getNavBottom() {
        return panelBottom - NAV_FOOTER_HEIGHT;
    }

    private int getNavMaxScroll() {
        return Math.max(0, visibleBags.size() * NAV_ROW_HEIGHT - (getNavBottom() - getNavTop()));
    }

    private int getContentLeft() {
        return panelLeft + NAV_WIDTH + PADDING;
    }

    private int getContentRight() {
        return panelRight - PADDING;
    }

    private int getLuckY() {
        return getHeaderBottom() + PADDING + 26;
    }

    private int getGridTop() {
        return getHeaderBottom() + PADDING + 44;
    }

    private int getGridBottom() {
        return panelBottom - PADDING;
    }

    private int getGridColumns() {
        return Math.max(1, (getContentRight() - getContentLeft()) / SLOT_SIZE);
    }

    private int getSectionHeight(Section section) {
        return SECTION_HEADER_HEIGHT + (section.drops.size() + getGridColumns() - 1) / getGridColumns() * SLOT_SIZE;
    }

    private int getGridContentHeight() {
        int contentHeight = 0;
        for (Section section : sections) {
            contentHeight += getSectionHeight(section) + SECTION_GAP;
        }
        return Math.max(0, contentHeight - SECTION_GAP);
    }

    private int getGridMaxScroll() {
        return Math.max(0, getGridContentHeight() - (getGridBottom() - getGridTop()));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private static boolean isInside(int x, int y, int left, int top, int right, int bottom) {
        return x >= left && x < right && y >= top && y < bottom;
    }

    private BagEntry getBagAt(int x, int y) {
        if (!isInside(x, y, panelLeft, getNavTop(), panelLeft + NAV_WIDTH, getNavBottom())) {
            return null;
        }
        int index = (y - getNavTop() + navScroll) / NAV_ROW_HEIGHT;
        return index < visibleBags.size() ? visibleBags.get(index) : null;
    }

    private DropEntry getDropAt(int x, int y) {
        int columns = getGridColumns();
        if (!isInside(x, y, getContentLeft(), getGridTop(), getContentLeft() + columns * SLOT_SIZE, getGridBottom())) {
            return null;
        }
        int sectionTop = getGridTop() - gridScroll;
        for (Section section : sections) {
            int itemsTop = sectionTop + SECTION_HEADER_HEIGHT;
            if (y >= itemsTop && y < sectionTop + getSectionHeight(section)) {
                int index = (y - itemsTop) / SLOT_SIZE * columns + (x - getContentLeft()) / SLOT_SIZE;
                return index < section.drops.size() ? section.drops.get(index) : null;
            }
            sectionTop += getSectionHeight(section) + SECTION_GAP;
        }
        return null;
    }

    private String getSortLabel() {
        return StatHelper.get("gui.stats.sort_" + sortMode.name().toLowerCase(Locale.ROOT));
    }

    private String getFilterLabel() {
        return StatHelper.get(showUnopened ? "gui.stats.filter_all" : "gui.stats.filter_opened");
    }

    private boolean isOverSortLink(int x, int y) {
        if (drops.isEmpty()) {
            return false;
        }
        int textWidth = fontRendererObj.getStringWidth(getSortLabel());
        int linkY = getHeaderBottom() + PADDING + 12;
        return isInside(x, y, getContentRight() - textWidth - 1, linkY - 1, getContentRight() + 1, linkY + 9);
    }

    private boolean isOverFilterLink(int x, int y) {
        int linkY = panelBottom - NAV_FOOTER_HEIGHT + 4;
        int textWidth = fontRendererObj.getStringWidth(getFilterLabel());
        return isInside(x, y, panelLeft + PADDING - 1, linkY - 1, panelLeft + PADDING + textWidth + 1, linkY + 9);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (searchField.textboxKeyTyped(typedChar, keyCode)) {
            updateVisibleBags();
            return;
        }

        if (keyCode == Keyboard.KEY_ESCAPE
                || (keyCode != Keyboard.KEY_NONE && (keyCode == mc.gameSettings.keyBindInventory.getKeyCode()
                        || keyCode == LootBagStatsClientHandler.KEY_OPEN_STATS.getKeyCode()))) {
            mc.displayGuiScreen(null);
        }
    }

    @Override
    protected void mouseClicked(int x, int y, int button) {
        searchField.mouseClicked(x, y, button);

        if (button == 1 && isInside(x, y, panelLeft, getHeaderBottom(), panelLeft + NAV_WIDTH, getNavTop())) {
            searchField.setText("");
            updateVisibleBags();
            return;
        }

        if ((button == 0 || button == 1) && isOverSortLink(x, y)) {
            int count = SortMode.values().length;
            sortMode = SortMode.values()[(sortMode.ordinal() + (button == 0 ? 1 : count - 1)) % count];
            updateDrops();
            return;
        }
        if (button != 0) {
            return;
        }

        if (isOverFilterLink(x, y)) {
            showUnopened = !showUnopened;
            updateVisibleBags();
            return;
        }

        if (getNavMaxScroll() > 0
                && isInside(x, y, panelLeft + NAV_WIDTH - 5, getNavTop(), panelLeft + NAV_WIDTH, getNavBottom())) {
            draggingNav = true;
            mouseClickMove(x, y, button, 0);
            return;
        }
        if (getGridMaxScroll() > 0 && isInside(x, y, panelRight - 5, getGridTop(), panelRight, getGridBottom())) {
            draggingGrid = true;
            mouseClickMove(x, y, button, 0);
            return;
        }

        BagEntry bag = getBagAt(x, y);
        if (bag != null && bag != selectedBag) {
            selectedBag = bag;
            selectedID = bag.id;
            gridScroll = 0;
            updateDrops();
        }
    }

    @Override
    protected void mouseClickMove(int x, int y, int button, long time) {
        if (draggingNav) {
            float pos = (y - getNavTop()) / (float) (getNavBottom() - getNavTop());
            navScroll = clamp(Math.round(pos * getNavMaxScroll()), 0, getNavMaxScroll());
        } else if (draggingGrid) {
            float pos = (y - getGridTop()) / (float) (getGridBottom() - getGridTop());
            gridScroll = clamp(Math.round(pos * getGridMaxScroll()), 0, getGridMaxScroll());
        }
    }

    @Override
    protected void mouseMovedOrUp(int x, int y, int button) {
        super.mouseMovedOrUp(x, y, button);
        if (button >= 0) {
            draggingNav = false;
            draggingGrid = false;
        }
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }

        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int delta = (wheel > 0 ? -1 : 1) * SCROLL_STEP * (GuiScreen.isShiftKeyDown() ? 5 : 1);
        if (mouseX < panelLeft + NAV_WIDTH) {
            navScroll = clamp(navScroll + delta, 0, getNavMaxScroll());
        } else {
            gridScroll = clamp(gridScroll + delta, 0, getGridMaxScroll());
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, COLOR_OUTSIDE);
        drawRect(panelLeft, panelTop, panelRight, panelBottom, COLOR_BACKGROUND);

        drawRect(panelLeft, panelTop, panelRight, panelTop + 1, COLOR_LINE);
        drawRect(panelLeft, panelBottom - 1, panelRight, panelBottom, COLOR_LINE);
        drawRect(panelLeft, panelTop + 1, panelLeft + 1, panelBottom - 1, COLOR_LINE);
        drawRect(panelRight - 1, panelTop + 1, panelRight, panelBottom - 1, COLOR_LINE);

        fontRendererObj.drawString(StatHelper.get("gui.stats.title"), panelLeft + PADDING, panelTop + 8, COLOR_TEXT);
        String total = String.format(StatHelper.get("gui.stats.total_opened"), formatFull(stats.getTotalOpened()));
        fontRendererObj.drawString(
                total,
                panelRight - PADDING - fontRendererObj.getStringWidth(total),
                panelTop + 8,
                COLOR_DIM);
        drawRect(panelLeft, getHeaderBottom() - 1, panelRight, getHeaderBottom(), COLOR_LINE);
        drawRect(panelLeft + NAV_WIDTH - 1, getHeaderBottom(), panelLeft + NAV_WIDTH, panelBottom, COLOR_LINE);

        drawNavigation(mouseX, mouseY);
        DropEntry hovered = drawContent(mouseX, mouseY);

        if (hovered != null) {
            drawDropTooltip(hovered, mouseX, mouseY);
        } else {
            BagEntry bag = getBagAt(mouseX, mouseY);
            if (bag != null && fontRendererObj.getStringWidth(bag.name) > getNavNameWidth(bag)) {
                List<String> tip = new ArrayList<>();
                tip.add(bag.rarity.rarityColor + bag.name);
                drawHoveringText(tip, mouseX, mouseY, fontRendererObj);
            }
        }
    }

    private int getNavNameWidth(BagEntry entry) {
        return NAV_WIDTH - PADDING - 22 - 6 - fontRendererObj.getStringWidth(getNavCount(entry));
    }

    private String getNavCount(BagEntry entry) {
        return formatCompact(entry.id == ALL_BAGS_ID ? stats.getTotalOpened() : entry.getOpened());
    }

    private void drawNavigation(int mouseX, int mouseY) {
        searchField.drawTextBox();
        if (searchField.getText().isEmpty() && !searchField.isFocused()) {
            fontRendererObj.drawString(
                    StatHelper.get("gui.stats.search"),
                    searchField.xPosition,
                    searchField.yPosition,
                    COLOR_FAINT);
        }
        int lineY = getHeaderBottom() + 17;
        drawRect(
                panelLeft + PADDING,
                lineY,
                panelLeft + NAV_WIDTH - PADDING,
                lineY + 1,
                searchField.isFocused() ? 0xFF000000 | COLOR_ACCENT : COLOR_LINE);

        int top = getNavTop();
        int bottom = getNavBottom();
        BagEntry hovered = getBagAt(mouseX, mouseY);

        beginScissor(panelLeft, top, NAV_WIDTH - 1, bottom - top);
        for (int i = 0; i < visibleBags.size(); i++) {
            int y = top + i * NAV_ROW_HEIGHT - navScroll;
            if (y + NAV_ROW_HEIGHT <= top || y >= bottom) {
                continue;
            }

            BagEntry entry = visibleBags.get(i);
            boolean selected = entry == selectedBag;
            boolean opened = entry.id == ALL_BAGS_ID || entry.getOpened() > 0;

            if (selected) {
                drawRect(panelLeft, y, panelLeft + 2, y + NAV_ROW_HEIGHT, 0xFF000000 | COLOR_ACCENT);
            }
            if (entry == hovered) {
                drawRect(panelLeft, y, panelLeft + NAV_WIDTH - 1, y + NAV_ROW_HEIGHT, COLOR_HOVER);
            }

            drawItem(entry.icon, panelLeft + PADDING - 2, y + 1, null);

            int nameColor = selected ? COLOR_TEXT : entry == hovered ? COLOR_ACCENT : opened ? COLOR_BODY : COLOR_DIM;
            fontRendererObj.drawString(
                    fontRendererObj.trimStringToWidth(entry.name, getNavNameWidth(entry)),
                    panelLeft + PADDING + 18,
                    y + 5,
                    nameColor);
            String count = getNavCount(entry);
            fontRendererObj.drawString(
                    count,
                    panelLeft + NAV_WIDTH - 6 - fontRendererObj.getStringWidth(count),
                    y + 5,
                    opened ? COLOR_DIM : COLOR_FAINT);
        }
        endScissor();

        drawScrollbar(panelLeft + NAV_WIDTH - 3, top, bottom, navScroll, visibleBags.size() * NAV_ROW_HEIGHT);

        String filter = getFilterLabel();
        fontRendererObj.drawString(
                filter,
                panelLeft + PADDING,
                panelBottom - NAV_FOOTER_HEIGHT + 4,
                isOverFilterLink(mouseX, mouseY) ? COLOR_ACCENT : COLOR_DIM);
    }

    private DropEntry drawContent(int mouseX, int mouseY) {
        int left = getContentLeft();
        int right = getContentRight();
        int y = getHeaderBottom() + PADDING;

        fontRendererObj.drawString(
                selectedBag.rarity.rarityColor + fontRendererObj.trimStringToWidth(selectedBag.name, right - left),
                left,
                y,
                COLOR_TEXT);
        String summary = EnumChatFormatting.GRAY + String.format(
                StatHelper.get("gui.stats.summary"),
                highlight(formatFull(getSelectedOpened())),
                highlight(formatFull(getSelectedTotalItems())),
                highlight(formatFull(drops.size())));
        String sort = getSortLabel();
        int sortWidth = fontRendererObj.getStringWidth(sort);
        fontRendererObj.drawString(
                fontRendererObj.trimStringToWidth(summary, right - left - sortWidth - 8),
                left,
                y + 12,
                COLOR_DIM);
        if (!drops.isEmpty()) {
            fontRendererObj.drawString(
                    sort,
                    right - sortWidth,
                    y + 12,
                    isOverSortLink(mouseX, mouseY) ? COLOR_ACCENT : COLOR_DIM);
        }

        double luck = getLuckPercent();
        if (luck >= 0) {
            int luckY = getLuckY();
            int luckColor = getLuckColor(luck);
            String label = StatHelper.get("gui.stats.luck") + " ";
            String value = String.format("%.1f%%", luck);
            fontRendererObj.drawString(label, left, luckY, COLOR_DIM);
            int valueX = left + fontRendererObj.getStringWidth(label);
            fontRendererObj.drawString(value, valueX, luckY, luckColor);
            int barLeft = valueX + fontRendererObj.getStringWidth(value) + 8;
            int barY = luckY + 3;
            if (right - barLeft > 10) {
                drawRect(barLeft, barY, right, barY + 3, COLOR_LINE);
                int fill = (int) Math.round((right - barLeft) * luck / 100.0D);
                drawRect(barLeft, barY, barLeft + fill, barY + 3, 0xFF000000 | luckColor);
            }
        }

        int gridTop = getGridTop();
        int gridBottom = getGridBottom();

        if (drops.isEmpty()) {
            String message = StatHelper
                    .get(getSelectedOpened() == 0 ? "gui.stats.never_opened" : "gui.stats.nothing_dropped");
            @SuppressWarnings("unchecked")
            List<String> lines = fontRendererObj.listFormattedStringToWidth(message, right - left);
            int textY = gridTop + (gridBottom - gridTop) / 2 - lines.size() * 5;
            for (String line : lines) {
                fontRendererObj.drawString(
                        line,
                        (left + right - fontRendererObj.getStringWidth(line)) / 2,
                        textY,
                        COLOR_FAINT);
                textY += 10;
            }
            return null;
        }

        int columns = getGridColumns();
        DropEntry hovered = getDropAt(mouseX, mouseY);
        long totalTimes = getSelectedTotalTimes();
        beginScissor(left, gridTop, panelRight - left, gridBottom - gridTop);
        int sectionTop = gridTop - gridScroll;
        for (Section section : sections) {
            if (sectionTop + SECTION_HEADER_HEIGHT > gridTop && sectionTop < gridBottom) {
                String share = String.format(
                        "%s  %s",
                        formatFull(getTimes(section.drops)),
                        formatPercent(getTimes(section.drops), totalTimes));
                fontRendererObj.drawString(section.title, left, sectionTop + 1, COLOR_BODY);
                fontRendererObj.drawString(
                        share,
                        left + fontRendererObj.getStringWidth(section.title) + 6,
                        sectionTop + 1,
                        COLOR_FAINT);
            }

            int itemsTop = sectionTop + SECTION_HEADER_HEIGHT;
            for (int i = 0; i < section.drops.size(); i++) {
                int x = left + (i % columns) * SLOT_SIZE;
                int slotY = itemsTop + (i / columns) * SLOT_SIZE;
                if (slotY + SLOT_SIZE <= gridTop || slotY >= gridBottom) {
                    continue;
                }

                DropEntry drop = section.drops.get(i);
                if (drop == hovered) {
                    drawRect(x, slotY, x + SLOT_SIZE, slotY + SLOT_SIZE, COLOR_HOVER);
                }
                drawItem(drop.stack, x + 1, slotY + 1, formatCompact(drop.itemCount));
            }
            sectionTop += getSectionHeight(section) + SECTION_GAP;
        }
        endScissor();

        drawScrollbar(panelRight - 3, gridTop, gridBottom, gridScroll, getGridContentHeight());
        return hovered;
    }

    private void drawScrollbar(int x, int top, int bottom, int scroll, int contentHeight) {
        int viewHeight = bottom - top;
        int maxScroll = contentHeight - viewHeight;
        if (maxScroll <= 0) {
            return;
        }
        int thumbHeight = Math.max(10, viewHeight * viewHeight / contentHeight);
        int thumbTop = top + (viewHeight - thumbHeight) * scroll / maxScroll;
        drawRect(x, thumbTop, x + 2, thumbTop + thumbHeight, COLOR_SCROLLBAR);
    }

    private void drawItem(ItemStack stack, int x, int y, String overlay) {
        GL11.glPushMatrix();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        itemRender.zLevel = 100.0F;
        try {
            itemRender.renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(), stack, x + 1, y + 1);
            if (overlay != null) {
                itemRender.renderItemOverlayIntoGUI(fontRendererObj, mc.getTextureManager(), stack, x + 1, y + 1, "");
            }
        } catch (Exception ignored) {}
        itemRender.zLevel = 0.0F;
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glDisable(GL11.GL_LIGHTING);
        if (overlay != null) {
            drawStackCount(overlay, x, y);
        }
        GL11.glPopMatrix();
    }

    private void drawStackCount(String count, int x, int y) {
        float scale = count.length() <= 2 ? 1.0F : 0.5F;
        GL11.glPushMatrix();
        GL11.glTranslatef(x + 17, y + 17, 0.0F);
        GL11.glScalef(scale, scale, 1.0F);
        fontRendererObj.drawStringWithShadow(count, -fontRendererObj.getStringWidth(count), -7, 0xFFFFFF);
        GL11.glPopMatrix();
    }

    @SuppressWarnings("unchecked")
    private void drawDropTooltip(DropEntry drop, int mouseX, int mouseY) {
        List<String> tip = new ArrayList<>();
        List<String> itemTip;
        try {
            itemTip = drop.stack.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips);
        } catch (Exception e) {
            itemTip = new ArrayList<>();
            itemTip.add(drop.name);
        }
        for (int i = 0; i < itemTip.size(); i++) {
            if (i == 0) {
                tip.add(drop.stack.getRarity().rarityColor + itemTip.get(i));
            } else {
                tip.add(EnumChatFormatting.GRAY + itemTip.get(i));
            }
        }

        long opened = getSelectedOpened();
        long totalTimes = getSelectedTotalTimes();

        if (tip.size() > 1) {
            tip.add("");
        }

        tip.add(String.format(StatHelper.get("gui.stats.tip_received"), formatFull(drop.itemCount)));
        tip.add(String.format(StatHelper.get("gui.stats.tip_times"), formatFull(drop.timesDropped)));
        if (opened > 0) {
            tip.add(
                    String.format(
                            StatHelper.get("gui.stats.tip_per_bag"),
                            String.format("%.2f", (double) drop.itemCount / opened)));
        }
        if (totalTimes > 0) {
            tip.add(String.format(StatHelper.get("gui.stats.tip_share"), formatPercent(drop.timesDropped, totalTimes)));
        }

        drawHoveringText(tip, mouseX, mouseY, fontRendererObj);
    }

    private void beginScissor(int x, int y, int clipWidth, int clipHeight) {
        int scale = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(
                x * scale,
                mc.displayHeight - (y + clipHeight) * scale,
                Math.max(0, clipWidth * scale),
                Math.max(0, clipHeight * scale));
    }

    private static void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private String highlight(String number) {
        return EnumChatFormatting.WHITE + number + EnumChatFormatting.GRAY;
    }

    private static String formatPercent(long part, long total) {
        return total <= 0 ? "-" : String.format("%.1f%%", 100.0D * part / total);
    }

    private static String formatFull(long number) {
        return String.format(Locale.ROOT, "%,d", number).replace(',', ' ');
    }

    private static String formatCompact(long number) {
        if (number < 1000) {
            return Long.toString(number);
        }
        if (number < 10000) {
            return (number / 1000) + "." + (number / 100 % 10) + "k";
        }
        if (number < 1000000) {
            return (number / 1000) + "k";
        }
        if (number < 10000000) {
            return (number / 1000000) + "." + (number / 100000 % 10) + "M";
        }
        if (number < 1000000000) {
            return (number / 1000000) + "M";
        }
        return (number / 1000000000) + "G";
    }

    private static class BagEntry {

        private final int id;
        private final String name;
        private final EnumRarity rarity;
        private final ItemStack icon;
        private final GroupStats stats;

        private BagEntry(int id, String name, EnumRarity rarity, ItemStack icon, GroupStats stats) {
            this.id = id;
            this.name = name;
            this.rarity = rarity;
            this.icon = icon;
            this.stats = stats;
        }

        private int getOpened() {
            return stats == null ? 0 : stats.getOpened();
        }
    }

    private static class DropEntry {

        private final ItemStack stack;
        private final String name;
        private long itemCount;
        private int timesDropped;
        private final boolean trash;

        private DropEntry(ItemStack stack, boolean trash) {
            this.stack = stack;
            this.trash = trash;
            String name;
            try {
                name = stack.getDisplayName();
            } catch (Exception e) {
                name = String.valueOf(Item.itemRegistry.getNameForObject(stack.getItem()));
            }
            this.name = name;
        }
    }

    private static class Section {

        private final String title;
        private final List<DropEntry> drops;

        private Section(String title, List<DropEntry> drops) {
            this.title = title;
            this.drops = drops;
        }
    }
}

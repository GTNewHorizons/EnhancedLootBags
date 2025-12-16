package eu.usrv.enhancedlootbags.integration.nei;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.ItemStackMap;
import codechicken.nei.NEIServerUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.TemplateRecipeHandler;
import eu.usrv.enhancedlootbags.EnhancedLootBags;
import eu.usrv.enhancedlootbags.core.LootGroupsHandler.FortuneLevel;
import eu.usrv.enhancedlootbags.core.serializer.LootGroups.LootGroup;
import eu.usrv.enhancedlootbags.core.serializer.LootGroups.LootGroup.Drop;

public class LootBagRecipeHandler extends TemplateRecipeHandler {

    private static final int SLOT_NUM_X = 9;
    private static final DecimalFormat chanceFormat = new DecimalFormat("##0.##");

    private class CachedLootBagRecipe extends CachedRecipe {

        private final List<PositionedStack> input = new ArrayList<>();
        private final List<PositionedStack> outputs = new ArrayList<>();
        private int rows = 0;
        private final LootGroup lootGroup;
        private Point focus;

        public CachedLootBagRecipe(LootGroup lootGroup, ItemStack focusStack, int fortuneLevel) {
            this.lootGroup = lootGroup;

            ItemStack lootBagStack = lootGroup.createLootBagItemStack();
            if (fortuneLevel > 0) {
                lootBagStack.addEnchantment(Enchantment.fortune, Math.min(fortuneLevel, FortuneLevel.LV3.level));
            }
            this.input.add(new PositionedStack(lootBagStack, 3, 4));

            final List<List<Drop>> sortedDrops = getDropGroups(lootGroup);
            sortedDrops.sort(Comparator.comparingInt(d -> -getAccumulatedWeight(d)));

            int row = 0;
            int col = 0;
            for (List<Drop> dropGroup : sortedDrops) {
                ItemStackMap<List<String>> tooltips = new ItemStackMap<>();

                List<ItemStack> dropItems = new ArrayList<>();
                for (Drop drop : dropGroup) {
                    List<String> tooltip = new ArrayList<>();
                    ItemStack dropItem = drop.getItemStack();
                    if (dropItem == null) {
                        dropItem = new ItemStack(Blocks.fire);
                        tooltip.add(String.format("no entries found for item name \"%s\"", drop.getItemName()));
                    }
                    addTooltip(drop, tooltip, dropGroup, fortuneLevel);

                    dropItems.add(dropItem);
                    tooltips.put(dropItem, tooltip);
                }

                int xPos = 3 + 18 * col;
                int yPos = 33 + 18 * row;
                this.outputs.add(new TooltipStack(dropItems, xPos, yPos, tooltips));

                col++;
                if (col >= SLOT_NUM_X) {
                    col = 0;
                    row++;
                }

                if (focusStack != null && dropItems.stream()
                        .anyMatch(stack -> NEIServerUtils.areStacksSameTypeCrafting(focusStack, stack))) {
                    this.focus = new Point(xPos - 1, yPos - 1);
                }
            }

            this.rows = col == 0 ? row - 1 : row;
        }

        private void addTooltip(Drop drop, List<String> tooltip, List<Drop> dropGroup, int fortuneLevel) {
            if (Minecraft.getMinecraft().gameSettings.advancedItemTooltips) {
                tooltip.add(I18n.format("enhancedlootbags.nei.recipe.drop_id", drop.getIdentifier()));
            }
            if (drop.getIsRandomAmount()) {
                tooltip.add(I18n.format("enhancedlootbags.nei.recipe.amount_random", 1, drop.getAmount()));
            } else {
                tooltip.add(I18n.format("enhancedlootbags.nei.recipe.amount_exact", drop.getAmount()));
            }
            if (Minecraft.getMinecraft().gameSettings.advancedItemTooltips) {
                tooltip.add(I18n.format("enhancedlootbags.nei.recipe.weight", drop.getChance()));
                tooltip.add(
                        I18n.format("enhancedlootbags.nei.recipe.weight_accumulated", getAccumulatedWeight(dropGroup)));
            }
            tooltip.add(
                    I18n.format(
                            "enhancedlootbags.nei.recipe.chance",
                            getPercentageString(dropGroup, lootGroup, FortuneLevel.LV0, fortuneLevel),
                            getPercentageString(dropGroup, lootGroup, FortuneLevel.LV1, fortuneLevel),
                            getPercentageString(dropGroup, lootGroup, FortuneLevel.LV2, fortuneLevel),
                            getPercentageString(dropGroup, lootGroup, FortuneLevel.LV3, fortuneLevel)));
            if (Minecraft.getMinecraft().gameSettings.advancedItemTooltips && !drop.getItemDropGroup().isEmpty()) {
                tooltip.add(I18n.format("enhancedlootbags.nei.recipe.drop_group", drop.getItemDropGroup()));
            }
            if (drop.getLimitedDropCount() > 0) {
                int currentDropCount = EnhancedLootBags.LootGroupHandler
                        .getDropCount(Minecraft.getMinecraft().thePlayer, lootGroup, drop);
                String toAdd = I18n
                        .format("enhancedlootbags.nei.recipe.limit", currentDropCount, drop.getLimitedDropCount());
                if (currentDropCount >= drop.getLimitedDropCount()) {
                    toAdd += " " + EnumChatFormatting.DARK_RED
                            + I18n.format("enhancedlootbags.nei.recipe.limit_reached");
                }
                tooltip.add(toAdd);
            }
        }

        @Override
        public List<PositionedStack> getIngredients() {
            return input;
        }

        @Override
        public PositionedStack getResult() {
            return null;
        }

        @Override
        public List<PositionedStack> getOtherStacks() {
            return getCycledIngredients(cycleticks / 20, outputs);
        }

        private List<List<Drop>> getDropGroups(LootGroup lootGroup) {
            List<List<Drop>> ret = new ArrayList<>();
            outer: for (Drop drop : lootGroup.getDrops()) {
                if (drop.getItemDropGroup().isEmpty()) {
                    ret.add(Collections.singletonList(drop));
                } else {
                    for (List<Drop> searching : ret) {
                        if (searching.get(0).getItemDropGroup().equalsIgnoreCase(drop.getItemDropGroup())) {
                            searching.add(drop);
                            continue outer;
                        }
                    }
                    ret.add(new ArrayList<>(Collections.singletonList(drop)));
                }
            }
            return ret;
        }

        private String getPercentageString(List<Drop> dropGroup, LootGroup lootGroup, FortuneLevel levelToCompare,
                int actualLevel) {
            // Item is chosen from all drop pools, then items from the same group are also added to result
            // Thus chance of the same group accumulates
            // c.f. LootGroupsHandler#getItemGroupDrops
            double percentage = dropGroup.stream().mapToDouble(
                    d -> EnhancedLootBags.LootGroupHandler.calcPercentageFromWeight(d, lootGroup, levelToCompare))
                    .sum();
            return (levelToCompare.level == actualLevel ? EnumChatFormatting.UNDERLINE.toString() : "")
                    + chanceFormat.format(percentage)
                    + EnumChatFormatting.RESET;
        }

        private int getAccumulatedWeight(List<Drop> dropGroup) {
            return dropGroup.stream().mapToInt(Drop::getChance).sum();
        }
    }

    @Override
    public void loadTransferRects() {
        transferRects.add(new RecipeTransferRect(new Rectangle(4, 20, 15, 13), getOverlayIdentifier()));
    }

    @Override
    public void loadCraftingRecipes(String outputId, Object... results) {
        if (outputId.equals(getOverlayIdentifier()) && getClass() == LootBagRecipeHandler.class) {
            for (LootGroup lootGroup : getLootGroups()) {
                arecipes.add(new CachedLootBagRecipe(lootGroup, null, 0));
            }
        } else {
            super.loadCraftingRecipes(outputId, results);
        }
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        for (LootGroup lootGroup : getLootGroups()) {
            if (lootGroup.getDrops().stream()
                    .anyMatch(d -> NEIServerUtils.areStacksSameTypeCrafting(result, d.getItemStack()))) {
                arecipes.add(new CachedLootBagRecipe(lootGroup, result, 0));
            }
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        for (LootGroup lootGroup : getLootGroups()) {
            ItemStack lootBagStack = lootGroup.createLootBagItemStack();
            if (NEIServerUtils.areStacksSameTypeCrafting(ingredient, lootBagStack)) {
                arecipes.add(
                        new CachedLootBagRecipe(
                                lootGroup,
                                null,
                                EnchantmentHelper.getEnchantmentLevel(Enchantment.fortune.effectId, ingredient)));
            }
        }
    }

    private List<LootGroup> getLootGroups() {
        return EnhancedLootBags.LootGroupHandler.getLootGroups().getLootTable();
    }

    @Override
    public void drawExtras(int recipe) {
        CachedLootBagRecipe cachedRecipe = (CachedLootBagRecipe) this.arecipes.get(recipe);
        if (cachedRecipe.lootGroup.getCombineWithTrash() && !isEnchantedLootbagIngredient(cachedRecipe)) {
            drawTrashBagInformation(cachedRecipe);
        }
    }

    private static boolean isEnchantedLootbagIngredient(CachedLootBagRecipe cachedRecipe) {
        List<PositionedStack> ingredients = cachedRecipe.getIngredients();
        if (ingredients.isEmpty()) return false;
        int fortuneLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.fortune.effectId, ingredients.get(0).item);
        return fortuneLevel == 3;
    }

    private static void drawTrashBagInformation(CachedLootBagRecipe cachedRecipe) {
        String trashBagHeader = StatCollector.translateToLocal("enhancedlootbags.nei.recipe.also_has_loot_from");
        GuiDraw.drawString(trashBagHeader, 24, 4, 0x000000, false);
        LootGroup trashBag = EnhancedLootBags.LootGroupHandler.getGroupByID(cachedRecipe.lootGroup.getTrashGroup());
        GuiDraw.drawString(StatCollector.translateToLocal(trashBag.getGroupName()), 24, 13, 0x000000, false);
    }

    @Override
    public int getRecipeHeight(int recipeIndex) {
        return 32 + 18 * (((CachedLootBagRecipe) this.arecipes.get(recipeIndex)).rows + 1) + 33;
    }

    @Override
    public void drawBackground(int recipeIndex) {
        GL11.glColor4f(1, 1, 1, 1);
        GuiDraw.changeTexture(getGuiTexture());
        GuiDraw.drawTexturedModalRect(2, 3, 7, 14, 162, 47);

        for (int r = 1; r <= ((CachedLootBagRecipe) this.arecipes.get(recipeIndex)).rows; r++) {
            GuiDraw.drawTexturedModalRect(2, 32 + 18 * r, 7, 43, 162, 18);
        }
    }

    @Override
    public List<String> handleItemTooltip(GuiRecipe<?> gui, ItemStack cursorStack, List<String> currenttip,
            int recipe) {
        CachedLootBagRecipe cachedRecipe = (CachedLootBagRecipe) this.arecipes.get(recipe);
        for (PositionedStack pStack : cachedRecipe.outputs) {
            if (!gui.isMouseOver(pStack, recipe)) continue;
            if (!(pStack instanceof TooltipStack)) continue;

            TooltipStack tStack = (TooltipStack) pStack;
            List<String> tooltip = tStack.getTooltip(cursorStack);
            if (tooltip != null) {
                currenttip.addAll(tooltip);
                break;
            }
        }
        return currenttip;
    }

    @Override
    public String getGuiTexture() {
        return new ResourceLocation(EnhancedLootBags.MODID, "textures/gui/nei.png").toString();
    }

    @Override
    public String getOverlayIdentifier() {
        return EnhancedLootBags.MODID;
    }

    @Override
    public String getHandlerId() {
        return EnhancedLootBags.MODID;
    }

    @Override
    public String getRecipeName() {
        return I18n.format("enhancedlootbags.nei.category");
    }

}

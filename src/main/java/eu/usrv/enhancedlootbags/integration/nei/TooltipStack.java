package eu.usrv.enhancedlootbags.integration.nei;

import java.util.List;

import net.minecraft.item.ItemStack;

import codechicken.nei.ItemStackMap;
import codechicken.nei.PositionedStack;

public class TooltipStack extends PositionedStack {

    private final ItemStackMap<List<String>> tooltips;

    public TooltipStack(Object object, int x, int y, boolean genPerms, ItemStackMap<List<String>> tooltips) {
        super(object, x, y, genPerms);
        this.tooltips = tooltips;
    }

    public TooltipStack(Object object, int x, int y, ItemStackMap<List<String>> tooltips) {
        this(object, x, y, true, tooltips);
    }

    public List<String> getTooltip(ItemStack stack) {
        return tooltips.get(stack);
    }
}

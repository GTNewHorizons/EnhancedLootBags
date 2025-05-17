/*
 * Copyright 2016 Stefan 'Namikon' Thomanek <sthomanek at gmail dot com> This program is free software: you can
 * redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in
 * the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a
 * copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package eu.usrv.enhancedlootbags.core.items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import eu.usrv.enhancedlootbags.EnhancedLootBags;
import eu.usrv.enhancedlootbags.GuiHandler;
import eu.usrv.enhancedlootbags.StatHelper;
import eu.usrv.enhancedlootbags.core.LootGroupsHandler;
import eu.usrv.enhancedlootbags.core.serializer.LootGroups.LootGroup;
import eu.usrv.enhancedlootbags.core.serializer.LootGroups.LootGroup.Drop;
import eu.usrv.yamcore.auxiliary.LogHelper;
import eu.usrv.yamcore.auxiliary.PlayerChatHelper;

public class ItemLootBag extends Item {

    private IIcon _mIcoDefault;
    private final Map<Integer, IIcon> _mGroupIcons = new HashMap<>();
    private final LootGroupsHandler _mLGHandler;
    private LogHelper _mLogger = EnhancedLootBags.Logger;
    private static final boolean isDreamcraftLoaded = Loader.isModLoaded("dreamcraft");

    public ItemLootBag(LootGroupsHandler pLGHandler) {
        setHasSubtypes(true);
        setMaxDamage(0);
        _mLGHandler = pLGHandler;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return true;
    }

    @Override
    public int getItemEnchantability(ItemStack stack) {
        return 15;
    }

    @Override
    public int getItemEnchantability() {
        return 15;
    }

    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister pIconRegister) {
        _mIcoDefault = pIconRegister.registerIcon(String.format("%s:lootbag_generic", EnhancedLootBags.MODID));
        _mGroupIcons.clear();
        for (LootGroup tGrp : _mLGHandler.getLootGroups().getLootTable()) {
            tGrp.getGroupIconResource()
                    .ifPresent(icon -> _mGroupIcons.put(tGrp.getGroupID(), pIconRegister.registerIcon(icon)));
        }
    }

    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int meta) {
        return _mGroupIcons.getOrDefault(meta, _mIcoDefault);
    }

    @Override
    public String getItemStackDisplayName(ItemStack pStack) {
        String tReturn = StatHelper.get("string.lootbag_templatename");
        String tInnerName = "?Error";

        if (pStack.getItemDamage() == 0) tInnerName = StatHelper.get("string.default");
        else {
            LootGroup tGrp = _mLGHandler.getGroupByIDClient(pStack.getItemDamage());
            tInnerName = tGrp == null ? "Error" : StatCollector.translateToLocal(tGrp.getGroupName());
        }

        return String.format(tReturn, tInnerName);
    }

    @SideOnly(Side.CLIENT)
    public void getSubItems(Item par1, CreativeTabs par2CreativeTabs, List par3List) {
        for (LootGroup tGrp : _mLGHandler.getLootGroupsClient().getLootTable()) {
            ItemStack s1 = new ItemStack(this, 1, tGrp.getGroupID());
            par3List.add(s1);

            if (EnhancedLootBags.ELBCfg.AllowFortuneBags && tGrp.getCombineWithTrash()) {
                ItemStack s2 = s1.copy();
                s2.addEnchantment(Enchantment.fortune, 3);
                par3List.add(s2);
            }
        }
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        LootGroup tGrp = _mLGHandler.getGroupByIDClient(stack.getItemDamage());
        return tGrp == null ? EnumRarity.common : tGrp.getGroupRarity();
    }

    @Override
    public ItemStack onItemRightClick(ItemStack pStack, World pWorld, EntityPlayer pPlayer) {
        if (!pWorld.isRemote) {
            if (pPlayer.capabilities.isCreativeMode && pPlayer.isSneaking()) {
                pPlayer.openGui(
                        EnhancedLootBags.instance,
                        GuiHandler.GUI_LOOTBAG,
                        pWorld,
                        (int) pPlayer.posX,
                        (int) pPlayer.posY,
                        (int) pPlayer.posZ);
                return pStack;
            }

            int tGroupID = pStack.getItemDamage();
            int tFortuneLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.fortune.effectId, pStack);
            LootGroup tGrp = _mLGHandler.getMergedGroupFromID(tGroupID, tFortuneLevel);
            if (tGrp != null) {
                int q = tGrp.getMinItems();
                if (tGrp.getMaxItems() > tGrp.getMinItems()) q = pWorld.rand.nextInt(tGrp.getMaxItems()) + 1;

                // _mLogger.info(String.format("MinMax %d / %d", tGrp.mMinItems, tGrp.mMaxItems));
                while (q > 0) {
                    // _mLogger.info(String.format("q: %d", q));
                    List<ItemStack> isList = getRandomLootItems(pPlayer, tGrp);
                    if (isList.isEmpty()) {
                        PlayerChatHelper.SendNotifyWarning(pPlayer, StatHelper.get("string.try_again"));
                        return pStack;
                    }
                    q -= isList.size();
                    // _mLogger.info(String.format("NewQ: %d", q));

                    for (ItemStack tStack : isList) {
                        try {
                            EntityItem eti = new EntityItem(
                                    pWorld,
                                    pPlayer.posX,
                                    pPlayer.posY,
                                    pPlayer.posZ,
                                    tStack.copy());
                            eti.delayBeforeCanPickup = 0;
                            pWorld.spawnEntityInWorld(eti);
                        } catch (Exception e) {
                            _mLogger.error("Unable to spawn dropitem in world");
                            e.printStackTrace();
                        }
                    }
                }

                pWorld.playSoundAtEntity(
                        pPlayer,
                        String.format("%s:lootbag_open", EnhancedLootBags.MODID),
                        0.75F,
                        1.0F);
                pStack.stackSize -= 1;
            } else {
                PlayerChatHelper.SendNotifyWarning(pPlayer, StatHelper.get("string.sorry_damaged"));
            }
        }
        return pStack;
    }

    private List<ItemStack> getRandomLootItems(EntityPlayer player, LootGroup pGrp) {
        List<ItemStack> tReturnList = new ArrayList<ItemStack>();
        List<Drop> tPendingDrops = new ArrayList<Drop>();

        double tRnd;
        int tMaxRuns = 0;
        Drop tSelectedDrop = null;

        do {
            // Step 1: Get a random drop by weight
            tRnd = EnhancedLootBags.Rnd.nextDouble() * pGrp.getMaxWeight();
            for (Drop tDr : pGrp.getDrops()) {
                tRnd -= tDr.getChance();
                if (tRnd <= 0.0D) {
                    tSelectedDrop = tDr;
                    break;
                }
            }

            // _mLogger.info(String.format("Maxruns: %d", tMaxRuns));

            // Step 2: Was the selection successful?
            if (tSelectedDrop != null) {
                // _mLogger.info(String.format("SelectedDrop: %s", tSelectedDrop.mItemName));
                // Ask the LootGroupHandler to provide a list with drops we shall use,
                // based on the current drop. See JDoc on that function for details.
                List<Drop> tPossibleItemDrops = _mLGHandler.getItemGroupDrops(pGrp, tSelectedDrop);
                // _mLogger.info(String.format("Dump tPossibleDrops. Count: %d", tPossibleItemDrops.size()));

                // Now check for each item if the player is allowed to get
                // another one of these.
                // The check for isLimitedDrop is done in that function; So we
                // only query the local
                // Storage when it's required
                for (Drop dr : tPossibleItemDrops) {
                    // _mLogger.info(String.format("PossibleDrop: %s", dr.mItemName));
                    if (_mLGHandler.isDropAllowedForPlayer(player, pGrp, dr, true)) {
                        // ... so add it to the pending items list.
                        tPendingDrops.add(dr);
                    }
                }

                // At this point, we have a list of 1 to x items, depending on
                // how the lootgroups are defined
                // now we have to loop the chosen drops and get the actual
                // ItemStacks with NBT and metavalues

                // _mLogger.info(String.format("PendingDrops dump. Size : %d", tPendingDrops.size()));
                for (Drop td : tPendingDrops) {
                    // _mLogger.info(String.format("PendingDrop: %s", td.mItemName));
                    // How much to drop
                    int tAmount = td.getAmount();
                    // _mLogger.info(String.format("PendingDrop amount: %d", tAmount));
                    // Random drop? Alter amount by random
                    // Then get random amount between 1 and tAmount
                    if (td.getIsRandomAmount()) tAmount = EnhancedLootBags.Rnd.nextInt(tAmount) + 1;

                    // _mLogger.info(String.format("PD fixed amount: %d", tAmount));

                    ItemStack tStackAll = td.getItemStack(tAmount);
                    if (tStackAll == null) {
                        _mLogger.error(
                                String.format(
                                        "Skipping loot %s; Unable to get ItemStack. Make sure this item exists!",
                                        td.getItemName()));
                        continue;
                    }
                    while (tStackAll.stackSize > tStackAll.getMaxStackSize())
                        tReturnList.add(tStackAll.splitStack(tStackAll.getMaxStackSize()));
                    tReturnList.add(tStackAll);
                    // _mLogger.info(String.format("ReturnList contains now %d items", tReturnList.size()));
                }

                // tReturnList contains now all ItemStacks that should drop for
                // this turn

            }

            tMaxRuns++;
        } while (tReturnList.isEmpty() && tMaxRuns < 10);

        // _mLogger.info(String.format("Final returnList contains %d items", tReturnList.size()));
        return tReturnList;
    }

    @SuppressWarnings("unchecked")
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack pItemStack, EntityPlayer pEntityPlayer, List pTooltipList,
            boolean pSomeBooleanValue) {
        if (!_mLGHandler.getGroupByID(pItemStack.getItemDamage()).getCombineWithTrash()) {
            pTooltipList.add(StatHelper.get("string.no_fortune_needed"));
        } else if (EnhancedLootBags.ELBCfg.AllowFortuneBags) {
            int tFortuneLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.fortune.effectId, pItemStack);
            if (tFortuneLevel == 0) {
                pTooltipList.add(StatHelper.get("string.not_fortuned"));
                if (isDreamcraftLoaded) {
                    pTooltipList.add(StatCollector.translateToLocal("enhancedlootbags.dreamcraft.enchantment_tip"));
                }
            } else {
                pTooltipList.add(
                        String.format(
                                StatHelper.get("string.fortuned"),
                                (tFortuneLevel == 3 ? 100 : 33 * tFortuneLevel)));
            }
        }
    }
}

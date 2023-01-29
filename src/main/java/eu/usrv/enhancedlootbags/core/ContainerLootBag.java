/*
 * Copyright 2016 Stefan 'Namikon' Thomanek <sthomanek at gmail dot com> This program is free software: you can
 * redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in
 * the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a
 * copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package eu.usrv.enhancedlootbags.core;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import eu.usrv.enhancedlootbags.EnhancedLootBags;

public class ContainerLootBag extends Container {

    /**
     * This is a fake container class that is used to provide ghost-item slots for the lootbags. Inventory won't accept
     * any item, nor can you take items from it
     */
    public static class FakeLootBagInventory implements IInventory {

        private final ItemStack[] mInventory;
        private final LootGroupsHandler mLGH = EnhancedLootBags.LootGroupHandler;
        private final int mSlotCount = 108;

        public FakeLootBagInventory(int pLootGroupID) {
            // MainRegistry.Logger.info(String.format("Creating fakeInventory for lootgroup %d", pLootGroupID));
            mInventory = mLGH.createFakeInventoryFromID(pLootGroupID, mSlotCount);
        }

        @Override
        public int getSizeInventory() {
            return mSlotCount;
        }

        @Override
        public ItemStack getStackInSlot(int pSlot) {
            if (pSlot < 0 || pSlot >= mSlotCount) return null;
            else return mInventory[pSlot];
        }

        @Override
        public ItemStack decrStackSize(int p_70298_1_, int p_70298_2_) {
            return null;
        }

        @Override
        public ItemStack getStackInSlotOnClosing(int p_70304_1_) {
            return null;
        }

        @Override
        public void setInventorySlotContents(int p_70299_1_, ItemStack p_70299_2_) {}

        @Override
        public String getInventoryName() {
            return null;
        }

        @Override
        public boolean hasCustomInventoryName() {
            return false;
        }

        @Override
        public int getInventoryStackLimit() {
            return 64;
        }

        @Override
        public void markDirty() {}

        @Override
        public boolean isUseableByPlayer(EntityPlayer p_70300_1_) {
            return true;
        }

        @Override
        public void openInventory() {}

        @Override
        public void closeInventory() {}

        @Override
        public boolean isItemValidForSlot(int p_94041_1_, ItemStack p_94041_2_) {
            return false;
        }
    }

    private final FakeLootBagInventory mInventory;
    private static final int GUI_ROWS = 9;
    private static final int GUI_COLUMNS = 12;
    private static final int GUI_STARTX = 12;
    private static final int GUI_STARTY = 8;
    private static final int GUI_SLOTHEIGHT = 18;
    private static final int GUI_SLOTWIDTH = 18;

    /**
     * Populate the GUI with inventory slots. This is now sort of dynamic; as we use static final's to define max/min
     * for rows, and the start locations
     *
     * @param inventoryPlayer
     * @param pLootGroupMeta
     */
    public ContainerLootBag(InventoryPlayer inventoryPlayer, int pLootGroupMeta) {
        mInventory = new FakeLootBagInventory(pLootGroupMeta);
        for (int row = 0; row < GUI_ROWS; row++) {
            for (int col = 0; col < GUI_COLUMNS; col++) addSlotToContainer(
                    new SlotLootBag(
                            mInventory,
                            col + row * GUI_COLUMNS,
                            GUI_STARTX + GUI_SLOTWIDTH * col,
                            GUI_STARTY + GUI_SLOTHEIGHT * row));
        }
        bindPlayerInventory(inventoryPlayer, 238, 256);
    }

    protected void bindPlayerInventory(InventoryPlayer pInventoryPlayer, int pSizeX, int pSizeY) {
        int tLeftCol = (pSizeX - 162) / 2 + 1;

        for (int playerInvRow = 0; playerInvRow < 3; playerInvRow++) {
            for (int playerInvCol = 0; playerInvCol < 9; playerInvCol++) {
                addSlotToContainer(
                        new Slot(
                                pInventoryPlayer,
                                playerInvCol + playerInvRow * 9 + 9,
                                tLeftCol + playerInvCol * 18,
                                pSizeY - (4 - playerInvRow) * 18 - 10));
            }
        }

        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            addSlotToContainer(new Slot(pInventoryPlayer, hotbarSlot, tLeftCol + hotbarSlot * 18, pSizeY - 24));
        }
    }

    /*
     * Probably not required here; This prevents the player from moving the lootbag he just opened. Basically, it is
     * meant to prevent you from moving the "inHand" item away, so the game is able to store the changed inventory. If
     * you don't do that, one could dupe items with it. (But we don't allow add/remove of items here anyways, so not a
     * problem
     */
    @Override
    public ItemStack slotClick(int slot, int button, int flag, EntityPlayer player) {
        if (slot >= 0 && getSlot(slot) != null && getSlot(slot).getStack() == player.getHeldItem()) return null;

        return super.slotClick(slot, button, flag, player);
    }

    @Override
    public boolean canInteractWith(EntityPlayer entityplayer) {
        return true;
    }

    /**
     * Just do nothing on shift click
     */
    @Override
    public ItemStack transferStackInSlot(EntityPlayer par1EntityPlayer, int index) {
        return null;
    }

    /*
     * No, we don't merge
     */
    @Override
    protected boolean mergeItemStack(ItemStack stack, int start, int end, boolean backwards) {
        return false;
    }
}

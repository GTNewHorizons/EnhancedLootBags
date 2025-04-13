/*
 * Copyright 2016 Stefan 'Namikon' Thomanek <sthomanek at gmail dot com> This program is free software: you can
 * redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in
 * the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a
 * copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package eu.usrv.enhancedlootbags.core.serializer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;

import eu.usrv.enhancedlootbags.EnhancedLootBags;
import eu.usrv.enhancedlootbags.core.LootGroupsHandler;
import eu.usrv.yamcore.auxiliary.ItemDescriptor;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "LootGroups")
public class LootGroups {

    @XmlElement(name = "LootGroup")
    private List<LootGroups.LootGroup> mLootGroups;

    private void Init() {
        if (mLootGroups == null) mLootGroups = new ArrayList<LootGroups.LootGroup>();
    }

    public List<LootGroups.LootGroup> getLootTable() {
        Init();
        return mLootGroups;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType
    public static class LootGroup {

        @XmlAttribute(name = "GroupMetaID")
        protected int mGroupID;

        @XmlAttribute(name = "GroupName")
        protected String mGroupName;

        @XmlAttribute(name = "Rarity")
        protected int mRarity;

        @XmlAttribute(name = "MinItems")
        protected int mMinItems;

        public int getMinItems() {
            return mMinItems;
        }

        public int getMaxItems() {
            return mMaxItems;
        }

        @XmlAttribute(name = "MaxItems")
        protected int mMaxItems;

        @XmlAttribute(name = "CombineTrashGroup")
        protected boolean mCombineWithTrash;

        @XmlAttribute(name = "TrashGroup")
        protected int mTrashGroup;

        @XmlTransient
        private int mMaxWeight = -1;

        public int getMaxWeight() {
            if (mMaxWeight == -1) {
                mMaxWeight = 0;
                for (Drop tDr : mDrops) mMaxWeight += tDr.mChance;
            }

            return mMaxWeight;
        }

        @XmlAttribute(name = "LootbagIcon")
        private String mGroupIconResource;

        public Optional<String> getGroupIconResource() {
            return Optional.ofNullable(mGroupIconResource);
        }

        @XmlElement(name = "Loot")
        private List<LootGroups.LootGroup.Drop> mDrops;

        public List<LootGroups.LootGroup.Drop> getDrops() {
            Init();
            return mDrops;
        }

        private void Init() {
            if (mDrops == null) mDrops = new ArrayList<LootGroups.LootGroup.Drop>();
        }

        public String getGroupName() {
            return mGroupName;
        }

        public int getGroupID() {
            return mGroupID;
        }

        public boolean getCombineWithTrash() {
            return mCombineWithTrash;
        }

        public int getTrashGroup() {
            return mTrashGroup;
        }

        public void setTrashGroup(int mTrashGroup) {
            this.mTrashGroup = mTrashGroup;
        }

        public EnumRarity getGroupRarity() {
            if (mRarity >= 0 && mRarity < EnumRarity.values().length) return EnumRarity.values()[mRarity];
            else return EnumRarity.common;
        }

        public ItemStack createLootBagItemStack() {
            return new ItemStack(LootGroupsHandler.getLootBagItem(), 1, getGroupID());
        }

        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType
        public static class Drop {

            @XmlAttribute(name = "Identifier")
            protected String mDropID;

            @XmlAttribute(name = "ItemName")
            protected String mItemName;

            @XmlAttribute(name = "Amount")
            protected int mAmount;

            @XmlAttribute(name = "NBTTag")
            protected String mTag;

            @XmlAttribute(name = "Chance")
            protected int mChance;

            @XmlAttribute(name = "ItemGroup")
            protected String mItemGroup;

            @XmlAttribute(name = "LimitedDropCount")
            protected int mLimitedDropCount;

            @XmlAttribute(name = "RandomAmount")
            protected boolean mIsRandomAmount;

            public String getIdentifier() {
                return mDropID;
            }

            public String getItemDropGroup() {
                return (mItemGroup == null) ? "" : mItemGroup;
            }

            public String getItemName() {
                return mItemName;
            }

            @Nullable
            public ItemStack getItemStack() {
                return getItemStack(getAmount());
            }

            @Nullable
            public ItemStack getItemStack(int amount) {
                ItemDescriptor itemDesc = ItemDescriptor.fromString(getItemName(), true);
                if (itemDesc == null) return null;
                return itemDesc.getItemStackwNBT(amount, getNBTTag());
            }

            public int getAmount() {
                return mAmount;
            }

            public int getChance() {
                return mChance;
            }

            public int getLimitedDropCount() {
                return mLimitedDropCount;
            }

            public boolean getIsRandomAmount() {
                return mIsRandomAmount;
            }

            public String getNBTTag() {
                return mTag;
            }
        }

        /**
         * Do NOT run this on a Group you want to save. It will drive you nuts that after each time you load/save the
         * items will shuffle randomly around! This is ONLY to mix the trash-merged lootgroups
         */
        public void shuffleLoot() {
            for (int i = 0; i < EnhancedLootBags.Rnd.nextInt(10); i++) Collections.shuffle(mDrops);
        }

        public void updateMaxWeight() {
            mMaxWeight = -1;
            getMaxWeight();
        }
    }
}

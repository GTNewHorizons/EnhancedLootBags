package eu.usrv.enhancedlootbags.core.stats;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class LootBagStats {

    private static final String NBT_GROUPS = "Groups";
    private static final String NBT_GROUP_ID = "ID";
    private static final String NBT_OPENED = "Opened";
    private static final String NBT_DROPS = "Drops";
    private static final String NBT_DROP_ID = "Id";
    private static final String NBT_DROP_STACK = "Stack";
    private static final String NBT_DROP_ITEMS = "Items";
    private static final String NBT_DROP_TIMES = "Times";
    private static final String NBT_DROP_TRASH = "Trash";

    private final Map<Integer, GroupStats> groups = new LinkedHashMap<>();

    public GroupStats getGroup(int groupID) {
        return groups.get(groupID);
    }

    public GroupStats getOrCreateGroup(int groupID) {
        GroupStats stats = groups.get(groupID);
        if (stats == null) {
            stats = new GroupStats(groupID);
            groups.put(groupID, stats);
        }
        return stats;
    }

    public void putGroup(GroupStats stats) {
        groups.put(stats.getGroupID(), stats);
    }

    public Collection<GroupStats> getGroups() {
        return groups.values();
    }

    public long getTotalOpened() {
        long total = 0;
        for (GroupStats grp : groups.values()) {
            total += grp.getOpened();
        }
        return total;
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList groupList = new NBTTagList();
        for (GroupStats grp : groups.values()) {
            groupList.appendTag(grp.writeToNBT());
        }
        tag.setTag(NBT_GROUPS, groupList);
        return tag;
    }

    public static LootBagStats readFromNBT(NBTTagCompound tag) {
        LootBagStats stats = new LootBagStats();
        NBTTagList groupList = tag.getTagList(NBT_GROUPS, 10);
        for (int i = 0; i < groupList.tagCount(); i++) {
            stats.putGroup(GroupStats.readFromNBT(groupList.getCompoundTagAt(i)));
        }
        return stats;
    }

    public static class GroupStats {

        private final int groupID;
        private int opened;
        private final Map<String, DropStats> drops = new LinkedHashMap<>();

        public GroupStats(int groupID) {
            this.groupID = groupID;
        }

        public int getGroupID() {
            return groupID;
        }

        public int getOpened() {
            return opened;
        }

        public void incrementOpened() {
            opened++;
        }

        public Collection<DropStats> getDrops() {
            return drops.values();
        }

        public void recordDrop(String dropID, ItemStack stack, boolean trash) {
            DropStats drop = drops.get(dropID);
            if (drop == null) {
                ItemStack display = stack.copy();
                display.stackSize = 1;
                drop = new DropStats(dropID, display.writeToNBT(new NBTTagCompound()));
                drops.put(dropID, drop);
            }
            drop.add(stack.stackSize);
            drop.setTrash(trash);
        }

        public NBTTagCompound writeToNBT() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger(NBT_GROUP_ID, groupID);
            tag.setInteger(NBT_OPENED, opened);
            NBTTagList dropList = new NBTTagList();
            for (DropStats drop : drops.values()) {
                dropList.appendTag(drop.writeToNBT());
            }
            tag.setTag(NBT_DROPS, dropList);
            return tag;
        }

        public static GroupStats readFromNBT(NBTTagCompound tag) {
            GroupStats stats = new GroupStats(tag.getInteger(NBT_GROUP_ID));
            stats.opened = tag.getInteger(NBT_OPENED);
            NBTTagList dropList = tag.getTagList(NBT_DROPS, 10);
            for (int i = 0; i < dropList.tagCount(); i++) {
                DropStats drop = DropStats.readFromNBT(dropList.getCompoundTagAt(i));
                stats.drops.put(drop.getDropID(), drop);
            }
            return stats;
        }
    }

    public static class DropStats {

        private final String dropID;
        private final NBTTagCompound stackTag;
        private final ItemStack displayStack;
        private long itemCount;
        private int timesDropped;
        private boolean trash;

        private DropStats(String dropID, NBTTagCompound stackTag) {
            this.dropID = dropID;
            this.stackTag = stackTag;
            displayStack = ItemStack.loadItemStackFromNBT(stackTag);
        }

        public String getDropID() {
            return dropID;
        }

        public ItemStack getDisplayStack() {
            return displayStack;
        }

        public long getItemCount() {
            return itemCount;
        }

        public int getTimesDropped() {
            return timesDropped;
        }

        public boolean isTrash() {
            return trash;
        }

        public void setTrash(boolean trash) {
            this.trash = trash;
        }

        public void add(long amount) {
            itemCount += amount;
            timesDropped++;
        }

        public NBTTagCompound writeToNBT() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString(NBT_DROP_ID, dropID);
            tag.setTag(NBT_DROP_STACK, stackTag);
            tag.setLong(NBT_DROP_ITEMS, itemCount);
            tag.setInteger(NBT_DROP_TIMES, timesDropped);
            tag.setBoolean(NBT_DROP_TRASH, trash);
            return tag;
        }

        public static DropStats readFromNBT(NBTTagCompound tag) {
            DropStats drop = new DropStats(tag.getString(NBT_DROP_ID), tag.getCompoundTag(NBT_DROP_STACK));
            drop.itemCount = tag.getLong(NBT_DROP_ITEMS);
            drop.timesDropped = tag.getInteger(NBT_DROP_TIMES);
            drop.trash = tag.getBoolean(NBT_DROP_TRASH);
            return drop;
        }
    }
}

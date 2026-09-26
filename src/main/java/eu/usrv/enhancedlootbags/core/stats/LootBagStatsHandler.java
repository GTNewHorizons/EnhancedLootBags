package eu.usrv.enhancedlootbags.core.stats;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.world.WorldEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import eu.usrv.enhancedlootbags.EnhancedLootBags;
import eu.usrv.enhancedlootbags.core.serializer.LootGroups.LootGroup.Drop;
import eu.usrv.enhancedlootbags.core.stats.LootBagStats.GroupStats;
import eu.usrv.enhancedlootbags.net.msg.LootBagStatsSyncMessage;
import eu.usrv.yamcore.auxiliary.LogHelper;

public class LootBagStatsHandler {

    private static final String FILE_NAME = "LootBagStats.dat";

    private final LogHelper logger = EnhancedLootBags.Logger;
    private final Map<UUID, LootBagStats> playerStats = new HashMap<>();
    private File saveDir = null;
    private boolean dirty = false;

    private void initStorage() {
        File currentDir = DimensionManager.getCurrentSaveRootDirectory();
        if (currentDir == null || currentDir.equals(saveDir)) {
            return;
        }

        playerStats.clear();
        dirty = false;
        saveDir = currentDir;

        File file = new File(currentDir, FILE_NAME);
        if (!file.exists()) {
            return;
        }

        try (FileInputStream in = new FileInputStream(file)) {
            NBTTagCompound root = CompressedStreamTools.readCompressed(in);
            for (Object key : root.func_150296_c()) {
                String uuid = (String) key;
                try {
                    LootBagStats stats = LootBagStats.readFromNBT(root.getCompoundTag(uuid));
                    updateTrashFlags(stats);
                    playerStats.put(UUID.fromString(uuid), stats);
                } catch (IllegalArgumentException e) {
                    logger.warn(String.format("[LootBags] Skipping invalid player entry %s in %s", uuid, FILE_NAME));
                }
            }
        } catch (Exception e) {
            logger.error(String.format("[LootBags] Unable to load %s; Statistics will start fresh", FILE_NAME));
            e.printStackTrace();
        }
    }

    private void updateTrashFlags(LootBagStats stats) {
        for (GroupStats group : stats.getGroups()) {
            if (EnhancedLootBags.LootGroupHandler.getGroupByID(group.getGroupID()) == null) {
                continue;
            }
            for (LootBagStats.DropStats drop : group.getDrops()) {
                drop.setTrash(EnhancedLootBags.LootGroupHandler.isTrashDrop(group.getGroupID(), drop.getDropID()));
            }
        }
    }

    public void onLootConfigReloaded() {
        if (saveDir == null) {
            return;
        }
        for (LootBagStats stats : playerStats.values()) {
            updateTrashFlags(stats);
        }
        dirty = true;

        for (Object player : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
            EntityPlayerMP playerMP = (EntityPlayerMP) player;
            EnhancedLootBags.NW.sendTo(LootBagStatsSyncMessage.full(getStats(playerMP)), playerMP);
        }
    }

    public void save() {
        if (!dirty || saveDir == null) {
            return;
        }

        NBTTagCompound root = new NBTTagCompound();
        for (Map.Entry<UUID, LootBagStats> entry : playerStats.entrySet()) {
            if (entry.getValue().getGroups().isEmpty()) {
                continue;
            }
            root.setTag(entry.getKey().toString(), entry.getValue().writeToNBT());
        }

        File file = new File(saveDir, FILE_NAME);
        File tmpFile = new File(saveDir, FILE_NAME + ".tmp");
        try {
            try (FileOutputStream out = new FileOutputStream(tmpFile)) {
                CompressedStreamTools.writeCompressed(root, out);
            }
            if (file.exists() && !file.delete()) {
                throw new IllegalStateException("Unable to replace " + file);
            }
            if (!tmpFile.renameTo(file)) {
                throw new IllegalStateException("Unable to rename " + tmpFile);
            }
            dirty = false;
        } catch (Exception e) {
            logger.error(String.format("[LootBags] Unable to save %s", FILE_NAME));
            e.printStackTrace();
        }
    }

    public void unload() {
        save();
        playerStats.clear();
        saveDir = null;
    }

    public LootBagStats getStats(EntityPlayer player) {
        initStorage();
        UUID uuid = player.getUniqueID();
        LootBagStats stats = playerStats.get(uuid);
        if (stats == null) {
            stats = new LootBagStats();
            playerStats.put(uuid, stats);
        }
        return stats;
    }

    public void recordOpening(EntityPlayer player, int groupID, OpenRecord record) {
        GroupStats group = getStats(player).getOrCreateGroup(groupID);
        group.incrementOpened();
        for (int i = 0; i < record.drops.size(); i++) {
            String dropID = record.drops.get(i).getIdentifier();
            group.recordDrop(
                    dropID,
                    record.stacks.get(i),
                    EnhancedLootBags.LootGroupHandler.isTrashDrop(groupID, dropID));
        }
        dirty = true;

        if (player instanceof EntityPlayerMP) {
            EnhancedLootBags.NW.sendTo(LootBagStatsSyncMessage.partial(group), (EntityPlayerMP) player);
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            EnhancedLootBags.NW
                    .sendTo(LootBagStatsSyncMessage.full(getStats(event.player)), (EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public void onWorldSave(WorldEvent.Save event) {
        if (!event.world.isRemote && event.world.provider.dimensionId == 0) {
            save();
        }
    }

    public static class OpenRecord {

        private final List<Drop> drops = new ArrayList<>();
        private final List<ItemStack> stacks = new ArrayList<>();

        public void add(Drop drop, ItemStack stack) {
            drops.add(drop);
            stacks.add(stack.copy());
        }
    }
}

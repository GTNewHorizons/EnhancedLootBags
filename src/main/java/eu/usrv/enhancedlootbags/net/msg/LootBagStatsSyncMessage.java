package eu.usrv.enhancedlootbags.net.msg;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import eu.usrv.enhancedlootbags.EnhancedLootBags;
import eu.usrv.enhancedlootbags.client.ClientLootBagStats;
import eu.usrv.enhancedlootbags.core.stats.LootBagStats;
import eu.usrv.enhancedlootbags.core.stats.LootBagStats.GroupStats;
import eu.usrv.yamcore.network.client.AbstractClientMessageHandler;
import io.netty.buffer.ByteBuf;

public class LootBagStatsSyncMessage implements IMessage {

    protected boolean fullSync;
    protected NBTTagCompound payload;

    public LootBagStatsSyncMessage() {}

    private LootBagStatsSyncMessage(boolean fullSync, NBTTagCompound payload) {
        this.fullSync = fullSync;
        this.payload = payload;
    }

    public static LootBagStatsSyncMessage full(LootBagStats stats) {
        return new LootBagStatsSyncMessage(true, stats.writeToNBT());
    }

    public static LootBagStatsSyncMessage partial(GroupStats group) {
        return new LootBagStatsSyncMessage(false, group.writeToNBT());
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        fullSync = buffer.readBoolean();
        byte[] data = new byte[buffer.readInt()];
        buffer.readBytes(data);
        try {
            payload = CompressedStreamTools.readCompressed(new ByteArrayInputStream(data));
        } catch (IOException e) {
            EnhancedLootBags.Logger.error("[LootBags] Received invalid statistics from server");
            payload = null;
        }
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeBoolean(fullSync);
        try {
            byte[] data = CompressedStreamTools.compress(payload);
            buffer.writeInt(data.length);
            buffer.writeBytes(data);
        } catch (IOException e) {
            throw new RuntimeException("Unable to serialize lootbag statistics", e);
        }
    }

    public static class LootBagStatsSyncMessageHandler extends AbstractClientMessageHandler<LootBagStatsSyncMessage> {

        @Override
        public IMessage handleClientMessage(EntityPlayer player, LootBagStatsSyncMessage message, MessageContext ctx) {
            if (message.payload == null) {
                return null;
            }

            if (message.fullSync) {
                ClientLootBagStats.setStats(LootBagStats.readFromNBT(message.payload));
            } else {
                ClientLootBagStats.updateGroup(GroupStats.readFromNBT(message.payload));
            }
            return null;
        }
    }
}

/*
 * Copyright 2016 Stefan 'Namikon' Thomanek <sthomanek at gmail dot com> This program is free software: you can
 * redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in
 * the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a
 * copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package eu.usrv.enhancedlootbags.net.msg;

import net.minecraft.entity.player.EntityPlayer;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import eu.usrv.enhancedlootbags.EnhancedLootBags;
import eu.usrv.yamcore.network.client.AbstractClientMessageHandler;
import io.netty.buffer.ByteBuf;

public class LootBagClientSyncMessage implements IMessage {

    protected String _mPayload;

    public LootBagClientSyncMessage() {}

    public LootBagClientSyncMessage(String pPayload) {
        _mPayload = pPayload;
    }

    @Override
    public void fromBytes(ByteBuf pBuffer) {
        _mPayload = ByteBufUtils.readUTF8String(pBuffer);
    }

    @Override
    public void toBytes(ByteBuf pBuffer) {
        ByteBufUtils.writeUTF8String(pBuffer, _mPayload);
    }

    public static class LootBagClientSyncMessageHandler extends AbstractClientMessageHandler<LootBagClientSyncMessage> {

        @Override
        public IMessage handleClientMessage(EntityPlayer pPlayer, LootBagClientSyncMessage pMessage,
                MessageContext pCtx) {
            EnhancedLootBags.LootGroupHandler.processServerConfig(pMessage._mPayload);
            return null;
        }
    }
}

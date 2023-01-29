/*
 * Copyright 2016 Stefan 'Namikon' Thomanek <sthomanek at gmail dot com> This program is free software: you can
 * redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in
 * the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a
 * copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package eu.usrv.enhancedlootbags.net;

import eu.usrv.enhancedlootbags.EnhancedLootBags;
import eu.usrv.enhancedlootbags.net.msg.LootBagClientSyncMessage;
import eu.usrv.yamcore.network.PacketDispatcher;

public class ELBDispatcher extends PacketDispatcher {

    public ELBDispatcher() {
        super(EnhancedLootBags.MODID);
    }

    @Override
    public void registerPackets() {
        registerMessage(LootBagClientSyncMessage.LootBagClientSyncMessageHandler.class, LootBagClientSyncMessage.class);
    }
}

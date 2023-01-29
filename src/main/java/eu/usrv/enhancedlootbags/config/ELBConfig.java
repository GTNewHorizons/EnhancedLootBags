/*
 * Copyright 2016 Stefan 'Namikon' Thomanek <sthomanek at gmail dot com> This program is free software: you can
 * redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in
 * the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have received a
 * copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package eu.usrv.enhancedlootbags.config;

import java.io.File;

import eu.usrv.yamcore.config.ConfigManager;

public class ELBConfig extends ConfigManager {

    public ELBConfig(File pConfigBaseDirectory, String pModCollectionDirectory, String pModID) {
        super(pConfigBaseDirectory, pModCollectionDirectory, pModID);
    }

    public boolean AllowFortuneBags;

    @Override
    protected void PreInit() {
        AllowFortuneBags = true;
    }

    @Override
    protected void Init() {
        AllowFortuneBags = _mainConfig.getBoolean(
                "AllowFortuneBags",
                "Generic",
                AllowFortuneBags,
                "Set to true to disable the TrashGroup merging if FortuneIII is applied to a lootbag");
    }

    @Override
    protected void PostInit() {}
}

/*
    Copyright 2016 Stefan 'Namikon' Thomanek

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package eu.usrv.enhancedlootbags;


import java.util.Random;

import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

import eu.usrv.enhancedlootbags.proxy.CommonProxy;
import eu.usrv.enhancedlootbags.server.LootBagCommand;
import eu.usrv.yamcore.auxiliary.IngameErrorLog;
import eu.usrv.enhancedlootbags.net.ELBDispatcher;
import eu.usrv.yamcore.auxiliary.LogHelper;
import eu.usrv.enhancedlootbags.core.LootGroupsHandler;


@Mod( modid = EnhancedLootBags.MODID, version = EnhancedLootBags.VERSION, dependencies = "required-after:Forge@[10.13.2.1291,);required-after:YAMCore@[0.5.62,);" )
public class EnhancedLootBags
{
	public static final String MODID = "enhancedlootbags";
	public static final String VERSION = "GRADLETOKEN_VERSION";
	public static LootGroupsHandler LootGroupHandler = null;
	public static IngameErrorLog AdminLogonErrors = null;
	public static LogHelper Logger = new LogHelper( MODID );
	public static ELBDispatcher NW;
	public static Random Rnd = null;

	@SidedProxy( clientSide = "eu.usrv.enhancedlootbags.proxy.ClientProxy", serverSide = "eu.usrv.enhancedlootbags.proxy.CommonProxy" )
	public static CommonProxy proxy;

	@Instance( MODID )
	public static EnhancedLootBags instance;

	@EventHandler
	public void PreInit( FMLPreInitializationEvent pEvent )
	{
		Rnd = new Random( System.currentTimeMillis() );
		AdminLogonErrors = new IngameErrorLog();
		NW = new ELBDispatcher();
		NW.registerPackets();

		LootGroupHandler = new LootGroupsHandler( pEvent.getModConfigurationDirectory() );
		LootGroupHandler.LoadConfig();
		LootGroupHandler.registerBagItem();
	}

	@EventHandler
	public void init( FMLInitializationEvent event )
	{
		FMLCommonHandler.instance().bus().register( AdminLogonErrors );
		FMLCommonHandler.instance().bus().register( LootGroupHandler );
		MinecraftForge.EVENT_BUS.register( LootGroupHandler );
	}

	/**
	 * Do some stuff once the server starts
	 * 
	 * @param pEvent
	 */
	@EventHandler
	public void serverLoad( FMLServerStartingEvent pEvent )
	{
		pEvent.registerServerCommand( new LootBagCommand() );
	}
}

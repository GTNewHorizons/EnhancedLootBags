package eu.usrv.enhancedlootbags.client;

import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import eu.usrv.enhancedlootbags.client.gui.GuiLootBagStats;

public class LootBagStatsClientHandler {

    public static final KeyBinding KEY_OPEN_STATS = new KeyBinding(
            "enhancedlootbags.key.open_stats",
            Keyboard.KEY_NONE,
            "enhancedlootbags.key.category");

    private static int openRequestTicks = 0;

    public static void requestOpen() {
        openRequestTicks = 20;
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (KEY_OPEN_STATS.isPressed() && Minecraft.getMinecraft().currentScreen == null) {
            requestOpen();
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || openRequestTicks <= 0) {
            return;
        }

        openRequestTicks--;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.currentScreen != null) {
            return;
        }

        openRequestTicks = 0;
        mc.displayGuiScreen(new GuiLootBagStats());
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        ClientLootBagStats.clear();
    }

    public static class StatsCommand extends CommandBase {

        @Override
        public String getCommandName() {
            return "lootbagstats";
        }

        @Override
        public List getCommandAliases() {
            return Arrays.asList("lbstats");
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/lootbagstats";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }

        @Override
        public boolean canCommandSenderUseCommand(ICommandSender sender) {
            return true;
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            requestOpen();
        }
    }
}

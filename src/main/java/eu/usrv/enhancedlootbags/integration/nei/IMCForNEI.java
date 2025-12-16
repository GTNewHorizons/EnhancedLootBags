package eu.usrv.enhancedlootbags.integration.nei;

import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.common.event.FMLInterModComms;
import eu.usrv.enhancedlootbags.EnhancedLootBags;

public class IMCForNEI {

    public static void IMCSender() {
        sendHandler(EnhancedLootBags.MODID, "enhancedlootbags:lootbag", 166, 238);
    }

    @SuppressWarnings("SameParameterValue")
    private static void sendHandler(String handlerName, String stack, int width, int height) {
        NBTTagCompound NBT = new NBTTagCompound();
        NBT.setString("handler", handlerName);
        NBT.setString("modName", EnhancedLootBags.MODNAME);
        NBT.setString("modId", EnhancedLootBags.MODID);
        NBT.setBoolean("modRequired", true);
        NBT.setString("itemName", stack);
        NBT.setInteger("yShift", 2);
        NBT.setInteger("handlerWidth", width);
        NBT.setInteger("handlerHeight", height);
        NBT.setInteger("maxRecipesPerPage", 1);
        FMLInterModComms.sendMessage("NotEnoughItems", "registerHandlerInfo", NBT);
    }
}
